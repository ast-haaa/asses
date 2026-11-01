$html = Get-Content -Path .\intern_page.html -Raw

Write-Host "=== JOB PAGE FORMS & INPUTS ==="
$matchesForms = [regex]::Matches($html, '<form[^>]*>(.*?)</form>', [System.Text.RegularExpressions.RegexOptions]::Singleline)
foreach ($m in $matchesForms) {
    Write-Host $m.Value.Substring(0, [Math]::Min(300, $m.Value.Length))
}

Write-Host "`n=== BUTTONS & LINKS ==="
$matchesLinks = [regex]::Matches($html, '<a[^>]+href="([^"]+)"[^>]*>(.*?)</a>')
foreach ($m in $matchesLinks) {
    if ($m.Groups[1].Value -match 'apply' -or $m.Groups[1].Value -match 'form' -or $m.Groups[1].Value -match 'mailto' -or $m.Groups[1].Value -match 'career') {
        Write-Host $m.Groups[1].Value " --> " $m.Groups[2].Value
    }
}
