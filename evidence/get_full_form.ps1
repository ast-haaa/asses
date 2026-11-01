$html = Get-Content -Path .\intern_page.html -Raw

$idx = $html.IndexOf("<form")
if ($idx -gt 0) {
    $endIdx = $html.IndexOf("</form>", $idx)
    Write-Host "Form length: " ($endIdx - $idx)
    $formContent = $html.Substring($idx, ($endIdx - $idx + 7))
    $inputs = [regex]::Matches($formContent, '<(input|select|textarea|button)[^>]*>')
    foreach ($inp in $inputs) {
        Write-Host $inp.Value
    }
}
