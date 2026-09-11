# Fietsweer — *welke jas moet mee?*

An Android app that answers one question every morning: **do you need a rain
jacket, a warm jacket, or both, for today's bike commute?**

It is a native rewrite and considerable extension of `fietsweer.html`: the same
multi-model consensus engine, now with temperature, wind chill on the bike,
both commute directions, scheduled notifications, a map-based setup and a home
screen widget.

---

## What it does

**Set up once.** Pick home and work on an OpenStreetMap map (search by name,
drag the pin, or use your GPS location), set your two departure times and your
cycling pace. That is the whole configuration.

**Get told what to wear.** At the times you choose, a notification tells you
what to take. The verdict covers the ride to work *and* the ride home, because
that is the decision you actually make in the morning.

> **Take a rain jacket**
> Rain jacket · Warm jacket
> To work 08:00 — 95% rain · feels like 12°
> Back home 17:30 — dry · feels like 16°

**See the whole day.** Four screens:

| Screen | What is on it |
|---|---|
| **Today** | The verdict, both rides with rain risk / felt temperature / wind, current conditions, a 24-hour rain-and-temperature chart, and the best moment to leave |
| **Forecast** | A 24-hour departure ribbon at 15-minute resolution, the detail table for any departure slot, dry windows, a per-model agreement matrix, a multi-day outlook and source health |
| **Alerts** | Your notification times — any number of them, per weekday, each covering the outbound ride, the return ride or both |
| **Settings** | Locations, departure times, still-air pace, whether the wind changes the ride time, the thresholds that decide "wet" and "cold", theme, language, map style and the widget |

**Widget.** A 4×2 home screen widget with the headline, what to bring and both
rides. Add it from Settings with one tap.

---

## How the verdict is made

For each ride the route is sampled at four points between home and work, and
the ride is walked in five-minute steps: every stretch of road is judged at the
place you will be, at the moment you will be there.

**Rain risk** blends three independent opinions:

1. **Fifteen deterministic models** served by Open-Meteo for your coordinates —
   KNMI Harmonie, DWD ICON-D2/EU, Météo-France AROME, ECMWF IFS and AIFS, UK Met
   Office, NOAA GFS, ECCC GEM, JMA, MET Norway, CMA GRAPES. Models with no data
   for your location simply drop out.
2. **Two ensemble systems** (ICON-D2 and ECMWF IFS, ~69 members) for a genuine
   probability rather than a yes/no.
3. **The Buienradar rain radar nowcast** for the next two hours, which dominates
   the blend for imminent departures and is ignored outside the Benelux.

**Ride time** is not a constant. Your configured pace is the speed you hold in
still air; from it the app derives the power you are producing, then solves the
same power balance again against the wind you will actually meet to get the
speed — and therefore the duration and the arrival time. Head-, cross- and
tailwind all count: a crosswind still raises the airspeed you have to push
through. Three things keep the model honest rather than theoretical:

- Riders do not hold constant power into a gale. Effort is allowed to rise by
  about a quarter into a headwind and drop with the wind behind, which is what
  people actually do.
- A route is not a straight line. The same wandering that makes it a quarter
  longer than the crow flies keeps swinging the wind angle around, so the times
  are averaged over a spread of headings instead of charging a nominal
  "pure headwind" for the whole ride.
- The wind during the ride depends on how long the ride is, so the estimate is
  refined once: a first pass over the still-air window, a second over the window
  that implies.

On a 17 km commute at 19 km/h in still air that works out at roughly +12 minutes
into a force 3, +21 into a force 4 and +37 into a force 5 — and about ten to
fifteen minutes saved with the same wind behind you. Recognisable numbers, which
is the point. It can be switched off in Settings.

There is deliberately no "bike type" setting. Because the power is calibrated
from your own still-air pace, a road bike and an omafiets held at the same speed
come out within a few percent of each other; the knob would look meaningful and
change nothing.

**Felt temperature on the bike** starts from Open-Meteo's apparent temperature
(which already accounts for humidity and radiation) and subtracts the extra
chill you generate yourself. Ambient wind is scaled from 10 m down to bike
height, resolved into head- and cross-components against your travel bearing,
added to your own speed, and charged through the JAG/TI wind chill relation.
A 5 °C day with a 20 km/h headwind is not a 5 °C ride.

**The advice** then follows two thresholds you control in Settings: the rain
risk at which the rain jacket becomes a yes (default 30%), and the bike-felt
temperature below which the warm jacket becomes a yes (default 11 °C). Between
"no" and "yes" there is a "maybe". Gloves, a hat, ice warnings, strong wind and
a water bottle are added when the numbers call for them.

---

## Building

No Android Studio required. You need a JDK 17 and an Android SDK with platform
36 and build-tools 36.0.0; the Gradle wrapper fetches Gradle itself.

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleRelease
```

`build.sh` wraps that up, defaulting `JAVA_HOME` to `~/.local/opt/jdk17` and
`ANDROID_HOME` to `~/Android/Sdk`:

```bash
./build.sh            # release APK
./build.sh debug      # debug APK
./build.sh install    # build release and install on the attached device
```

`emulator.sh` starts a test emulator, assuming an AVD called `fietsweer`.

The APK lands in `app/build/outputs/apk/release/app-release.apk` (~1.5 MB).
Built releases are published on the
[releases page](https://github.com/Willgo97/fietsweer/releases) rather than
committed.

### Signing

Release builds are signed with a self-signed personal key. Neither the keystore
nor its passwords live in this repository, and they never should: a signing key
in a public git history is burned permanently.

The build reads four Gradle properties, from `~/.gradle/gradle.properties` or
the environment:

```properties
FIETSWEER_STORE_FILE=/home/you/.android/keystores/fietsweer-release.jks
FIETSWEER_STORE_PASSWORD=…
FIETSWEER_KEY_ALIAS=fietsweer
FIETSWEER_KEY_PASSWORD=…
```

If they are absent the release build still succeeds and falls back to the debug
key, so a fresh clone is buildable by anyone. Generate your own with:

```bash
keytool -genkeypair -v -keystore ~/.android/keystores/fietsweer-release.jks \
  -alias fietsweer -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=Fietsweer, O=Fietsweer, C=NL"
```

Keep that file safe: losing it means future builds can no longer upgrade an
already installed copy, only replace it.

**Installing:** grab the APK from the
[releases page](https://github.com/Willgo97/fietsweer/releases) and open it on
your phone, or sideload it over USB:

```bash
adb install -r Fietsweer.apk
```

Android will warn that it comes from an unknown source, because it is signed
with a personal key rather than through Google Play.

`minSdk` is 26 (Android 8.0), `targetSdk` 36.

---

## Permissions, and why

| Permission | Why |
|---|---|
| `INTERNET` | Weather, geocoding and map tiles |
| `POST_NOTIFICATIONS` | The whole point of the app |
| `SCHEDULE_EXACT_ALARM` | So a 07:15 notification arrives at 07:15. The app works without it, just less punctually, and says so |
| `RECEIVE_BOOT_COMPLETED` | Alarms do not survive a reboot; this puts them back |
| `ACCESS_COARSE/FINE_LOCATION` | Only for the "use my location" button in the map picker. Never read in the background |

The Alerts screen shows a card for each of these when Android has switched
something off, with a button that opens the right settings page.

---

## Layout of the code

```
app/src/main/java/nl/fietsweer/app/
  data/       Settings + storage, Open-Meteo and Buienradar clients, geocoding,
              the forecast repository
  domain/     Geometry, the scoring engine, the jacket decision, formatting,
              and all user-facing copy (Dutch and English, switchable in-app)
  notify/     Alarm scheduling, the receiver, the fetch-and-notify worker
  widget/     Home screen widget provider, renderer and refresh worker
  ui/         Compose theme, shared components and charts, the slippy map,
              and the four screens plus onboarding
```

A few things worth knowing if you come back to this later:

- **Settings are one JSON blob in SharedPreferences.** Reads are synchronous,
  which is what alarm receivers and workers need.
- **The map is hand-rolled.** Roughly 350 lines of Web-Mercator maths, a
  two-level tile cache and Compose gestures. It draws OpenStreetMap raster
  tiles; dark mode is the same tiles run through an invert + hue-rotate colour
  matrix rather than a second tile server.
- **Forecasts are cached in memory only**, 15 minutes. A cold start refetches;
  it takes a second or two.
- **Times come back from Open-Meteo as unix timestamps**, so nothing depends on
  parsing local-time strings.

## Data and attribution

- Weather: [Open-Meteo](https://open-meteo.com) (CC BY 4.0), forecast and
  ensemble APIs.
- Rain radar: Buienradar nowcast, Netherlands and Belgium only.
- Geocoding: Open-Meteo geocoding for search, Nominatim for reverse lookup.
- Map tiles: © OpenStreetMap contributors. The app sends an identifying
  User-Agent and caches tiles on disk, as the tile usage policy asks. This is
  fine for personal use; a widely distributed build should move to its own tile
  source.
