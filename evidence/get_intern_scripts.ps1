$html = Get-Content -Path .\intern_page.html -Raw
$matchesScript = [regex]::Matches($html, '<script[^>]*src="([^"]+)"')
foreach ($m in $matchesScript) {
    Write-Host $m.Groups[1].Value
}
