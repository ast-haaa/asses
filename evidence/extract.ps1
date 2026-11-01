$content = Get-Content -Path .\cynbit_home.html -Raw

Write-Host "=== SCRIPTS ==="
$matchesScript = [regex]::Matches($content, '<script[^>]*src="([^"]+)"')
foreach ($m in $matchesScript) {
    Write-Host $m.Groups[1].Value
}

Write-Host "`n=== LINKS ==="
$matchesHref = [regex]::Matches($content, 'href="([^"]+)"')
$uniqueHrefs = @{}
foreach ($m in $matchesHref) {
    $uniqueHrefs[$m.Groups[1].Value] = $true
}
$uniqueHrefs.Keys | Sort-Object | ForEach-Object { Write-Host $_ }

Write-Host "`n=== FORMS & ACTIONS ==="
$matchesForms = [regex]::Matches($content, '<form[^>]*>')
foreach ($m in $matchesForms) {
    Write-Host $m.Value
}

Write-Host "`n=== NEXT_DATA or Self Scripts ==="
$matchesNext = [regex]::Matches($content, '<script[^>]*>(.*?)</script>', [System.Text.RegularExpressions.RegexOptions]::Singleline)
foreach ($m in $matchesNext) {
    if ($m.Groups[1].Value -match 'NEXT' -or $m.Groups[1].Value -match 'env' -or $m.Groups[1].Value -match 'api') {
        $val = $m.Groups[1].Value
        Write-Host "Found inline script snippet: " $val.Substring(0, [Math]::Min(200, $val.Length))
    }
}
