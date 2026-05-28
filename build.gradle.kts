plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.0.1"
}

group = "com.ravi.notesapp"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.fxmisc.richtext:richtextfx:0.11.3")
    implementation("org.fxmisc.flowless:flowless:0.7.3")
}

application {
    mainModule.set("notesapp")
    mainClass.set("com.ravi.notesapp.app.MainApp")
    applicationDefaultJvmArgs = listOf(
        "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"
    )
}

jlink {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    addExtraDependencies("javafx")
    launcher {
        name = "Notepad_Pro"
    }
    jpackage {
        val os = org.gradle.internal.os.OperatingSystem.current()
        val type = project.findProperty("installerType")?.toString() ?: if (os.isWindows) "msi" else if (os.isMacOsX) "dmg" else "deb"
        installerType = type
        
        if (os.isWindows) {
            val iconPath = project.file("src/main/resources/com/ravi/notesapp/app_icon.ico").absolutePath
            imageOptions = listOf("--icon", iconPath)
            installerOptions = mutableListOf("--win-dir-chooser", "--win-menu", "--win-shortcut", "--win-per-user-install", "--vendor", "Redix Systems").apply {
                if (type == "exe") {
                    add("--icon")
                    add(iconPath)
                }
            }.toList()
        } else if (os.isMacOsX) {
            installerOptions = listOf("--vendor", "Redix Systems", "--mac-package-name", "Notepad_Pro")
            val icnsFile = project.file("src/main/resources/com/ravi/notesapp/app_icon.icns")
            if (icnsFile.exists()) {
                imageOptions = listOf("--icon", icnsFile.absolutePath)
            }
        } else if (os.isLinux) {
            val pngPath = project.file("src/main/resources/com/ravi/notesapp/app_icon.png").absolutePath
            imageOptions = listOf("--icon", pngPath)
            installerOptions = listOf("--linux-shortcut", "--linux-menu-group", "Utility", "--vendor", "Redix Systems")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
