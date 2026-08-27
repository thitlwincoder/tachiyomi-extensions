# AGENTS.md

Tachiyomi/Mihon extension repository. Each extension is its own Gradle module under
`src/<lang>/<name>/` (currently only `src/all/manhwamyanmar`). Building produces one
standalone APK per extension.

## Release / build pipeline (CI)
- `.github/workflows/build.yml` triggers on `v*` tags (and `workflow_dispatch`).
- It runs `./gradlew assembleRelease`, then collects every
  `src/**/build/outputs/apk/release/*.apk` (via `find src -path ...`), copies each to
  `apks/<module>.apk` (e.g. `manhwamyanmar.apk`), and publishes them as assets to a
  GitHub release named after the tag via `softprops/action-gh-release`.
- `repo.json` is the extension index. Each entry's `apkUrl` MUST be
  `https://github.com/<owner>/<repo>/releases/download/<tag>/<name>.apk`
  or the installed extension will not find its APK.

## How a module is built (do NOT use the `tachiyomi.extension` plugin)
The legacy `tachiyomi.extension` Gradle plugin and its `maven.tachiyomi.org` /
`repo.mihon.app` repos are **dead** (unreachable), so modules are built with a
self-contained Android module instead:
- `plugins { id("com.android.application") version "8.7.3"; id("org.jetbrains.kotlin.android") version "2.3.0" }`
- `compileOnly("com.github.keiyoushi:extensions-lib:18a8e26be2")` — the keiyoushi v14
  lib (live on JitPack), which provides the `eu.kanade.tachiyomi.*` API.
- Static `src/main/AndroidManifest.xml` + a hand-written `ExtensionGenerated` class
  (see "Extension conventions" below). No `build-logic` composite, no KSP.
- `applicationId` = namespace `eu.kanade.tachiyomi.extension` + suffix `all.manhwamyanmar`
  = `eu.kanade.tachiyomi.extension.all.manhwamyanmar` (MUST match `repo.json` `packageName`).

## Critical gotchas
- **Gradle wrapper must be committed** (`gradlew` + `gradle/wrapper/`, Gradle 8.10.2).
  `gradle/actions/setup-gradle` runs `./gradlew`. Do NOT pin `gradle-version` in the
  setup-gradle step — doing so overrides the wrapper (AGP 8.7.3 needs Gradle ≥ 8.9; the
  old `gradle-version: "8.0"` made the build fail with "Minimum supported Gradle version is 8.9").
- **CI needs `permissions: contents: write`** on the job, or `softprops/action-gh-release`
  fails with `403 Resource not accessible by integration`.
- **Kotlin version is coupled to the lib.** The keiyoushi lib was compiled with Kotlin
  metadata 2.3.0, so the module MUST use Kotlin `2.3.0`; `2.1.x` fails with
  "Incompatible classes were found in dependencies" and the lib API becomes invisible
  (supertypes look `final`). AGP 8.7.3 + Gradle 8.10.2 + JDK 17 is the working combo.
- **D8 warnings about Kotlin metadata are benign.** AGP 8.7.3's R8 predates Kotlin 2.3
  metadata, so it logs "An error occurred when parsing kotlin metadata" while rewriting
  stdlib classes — the build still succeeds and the APK still works.
- **Signing is hardcoded** in each module `build.gradle.kts` (keystore `signingkey.jks`,
  alias `key0`, password `mmanhwapass`) and the keystore is committed — do not "fix" it by
  reading env vars unless you also update CI.
- **Re-running the build:** the tag workflow only fires on tag push. After fixing the
  build, move the `vX.Y.Z` tag to the new commit and force-push it to retrigger the
  release: `git tag -f vX.Y.Z && git push origin vX.Y.Z --force`.
- `settings.gradle.kts` auto-includes modules by walking `src` for `build.gradle.kts`;
  it also sets each ancestor project's `projectDir` (the `:all` parent has no real dir).
  New extensions need no settings change, only a new `src/<lang>/<name>/` folder.

## Extension conventions (keiyoushi v14 API)
- **Manifest meta-data** (under `<application>`): `tachiyomix.name`,
  `tachiyomi.extension.class` = `.ExtensionGenerated`, `tachiyomi.extension.nsfw` = `1`,
  `tachiyomix.contentWarning` = `2` (NSFW), `tachiyomix.extensionLib` = `1.4`, icon
  `@mipmap/ic_launcher` (copy the 5 densities from keiyoushi `core/src/main/res`).
- **`ExtensionGenerated`** extends the source class and overrides `versionId` (an Int).
  Do NOT override `id` — it is `final` in v14 and is computed by the app from
  `name`/`lang`/`versionId` (lowercased name + `/lang/versionId`, first 8 bytes of MD5,
  big-endian, sign bit cleared). `repo.json`'s source `id` must equal that computed value
  or the repo will not link updates to the installed extension.
- **Source class API** (v14): parses take `Response` (not `Document`);
  `latestUpdatesRequest`/`latestUpdatesParse` (NOT `latestMangaRequest`);
  set custom headers via `override fun headersBuilder()` (NOT `override val headers`);
  `GET` comes from `eu.kanade.tachiyomi.network.GET`; `asJsoup()` is NOT in the lib, so
  define a local `fun Response.asJsoup(): Document = Jsoup.parse(body?.string().orEmpty())`.
- The source class must be `open` so `ExtensionGenerated` can extend it.

## Commit Rules
- Always use Conventional Commits format (`type(scope): subject`).
- Keep subjects under 72 characters in the imperative mood.
