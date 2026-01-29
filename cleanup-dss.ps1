param(
  [string]$ProjectId = "intranet-483419",
  [string]$Region = "europe-west1",
  [string]$Service = "dss-validation-service",
  [string]$Image = "gcr.io/intranet-483419/dss-validation-service",
  [int]$KeepUntagged = 0,
  [switch]$KeepOnlyLatest,
  [int]$KeepRevisions = 0,
  [switch]$ImagesOnly,
  [switch]$RevisionsOnly
)

$ErrorActionPreference = "Stop"

function Cleanup-Images {
  $tagsJson = gcloud container images list-tags $Image --format=json
  $tags = $tagsJson | ConvertFrom-Json

  if (-not $tags) {
    Write-Host "No image tags found for $Image."
    return
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
    Write-Host "No image digests to delete."
    return
  }

  Write-Host "Image digests to delete:"
  $toDelete | ForEach-Object { Write-Host " - $($_.digest) ($($_.timestamp))" }

  foreach ($entry in $toDelete) {
    $digest = $entry.digest
    if (-not $digest) {
      continue
    }
    Write-Host "Deleting $digest..."
    gcloud container images delete "$Image@$digest" --force-delete-tags --quiet
  }
}

function Cleanup-Revisions {
  $revisionsJson = gcloud run revisions list `
    --service $Service `
    --region $Region `
    --project $ProjectId `
    --format=json

  $revisions = $revisionsJson | ConvertFrom-Json

  if (-not $revisions) {
    Write-Host "No revisions found for $Service."
    return
  }

  $activeRevision = gcloud run services describe $Service `
    --region $Region `
    --project $ProjectId `
    --format="value(status.latestReadyRevisionName)"

  $inactive = $revisions |
    Where-Object { $_.metadata.name -ne $activeRevision -and (-not $_.traffic) } |
    Sort-Object createTime -Descending

  if ($KeepRevisions -gt 0) {
    $inactive = $inactive | Select-Object -Skip $KeepRevisions
  }

  if (-not $inactive) {
    Write-Host "No inactive revisions to delete."
    return
  }

  Write-Host "Inactive revisions to delete:"
  $inactive | ForEach-Object { Write-Host " - $($_.metadata.name)" }

  foreach ($rev in $inactive) {
    $name = $rev.metadata.name
    Write-Host "Deleting $name..."
    gcloud run revisions delete $name `
      --region $Region `
      --project $ProjectId `
      --quiet
  }
}

if ($ImagesOnly -and $RevisionsOnly) {
  throw "Choose only one: -ImagesOnly or -RevisionsOnly."
}

if (-not $ImagesOnly -and -not $RevisionsOnly) {
  Cleanup-Images
  Cleanup-Revisions
} elseif ($ImagesOnly) {
  Cleanup-Images
} else {
  Cleanup-Revisions
}

Write-Host "Done."
