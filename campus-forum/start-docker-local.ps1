$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
Set-Location -LiteralPath $PSScriptRoot
$forumLinuxRoot = (& wsl -d CampusDocker -- wslpath -u $PSScriptRoot).Trim()
if ($LASTEXITCODE -ne 0) { throw 'CampusDocker WSL environment is not ready.' }
& wsl -d CampusDocker -- bash "$forumLinuxRoot/start-docker-engine.sh"
if ($LASTEXITCODE -ne 0) { throw 'Docker Engine could not start.' }
& wsl -d CampusDocker -- docker compose -p campus-local --env-file "$forumLinuxRoot/.env" -f "$forumLinuxRoot/compose.local.yml" up -d --wait --wait-timeout 180 redis rabbitmq mailpit
if ($LASTEXITCODE -ne 0) { throw 'Middleware did not become healthy. Check docker compose logs.' }
& (Join-Path $PSScriptRoot 'start-middleware-local.ps1')
