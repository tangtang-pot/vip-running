$ErrorActionPreference='Stop'
$env:PSModulePath=Join-Path $PSHOME 'Modules'
Set-Location -LiteralPath $PSScriptRoot
$forumDb=Import-Clixml -LiteralPath (Join-Path $PSScriptRoot '.local/db-connection.clixml')
$forumAppPath=Join-Path $PSScriptRoot '.local/db-app.clixml'
if(Test-Path -LiteralPath $forumAppPath){Write-Host 'Existing encrypted application connection preserved.';exit 0}
$forumMysql=Join-Path $env:ProgramFiles 'MySQL/MySQL Server 8.0/bin/mysql.exe'
if(!(Test-Path -LiteralPath $forumMysql)){throw 'MySQL 8.0 client not found; update the client path in this script.'}
$forumAppName='forum_it_'+[guid]::NewGuid().ToString('N').Substring(0,8)
$forumAppSecret=[guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N')
$env:MYSQL_PWD=$forumDb.Credential.GetNetworkCredential().Password
try {
    $forumSql="CREATE DATABASE IF NOT EXISTS campus_forum_integration CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER '$forumAppName'@'127.0.0.1' IDENTIFIED BY '$forumAppSecret'; GRANT ALL PRIVILEGES ON campus_forum_integration.* TO '$forumAppName'@'127.0.0.1';"
    $forumSql | & $forumMysql --protocol=TCP --host=$($forumDb.Host) --port=$($forumDb.Port) --user=$($forumDb.Credential.UserName) --batch
    if($LASTEXITCODE -ne 0){throw 'Could not create the isolated test schema/account.'}
    $forumSecure=New-Object System.Security.SecureString
    foreach($forumCharacter in $forumAppSecret.ToCharArray()){$forumSecure.AppendChar($forumCharacter)}
    $forumSecure.MakeReadOnly()
    $forumCredential=New-Object System.Management.Automation.PSCredential($forumAppName,$forumSecure)
    [pscustomobject]@{Host='127.0.0.1';Port=$forumDb.Port;Database='campus_forum_integration';Credential=$forumCredential} | Export-Clixml -LiteralPath $forumAppPath
    Write-Host 'Independent forum schema and limited application account prepared.'
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    $forumAppSecret=$null
    $forumSql=$null
}
