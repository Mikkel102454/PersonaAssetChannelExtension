plugins { `java-library` }

group = "nu.miguel"
version = "2.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.citizensnpcs.co/repo")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    compileOnly(files("../Persona/build/libs/Persona-2.0.0.jar"))
    compileOnly("net.citizensnpcs:citizens-main:2.0.43-SNAPSHOT") { isTransitive = false }
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.papermc.paper:paper-api:26.2.build.+")
    testImplementation(files("../Persona/build/libs/Persona-2.0.0.jar"))
    testImplementation("net.citizensnpcs:citizens-main:2.0.43-SNAPSHOT") { isTransitive = false }
    testImplementation("org.mockito:mockito-core:5.19.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java { toolchain.languageVersion = JavaLanguageVersion.of(25) }
tasks.test { useJUnitPlatform() }

val verifyNoAssetChannelClasses by tasks.registering {
    dependsOn(tasks.jar)
    doLast {
        val output = tasks.jar.get().archiveFile.get().asFile
        zipTree(output).visit {
            if (!isDirectory && (path.startsWith("nu/miguel/assetChannel/") ||
                    path.endsWith(".class") && file.readBytes().toString(Charsets.ISO_8859_1)
                        .contains("nu/miguel/assetChannel/"))) {
                throw GradleException("Extension jar contains an AssetChannel class or direct package reference: $path")
            }
        }
    }
}

tasks.check { dependsOn(verifyNoAssetChannelClasses) }
tasks.build { dependsOn(verifyNoAssetChannelClasses) }
