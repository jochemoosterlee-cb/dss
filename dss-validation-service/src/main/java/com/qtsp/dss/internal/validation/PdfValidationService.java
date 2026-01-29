package com.qtsp.dss.internal.validation;

import com.qtsp.dss.internal.app.ServiceConfig;
import com.qtsp.dss.internal.model.SignatureSummary;
import com.qtsp.dss.internal.model.TrustListStatusResponse;
import com.qtsp.dss.internal.model.ValidationSummaryResponse;
import eu.europa.esig.dss.diagnostic.CertificateRevocationWrapper;
import eu.europa.esig.dss.diagnostic.CertificateWrapper;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.enumerations.Indication;
import eu.europa.esig.dss.enumerations.MimeTypeEnum;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.pades.validation.PDFDocumentValidator;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.simplereport.SimpleReport;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.spi.validation.CertificateVerifier;
import eu.europa.esig.dss.spi.validation.CertificateVerifierBuilder;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.tsl.source.TLSource;
import eu.europa.esig.dss.validation.policy.ValidationPolicyLoader;
import eu.europa.esig.dss.validation.reports.Reports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import eu.europa.esig.dss.alert.LogOnStatusAlert;

import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PdfValidationService {
	private static final Logger LOG = LoggerFactory.getLogger(PdfValidationService.class);
	private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

	private final ServiceConfig config;
	private final CommonCertificateVerifier baseVerifier;
	private final TrustedListsCertificateSource trustedListsCertificateSource;
	private volatile boolean trustListRefreshFailed;
	private volatile boolean trustListRefreshInProgress;
	private volatile Instant lastTrustListRefreshAttemptAt;
	private volatile Instant lastTrustListRefreshSuccessAt;
	private volatile String lastTrustListRefreshError;
	private volatile Instant trustListRefreshStartedAt;
	private volatile Instant trustListRefreshProgressAt;
	private volatile String trustListRefreshPhase;
	private volatile Integer lastProcessedLotlCount;
	private volatile Integer lastProcessedTlCount;
	private volatile Integer totalLotlCountEstimate;
	private volatile Integer totalTlCountEstimate;
	private final ScheduledExecutorService refreshProgressExecutor = Executors.newSingleThreadScheduledExecutor();
	private volatile ScheduledFuture<?> refreshProgressTask;

	public PdfValidationService(ServiceConfig config) {
		this.config = config;
		this.baseVerifier = buildBaseVerifier();
		this.trustedListsCertificateSource = new TrustedListsCertificateSource();
		if (config.isTrustListRefreshOnStart()) {
			refreshTrustLists();
		}
	}

	public ValidationSummaryResponse validate(byte[] pdfBytes, String filename, ValidationRequestOptions options) {
		Reports reports = runValidation(pdfBytes, filename);
		return mapSummary(reports, options);
	}

	public Reports validateReports(byte[] pdfBytes, String filename, ValidationRequestOptions options) {
		return runValidation(pdfBytes, filename);
	}

	public ValidationSummaryResponse summarizeReports(Reports reports, ValidationRequestOptions options) {
		return mapSummary(reports, options);
	}

	private Reports runValidation(byte[] pdfBytes, String filename) {
		if (pdfBytes == null || pdfBytes.length == 0) {
			throw new ValidationException(400, "Missing PDF file");
		}
		if (!isPdf(pdfBytes)) {
			throw new ValidationException(400, "Invalid PDF document");
		}

		InMemoryDocument document = new InMemoryDocument(pdfBytes, filename, MimeTypeEnum.PDF);
		PDFDocumentValidator validator = new PDFDocumentValidator(document);
		CertificateVerifier verifier = new CertificateVerifierBuilder(baseVerifier).buildCompleteCopyForValidation();
		validator.setCertificateVerifier(verifier);

		Reports reports;
		try {
			reports = validator.validateDocument(ValidationPolicyLoader.fromDefaultValidationPolicy().create());
		} catch (Exception e) {
			throw new ValidationException(500, "Validation failed: " + e.getMessage());
		}

		return reports;
	}

	private ValidationSummaryResponse mapSummary(Reports reports, ValidationRequestOptions options) {
		SimpleReport simpleReport = reports.getSimpleReport();
		DiagnosticData diagnosticData = new DiagnosticData(reports.getDiagnosticDataJaxb());

		Map<String, SignatureWrapper> signatureMap = new HashMap<>();
		for (SignatureWrapper signature : diagnosticData.getSignatures()) {
			signatureMap.put(signature.getId(), signature);
		}

		List<String> signatureIds = simpleReport.getSignatureIdList();
		List<SignatureSummary> signatures = new ArrayList<>();
		boolean anyInvalid = false;
		boolean anyIndeterminate = false;
		boolean trustUnavailable = isTrustListUnavailable();
		boolean revocationUnavailable = false;

		for (String signatureId : signatureIds) {
			SignatureSummary summary = new SignatureSummary();
			summary.setId(signatureId);

			Indication indication = simpleReport.getIndication(signatureId);
			String status = mapStatus(indication, signatureMap.get(signatureId), options);
			summary.setStatus(status);
			if ("INVALID".equals(status)) {
				anyInvalid = true;
			} else if ("INDETERMINATE".equals(status)) {
				anyIndeterminate = true;
			}

			Date signingTime = simpleReport.getSigningTime(signatureId);
			if (signingTime == null) {
				signingTime = simpleReport.getBestSignatureTime(signatureId);
			}
			if (signingTime != null) {
				summary.setSigningTime(ISO_INSTANT.format(Instant.ofEpochMilli(signingTime.getTime())));
			}
			summary.setSigner(simpleReport.getSignedBy(signatureId));

			String revocation = mapRevocationStatus(diagnosticData, signatureId, options);
			if ("INDETERMINATE".equals(revocation) && options.isRequireRevocation()) {
				revocationUnavailable = true;
			}
			summary.setRevocation(revocation);

			String trust = mapTrustStatus(signatureMap.get(signatureId), trustUnavailable);
			summary.setTrust(trust);

			List<String> substatus = new ArrayList<>();
			SignatureLevel signatureLevel = simpleReport.getSignatureFormat(signatureId);
			if (signatureLevel != null) {
				substatus.add(signatureLevel.name());
			}
			SignatureWrapper wrapper = signatureMap.get(signatureId);
			if (wrapper != null && (wrapper.isThereTLevel() || wrapper.isThereXLevel() || wrapper.isThereALevel())) {
				substatus.add("LTV_OK");
			}
			summary.setSubstatus(substatus);

			signatures.add(summary);
		}

		ValidationSummaryResponse response = new ValidationSummaryResponse();
		response.setSignatureCount(simpleReport.getSignaturesCount());
		response.setSignatures(signatures);

		List<String> errors = new ArrayList<>();
		if (signatureIds.isEmpty()) {
			errors.add("No signature found");
			anyIndeterminate = true;
		}
		if (trustUnavailable) {
			errors.add("Trust list checks unavailable");
			anyIndeterminate = true;
		}
		if (revocationUnavailable) {
			errors.add("Revocation checks unavailable");
			anyIndeterminate = true;
		}
		if (trustListRefreshFailed) {
			errors.add("Trust list refresh failed");
			anyIndeterminate = true;
		}

		response.setErrors(errors);

		if (anyInvalid) {
			response.setOverall("INVALID");
		} else if (anyIndeterminate) {
			response.setOverall("INDETERMINATE");
		} else {
			response.setOverall("VALID");
		}

		return response;
	}

	private String mapStatus(Indication indication, SignatureWrapper signature, ValidationRequestOptions options) {
		if (indication == Indication.TOTAL_PASSED || indication == Indication.PASSED) {
			return "VALID";
		}
		if (indication == Indication.TOTAL_FAILED || indication == Indication.FAILED) {
			return "INVALID";
		}
		if (!options.isRequireRevocation() && signature != null && signature.isSignatureValid()) {
			return "VALID";
		}
		return "INDETERMINATE";
	}

	private String mapRevocationStatus(DiagnosticData diagnosticData, String signatureId, ValidationRequestOptions options) {
		if (!options.isRequireRevocation()) {
			return "OK";
		}
		String signingCertificateId = diagnosticData.getSigningCertificateId(signatureId);
		if (signingCertificateId == null) {
			return "INDETERMINATE";
		}
		CertificateWrapper certificate = diagnosticData.getCertificateById(signingCertificateId);
		if (certificate == null) {
			return "INDETERMINATE";
		}
		if (certificate.isTrusted() || certificate.isNoRevAvail() || certificate.isIdPkixOcspNoCheck()
				|| certificate.isValAssuredShortTermCertificate()) {
			return "OK";
		}
		CertificateRevocationWrapper latestRevocation = diagnosticData.getLatestRevocationDataForCertificate(certificate);
		if (latestRevocation == null || latestRevocation.getStatus() == null) {
			return "INDETERMINATE";
		}
		if (latestRevocation.getStatus().isGood()) {
			return "OK";
		}
		if (latestRevocation.getStatus().isRevoked()) {
			return "FAIL";
		}
		return "INDETERMINATE";
	}

	private String mapTrustStatus(SignatureWrapper signature, boolean trustUnavailable) {
		if (trustUnavailable) {
			return "INDETERMINATE";
		}
		if (signature != null && signature.isTrustedChain()) {
			return "OK";
		}
		return "FAIL";
	}

	private boolean isTrustListUnavailable() {
		if (trustListRefreshFailed) {
			return true;
		}
		return trustedListsCertificateSource.getNumberOfTrustedEntityKeys() == 0;
	}

	private boolean isPdf(byte[] bytes) {
		if (bytes.length < 5) {
			return false;
		}
		return bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-';
	}

	private CommonCertificateVerifier buildBaseVerifier() {
		CommonCertificateVerifier verifier = new CommonCertificateVerifier();
		CommonsDataLoader dataLoader = new CommonsDataLoader();

		OnlineCRLSource crlSource = new OnlineCRLSource();
		crlSource.setDataLoader(dataLoader);
		OnlineOCSPSource ocspSource = new OnlineOCSPSource();
		ocspSource.setDataLoader(dataLoader);

		verifier.setCrlSource(crlSource);
		verifier.setOcspSource(ocspSource);
		verifier.setAlertOnInvalidSignature(new LogOnStatusAlert(Level.WARN));
		verifier.setAlertOnInvalidTimestamp(new LogOnStatusAlert(Level.WARN));
		verifier.setAlertOnMissingRevocationData(new LogOnStatusAlert(Level.WARN));
		verifier.setAlertOnRevokedCertificate(new LogOnStatusAlert(Level.WARN));
		verifier.setAlertOnExpiredCertificate(new LogOnStatusAlert(Level.WARN));
		return verifier;
	}

	public synchronized void refreshTrustLists() {
		trustListRefreshInProgress = true;
		lastTrustListRefreshAttemptAt = Instant.now();
		trustListRefreshStartedAt = lastTrustListRefreshAttemptAt;
		trustListRefreshProgressAt = lastTrustListRefreshAttemptAt;
		trustListRefreshPhase = "starting";
		lastTrustListRefreshError = null;
		setTotalCountsFromSummary(trustedListsCertificateSource.getSummary());
		try {
			CommonsDataLoader dataLoader = new CommonsDataLoader();
			FileCacheDataLoader onlineLoader = new FileCacheDataLoader(dataLoader);
			FileCacheDataLoader offlineLoader = new FileCacheDataLoader(dataLoader);
			offlineLoader.setCacheExpirationTime(-1);
			File cacheDir = resolveTrustListCacheDir();
			if (cacheDir != null) {
				onlineLoader.setFileCacheDirectory(cacheDir);
				offlineLoader.setFileCacheDirectory(cacheDir);
			}
			LOTLSource lotlSource = new LOTLSource();
			lotlSource.setUrl(config.getLotlUrl());
			CertificateSource lotlCertificateSource = loadLotlCertificateSource();
			if (lotlCertificateSource != null) {
				lotlSource.setCertificateSource(lotlCertificateSource);
			}
			if (config.getOjUrl() != null && !config.getOjUrl().trim().isEmpty()) {
				lotlSource.setSigningCertificatesAnnouncementPredicate(new OfficialJournalSchemeInformationURI(config.getOjUrl()));
			}
			lotlSource.setPivotSupport(true);
			lotlSource.setTLVersions(Arrays.asList(5, 6));

			TLSource nlSource = new TLSource();
			nlSource.setUrl(config.getNlTlUrl());

			TLValidationJob job = new TLValidationJob();
			job.setListOfTrustedListSources(lotlSource);
			job.setTrustedListSources(nlSource);
			job.setTrustedListCertificateSource(trustedListsCertificateSource);
			job.setOnlineDataLoader(onlineLoader);
			job.setOfflineDataLoader(offlineLoader);

			trustListRefreshPhase = "downloading";
			trustListRefreshProgressAt = Instant.now();
			startProgressTracking(cacheDir);
			job.onlineRefresh();
			trustListRefreshPhase = "summarizing";
			trustListRefreshProgressAt = Instant.now();
			TLValidationJobSummary summary = job.getSummary();
			lastProcessedLotlCount = summary != null ? summary.getNumberOfProcessedLOTLs() : null;
			lastProcessedTlCount = summary != null ? summary.getNumberOfProcessedTLs() : null;
			setTotalCountsFromSummary(summary);
			trustedListsCertificateSource.setSummary(job.getSummary());
			baseVerifier.setTrustedCertSources(trustedListsCertificateSource);
			trustListRefreshFailed = false;
			lastTrustListRefreshSuccessAt = Instant.now();
			trustListRefreshPhase = "done";
			trustListRefreshProgressAt = lastTrustListRefreshSuccessAt;
			LOG.info("Trust lists refreshed. Trusted entities: {}", trustedListsCertificateSource.getNumberOfTrustedEntityKeys());
		} catch (Exception e) {
			trustListRefreshFailed = true;
			lastTrustListRefreshError = e.getMessage();
			trustListRefreshPhase = "failed";
			trustListRefreshProgressAt = Instant.now();
			LOG.warn("Trust list refresh failed: {}", e.getMessage());
		} finally {
			trustListRefreshInProgress = false;
			stopProgressTracking();
		}
	}

	public TrustListStatusResponse getTrustListStatus() {
		TrustListStatusResponse status = new TrustListStatusResponse();
		status.setLotlUrl(config.getLotlUrl());
		status.setNlTlUrl(config.getNlTlUrl());
		status.setTrustedEntities(trustedListsCertificateSource.getNumberOfTrustedEntityKeys());
		status.setRefreshInProgress(trustListRefreshInProgress);
		status.setRefreshFailed(trustListRefreshFailed);
		status.setLastRefreshAttemptAt(formatInstant(lastTrustListRefreshAttemptAt));
		status.setLastRefreshSuccessAt(formatInstant(lastTrustListRefreshSuccessAt));
		status.setLastRefreshError(lastTrustListRefreshError);
		status.setRefreshStartedAt(formatInstant(trustListRefreshStartedAt));
		status.setLastProgressAt(formatInstant(trustListRefreshProgressAt));
		status.setRefreshPhase(trustListRefreshPhase);
		status.setProcessedLotlCount(lastProcessedLotlCount);
		status.setProcessedTlCount(lastProcessedTlCount);
		status.setTotalLotlCountEstimate(totalLotlCountEstimate);
		status.setTotalTlCountEstimate(totalTlCountEstimate);
		return status;
	}

	private String formatInstant(Instant instant) {
		if (instant == null) {
			return null;
		}
		return ISO_INSTANT.format(instant);
	}

	private CertificateSource loadLotlCertificateSource() {
		String resource = config.getLotlKeystoreResource();
		if (resource == null || resource.trim().isEmpty()) {
			LOG.warn("LOTL keystore resource not configured.");
			return null;
		}
		try (InputStream stream = PdfValidationService.class.getClassLoader().getResourceAsStream(resource)) {
			if (stream == null) {
				LOG.warn("LOTL keystore resource '{}' not found on classpath.", resource);
				return null;
			}
			char[] password = config.getLotlKeystorePassword().toCharArray();
			return new KeyStoreCertificateSource(stream, "PKCS12", password);
		} catch (Exception e) {
			LOG.warn("Failed to load LOTL keystore: {}", e.getMessage());
			return null;
		}
	}

	private File resolveTrustListCacheDir() {
		String dir = config.getTrustListCacheDir();
		if (dir == null || dir.trim().isEmpty()) {
			return null;
		}
		File cacheDir = new File(dir);
		if (cacheDir.exists()) {
			return cacheDir;
		}
		if (cacheDir.mkdirs()) {
			LOG.info("TL cache directory: {}", cacheDir.getAbsolutePath());
			return cacheDir;
		}
		LOG.warn("Unable to create TL cache directory: {}", cacheDir.getAbsolutePath());
		return null;
	}

	private void setTotalCountsFromSummary(TLValidationJobSummary summary) {
		if (summary == null) {
			return;
		}
		totalLotlCountEstimate = summary.getNumberOfProcessedLOTLs();
		totalTlCountEstimate = summary.getNumberOfProcessedTLs();
	}

	private void startProgressTracking(File cacheDir) {
		if (cacheDir == null || refreshProgressTask != null) {
			return;
		}
		refreshProgressTask = refreshProgressExecutor.scheduleAtFixedRate(() -> {
			try {
				ProgressCounts counts = countCachedTrustLists(cacheDir);
				if (counts != null) {
					lastProcessedLotlCount = counts.lotlCount;
					lastProcessedTlCount = counts.tlCount;
					trustListRefreshProgressAt = Instant.now();
				}
			} catch (Exception e) {
				LOG.debug("Progress tracking failed: {}", e.getMessage());
			}
		}, 1, 2, TimeUnit.SECONDS);
	}

	private void stopProgressTracking() {
		ScheduledFuture<?> task = refreshProgressTask;
		if (task != null) {
			task.cancel(false);
			refreshProgressTask = null;
		}
	}

	private ProgressCounts countCachedTrustLists(File cacheDir) {
		if (!cacheDir.exists()) {
			return null;
		}
		File[] files = cacheDir.listFiles();
		if (files == null) {
			return null;
		}
		int lotl = 0;
		int tl = 0;
		for (File file : files) {
			if (!file.isFile()) continue;
			String name = file.getName().toLowerCase();
			if (!name.endsWith("xml")) continue;
			if (name.contains("lotl")) {
				lotl += 1;
			} else if (name.contains("tsl") || name.contains("tl_") || name.contains("_tl_")) {
				tl += 1;
			}
		}
		return new ProgressCounts(lotl, tl);
	}

	private static class ProgressCounts {
		final int lotlCount;
		final int tlCount;
		ProgressCounts(int lotlCount, int tlCount) {
			this.lotlCount = lotlCount;
			this.tlCount = tlCount;
		}
	}
}
