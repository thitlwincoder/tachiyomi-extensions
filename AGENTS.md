# AGENTS.md

Tachiyomi/Mihon extension repository. Each extension is its own Gradle module under
`src/<lang>/<name>/` (currently only `src/all/manhwamyanmar`). Building produces one
standalone APK per extension.

## Release / build pipeline (CI)
- `.github/workflows/build.yml` triggers on `v*` tags (and `workflow_dispatch`).
- It runs `./gradlew assembleRelease`, then collects every
  `*/build/outputs/apk/release/*.apk`, renames each by stripping the `src/all/`
  prefix (e.g. `manhwamyanmar.apk`), and publishes them as assets to a GitHub
  release named after the tag via `softprops/action-gh-release`.
- `repo.json` is the extension index. Each entry's `apkUrl` MUST be
  `https://github.com/<owner>/<repo>/releases/download/<tag>/<name>.apk`
  or the installed extension will not find its APK.

## Critical gotchas
- **Gradle wrapper must be committed.** `gradle/actions/setup-gradle` runs
  `./gradlew`, not `gradle`. Without `gradlew` + `gradle/wrapper/` the build dies
  with "No such file or directory" and no release is created.
- **Re-running the build:** the tag workflow only fires on tag push. After fixing
  the build, move the `vX.Y.Z` tag to the new commit and force-push it to retrigger
  the release: `git tag -f vX.Y.Z && git push origin vX.Y.Z --force`.
- **Signing is hardcoded** in each module `build.gradle.kts` (keystore
  `signingkey.jks`, alias `key0`, password `mmanhwapass`) and the keystore is
  committed — do not "fix" it by reading env vars unless you also update CI.
- `settings.gradle.kts` auto-includes modules by walking `src` for `build.gradle.kts`;
  new extensions need no settings change, only a new `src/<lang>/<name>/` folder.

## Commit Rules
- Always use Conventional Commits format (`type(scope): subject`).
- Keep subjects under 72 characters in the imperative mood.

## OPEN BLOCKER (verified failing 2026-08-27)
The `tachiyomi.extension` Gradle plugin (`id("tachiyomi.extension") version "X"` in
module `build.gradle.kts`) cannot be resolved from any repo in
`settings.gradle.kts` `pluginManagement` (maven.tachiyomi.org, repo.mihon.app,
maven.keiyoushi.dev, jitpack). Versions `1.3.2` and `1.4.2` both fail with
"Plugin ... was not found". CI therefore fails at plugin resolution and produces no
APK. Fix by either finding a valid published version+repo, or migrating to the local
`gradle/build-logic` composite build used by current forks (keiyoushi/yuzono) and
updating the module DSL accordingly.
