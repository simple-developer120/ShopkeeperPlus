plugins {
    java
    id("com.gradleup.shadow") version "9.5.0"
}

group = "de.shop"
version = "2.0.3"

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/") {
        content { 
            includeGroupByRegex("io\\.papermc.*")
            includeGroup("net.md-5")
        }
    }

    maven("https://repo.minebench.de/") {
        content { includeGroupByRegex("net\\.kyori.*") }
    }

    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        content { includeGroup("me.clip") }
    }

    maven("https://libraries.minecraft.net") {
        content { includeGroup("com.mojang") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.5-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    compileJava { options.encoding = "UTF-8" }
    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand("version" to project.version) }
    }
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("ShopKeeperPlus")
        minimize()
    }
    build { dependsOn(shadowJar) }
}