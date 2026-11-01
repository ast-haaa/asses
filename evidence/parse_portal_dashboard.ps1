$html = Get-Content -Path .\portal_dashboard.html -Raw

Write-Host "=== LINKS IN PORTAL DASHBOARD ==="
$matchesHref = [regex]::Matches($html, 'href=\\?"([^"\\]+)\\?"')
foreach ($m in $matchesHref) {
    Write-Host $m.Groups[1].Value
}

Write-Host "`n=== TITLES & HEADINGS ==="
$matchesH = [regex]::Matches($html, 'children\\?":\\?"([^"\\]+)\\?"')
foreach ($m in $matchesH) {
    if ($m.Groups[1].Value.Length -gt 3) {
        Write-Host $m.Groups[1].Value
    }
}
