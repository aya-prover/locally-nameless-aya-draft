// Copyright (c) 2020-2024 Tesla (Yinsen) Zhang.
// Use of this source code is governed by the MIT license that can be found in the LICENSE.md file.
dependencies {
  api(project(":tools-kala"))
  api(libs.annotations)
  implementation(libs.commonmark)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest)
}
