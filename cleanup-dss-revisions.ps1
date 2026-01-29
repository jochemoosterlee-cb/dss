param(
  [string]$ProjectId = "intranet-483419",
  [string]$Region = "europe-west1",
  [string]$Service = "dss-validation-service",
  [int]$Keep = 0
)

$ErrorActionPreference = "Stop"

& "$PSScriptRoot\\cleanup-dss.ps1" `
  -ProjectId $ProjectId `
  -Region $Region `
  -Service $Service `
  -KeepRevisions $Keep `
  -RevisionsOnly
