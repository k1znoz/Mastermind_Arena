param([ValidateSet('Backend', 'Frontend')][string]$Target = 'Backend')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$profilePath = Join-Path $projectRoot '.env.local'
if (-not (Test-Path -LiteralPath $profilePath)) { throw 'Missing root .env.local profile.' }
$previousValues = @{}
function Import-LocalProfile([string]$Path) {
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $name = $Matches[1]
            if (-not $previousValues.ContainsKey($name)) {
                $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
            }
            [Environment]::SetEnvironmentVariable($name, $Matches[2], 'Process')
        }
    }
}
Push-Location $projectRoot
try {
    Import-LocalProfile $profilePath
    if ($env:APP_DB_URL -notmatch '^jdbc:postgresql://(127\.0\.0\.1|localhost):') {
        throw 'Local launcher requires a loopback PostgreSQL URL.'
    }
    if ($Target -eq 'Backend') {
        docker compose --project-name mastermind-local --env-file $profilePath -f docker-compose.local.yml up -d --wait postgres
        if ($LASTEXITCODE -ne 0) { throw 'Cannot start local PostgreSQL. Check Docker Desktop.' }
        Write-Host 'Local API: http://localhost:8081 ; PostgreSQL data stays in the Docker volume.'
        mvn -pl deduction-engine compile exec:java '-Dexec.mainClass=io.mastermindarena.deduction.api.submitaction.LocalSubmitActionHttpServerMain'
    } else {
        Import-LocalProfile (Join-Path $projectRoot 'App_Front/.env.development.local')
        Set-Location (Join-Path $projectRoot 'App_Front')
        npm run dev -- --host 127.0.0.1 --port 5173 --strictPort
    }
    if ($LASTEXITCODE -ne 0) { throw "$Target exited with code $LASTEXITCODE" }
} finally {
    foreach ($name in $previousValues.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousValues[$name], 'Process')
    }
    Pop-Location
}
