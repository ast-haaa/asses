$html = Get-Content -Path .\portal_dashboard.html -Raw

$idx = $html.IndexOf("Offer Letter Generator")
if ($idx -gt 0) {
    $start = [Math]::Max(0, $idx - 500)
    $len = [Math]::Min(1500, $html.Length - $start)
    Write-Host $html.Substring($start, $len)
}
