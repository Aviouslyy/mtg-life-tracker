# MTG Life Tracker

A clean, simple life counter for Magic: The Gathering on Android.

## Download

**[Download the latest APK](https://github.com/aviouslyy/mtg-life-tracker/releases/latest/download/mtg-life-tracker.apk)**

Open the downloaded file on your phone to install it. Android will ask you to allow installs from your
browser or file manager the first time. Requires Android 8.0 or newer.

## How to use

- **Tap** the right half of a player's panel to gain 1 life, the left half to lose 1.
- **Press and hold** to gain or lose 10.
- A small badge above the total shows how much it just changed.
- **Tap a player's name** to rename them or pick a different color.
- **Tap the ⚙ button** in the middle to:
  - choose 2–6 players and 20, 30 or 40 starting life
  - roll a d6 or d20, or flip a coin
  - randomly pick who goes first
  - start a new game

Panels turn to face each player: with two players the top panel is upside down, and with three or more
the phone lies between two rows of players. The screen stays on during a game, and the current game is
saved if you leave the app.

## Building

Every push is built by GitHub Actions (`.github/workflows/build.yml`), and every build of the default
branch is published as a GitHub release.

To build locally you need JDK 17 and the Android SDK:

```sh
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

### Signing

Release builds are signed with `app/release.keystore`, which is committed so that every build, including
CI builds, has the same signature and installs as an update over the previous one. That key is public,
so it only suits a sideloaded hobby app. Before publishing to the Play Store, generate a private keystore
and supply its passwords through the `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS` and
`SIGNING_KEY_PASSWORD` environment variables.
