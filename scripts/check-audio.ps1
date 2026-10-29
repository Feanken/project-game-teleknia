# Desktop check: decode and play all supplied MP3s silently; no player save/settings IO.
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & mvn -o test-compile
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Use JDK 21.' }
    $repo = Join-Path $env:USERPROFILE '.m2/repository'
    $jars = Get-ChildItem -LiteralPath $repo -Filter '*.jar' -Recurse | Where-Object {
        $_.FullName -match 'javafx-(base|graphics|controls|media).*21.0.5.*win.jar'
    } | ForEach-Object FullName
    $classpath = (@('target/classes', 'target/test-classes') + $jars) -join ';'
    & java "-Djavafx.cachedir=target/javafx-cache" -cp $classpath com.rpgchatgame.MusicPlaybackChecks
    if ($LASTEXITCODE -ne 0) { throw 'MP3 playback checks failed.' }
} finally { Pop-Location }
