$js = Get-Content -Path .\careers_slug_chunk.js -Raw
Write-Host $js.Substring(0, 2641)
