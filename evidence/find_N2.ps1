$js = Get-Content -Path .\careers_slug_chunk.js -Raw

$idx = $js.IndexOf("onSubmit:N")
if ($idx -gt 0) {
    $sub = $js.Substring(0, $idx)
    $lastN = $sub.LastIndexOf("N=")
    if ($lastN -gt 0) {
        Write-Host "Found N= at ${lastN}:"
        Write-Host $js.Substring($lastN, [Math]::Min(1500, $js.Length - $lastN))
    }
}
