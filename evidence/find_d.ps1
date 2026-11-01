$js = Get-Content -Path .\contact_chunk.js -Raw

$idx = $js.IndexOf("onSubmit:d")
if ($idx -gt 0) {
    $start = [Math]::Max(0, $idx - 1000)
    $len = [Math]::Min($js.Length - $start, 2000)
    Write-Host $js.Substring($start, $len)
}
