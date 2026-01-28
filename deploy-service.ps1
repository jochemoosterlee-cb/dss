$ErrorActionPreference = "Stop"

$ProjectId = "intranet-483419"
$Region = "europe-west1"
$Service = "dss-validation-service"
$Image = "gcr.io/$ProjectId/dss-validation-service:latest"
$Cpu = "1"
$Memory = "4Gi"
$Timeout = "300"

Write-Host "Deploying $Service from $Image"

gcloud run deploy $Service `
  --image $Image `
  --region $Region `
  --cpu $Cpu `
  --memory $Memory `
  --timeout $Timeout `
  --set-env-vars DSS_TRUST_LIST_REFRESH_ON_START=false,DSS_TRUST_LIST_REFRESH_INTERVAL_MINUTES=0 `
  --no-allow-unauthenticated

if ($LASTEXITCODE -ne 0) {
  throw "Deploy failed with exit code $LASTEXITCODE"
}

Write-Host "Done."
