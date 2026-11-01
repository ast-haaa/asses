$js = Get-Content -Path .\contact_chunk.js -Raw

$idx = $js.IndexOf("onSubmit:d")
if ($idx -gt 0) {
    $start = [Math]::Max(0, $idx - 3000)
    $len = 3000
    Write-Host $js.Substring($start, $len)
}
