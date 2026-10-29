# Run from the project root with JDK 21 and Maven available.
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & mvn -o test-compile
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Use JDK 21; run mvn compile online once if dependencies are missing.' }
    $repo = Join-Path $env:USERPROFILE '.m2/repository'
    $jars = Get-ChildItem -LiteralPath $repo -Filter '*.jar' -Recurse | Where-Object {
        $_.FullName -match 'sqlite-jdbc.*3.46.1.3|jackson-(databind|core|annotations).*2.17.2|slf4j-api'
    } | ForEach-Object FullName
    $classpath = (@('target/classes', 'target/test-classes') + $jars) -join ';'
    & java -cp $classpath com.rpgchatgame.CampaignChecks
    if ($LASTEXITCODE -ne 0) { throw 'Campaign checks failed.' }
    & java -cp $classpath com.rpgchatgame.EssentialsChecks
    if ($LASTEXITCODE -ne 0) { throw 'Essentials checks failed.' }
} finally { Pop-Location }
