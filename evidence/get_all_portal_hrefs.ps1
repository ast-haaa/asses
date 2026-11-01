$html = Get-Content -Path .\portal_dashboard.html -Raw

$matchesHref = [regex]::Matches($html, 'href(?:\\*")*:(?:\\*")*([^\\",}]+)')
foreach ($m in $matchesHref) {
    Write-Host $m.Groups[1].Value
}
