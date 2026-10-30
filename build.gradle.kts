plugins {
    java
    id("org.springframework.boot") version "3.5.0" apply false
    id("com.diffplug.spotless") version "7.0.2" apply false
}

allprojects {
    group = "com.hotelbooking"
    version = "0.0.1-SNAPSHOT"
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.diffplug.spotless")

    dependencies {
        add(
            "implementation",
            platform("org.springframework.boot:spring-boot-dependencies:3.5.0")
        )
        add(
            "implementation",
            platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        )
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            targetExclude("**/build/**", "**/generated/**")
            removeUnusedImports()
            eclipse()
            importOrder(
                "java",
                "javax",
                "jakarta",
                "org.springframework",
                "com.hotelbooking",
                "",
                "\\#"
            )
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.5.0")
        }
        format("yaml") {
            target("src/**/*.yml", "src/**/*.yaml")
            targetExclude("**/build/**")
            trimTrailingWhitespace()
            endWithNewline()
            leadingTabsToSpaces(2)
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.named("check") {
        dependsOn("spotlessCheck")
    }
}
