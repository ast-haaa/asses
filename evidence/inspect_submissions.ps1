$js = Get-Content -Path .\submissions_chunk.js -Raw

Write-Host "=== CREATESERVERREFERENCE IN SUBMISSIONS CHUNK ==="
$matchesAct = [regex]::Matches($js, '([a-zA-Z0-9_]+)\s*=\s*\(0,[a-zA-Z0-9_]+\.createServerReference\)\("([^"]+)",[^,]+,[^,]+,[^,]+,"([^"]+)"\)')
foreach ($m in $matchesAct) {
    Write-Host $m.Value
}
