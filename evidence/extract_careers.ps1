$careers = Get-Content -Path .\cynbit_careers.html -Raw

Write-Host "=== CAREERS SCRIPTS ==="
$matchesScript = [regex]::Matches($careers, '<script[^>]*src="([^"]+)"')
foreach ($m in $matchesScript) {
    Write-Host $m.Groups[1].Value
}

Write-Host "`n=== CAREERS FORMS ==="
$matchesForms = [regex]::Matches($careers, '<form[^>]*>(.*?)</form>', [System.Text.RegularExpressions.RegexOptions]::Singleline)
foreach ($m in $matchesForms) {
    Write-Host $m.Value.Substring(0, [Math]::Min(300, $m.Value.Length))
}

Write-Host "`n=== CAREERS BUTTONS / INPUTS ==="
$matchesInputs = [regex]::Matches($careers, '<(input|textarea|button)[^>]*>')
foreach ($m in $matchesInputs | Select-Object -First 15) {
    Write-Host $m.Value
}
