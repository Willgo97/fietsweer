# Fietsweer — *welke jas moet mee?*

An Android app for the bike commute: do you need a rain jacket, a warm jacket,
or both?

Pick home and work on a map, set your departure times and pace. At the times
you choose, a notification says what to bring for the ride there and back.
Three tabs (Today, Forecast, Settings) and a home screen widget.

## How it decides

- **Rain:** fifteen Open-Meteo models, two ensembles and the Buienradar
  nowcast, sampled along the route at the place and moment you will be there.
- **Ride time:** your pace is your still-air speed; the wind changes it via a
  simple power balance.
- **Warmth:** apparent temperature minus the wind chill of your own speed, at
  the coldest moment of the ride.

## Building

JDK 17 and an Android SDK with platform 36.

```bash
./build.sh            # release APK, copied to ../uitgaven/Fietsweer.apk
./build.sh debug
./build.sh install    # release, then adb install
```

Signing: the keystore lives in `../sleutels`, its passwords in
`~/.gradle/gradle.properties` (`FIETSWEER_STORE_FILE`, `_STORE_PASSWORD`,
`_KEY_ALIAS`, `_KEY_PASSWORD`); without them the build uses the debug key.

Releases: [github.com/Willgo97/fietsweer/releases](https://github.com/Willgo97/fietsweer/releases).

## Data

Weather by [Open-Meteo](https://open-meteo.com) (CC BY 4.0), radar by
Buienradar, search by Open-Meteo and Nominatim, map tiles © OpenStreetMap
contributors.

## Licence

Public domain ([Unlicense](https://unlicense.org)).
