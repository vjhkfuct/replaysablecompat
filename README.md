
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

The build expects compatible upstream jars for:

- `sable-neoforge-*.jar`
- `reforgedplaymod-*.jar`
