plugins {
    kotlin("jvm") version "1.4.32"
    id("com.utopia-rise.godot-kotlin-jvm") version "0.2.0-3.3.0"
}

repositories {
    mavenCentral()
}

godot {
    isAndroidExportEnabled.set(false)
    dxToolPath.set("dx")
}
