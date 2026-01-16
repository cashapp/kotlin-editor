Release procedure for KotlinEditor

> IMPORTANT. The `recipes/guardrails` project (group: `app.cash.gradle-guard`) configures a `group` and `version` that
> are different from the default for the rest of the repo. See `recipes/guardrails/RELEASING.md` for separate release
> instructions.

1. Update `CHANGELOG.md`.
1. Update `README.md` if needed.
1. Bump version number in `gradle.properties` to next stable version (removing the `-SNAPSHOT` 
   suffix).
1. `git commit -am "chore: prepare for release x.y." && git push`.
1. Publish the snapshot to Maven Central by invoking the `publish` action on github.
1. `git tag -a vx.y -m "Version x.y."`.
1. Update version number `gradle.properties` to next snapshot version (x.y-SNAPSHOT).
1. `git commit -am "chore: prepare next development version."`.
1. `git push && git push --tags`.
