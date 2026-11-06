param(
    [string]$OutputPath = (Join-Path $PSScriptRoot "generated")
)

New-Item -ItemType Directory -Force -Path $OutputPath | Out-Null
for ($i = 0; $i -lt 10000; $i++) {
    $file = Join-Path $OutputPath ("score-{0}.txt" -f $i)
    if ($i -lt 100) {
        if (Test-Path $file) {
            Remove-Item -LiteralPath $file
        }
        continue
    }
    if ($i -lt 200) {
        Set-Content -LiteralPath $file -Value "not-a-score" -Encoding ASCII
    } else {
        Set-Content -LiteralPath $file -Value ([string]($i % 101)) -Encoding ASCII
    }
}
Write-Output "Generated 9,900 score files; candidates 0-99 are missing and 100-199 are corrupt."
