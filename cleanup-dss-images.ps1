param(
  [string]$ProjectId = "intranet-483419",
  [string]$Image = "gcr.io/intranet-483419/dss-validation-service",
  [int]$KeepUntagged = 0,
  [switch]$KeepOnlyLatest,
  [switch]$Yes
)

$ErrorActionPreference = "Stop"

$tagsJson = gcloud container images list-tags $Image --format=json
$tags = $tagsJson | ConvertFrom-Json

if (-not $tags) {
  Write-Host "No image tags found for $Image."
  exit 0
}

$tagged = $tags | Where-Object { $_.tags -and $_.tags.Count -gt 0 }
$untagged = $tags | Where-Object { -not $_.tags -or $_.tags.Count -eq 0 } | Sort-Object timestamp -Descending

$toDelete = @()

if ($KeepOnlyLatest) {
  $toDelete += $tagged | Where-Object { $_.tags -notcontains "latest" }
  $toDelete += $untagged
} else {
  if ($KeepUntagged -gt 0) {
    $untagged = $untagged | Select-Object -Skip $KeepUntagged
  }
  $toDelete += $untagged
}

if (-not $toDelete -or $toDelete.Count -eq 0) {
  Write-Host "No digests to delete."
  exit 0
}

Write-Host "Digests to delete:"
$toDelete | ForEach-Object { Write-Host " - $($_.digest) ($($_.timestamp))" }

if (-not $Yes) {
  $confirm = Read-Host "Delete these digests? (y/N)"
  if ($confirm -notin @("y", "Y")) {
    Write-Host "Aborted."
    exit 0
  }
}

foreach ($entry in $toDelete) {
  $digest = $entry.digest
  if (-not $digest) {
    continue
  }
  Write-Host "Deleting $digest..."
  gcloud container images delete "$Image@$digest" --force-delete-tags --quiet
}

Write-Host "Done."
