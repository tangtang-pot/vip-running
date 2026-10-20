$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (!(Test-Path -LiteralPath '.env')) { throw 'Missing local .env file.' }
foreach ($forumEnvLine in [IO.File]::ReadAllLines((Join-Path $PSScriptRoot '.env'))) {
    if ($forumEnvLine -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process') }
}
$env:SPRING_PROFILES_ACTIVE='mysql'
$forumDbAppPath=Join-Path $PSScriptRoot '.local/db-app.clixml'
if (!(Test-Path -LiteralPath $forumDbAppPath)) { throw 'Configure the local MySQL application account first.' }
$forumDbApp=Import-Clixml -LiteralPath $forumDbAppPath
$env:DB_URL='jdbc:mysql://127.0.0.1:'+$forumDbApp.Port+'/campus_forum_integration?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
$env:DB_USERNAME=$forumDbApp.Credential.UserName
$env:DB_PASSWORD=$forumDbApp.Credential.GetNetworkCredential().Password
$env:INFRA_ENABLED='true'
$env:DEV_MAILBOX='false'
$env:NACOS_ENABLED='false'
$env:REDIS_HOST='127.0.0.1'
$env:SPRING_DATA_REDIS_PORT='16379'
$env:SPRING_RABBITMQ_HOST='127.0.0.1'
$env:SPRING_RABBITMQ_PORT='15672'
$env:SPRING_RABBITMQ_USERNAME='forum'
$env:SPRING_RABBITMQ_PASSWORD=$env:RABBITMQ_PASSWORD
$env:SMTP_HOST='127.0.0.1'
$env:SMTP_PORT='11025'
$env:PORT='8083'
$env:AI_ENABLED='false'
$forumAiPath=Join-Path $PSScriptRoot '.local/ai-connection.clixml'
if(Test-Path -LiteralPath $forumAiPath) {
    $forumAi=Import-Clixml -LiteralPath $forumAiPath
    if($forumAi.Enabled) {
        $env:AI_ENABLED='true'
        $env:AI_ENDPOINT=$forumAi.Endpoint
        $env:AI_MODEL=$forumAi.Model
        $env:AI_API_KEY=$forumAi.Credential.GetNetworkCredential().Password
    }
}
Write-Host 'Real middleware stage: http://127.0.0.1:8083 (existing 8081 demo is separate).'
try {
    & java '-Xms128m' '-Xmx384m' -jar 'forum-service/target/forum-service-1.0.0.jar'
    if ($LASTEXITCODE -ne 0) { throw "Forum exited with code $LASTEXITCODE" }
} finally {
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:AI_API_KEY -ErrorAction SilentlyContinue
}
