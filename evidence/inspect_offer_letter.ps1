$js = Get-Content -Path .\offer_letter_chunk.js -Raw

Write-Host "=== CREATESERVERREFERENCE IN OFFER LETTER CHUNK ==="
$matchesAct = [regex]::Matches($js, '([a-zA-Z0-9_]+)\s*=\s*\(0,[a-zA-Z0-9_]+\.createServerReference\)\("([^"]+)",[^,]+,[^,]+,[^,]+,"([^"]+)"\)')
foreach ($m in $matchesAct) {
    Write-Host $m.Value
}

Write-Host "`n=== FIRST 1000 CHARACTERS ==="
Write-Host $js.Substring(0, [Math]::Min(1000, $js.Length))
