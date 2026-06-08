# Notepad_ProApp Build & Publish Documentation

This document describes how to compile, package, and release/publish the **Notepad_ProApp** installer both locally and via CI/CD.

---

## Prerequisites

Before building, ensure the following are installed on your machine:

1. **Java Development Kit (JDK) 17**: Ensure `java` is in your environment PATH.
2. **WiX Toolset (v3.x)** (Windows MSI only):
   - Used by Java's `jpackage` to generate the `.msi` Windows installer.
   - The build script will automatically detect and add a local `./wix` toolset directory to the PATH if it is present in the project root. Otherwise, it will expect WiX to be installed globally.

---

## Local Build & Publish Script

An automated script is provided in the project root to run builds:

### Bash Shell Script (`build.sh`)

Designed for Unix, macOS, Linux, and Windows Git Bash/WSL environments.

**How to run:**

```bash
chmod +x build.sh
./build.sh
```

### What this script does:

1. **Parse Project Info**: Dynamically read the application name and version from [build.gradle.kts](file:///c:/Users/panthula/Desktop/Nt/build.gradle.kts).
2. **Prerequisite Check**: Validate that Java (and WiX Toolset on Windows) are installed and accessible.
3. **Clean & Build**: Execute `./gradlew clean jpackage` to compile code, assemble modules, and compile the installer executable.
4. **Stage Output**: Create a `releases/` directory in the project root and copy the generated installer into it.
5. **Generate Metadata Manifest**: Generate a `release-manifest.json` file inside the `releases/` folder with file size, release date, and a SHA-256 integrity hash of the installer.

---

## Staged Output Structure

After a successful run, the following files will be created in the `releases/` folder:

```
releases/
├── Notepad_ProApp-2.0.0.msi        # The Windows MSI installer containing the bundled modular JRE
└── release-manifest.json      # Metadata manifest for the release
```

### Example `release-manifest.json`:

```json
{
  "projectName": "Notepad_ProApp",
  "version": "2.0.0",
  "fileName": "Notepad_ProApp-2.0.0.msi",
  "sha256": "96ad01704b32377d435cea08d971789f1a2eba591df0a86729c409b59dd98e11",
  "releaseDate": "2026-05-26T18:50:01Z",
  "fileSize": "36.96 MB"
}
```

---

## CI/CD Publishing (GitHub Actions)

A GitHub Actions workflow is provided at [.github/workflows/build-publish.yml](file:///c:/Users/panthula/Desktop/Nt/.github/workflows/build-publish.yml).

### How it works:

- **Triggers**:
  - Automatically when you push a version tag matching `v*`
  - Manually via the **Run workflow** button in the GitHub Actions tab.
- **Process**:
  - Spins up a Windows virtual environment.
  - Installs JDK 17 (Temurin).
  - Caches Gradle dependencies for faster subsequent runs.
  - Packages the installer with the pre-installed WiX Toolset.
  - Automatically creates a GitHub Release and uploads the `.msi` file as a release asset.
