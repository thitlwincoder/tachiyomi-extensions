# thitlwincoder Extensions

Unofficial [Tachiyomi](https://github.com/tachiyomiorg/tachiyomi)/[Mihon](https://github.com/mihonapp/mihon) extension repository.

## Extensions

| Extension | Language | Notes |
|-----------|----------|-------|
| Manhwa Myanmar (Adult) | `my` | NSFW — adult manhwa from `adult.manhwamyanmar.com` |

## Install in Mihon / Tachiyomi

1. Open the app → **Settings → Extensions**.
2. Under **Extension repos** (or **Sources → Add repo**), add:
   `https://github.com/thitlwincoder/tachiyomi-extensions`
3. The extension above will appear — install **Manhwa Myanmar (Adult)**.

> **Content warning:** this repo ships an NSFW source. Enable content warnings in the app if prompted.

## Build from source

Requires **JDK 17** and the committed Gradle wrapper (`./gradlew`, Gradle 8.10.2).

```bash
./gradlew assembleRelease
```

The signed APK lands at `src/all/manhwamyanmar/build/outputs/apk/release/`.

CI builds and publishes the APK to a GitHub release automatically whenever a `v*` tag is pushed (see `AGENTS.md` for build/CI details and gotchas).
