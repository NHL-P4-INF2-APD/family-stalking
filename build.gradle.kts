// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.jetbrainsKotlinAndroid) apply false
  id("io.gitlab.arturbosch.detekt") version "1.23.4"
  id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

detekt {
  buildUponDefaultConfig = true
  config.setFrom(files("$projectDir/config/detekt/detekt.yml"))
  parallel = true
  autoCorrect = true
}

ktlint {
  version.set("1.0.1")
  android.set(true)
  verbose.set(true)
  outputToConsole.set(true)
  reporters {
    reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
  }
  filter {
    exclude("**/generated/**")
    include("**/kotlin/**")
  }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  reports {
    html.required.set(true)
    xml.required.set(true)
    txt.required.set(true)
    sarif.required.set(true)
  }
  jvmTarget = "17"
}

tasks.register("clean", Delete::class) {
  delete(layout.buildDirectory)
}

// Add a task to check code style
tasks.register("checkCodeStyle") {
  group = "verification"
  description = "Run all code style checks"
  dependsOn("ktlintCheck", "detekt")
}
