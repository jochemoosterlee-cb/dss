$ErrorActionPreference = "Stop"

$ProjectId = "intranet-483419"
$Region = "europe-west1"
$Service = "dss-validation-service"
$Image = "gcr.io/$ProjectId/dss-validation-service:latest"
$Cpu = "8"
$Memory = "8Gi"
$Timeout = "300"
$MinInstances = "0"
$NoCpuThrottling = $true
$CacheDir = $env:DSS_TL_CACHE_DIR
$CacheBucket = $env:DSS_TL_CACHE_BUCKET

Write-Host "Deploying $Service from $Image"

if ([string]::IsNullOrWhiteSpace($CacheDir)) {
  $CacheDir = "/mnt/dss-tsl-cache"
}

if ([string]::IsNullOrWhiteSpace($CacheBucket)) {
  $CacheBucket = "intranet-483419-dss-tl-cache"
}

$ignoreUrls = $env:DSS_TL_IGNORE_URLS
$tlTimeoutMs = $env:DSS_TL_HTTP_TIMEOUT_MS
$tlRetries = $env:DSS_TL_HTTP_RETRIES
$tlRetryBackoffMs = $env:DSS_TL_HTTP_RETRY_BACKOFF_MS

if ([string]::IsNullOrWhiteSpace($ignoreUrls)) {
  $ignoreUrls = ""
}
if ([string]::IsNullOrWhiteSpace($tlTimeoutMs)) {
  $tlTimeoutMs = "20000"
}
if ([string]::IsNullOrWhiteSpace($tlRetries)) {
  $tlRetries = "2"
}
if ([string]::IsNullOrWhiteSpace($tlRetryBackoffMs)) {
  $tlRetryBackoffMs = "500"
}

$args = @(
  "run", "deploy", $Service,
  "--image", $Image,
  "--region", $Region,
  "--cpu", $Cpu,
  "--memory", $Memory,
  "--timeout", $Timeout,
  "--min-instances", $MinInstances,
  "--set-env-vars", "DSS_TRUST_LIST_REFRESH_ON_START=false,DSS_TRUST_LIST_REFRESH_INTERVAL_MINUTES=0,DSS_TL_CACHE_DIR=$CacheDir,DSS_TL_IGNORE_URLS=$ignoreUrls,DSS_TL_HTTP_TIMEOUT_MS=$tlTimeoutMs,DSS_TL_HTTP_RETRIES=$tlRetries,DSS_TL_HTTP_RETRY_BACKOFF_MS=$tlRetryBackoffMs",
  "--no-allow-unauthenticated"
)

if ($NoCpuThrottling) {
  $args += "--no-cpu-throttling"
}

if (-not [string]::IsNullOrWhiteSpace($CacheBucket)) {
  $args += @(
    "--add-volume", "name=tlcache,type=cloud-storage,bucket=$CacheBucket",
    "--add-volume-mount", "volume=tlcache,mount-path=$CacheDir"
  )
}

gcloud @args

if ($LASTEXITCODE -ne 0) {
  throw "Deploy failed with exit code $LASTEXITCODE"
}

Write-Host "Done."
