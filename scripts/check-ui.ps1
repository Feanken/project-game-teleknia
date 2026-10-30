# Optional interactive desktop smoke checks. Never run this in headless CI.
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & mvn -o test-compile
    if ($LASTEXITCODE -ne 0) { throw 'Use JDK 21 and download dependencies with mvn test-compile first.' }
    $projectPath = (Get-Location).Path
    $repo = Join-Path $env:USERPROFILE '.m2/repository'
    $jars = Get-ChildItem -LiteralPath $repo -Filter '*.jar' -Recurse | Where-Object {
        $_.FullName -match 'javafx-(base|graphics|controls|media).*21.0.5.*win.jar|sqlite-jdbc.*3.46.1.3|jackson-(databind|core|annotations).*2.17.2|slf4j-api'
    } | ForEach-Object FullName
    $classpath = (@("$projectPath/target/maven-classes", "$projectPath/target/maven-test-classes") + $jars) -join ';'
    $casePath = Join-Path $projectPath ("target/ui-checks/run-" + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $casePath -Force | Out-Null
    Push-Location $casePath
    try {
        & java "-Dteleknia.uiCheck.root=$projectPath" "-Djavafx.cachedir=$projectPath/target/javafx-cache" -cp $classpath com.rpgchatgame.UiSmokeChecks
        if ($LASTEXITCODE -ne 0) { throw 'UI checks failed.' }
        Write-Output "UI screenshots: $casePath"
    } finally { Pop-Location }
} finally { Pop-Location }
