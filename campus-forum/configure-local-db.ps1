$env:PSModulePath = Join-Path $PSHOME 'Modules'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()
$dbForm = New-Object System.Windows.Forms.Form
$dbForm.Text = '校园论坛 - 本地 MySQL 配置'
$dbForm.ClientSize = New-Object System.Drawing.Size(550,390)
$dbForm.StartPosition = 'CenterScreen'
$dbForm.FormBorderStyle = 'FixedDialog'
$dbForm.MaximizeBox = $false
$dbForm.Font = New-Object System.Drawing.Font('Microsoft YaHei UI',10)
function Add-DbLabel($text,$x,$y,$width=460) {
    $label = New-Object System.Windows.Forms.Label
    $label.Text=$text; $label.Location=New-Object System.Drawing.Point($x,$y)
    $label.Size=New-Object System.Drawing.Size($width,28)
    $dbForm.Controls.Add($label)
}
function Add-DbInput($text,$y) {
    $inputBox=New-Object System.Windows.Forms.TextBox
    $inputBox.Text=$text; $inputBox.Location=New-Object System.Drawing.Point(155,$y)
    $inputBox.Size=New-Object System.Drawing.Size(355,28)
    $dbForm.Controls.Add($inputBox)
    return $inputBox
}
Add-DbLabel '填写已安装的 MySQL 登录信息，密码不会回显。' 25 20
Add-DbLabel '主机' 25 65 120
$dbHostBox=Add-DbInput '127.0.0.1' 62
Add-DbLabel '端口' 25 110 120
$dbPortBox=Add-DbInput '3306' 107
Add-DbLabel '数据库账号' 25 155 120
$dbUserBox=Add-DbInput 'root' 152
Add-DbLabel '数据库密码' 25 200 120
$dbPasswordBox=Add-DbInput '' 197
$dbPasswordBox.UseSystemPasswordChar=$true
Add-DbLabel '密码由 Windows 加密保存，仅当前 Windows 用户可解密。' 25 245 500
Add-DbLabel '此窗口仅保存连接配置，不会修改现有数据库。' 25 272 500
$dbSaveButton=New-Object System.Windows.Forms.Button
$dbSaveButton.Text='保存配置'; $dbSaveButton.Location=New-Object System.Drawing.Point(290,325)
$dbSaveButton.Size=New-Object System.Drawing.Size(105,35)
$dbCancelButton=New-Object System.Windows.Forms.Button
$dbCancelButton.Text='取消'; $dbCancelButton.Location=New-Object System.Drawing.Point(405,325)
$dbCancelButton.Size=New-Object System.Drawing.Size(105,35)
$dbForm.Controls.AddRange(@($dbSaveButton,$dbCancelButton))
$dbForm.AcceptButton=$dbSaveButton
$dbForm.CancelButton=$dbCancelButton
$dbCancelButton.Add_Click({$dbForm.Close()})
$dbSaveButton.Add_Click({
    try {
        $dbPortValue=0
        if (-not [int]::TryParse($dbPortBox.Text,[ref]$dbPortValue) -or $dbPortValue -lt 1 -or $dbPortValue -gt 65535) { throw '请输入正确端口。' }
        if ($dbHostBox.Text.Trim() -notin @('127.0.0.1','localhost')) { throw '本次只连接本机 MySQL，请使用 127.0.0.1 或 localhost。' }
        if ([string]::IsNullOrWhiteSpace($dbUserBox.Text)) { throw '请输入数据库账号。' }
        if ([string]::IsNullOrEmpty($dbPasswordBox.Text)) { throw '请输入数据库密码。' }
        $dbSecurePassword=New-Object System.Security.SecureString
        foreach ($dbPasswordCharacter in $dbPasswordBox.Text.ToCharArray()) { $dbSecurePassword.AppendChar($dbPasswordCharacter) }
        $dbSecurePassword.MakeReadOnly()
        $dbCredential=New-Object System.Management.Automation.PSCredential($dbUserBox.Text.Trim(),$dbSecurePassword)
        $dbLocalDirectory=Join-Path $PSScriptRoot '.local'
        New-Item -ItemType Directory -Path $dbLocalDirectory -Force | Out-Null
        [pscustomobject]@{Host=$dbHostBox.Text.Trim();Port=$dbPortValue;Credential=$dbCredential;Database='campus_forum_integration'} | Export-Clixml -LiteralPath (Join-Path $dbLocalDirectory 'db-connection.clixml')
        $dbPasswordBox.Clear()
        [System.Windows.Forms.MessageBox]::Show('本机连接配置已保存。接下来会验证连接，再创建独立测试库。','保存成功','OK','Information') | Out-Null
        $dbForm.Close()
    } catch {
        [System.Windows.Forms.MessageBox]::Show($_.Exception.Message,'未能保存','OK','Warning') | Out-Null
    }
})
$dbForm.Add_Shown({$dbForm.Activate();$dbPasswordBox.Focus()})
[void]$dbForm.ShowDialog()
$dbPasswordBox.Clear()
$dbForm.Dispose()
