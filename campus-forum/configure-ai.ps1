$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
Write-Host 'Online AI setup (DeepSeek default; no Docker required).'
Write-Host 'Matched knowledge excerpts and questions will be sent to your provider.'
$aiEndpoint = Read-Host 'Endpoint [https://api.deepseek.com/chat/completions]'
if ([string]::IsNullOrWhiteSpace($aiEndpoint)) { $aiEndpoint = 'https://api.deepseek.com/chat/completions' }
$aiUri = [uri]$aiEndpoint
if ($aiUri.Scheme -ne 'https') { throw 'Please use an HTTPS endpoint.' }
$aiModel = Read-Host 'Model [deepseek-flash]'
if ([string]::IsNullOrWhiteSpace($aiModel)) { $aiModel = 'deepseek-flash' }
$aiSecret = Read-Host 'API Key (hidden; saved only in local-ai.properties)' -AsSecureString
$aiPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($aiSecret)
try {
    $aiKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($aiPointer)
    if ([string]::IsNullOrWhiteSpace($aiKey)) { throw 'API Key is required.' }
    foreach ($aiValue in @($aiEndpoint, $aiModel, $aiKey)) {
        if ($aiValue -match '[\r\n\\]' -or $aiValue -match '[^\x20-\x7E]') { throw 'Use single-line ASCII configuration values without backslashes.' }
    }
    $aiLines = @('app.ai.enabled=true', "app.ai.endpoint=$aiEndpoint", "app.ai.model=$aiModel", "app.ai.api-key=$aiKey")
    [IO.File]::WriteAllLines((Join-Path $PSScriptRoot 'local-ai.properties'), $aiLines, [Text.UTF8Encoding]::new($false))
    Write-Host 'Saved. Restart the forum with start-local.cmd to enable online AI.'
    Write-Host 'Keep local-ai.properties private. API usage may incur provider charges.'
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($aiPointer)
    $aiKey = $null
    $aiLines = $null
}
