# Run from the project root with JDK 21 and Maven available.
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & mvn -o test-compile
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Use JDK 21; run mvn compile online once if dependencies are missing.' }
    $repo = Join-Path $env:USERPROFILE '.m2/repository'
    $jars = Get-ChildItem -LiteralPath $repo -Filter '*.jar' -Recurse | Where-Object {
        $_.FullName -match 'javafx-(base|graphics|controls|media).*21.0.5.*(win|linux|mac).jar|sqlite-jdbc.*3.46.1.3|jackson-(databind|core|annotations).*2.17.2|slf4j-api'
    } | ForEach-Object FullName
    $classpath = (@('target/maven-classes', 'target/maven-test-classes') + $jars) -join ';'
    & java -cp $classpath com.rpgchatgame.CampaignChecks
    if ($LASTEXITCODE -ne 0) { throw 'Campaign checks failed.' }
    & java -cp $classpath com.rpgchatgame.EssentialsChecks
    if ($LASTEXITCODE -ne 0) { throw 'Essentials checks failed.' }
    & java -cp $classpath com.rpgchatgame.BattleChecks
    if ($LASTEXITCODE -ne 0) { throw 'Battle checks failed.' }
    & java -cp $classpath com.rpgchatgame.WorldChecks
    if ($LASTEXITCODE -ne 0) { throw 'World checks failed.' }
    & java -cp $classpath com.rpgchatgame.AudioChecks
    if ($LASTEXITCODE -ne 0) { throw 'Audio checks failed.' }
    & java -cp $classpath com.rpgchatgame.BondChecks
    if ($LASTEXITCODE -ne 0) { throw 'Bond checks failed.' }
    & java -cp $classpath com.rpgchatgame.LivingChecks
    if ($LASTEXITCODE -ne 0) { throw 'Living checks failed.' }
    & java -cp $classpath com.rpgchatgame.AtlasChecks
    if ($LASTEXITCODE -ne 0) { throw 'Atlas checks failed.' }
    & java -cp $classpath com.rpgchatgame.PresentationChecks
    if ($LASTEXITCODE -ne 0) { throw 'Presentation checks failed.' }
} finally { Pop-Location }
