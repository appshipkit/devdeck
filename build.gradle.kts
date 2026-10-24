import kotlinx.validation.KotlinApiBuildTask
import kotlinx.validation.KotlinApiCompareTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.binary.compatibility.validator)
}

val ktlint: Configuration = configurations.create("ktlint")

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

val ktlintSources = listOf("**/src/**/*.kt", "**/*.gradle.kts", "!**/build/**")

val ktlintFormat = tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fixes Kotlin code style with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args(listOf("-F") + ktlintSources + listOf("--relative"))
    // Rewrites sources in place, so it always runs.
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Checks Kotlin code style with ktlint."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(ktlintSources + listOf("--relative", "--reporter=plain", "--reporter=checkstyle,output=build/reports/ktlint.xml"))
    inputs.files(
        fileTree(rootDir) {
            include("**/src/**/*.kt", "**/*.gradle.kts")
            exclude("**/build/**", ".*/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("sources")
    outputs.file(layout.buildDirectory.file("reports/ktlint.xml")).withPropertyName("report")
    mustRunAfter(ktlintFormat)
}

// Binary compatibility: the public API of each published module is recorded in <module>/api/<module>.api.
// `./gradlew apiCheck` fails when it changes; `./gradlew apiDump` records an intended change.
// The validator plugin only hooks into the standalone kotlin-android plugin, not AGP's built-in Kotlin,
// so its tasks are wired here against each library's release Kotlin classes.
apiValidation {
    ignoredProjects += "sample"
}

val publishedModules = setOf("devdeck", "devdeck-okhttp", "devdeck-timber", "devdeck-appdistribution")
val apiValidatorRuntime = libs.bundles.binary.compatibility.validator.runtime

subprojects {
    if (name !in publishedModules) return@subprojects

    val validatorClasspath = configurations.create("binaryCompatibilityValidator") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    dependencies { apiValidatorRuntime.get().forEach { add(validatorClasspath.name, it) } }

    val dumpFileName = "$name.api"
    val dumpFile = layout.projectDirectory.file("api/$dumpFileName")
    val apiBuild = tasks.register<KotlinApiBuildTask>("apiBuild") {
        description = "Builds the public API dump of the release variant."
        val classesDir = tasks.named<KotlinCompile>("compileReleaseKotlin").flatMap { it.destinationDirectory }
        inputClassesDirs.from(classesDir)
        // Compose compiler output whose names change with every lambda edit; not callable API.
        ignoredClasses.addAll(
            classesDir.map { dir ->
                dir.asFileTree.matching { include("**/ComposableSingletons$*.class") }.files.map {
                    it.relativeTo(dir.asFile).path.removeSuffix(".class").replace(File.separatorChar, '.')
                }
            },
        )
        outputApiFile.set(layout.buildDirectory.file("api/$dumpFileName"))
        runtimeClasspath.from(validatorClasspath)
    }
    val apiCheck = tasks.register<KotlinApiCompareTask>("apiCheck") {
        group = "verification"
        description = "Checks the public API against the committed dump in api/."
        projectApiFile.set(dumpFile)
        generatedApiFile.set(apiBuild.flatMap { it.outputApiFile })
    }
    tasks.register<Copy>("apiDump") {
        group = "other"
        description = "Records the current public API in api/."
        from(apiBuild.flatMap { it.outputApiFile })
        into(layout.projectDirectory.dir("api"))
    }
    plugins.withId("com.android.library") {
        tasks.named("check") { dependsOn(apiCheck) }
    }
}
