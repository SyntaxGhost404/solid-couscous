# What the Lake Keeps

A full-screen Android narrative investigation game, built natively in Kotlin with Jetpack Compose.
This is the initial build: an intro sequence, a case board, and two in-game phones.

| Studio ident | Advisory | Title | Case board | Theo's phone | Mira's phone |
|---|---|---|---|---|---|
| ![](docs/screens/1-studio.png) | ![](docs/screens/2-advisory.png) | ![](docs/screens/3-title.png) | ![](docs/screens/4-board.png) | ![](docs/screens/5-theo-phone.png) | ![](docs/screens/6-mira-phone.png) |

## Originality

The six reference screenshots this build was planned from belong to an existing commercial game.
This project keeps only their structure: the screen flow, the kinds of elements on each screen, and
how they behave. Everything you see or hear is original to this project: the studio name and
logo, game title, cast, story details, illustrations, icons, wallpapers, layout composition and
sounds. Treat **wickmoth** and **What the Lake Keeps** as placeholders. They live in
`res/values/strings.xml`.

## What's in the build

1. **Studio ident.** The wordmark writes itself in. The dot of its "i" catches as a candle flame,
   warms the nearby letters, and then gutters out in a curl of smoke.
2. **Headphones advisory.** The headphones settle in and hover while the line fades up beneath them.
3. **Title.** A moonlit lake fades up with a slow push-in. The lantern at the end of the pier
   catches and flickers, with its reflection on the water. Mist drifts, reeds sway, the empty boat
   bobs, stars twinkle, the subtitle tracks in, and a glint passes across the title.
4. **Case board.** Polaroids are pinned up outward from the missing girl, each with a pin sound.
   Threads run out from her pins, notes write themselves in, chalk lines and a marker symbol are
   drawn, and the stranger gets circled. Steam rises from the mug.
   - Tap a polaroid: it swings on its pin (a damped spring) while its threads and notes pulse.
     Tapping Mira sends a ripple through every thread.
   - Tap a desk prop: it lifts and settles.
   - Tap a phone: you pick it up.
5. **Phones (Theo's and Mira's).** The phone lifts off the desk and rotates upright to fill the
   screen, while the board recedes and dims. The screen wakes, the wallpaper settles, the status
   bar lights up, and the icons pop in with a stagger.
   - Icons respond with a press spring.
   - Locked apps (dimmed) shake, with a reject haptic.
   - The ◇ button, system back, or a predictive-back gesture puts the phone back down. During the
     gesture the phone shrinks with your swipe.

Intro screens advance on their own, and a tap skips ahead. App screens beyond the home screens are
not part of this build, so tapping an app gives press feedback only. Taps carry haptics, and a
synthesised lake ambience plays from the title on. The ambience pauses in the background and
respects audio focus.

## Build and run

Requirements: JDK 17+ and an Android SDK with platform 37. The Gradle wrapper fetches everything else.

```sh
./gradlew :app:installDebug          # build and install on a connected device
./gradlew :app:assembleRelease       # R8-shrunk APK at app/build/outputs/apk/release/
```

The release build is signed with the local debug key so it can be sideloaded. Set up your own
upload key before publishing.

Toolchain: AGP 9.4, Kotlin 2.4, Compose BOM 2026.09, minSdk 26, targetSdk 36. The game runs
immersive full-screen and is laid out on a fixed 360 × 800 dp frame (the 9:20 reference). That
frame is scaled uniformly to fit, with full-bleed backgrounds around it on other aspect ratios.

## Tests

```sh
./gradlew :app:testDebugUnitTest      # flow tests on the real activity (Robolectric)
./gradlew :app:recordRoborazziDebug   # also renders every screen and animation frame to app/build/shots/
```

## Art and audio pipelines

Illustrations are generated as vector drawables from Python sources in `tools/art`. Sounds are
synthesised from oscillators and filtered noise in `tools/audio`, so neither uses samples or
stock assets.

```sh
pip install fonttools numpy scipy
python3 tools/art/build.py [--preview]   # rewrites app/src/main/res/drawable/*.xml
python3 tools/audio/synth.py             # rewrites app/src/main/res/raw/*.ogg (needs ffmpeg)
```

`--preview` renders contact sheets to `tools/art/out/` using a local headless Chromium.

## Layout

```
app/src/main/kotlin/com/wickmoth/lakekeeps/
  MainActivity.kt          immersive window, splash hand-off, sound lifecycle
  game/                    stage flow and saved state
  ui/                      design frame, motion helpers, touch and haptics, fonts, palette
  audio/                   sound bank (SoundPool effects, looping ambience)
  screens/studio|advisory|title/
  screens/board/           board layout data, board rendering, desk, board-to-phone scene
  screens/phone/           home screens and the pick-up transition
```

## Licences

Fonts are under the SIL Open Font License: Quicksand, Caveat, Covered By Your Grace, IM Fell
English and Josefin Sans. The licence texts ship in `app/src/main/assets/licenses/`.
