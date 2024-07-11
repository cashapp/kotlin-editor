plugins {
  id("com.autonomousapps.dependency-analysis")
}

// https://github.com/autonomousapps/dependency-analysis-gradle-plugin/wiki
dependencyAnalysis {
  issues {
    all {
      onAny {
        severity("fail")
      }
    }
  }
}
