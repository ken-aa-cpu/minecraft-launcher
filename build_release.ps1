# build_release.ps1
# Build and Package Launcher

$ErrorActionPreference = "Stop"
$Version = "1.0.0"
$AppName = "MCL"
$OutputDir = "release"
$JarName = "minecraft-launcher-$Version.jar"

Write-Host "[1/5] Cleaning old build files..."
if (Test-Path $OutputDir) { Remove-Item $OutputDir -Recurse -Force }
if (Test-Path "target") { Remove-Item "target" -Recurse -Force }

Write-Host "[2/5] Running Maven Build (Fat JAR)..."
cmd /c mvn clean package

if (-not (Test-Path "target/$JarName")) {
    Write-Error "Maven build failed. JAR file not found."
    exit 1
}

Write-Host "[3/5] Creating Application Image with jpackage..."
# Ensure jpackage exists
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    Write-Error "jpackage not found. Please ensure JDK 14+ is installed and in PATH."
    exit 1
}

# Run jpackage
jpackage `
  --type app-image `
  --input target `
  --main-jar $JarName `
  --main-class com.mcserver.launcher.LauncherMain `
  --dest $OutputDir `
  --name $AppName `
  --vendor "MCServer" `
  --win-console

Write-Host "[4/5] Integrating Game Folder..."
$AppPath = "$OutputDir/$AppName"

# Copy game folder to the app root
if (Test-Path "game") {
    Copy-Item "game" -Destination "$AppPath" -Recurse
    Write-Host "Game folder copied successfully."
} else {
    Write-Warning "Game folder not found. The release will not contain pre-installed game files."
}

Write-Host "[5/5] Build Complete!"
Write-Host "Release Location: $AppPath"
Write-Host "Please zip this folder and distribute it to players."
Write-Host "Players should run: $AppName.exe inside the folder."
