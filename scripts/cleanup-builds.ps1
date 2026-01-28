param(
  [int]$Days = 7,
  [int]$KeepLast = 5,
  [string]$Project = "intranet-483419",
  [string[]]$Statuses = @("FAILURE","CANCELLED","TIMEOUT"),
  [string]$Image = "gcr.io/intranet-483419/dss-validation-service:latest",
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$cutoff = (Get-Date).AddDays(-$Days)

# Keep the most recent builds regardless of status
$recentSet = @{}
if ($KeepLast -ge 1) {
  $recentIds = gcloud builds list --project $Project --limit $KeepLast --format "value(ID)"
  foreach ($id in $recentIds) { if ($id) { $recentSet[$id] = $true } }
}

$format = "value(ID,STATUS,CREATE_TIME)"
$rows = gcloud builds list --project $Project --filter "images:$Image" --format $format

function Remove-Build {
  param(
    [string]$Id,
    [string]$Status
  )

  if ($Status -eq "WORKING" -or $Status -eq "QUEUED") {
    Write-Host "Cancelling build $Id ($Status)"
    gcloud builds cancel $Id --project $Project | Out-Null
  } else {
    Write-Host "Cloud Build does not support deleting finished builds via gcloud; skipping $Id ($Status)"
  }
}

foreach ($row in $rows) {
  if (-not $row) { continue }
  $parts = $row -split "\s+", 3
  if ($parts.Length -lt 3) { continue }
  $id = $parts[0]
  $status = $parts[1]
  $created = Get-Date $parts[2]

  if ($recentSet.ContainsKey($id)) { continue }
  if ($Statuses -notcontains $status) { continue }
  if ($created -gt $cutoff) { continue }

  if ($DryRun) {
    Write-Host "Would remove build $id ($status, $created)"
  } else {
    Remove-Build -Id $id -Status $status
  }
}
