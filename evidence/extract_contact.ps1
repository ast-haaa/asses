$contact = Get-Content -Path .\cynbit_contact.html -Raw

Write-Host "=== CONTACT SCRIPTS ==="
$matchesScript = [regex]::Matches($contact, '<script[^>]*src="([^"]+)"')
foreach ($m in $matchesScript) {
    Write-Host $m.Groups[1].Value
}

Write-Host "`n=== CONTACT FORMS ==="
$matchesForms = [regex]::Matches($contact, '<form[^>]*>(.*?)</form>', [System.Text.RegularExpressions.RegexOptions]::Singleline)
foreach ($m in $matchesForms) {
    Write-Host $m.Value.Substring(0, [Math]::Min(300, $m.Value.Length))
}

Write-Host "`n=== INPUT FIELDS ==="
$matchesInput = [regex]::Matches($contact, '<(input|textarea|button|select)[^>]*>')
foreach ($m in $matchesInput) {
    Write-Host $m.Value
}
