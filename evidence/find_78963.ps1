$js = Get-Content -Path .\interns_chunk.js -Raw

$idx = $js.IndexOf("78963:")
if ($idx -gt 0) {
    Write-Host $js.Substring($idx, [Math]::Min(1000, $js.Length - $idx))
}
