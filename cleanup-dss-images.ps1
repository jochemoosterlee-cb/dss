param(
  [string]$ProjectId = "intranet-483419",
  [string]$Image = "gcr.io/intranet-483419/dss-validation-service",
  [int]$KeepUntagged = 0,
  [switch]$KeepOnlyLatest
)

$ErrorActionPreference = "Stop"

& "$PSScriptRoot\\cleanup-dss.ps1" `
  -ProjectId $ProjectId `
  -Image $Image `
  -KeepUntagged $KeepUntagged `
  -KeepOnlyLatest:$KeepOnlyLatest `
  -ImagesOnly
