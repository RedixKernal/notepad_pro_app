param(
    [string]$Version = "2.0.0"
)

$ErrorActionPreference = "Stop"

Write-Host "Building Notepad Pro MSIX package..."

# 1. Generate the app image using Gradle
Write-Host "Generating app image via Gradle jpackage..."
# Run gradle to output the app image instead of the msi
# The output is typically placed in build/jpackage/Notepad_Pro
& .\gradlew.bat jpackageImage
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed."
    exit $LASTEXITCODE
}

# 2. Define paths
$AppImagePath = "build/jpackage/Notepad_Pro"
$MsixPath = "build/jpackage/Notepad_Pro-${Version}.msix"

if (-not (Test-Path $AppImagePath)) {
    Write-Error "App image not found at $AppImagePath"
    exit 1
}

# 3. Copy AppxManifest.xml
Write-Host "Copying AppxManifest.xml..."
Copy-Item -Path "msix/AppxManifest.xml" -Destination "$AppImagePath/AppxManifest.xml" -Force

# 4. Generate/Copy Icons
# For simplicity, we assume an existing app_icon.png and use it to create required Store logos.
$IconSource = "src/main/resources/com/ravi/notesapp/app_icon.png"

# We will load System.Drawing to resize images
Add-Type -AssemblyName System.Drawing

function Resize-Image($source, $target, $width, $height) {
    $srcImg = [System.Drawing.Image]::FromFile((Resolve-Path $source).Path)
    $destImg = New-Object System.Drawing.Bitmap($width, $height)
    $graphics = [System.Drawing.Graphics]::FromImage($destImg)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($srcImg, 0, 0, $width, $height)
    $destImg.Save((Join-Path (Resolve-Path .).Path $target), [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $destImg.Dispose()
    $srcImg.Dispose()
}

Write-Host "Generating Visual Assets..."
Resize-Image -source $IconSource -target "$AppImagePath/StoreLogo.png" -width 50 -height 50
Resize-Image -source $IconSource -target "$AppImagePath/Square150x150Logo.png" -width 150 -height 150
Resize-Image -source $IconSource -target "$AppImagePath/Square44x44Logo.png" -width 44 -height 44
Resize-Image -source $IconSource -target "$AppImagePath/Wide310x150Logo.png" -width 310 -height 150

# 5. Find makeappx.exe
Write-Host "Locating makeappx.exe..."
$MakeAppxPath = $null

$WinKitsPaths = @(
    "${env:ProgramFiles(x86)}\Windows Kits\10\bin",
    "${env:ProgramFiles}\Windows Kits\10\bin"
)

foreach ($WinKitsPath in $WinKitsPaths) {
    if (Test-Path $WinKitsPath) {
        $sdkVersions = Get-ChildItem -Path $WinKitsPath -Filter "10.0.*" -Directory | Sort-Object Name -Descending
        foreach ($sdk in $sdkVersions) {
            $candidate = Join-Path $sdk.FullName "x64\makeappx.exe"
            if (Test-Path $candidate) {
                $MakeAppxPath = $candidate
                break
            }
        }
    }
    if ($MakeAppxPath) { break }
}

if ([string]::IsNullOrWhiteSpace($MakeAppxPath)) {
    # Fallback to older paths
    foreach ($WinKitsPath in $WinKitsPaths) {
        $candidate = Join-Path $WinKitsPath "x64\makeappx.exe"
        if (Test-Path $candidate) {
            $MakeAppxPath = $candidate
            break
        }
    }
}

if ([string]::IsNullOrWhiteSpace($MakeAppxPath)) {
    # One final fallback to see if it's on the PATH
    $MakeAppxPath = (Get-Command makeappx.exe -ErrorAction SilentlyContinue).Source
}

Write-Host "Using makeappx.exe from: $MakeAppxPath"

if ([string]::IsNullOrWhiteSpace($MakeAppxPath)) {
    Write-Error "makeappx.exe not found. Ensure Windows SDK is installed."
    exit 1
}

# 6. Build the MSIX
Write-Host "Packaging MSIX..."
if (Test-Path $MsixPath) { Remove-Item $MsixPath -Force }

& $MakeAppxPath pack /d $AppImagePath /p $MsixPath
if ($LASTEXITCODE -ne 0) {
    Write-Error "makeappx failed."
    exit $LASTEXITCODE
}

Write-Host "Successfully generated $MsixPath"
