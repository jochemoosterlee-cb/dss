param(
  [string]$ProjectId = "intranet-483419",
  [string]$Region = "europe-west1",
  [string]$Service = "dss-validation-service",
  [int]$Keep = 0,
  [switch]$Yes
)

$ErrorActionPreference = "Stop"

$revisionsJson = gcloud run revisions list `
  --service $Service `
  --region $Region `
  --project $ProjectId `
  --format=json

$revisions = $revisionsJson | ConvertFrom-Json

if (-not $revisions) {
  Write-Host "No revisions found for $Service."
  exit 0
}

$activeRevision = gcloud run services describe $Service `
  --region $Region `
  --project $ProjectId `
  --format="value(status.latestReadyRevisionName)"

$inactive = $revisions |
  Where-Object { $_.metadata.name -ne $activeRevision -and (-not $_.traffic) } |
  Sort-Object createTime -Descending

if ($Keep -gt 0) {
  $inactive = $inactive | Select-Object -Skip $Keep
}

if (-not $inactive) {
  Write-Host "No inactive revisions to delete."
  exit 0
}

Write-Host "Inactive revisions to delete:"
$inactive | ForEach-Object { Write-Host " - $($_.metadata.name)" }

if (-not $Yes) {
  $confirm = Read-Host "Delete these revisions? (y/N)"
  if ($confirm -notin @("y", "Y")) {
    Write-Host "Aborted."
    exit 0
  }
}

foreach ($rev in $inactive) {
  $name = $rev.metadata.name
  Write-Host "Deleting $name..."
  gcloud run revisions delete $name `
    --region $Region `
    --project $ProjectId `
    --quiet
}

Write-Host "Done."
