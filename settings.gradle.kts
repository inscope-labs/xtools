pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/inscope-labs/abx-server-1")
      credentials {
        username = providers.environmentVariable("GH_PACKAGES_USERNAME").orNull
          ?: providers.environmentVariable("GITHUB_ACTOR").orNull
          ?: "token"
        password = providers.environmentVariable("GH_PACKAGES_READ_TOKEN").orNull
          ?: providers.environmentVariable("GITHUB_TOKEN").orNull
      }
    }
  }
}

rootProject.name = "xtools"

include(":app")
