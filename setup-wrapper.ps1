Param(
    [string]$GradleVersion = "8.4.1"
)
$ErrorActionPreference = "Stop"
$base = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $base

Write-Host "Fetching Gradle wrapper files (this will download from services.gradle.org)..." -ForegroundColor Cyan
$distUrl = "https://services.gradle.org/distributions/gradle-${GradleVersion}-bin.zip"
$zip = "$base\gradle-dist.zip"
Invoke-WebRequest -Uri $distUrl -OutFile $zip
$tempDir = "$base\gradle-temp"
Expand-Archive -Path $zip -DestinationPath $tempDir -Force
# copy wrapper jar and templates
$wrapperSrc = Get-ChildItem -Path $tempDir -Recurse -Filter "gradle-wrapper.jar" | Select-Object -First 1
if (-not $wrapperSrc) { throw "gradle-wrapper.jar not found in downloaded distribution." }
if (-not (Test-Path "$base\gradle")) { New-Item -ItemType Directory -Path "$base\gradle\wrapper" -Force | Out-Null }
Copy-Item $wrapperSrc.FullName -Destination "$base\gradle\wrapper\gradle-wrapper.jar" -Force
# create gradle-wrapper.properties
$props = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-${GradleVersion}-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@
Set-Content -Path "$base\gradle\wrapper\gradle-wrapper.properties" -Value $props -Encoding UTF8
# create gradle-wrapper.jar done
Remove-Item $zip -Force
Remove-Item $tempDir -Recurse -Force
Write-Host "Gradle wrapper files created. You can now run .\gradlew.bat bootRun" -ForegroundColor Green
