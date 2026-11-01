$js = Get-Content -Path .\contact_chunk.js -Raw

Write-Host "=== SEARCHING FOR FETCH, AXIOS, API, POST IN CONTACT CHUNK ==="
$matchesFetch = [regex]::Matches($js, '(fetch\(|action|method|\/api\/[a-zA-Z0-9_\-\/]+|"POST"|''POST'')')
foreach ($m in $matchesFetch) {
    Write-Host $m.Value
}

Write-Host "`n=== SUBMISSION HANDLER SNIPPETS ==="
$matchesHandler = [regex]::Matches($js, 'onSubmit[^}]+}')
foreach ($m in $matchesHandler) {
    Write-Host $m.Value
}

Write-Host "`n=== ANY URLS OR STRINGS ==="
$matchesUrls = [regex]::Matches($js, 'https?://[^\s"'',]+')
foreach ($m in $matchesUrls) {
    Write-Host $m.Value
}
