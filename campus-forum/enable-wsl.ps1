$ErrorActionPreference = 'Stop'
$dockerSetupLog = Join-Path $PSScriptRoot 'wsl-setup-status.json'
try {
    $dockerFeatureResults = foreach ($dockerFeatureName in @('Microsoft-Windows-Subsystem-Linux','VirtualMachinePlatform')) {
        $dockerFeatureState = Get-WindowsOptionalFeature -Online -FeatureName $dockerFeatureName
        if ($dockerFeatureState.State -eq 'Enabled') {
            [pscustomobject]@{ Feature=$dockerFeatureName; State='Enabled'; RestartNeeded=$false }
        } else {
            $dockerFeatureResult = Enable-WindowsOptionalFeature -Online -FeatureName $dockerFeatureName -All -NoRestart
            [pscustomobject]@{ Feature=$dockerFeatureName; State=$dockerFeatureResult.Online; RestartNeeded=$dockerFeatureResult.RestartNeeded }
        }
    }
    @{ Success=$true; Features=@($dockerFeatureResults); CheckedAt=(Get-Date).ToString('o') } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $dockerSetupLog -Encoding UTF8
} catch {
    @{ Success=$false; Error=$_.Exception.Message } | ConvertTo-Json | Set-Content -LiteralPath $dockerSetupLog -Encoding UTF8
    exit 1
}
