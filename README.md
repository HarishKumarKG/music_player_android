# Harish — Music Player for Android

A modern, offline music player for Android built with **Kotlin** and **Jetpack Compose**.
It plays the songs already on your phone, with favourites, playlists, albums, folder browsing,
a rich notification, a sleep timer and polished light & dark themes.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.09.00-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-30-blue)
![targetSdk](https://img.shields.io/badge/targetSdk-37-blue)

---

## Features

### Library
- **Four tabs:** All songs · Playlists · Folders · Browse
- **Songs** – every song on the device with album art, sorted by *date modified* or *name (A–Z / Z–A)*
- **Favourites** – tap the ♥ on any song
- **Playlists** – create, rename, delete, add/remove songs and **drag to reorder**
- **Albums** – grouped automatically, with artwork
- **Folders** – every folder that contains music, sortable by name, song count or last update
- **Browse** – file-manager style navigation through internal storage and SD card, with breadcrumbs
- **Fast start-up** – the song list is cached and shown instantly, then refreshed in the background
- Pull new downloads in any time with the **refresh** button

### Playback
- **Play queue** that follows the list you played from (sort order or **shuffle**)
- **"Up next" sheet** – see the queue and jump to any song
- **Mini player** – tap to open, swipe left/right for next/previous, tap or drag the bar to seek
- **Full-screen player** – large artwork, blurred-artwork background, seek slider, shuffle and queue
- **Resumes where you left off** – last queue, song and position are restored on the next launch

### Notification & system integration
- Media notification with **artwork, ⏮ ⏪10s ⏯ ⏩10s ⏭** and a draggable progress bar
- **MediaSession**: lock-screen controls, Bluetooth / headset / car buttons, Android media panel
- **Audio focus** – pauses for calls and other apps, resumes after short interruptions
- **Pauses when headphones are unplugged** or Bluetooth disconnects
- **Built for long sessions** – foreground service + partial wake lock, broken files are skipped

### Sleep timer
- 15 / 30 / 45 min, 1 h, 2 h, **end of current song** or a **custom** time (5 min – 4 h)
- Music **fades out over the last 30 seconds**, then playback stops and the app closes
- Live countdown in the player and in the menu

### Look & feel
- **Light, Dark and System** themes, with a quick ☀️/🌙 toggle in the header
- Animated **splash screen** and animated **"H" logo** with a sound-wave crossbar
- Side menu with a profile card (rotating ring, photo slideshow)
- Edge-to-edge layout, predictive back support

---

## Screenshots

> Add screenshots to `docs/screenshots/` and reference them here, for example:
>
> `![Songs](docs/screenshots/songs.png)`

---

## Tech stack

| Area | Library / tool |
|---|---|
| Language | Kotlin 2.4.20 |
| UI | Jetpack Compose (BOM 2026.09.00), Material 3 |
| Navigation | Navigation Compose 2.10.2 |
| Dependency injection | Hilt 2.60.1 (KSP 2.3.11), androidx.hilt 1.4.0 |
| Async | Kotlin Coroutines & Flow (`StateFlow`, `SharedFlow`) |
| Lifecycle | AndroidX Lifecycle 2.11.0 (`collectAsStateWithLifecycle`) |
| Media | `MediaPlayer`, platform `MediaSession` + `Notification.MediaStyle` |
| Storage | `MediaStore` (songs), JSON files in app storage (cache, library, last played), `SharedPreferences` (theme) |
| Build | Gradle 9.8.0, Android Gradle Plugin 9.4.0 (built-in Kotlin), version catalog |
| Tests | JUnit 4 |

**SDK:** `minSdk 30` (Android 11) · `targetSdk / compileSdk 37` · Java 17

---

## Architecture

Layered **clean architecture** with unidirectional data flow:
`ui` → `domain` ← `data`, and a separate `playback` layer for the background service.

```
com.harish.mediaplayer
├── MediaPlayerApp.kt            Application (Hilt), restores the last played state
├── MainActivity.kt              Theme, splash, navigation host
├── domain/                      Pure Kotlin – no Android UI
│   ├── model/                   Song, SortOrder, Playlist, LibraryData, ThemeMode, PlaybackState
│   └── library/                 Grouping logic: albums, folders, folder sort, browse tree
├── data/
│   ├── song/                    SongRepository (MediaStore + JSON cache), SongJson
│   ├── library/                 LibraryRepository (favourites & playlists)
│   ├── settings/                ThemeRepository
│   └── playback/                PlaybackPersistence (last queue / song / position)
├── playback/
│   ├── MediaPlayerService.kt    Foreground service: playback, notification, MediaSession,
│   │                            audio focus, sleep timer
│   ├── PlayerController.kt      Single entry point the UI uses to control playback
│   └── PlaybackStateHolder.kt   Shared playback state (StateFlow) for service + UI
└── ui/
    ├── home/                    Home screen, header, ViewModel, UI state
    ├── library/                 Tabs, song row, dialogs, playlists, reorderable list
    ├── player/                  Mini player, full-screen player, queue, sleep timer
    ├── navigation/              Destinations, nav host, side menu
    ├── themepicker/             Theme screen
    ├── permission/              Media/notification permission screen
    ├── common/                  Artwork loading & caching, formatters
    └── brand/ splash/ theme/    Logo, colours, typography, backgrounds
```

**How playback flows**

```
UI ──PlayerController──▶ MediaPlayerService ──▶ PlaybackStateHolder (StateFlow) ──▶ UI
                                │
                                ├──▶ Notification / MediaSession (lock screen, Bluetooth)
                                └──▶ PlaybackPersistence (saved every few seconds)
```

---

## Getting started

### Requirements
- **Android Studio** (latest stable) with **Android SDK Platform 37**
- **JDK 17+** (Android Studio's bundled JDK works)
- A device or emulator running **Android 11 (API 30) or newer** with some music on it

### Build & run
```bash
git clone https://github.com/HarishKumarKG/media_player_android.git
cd media_player_android
./gradlew assembleDebug          # or press ▶ Run in Android Studio
```

Install on a connected device:
```bash
./gradlew installDebug
```

### Run the unit tests
```bash
./gradlew testDebugUnitTest
```
Tests cover sorting, album/folder grouping, the browse tree and the play queue / shuffle logic.

---

## Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_AUDIO` (Android 13+) / `READ_EXTERNAL_STORAGE` (Android 11–12) | Read the songs on the device |
| `POST_NOTIFICATIONS` | Show the playback notification (Android 13+) |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Keep playing in the background |
| `WAKE_LOCK` | Keep playing with the screen off |

No internet permission – the app works fully offline and nothing leaves the device.

---

## Data stored on the device

All files live in the app's private storage and are removed when the app is uninstalled.

| File | Contents |
|---|---|
| `songs_cache.json` | Last scanned song list (instant start-up) |
| `library.json` | Favourites and playlists |
| `last_playback.json` | Queue, current song, position, shuffle |
| `settings` (SharedPreferences) | Selected theme |

---

## Tips

**Music stops in the background on some phones?**
Some manufacturers (Xiaomi, Oppo, Vivo, OnePlus, Samsung…) add extra battery savers.
Allow the app to run in the background (e.g. *Battery → No restrictions / Unrestricted*) –
see [dontkillmyapp.com](https://dontkillmyapp.com) for step-by-step guides.

**Stopping playback from the notification:** pause, then swipe the player away.

---

## Roadmap ideas
- Search
- Equalizer
- Lyrics
- Home-screen widget
- Auto-scroll while dragging in long playlists

---

## Author

**Harish** — Mobile App Developer (Android · iOS · Flutter · KMP · .NET MAUI)
GitHub: [@HarishKumarKG](https://github.com/HarishKumarKG)

## License

No license has been chosen yet, so all rights are reserved by default.
Add a `LICENSE` file (for example MIT or Apache 2.0) if you want others to use the code.
