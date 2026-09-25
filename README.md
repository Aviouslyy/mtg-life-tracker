# Onyx — MTG Life Counter

A polished, simple life counter for Magic: The Gathering on Android.

## Download

**[Download the latest APK](https://github.com/aviouslyy/mtg-life-tracker/releases/latest/download/mtg-life-tracker.apk)**

Open the downloaded file on your phone to install it. Android will ask you to allow installs from your
browser or file manager the first time. Requires Android 8.0 or newer.

## How to use

The controls follow [Carbon](https://apps.apple.com/us/app/carbon-mtg-tabletop-utility/id1209153225),
the MTG life counter a lot of players liked:

- **Tap above** a life total to gain 1, **tap below** it to lose 1. **Press and hold** for 10.
  A badge beside the total shows how much it just changed.
- **Swipe down** on a player (or tap their name) to open their counters: poison, energy, experience,
  commander tax, storm, the monarch and the city's blessing, plus **commander damage from each opponent**.
  Commander damage also comes off the life total. Counters in use show as small badges beside the life total.
- A player whose life reaches 0, who has 10 poison, or who has taken 21 damage from one commander is
  dimmed and marked **OUT**.
- **Tap the gold medallion** in the middle to:
  - choose 2–6 players and 20, 30 or 40 starting life
  - roll a d6 or d20, flip a coin, or randomly pick who goes first
  - open the **game history**, where quick taps are merged ("−3", not "−1 −1 −1")
  - start a new game

Panels turn to face each player: with two players the top panel is upside down, and with three or more
the phone lies between two rows of players. The screen stays on during a game, and the current game is
saved if you leave the app.

## Building

Every push is built by GitHub Actions (`.github/workflows/build.yml`), and every build of `main` is
published as a GitHub release.

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

## Credits

The app uses the [Outfit](https://github.com/Outfitio/Outfit-Fonts) typeface, licensed under the
SIL Open Font License 1.1 (see `licenses/OFL-Outfit.txt`).
