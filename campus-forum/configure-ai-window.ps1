$env:PSModulePath=Join-Path $PSHOME 'Modules'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
[System.Windows.Forms.Application]::EnableVisualStyles()
$aiForm=New-Object System.Windows.Forms.Form
$aiForm.Text='校园论坛 - DeepSeek 本地配置'
$aiForm.ClientSize=New-Object System.Drawing.Size(610,370)
$aiForm.StartPosition='CenterScreen'
$aiForm.FormBorderStyle='FixedDialog'
$aiForm.MaximizeBox=$false
$aiForm.Font=New-Object System.Drawing.Font('Microsoft YaHei UI',10)
function Add-AiLabel($text,$x,$y,$width=550) {
    $label=New-Object System.Windows.Forms.Label
    $label.Text=$text; $label.Location=New-Object System.Drawing.Point($x,$y)
    $label.Size=New-Object System.Drawing.Size($width,30)
    $aiForm.Controls.Add($label)
}
function Add-AiInput($text,$y) {
    $box=New-Object System.Windows.Forms.TextBox
    $box.Text=$text; $box.Location=New-Object System.Drawing.Point(125,$y)
    $box.Size=New-Object System.Drawing.Size(450,28)
    $aiForm.Controls.Add($box)
    return $box
}
Add-AiLabel '在本机填写密钥，不要发到聊天里。' 25 18
Add-AiLabel '接口地址' 25 67 100
$aiEndpointBox=Add-AiInput 'https://api.deepseek.com/chat/completions' 64
$aiEndpointBox.ReadOnly=$true
Add-AiLabel '模型名称' 25 112 100
$aiModelBox=Add-AiInput 'deepseek-flash' 109
Add-AiLabel 'API Key' 25 157 100
$aiKeyBox=Add-AiInput '' 154
$aiKeyBox.UseSystemPasswordChar=$true
Add-AiLabel '密钥由 Windows 加密保存在 .local 中，不进入源码包。' 25 204
Add-AiLabel '提问与匹配的资料会发送给 DeepSeek，调用使用你的 API 额度。' 25 237
$aiSave=New-Object System.Windows.Forms.Button
$aiSave.Text='保存配置';$aiSave.Location=New-Object System.Drawing.Point(350,305)
$aiSave.Size=New-Object System.Drawing.Size(105,35)
$aiCancel=New-Object System.Windows.Forms.Button
$aiCancel.Text='取消';$aiCancel.Location=New-Object System.Drawing.Point(470,305)
$aiCancel.Size=New-Object System.Drawing.Size(105,35)
$aiForm.Controls.AddRange(@($aiSave,$aiCancel))
$aiForm.AcceptButton=$aiSave;$aiForm.CancelButton=$aiCancel
$aiCancel.Add_Click({$aiForm.Close()})
$aiSave.Add_Click({
    try {
        $aiKeyText=$aiKeyBox.Text.Trim()
        if([string]::IsNullOrWhiteSpace($aiKeyText) -or $aiKeyText -match '\s'){throw '请输入完整密钥，不要包含空格或换行。'}
        if($aiModelBox.Text.Trim() -notmatch '^[A-Za-z0-9._:-]+$'){throw '请输入正确的模型名称。'}
        $aiSecure=New-Object System.Security.SecureString
        foreach($aiCharacter in $aiKeyText.ToCharArray()){$aiSecure.AppendChar($aiCharacter)}
        $aiSecure.MakeReadOnly()
        $aiCredential=New-Object System.Management.Automation.PSCredential('DeepSeek',$aiSecure)
        $aiLocal=Join-Path $PSScriptRoot '.local'
        New-Item -ItemType Directory -Force -Path $aiLocal | Out-Null
        [pscustomobject]@{Endpoint=$aiEndpointBox.Text;Model=$aiModelBox.Text.Trim();Credential=$aiCredential;Enabled=$true} | Export-Clixml -LiteralPath (Join-Path $aiLocal 'ai-connection.clixml')
        $aiKeyBox.Clear();$aiKeyText=$null
        [System.Windows.Forms.MessageBox]::Show('配置已加密保存。告诉助手“保存好了”，再重启论坛并验证真实调用。','保存成功','OK','Information')|Out-Null
        $aiForm.Close()
    } catch {[System.Windows.Forms.MessageBox]::Show($_.Exception.Message,'未能保存','OK','Warning')|Out-Null}
})
$aiForm.Add_Shown({$aiForm.Activate();$aiKeyBox.Focus()})
[void]$aiForm.ShowDialog()
$aiKeyBox.Clear();$aiForm.Dispose()
