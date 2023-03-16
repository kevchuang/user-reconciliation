addCommandAlias("fmt", "scalafmt; Test / scalafmt; sFix;")
addCommandAlias("fix", "scalafix OrganizeImports; Test / scalafix OrganizeImports")
addCommandAlias("check", "scalafmtCheck; Test / scalafmtCheck; fix")
