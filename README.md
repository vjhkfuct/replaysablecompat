
# Replay Sable Compat

`Replay Sable Compat` is a small client-side NeoForge compatibility mod that
bridges `ReplayMod` and `Sable` without patching either upstream source tree.

It currently does two focused things:

- Mirrors Sable UDP-delivered clientbound payloads into ReplayMod recording.
- Suppresses Sable UDP activation while ReplayMod is in replay playback.

The project is intentionally minimal so that compatibility fixes can stay
isolated from `Sable`, `ReplayMod`, and other rendering mods.

## Distribution Notes

- Author: `vjhkfuct`
- Java root package: `com.vjhkfuct.replaysablecompat`
- License metadata: `LicenseRef-ReplaySableCompat-Composite-See-LICENSE.md`
- Required runtime mods: `reforgedplaymod`, `sable`
- Upstream notices and license details are documented in [LICENSE.md](LICENSE.md)

## Build

From the project root:

```powershell
.\gradlew.bat build
```

The Gradle build resolves its compile-time dependency on `Sable` from the
public `RyanHCode Maven` repository and does not require sibling workspace jars.

`ReplayMod` integration is implemented through reflection and string-targeted
mixins, so `reforgedplaymod` is still a required runtime mod in-game, but it is
not required on the compile classpath to build this project.
