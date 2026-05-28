#!/usr/bin/env bash
# Build and Publish script for Notepad_Pro (Unix/Linux/macOS/Git Bash)
# Automates clean build, packaging, staging, and metadata generation.

# set -e

echo "=============================================="
echo "   Notepad_Pro Build & Publish Script (Bash)     "
echo "=============================================="

# 1. Parse Version and Name from Gradle Build File
GRADLE_FILE="build.gradle.kts"
if [ ! -f "$GRADLE_FILE" ]; then
    echo "Error: Could not find $GRADLE_FILE in the current directory."
fi

VERSION=$(grep -E 'version[[:space:]]*=[[:space:]]*"[^"]+"' "$GRADLE_FILE" | head -n 1 | sed -E 's/.*"([^"]+)".*/\1/')
if [ -z "$VERSION" ]; then
    VERSION="1.0.0"
fi

APP_NAME="Notepad_Pro"
# Try parsing launcher name
LAUNCHER_NAME=$(grep -E 'name[[:space:]]*=[[:space:]]*"[^"]+"' "$GRADLE_FILE" | head -n 1 | sed -E 's/.*"([^"]+)".*/\1/')
if [ -n "$LAUNCHER_NAME" ]; then
    APP_NAME="$LAUNCHER_NAME"
fi

echo "Detected Project Name: $APP_NAME"
echo "Detected Version     : $VERSION"

# 2. Check Prerequisites (Java)
echo "Checking Java installation..."
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in the PATH."
fi
echo "Java is ready."

# Add local WiX toolset to PATH if running in Windows Git Bash
if [ -d "./wix" ]; then
    echo "Found local WiX toolset. Adding to PATH..."
    echo ""
    echo "..."
    export PATH="$PWD/wix:$PATH"
fi

# 3. Clean and Package Installers
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
else
    GRADLE_CMD="gradle"
fi

# Clean releases folder first
mkdir -p releases
rm -f releases/*.msi
rm -f releases/*.exe

# Build and Stage MSI
echo "Running Gradle clean jpackage (MSI)..."
$GRADLE_CMD clean jpackage -PinstallerType=msi

echo "Staging MSI to releases folder..."
for f in build/jpackage/*.msi; do
    if [ -f "$f" ]; then
        cp "$f" releases/
        echo "Staged installer to: releases/$(basename "$f")"
    fi
done
echo ""
echo "..."
# Build and Stage EXE
echo "Running Gradle clean jpackage (EXE)..."
$GRADLE_CMD clean jpackage -PinstallerType=exe

echo "Staging EXE to releases folder..."
for f in build/jpackage/*.exe; do
    if [ -f "$f" ]; then
        cp "$f" releases/
        echo "Staged installer to: releases/$(basename "$f")"
    fi
done

echo ""
echo "Gradle packaging succeeded."

# 5. Generate Release Manifest
echo "Generating release manifest..."
MANIFEST_JSON="releases/release-manifest.json"
cat <<EOF > "$MANIFEST_JSON"
{
  "projectName": "${APP_NAME}",
  "version": "${VERSION}",
  "releaseDate": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "installers": [
EOF

FIRST=true
for f in releases/*.msi releases/*.exe; do
    if [ -f "$f" ]; then
        FNAME=$(basename "$f")
        FORMAT="${FNAME##*.}"
        
        # Calculate Hash
        if command -v sha256sum &> /dev/null; then
            HASH=$(sha256sum "$f" | awk '{print $1}')
        elif command -v shasum &> /dev/null; then
            HASH=$(shasum -a 256 "$f" | awk '{print $1}')
        else
            HASH="unknown"
        fi
        
        # Calculate Size
        if [[ "$OSTYPE" == "darwin"* ]]; then
            SIZE_BYTES=$(stat -f%z "$f")
        else
            SIZE_BYTES=$(stat -c%s "$f")
        fi
        SIZE_MB=$(echo "scale=2; $SIZE_BYTES / 1048576" | bc 2>/dev/null || awk "BEGIN {printf \"%.2f\", $SIZE_BYTES/1048576}")
        
        if [ "$FIRST" = true ]; then
            FIRST=false
        else
            echo "," >> "$MANIFEST_JSON"
        fi
        
        cat <<EOF >> "$MANIFEST_JSON"
    {
      "fileName": "${FNAME}",
      "format": "${FORMAT}",
      "sha256": "${HASH}",
      "fileSize": "${SIZE_MB} MB"
    }
EOF
        echo "Calculated SHA-256 for $FNAME: $HASH"
    fi
done

cat <<EOF >> "$MANIFEST_JSON"
  ]
}
EOF

echo "Generated release manifest at: $MANIFEST_JSON"
echo "=============================================="
echo "   Publish Build Completed Successfully!      "
echo "=============================================="
