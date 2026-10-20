param([Parameter(Mandatory=$true)][ValidateSet('auth','content','interaction','operations','log','gateway')][string]$Role)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
foreach ($cloudEnvLine in [IO.File]::ReadAllLines((Join-Path $PSScriptRoot '.env'))) {
    if ($cloudEnvLine -match '^([A-Z_]+)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process') }
}
$env:NACOS_ADDRESS='127.0.0.1:18848'
$env:SPRING_CLOUD_NACOS_DISCOVERY_IP='127.0.0.1'
$env:SERVER_ADDRESS='127.0.0.1'
$env:NACOS_ENABLED='true'
$env:SENTINEL_ENABLED='false'
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE='3'
if($Role -eq 'gateway') {
    $env:PORT='8082'
    $cloudJar='.local/cloud-build/gateway/target/gateway-1.0.0.jar'
} else {
    $cloudDb=Import-Clixml -LiteralPath (Join-Path $PSScriptRoot '.local/db-app.clixml')
    $env:SPRING_PROFILES_ACTIVE='mysql'
    $env:DB_URL='jdbc:mysql://127.0.0.1:'+$cloudDb.Port+'/campus_forum_integration?characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
    $env:DB_USERNAME=$cloudDb.Credential.UserName
    $env:DB_PASSWORD=$cloudDb.Credential.GetNetworkCredential().Password
    $env:SERVICE_MODE=$Role
    $env:SERVICE_NAME=$Role+'-service'
    $env:PORT=@{auth='8091';content='8092';interaction='8093';operations='8094';log='8095'}[$Role]
    $env:INFRA_ENABLED='true'
    $env:DEV_MAILBOX='false'
    $env:REDIS_HOST='127.0.0.1'
    $env:SPRING_DATA_REDIS_PORT='16379'
    $env:SPRING_RABBITMQ_HOST='127.0.0.1'
    $env:SPRING_RABBITMQ_PORT='15672'
    $env:SPRING_RABBITMQ_USERNAME='forum'
    $env:SPRING_RABBITMQ_PASSWORD=$env:RABBITMQ_PASSWORD
    $env:SMTP_HOST='127.0.0.1'
    $env:SMTP_PORT='11025'
    $env:AI_ENABLED='false'
    $cloudJar='.local/cloud-build/forum-service/target/forum-service-1.0.0.jar'
}
try {
    & java '-Xms64m' '-Xmx192m' '-XX:MaxMetaspaceSize=160m' '-XX:ReservedCodeCacheSize=48m' '-XX:ActiveProcessorCount=2' '-Xss512k' -jar $cloudJar
    if($LASTEXITCODE -ne 0){throw "Cloud role $Role exited: $LASTEXITCODE"}
} finally {Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue}
