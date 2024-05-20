// Copyright (c) 2020-2023 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
import org.aya.gradle.CommonTasks

CommonTasks.nativeImageConfig(project)

dependencies {
  api(project(":base"))
  api(project(":parser"))
  api(libs.gson)
  implementation("com.javax0.sourcebuddy:SourceBuddy:2.5.0")
  implementation(project(":producer"))
  implementation(libs.commonmark)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest)
  testImplementation(project(":cli-console"))
}

tasks.named<Test>("test") {
  testLogging.showStandardStreams = true
  testLogging.showCauses = true
  val resources = projectDir.resolve("src/test/resources")
  resources.mkdirs()
  inputs.dir(resources)
}

tasks.register<JavaExec>("runCustomTest") {
  group = "Execution"
  classpath = sourceSets.test.get().runtimeClasspath
  mainClass.set("org.aya.test.TestRunner")
}
