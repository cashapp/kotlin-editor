Release procedure for Gradle Guard

1. Update `recipes/guardrails/CHANGELOG.md`.
1. Update `README.md` if needed.
1. Bump version number in each `build.gradle.kts` in `recipes/guardrails/` to next stable version (removing the 
   `-SNAPSHOT` suffix).
1. `git commit -am "chore(gradle-guard): prepare for release x.y." && git push`
1. Publish the snapshot to Maven Central by invoking the `publish` action on github.
1. `git tag -a gradle-guard-vx.y -m "Gradle Guard version x.y."`.
1. Update version number in each `build.gradle.kts` in `recipes/guardrails/` to next snapshot version (`x.y-SNAPSHOT`).
1. `git commit -am "chore(gradle-guard): prepare next development version."`.
1. `git push && git push --tags`.
