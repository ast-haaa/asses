$js = Get-Content -Path .\interns_chunk.js -Raw

Write-Host "=== CREATESERVERREFERENCE IN INTERNS CHUNK ==="
$matchesRef = [regex]::Matches($js, 'createServerReference\([^)]+\)')
foreach ($m in $matchesRef) {
    Write-Host $m.Value
}

Write-Host "`n=== CALLSERVER OR ACTIONS ==="
$matchesAct = [regex]::Matches($js, '([a-zA-Z0-9_]+)\s*=\s*\(0,[a-zA-Z0-9_]+\.createServerReference\)\("([^"]+)",[^,]+,[^,]+,[^,]+,"([^"]+)"\)')
foreach ($m in $matchesAct) {
    Write-Host $m.Value
}

Write-Host "`n=== SNIPPET OF INTERNS PAGE CODE ==="
Write-Host $js.Substring(0, [Math]::Min(1500, $js.Length))
