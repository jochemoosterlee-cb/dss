$ErrorActionPreference = "Stop"
$Image = "gcr.io/intranet-483419/dss-validation-service:latest"

Write-Host "Building and pushing $Image"

gcloud builds submit --tag $Image

if ($LASTEXITCODE -ne 0) {
  throw "Build failed with exit code $LASTEXITCODE"
}

Write-Host "Done."
