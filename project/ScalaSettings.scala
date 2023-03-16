trait ScalaSettings {
  val baseSettings = Seq(
    "-deprecation",      // Emit warning and location for usages of deprecated APIs.
    "-encoding",
    "utf-8",             // Specify character encoding used by source files.
    "-feature",          // Emit warning and location for usages of features that should be imported explicitly.
    "-unchecked",        // Enable additional warnings where generated code depends on assumptions.
    "-Xfatal-warnings",  // Fail the compilation if there are any warnings.
    "-Wunused:imports",  // Warn if an import selector is not referenced.
    "-Wunused:patvars",  // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates", // Warn if a private member is unused.
    "-Wunused:locals",   // Warn if a local definition is unused.
  )
}
