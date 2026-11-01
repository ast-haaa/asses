$js = Get-Content -Path .\careers_slug_chunk.js -Raw

Write-Host "=== CREATESERVERREFERENCE IN CAREERS SLUG CHUNK ==="
$matchesRef = [regex]::Matches($js, 'createServerReference\([^)]+\)')
foreach ($m in $matchesRef) {
    Write-Host $m.Value
}

Write-Host "`n=== SUBMIT / ACTION SNIPPETS ==="
$matchesSub = [regex]::Matches($js, '(onSubmit|handleSubmit|action)[^,;]{1,60}')
foreach ($m in $matchesSub) {
    Write-Host $m.Value
}
