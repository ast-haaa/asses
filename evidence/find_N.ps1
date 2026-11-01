$js = Get-Content -Path .\careers_slug_chunk.js -Raw

$idx = $js.IndexOf("onSubmit:N")
if ($idx -gt 0) {
    $start = [Math]::Max(0, $idx - 3000)
    $len = [Math]::Min(3000, $idx - $start)
    Write-Host $js.Substring($start, $len)
}
