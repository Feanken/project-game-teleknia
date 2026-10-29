# Interactive RPG Chat Story Game

MVP desktop game JavaFX bertema dark fantasy RPG. Player bermain melalui chat interaktif, sementara Game Master lokal menghasilkan narasi, pilihan aksi, dan konsekuensi sederhana tanpa API atau koneksi internet saat aplikasi berjalan.

## Tech Stack

- Java 21
- Maven
- JavaFX
- SQLite
- Jackson untuk JSON parsing pilihan cerita

## Cara Run

Pastikan Java 21 dan Maven sudah tersedia di PATH.

```bash
mvn clean javafx:run
```

Alternatif build:

```bash
mvn clean package
```

Database save lokal otomatis dibuat di:

```text
data/rpg_chat_game.db
```

Path tersebut relatif terhadap folder project, jadi tidak ada hardcoded absolute path.

## Fitur MVP

- Home Page dengan tombol Start New Story, Continue Story, Inventory, dan Character Profile.
- Story Chat Page dengan chat log Game Master, player message, system message, pilihan aksi, dan custom action field.
- Panel status karakter: HP bar, MP bar, EXP bar, level, dan gold.
- Character Profile berisi name, class, level, EXP, HP, MP, Attack, Strength, Agility, Intelligence, Defense, Gold, Current Location, dan Current Quest.
- Inventory card/list dengan item type, rarity, description, effect, quantity, dan tombol Use untuk consumable.
- Small Potion memulihkan 25 HP.
- Save/load memakai SQLite untuk character, inventory, story logs, dan game state.
- Story Generator lokal deterministic sederhana, mudah diganti dengan OpenAI API nanti.

## Gameplay Awal

Start New Story membuat karakter default:

- Name: Kael
- Class: Fighter
- Level: 1
- EXP: 0
- HP: 100 / 100
- MP: 30 / 30
- Strength: 10
- Agility: 8
- Intelligence: 6
- Defense: 8
- Gold: 20
- Location: Burned Village
- Quest: Find the source of the attack

Item awal:

- Rusty Sword x1
- Small Potion x2
- Old Map x1

Opening story:

```text
You wake up in a burned village. Smoke rises from broken houses, and a strange roar echoes from the northern gate.
```

## Struktur Project

```text
src/main/java/com/rpgchatgame/
  MainApp.java
  controller/
    HomeController.java
    StoryController.java
    InventoryController.java
    ProfileController.java
  model/
    Character.java
    Item.java
    InventoryItem.java
    GameState.java
    StoryLog.java
    Choice.java
    StoryResponse.java
  service/
    GameService.java
    StoryGeneratorService.java
    LocalStoryGeneratorService.java
    OpenAiStoryGeneratorService.java
    InventoryService.java
    CharacterService.java
  repository/
    DatabaseManager.java
    CharacterRepository.java
    InventoryRepository.java
    StoryLogRepository.java
  ui/
    StatusPanel.java
  util/
    JsonUtil.java
    Constants.java

src/main/resources/styles/
  fantasy.css
```

## Database Schema

SQLite dibuat otomatis oleh `DatabaseManager`:

- `characters`: data utama karakter.
- `inventory_items`: item inventory beserta quantity.
- `story_logs`: log chat Game Master, Player, dan System.
- `game_state`: story summary, current location, current quest, dan choices aktif dalam JSON.

## Rencana Integrasi OpenAI API

Integrasi disiapkan melalui interface:

```java
StoryGeneratorService
```

Saat ini implementasi aktif:

```java
LocalStoryGeneratorService
```

Untuk integrasi OpenAI:

1. Implementasikan logic API di `OpenAiStoryGeneratorService`.
2. Kirim `GameState`, action player, inventory, status karakter, dan story summary sebagai context.
3. Minta response JSON dengan struktur kompatibel `StoryResponse`.
4. Ganti wiring di `GameService` dari `LocalStoryGeneratorService` ke `OpenAiStoryGeneratorService`.
5. Simpan API key melalui environment variable, bukan hardcoded di source code.

Runtime MVP tetap bisa berjalan offline karena tidak memanggil API eksternal.

## Pixel UI redesign

All four screens use the ember/jade pixel theme in `fantasy.css`.
Sixteen native **64 x 64 PNG** sprites live in `src/main/resources/sprites`.
`PixelAssets` caches images and renders them at integer scales with smoothing disabled.
Home and Profile use 2x sprites (128 x 128 on screen); their source files remain 64 x 64.
The Profile page scrolls at small window sizes, and Story automatically scrolls to the latest message.

Asset generation: built-in imagegen, one 4 x 4 atlas, cropped into cells and
resampled with nearest-neighbor interpolation. Generation prompt is preserved in
`src/main/resources/sprites/GENERATION.md`.

## Music and sound

The game includes two original looping chiptune instrumentals: `embers.wav` for
Home and `journey.wav` for Story. Inventory and Profile keep the current track.
Six sound effects accompany buttons, story actions, damage, item/gold rewards,
potion consumption, and level ups. When an action levels up the character, the
level-up fanfare takes priority over damage/reward sounds.

The footer on every screen contains **Music**, **SFX**, and **Mute** controls.
Settings persist to `data/audio.properties`, independently of the game save.
Default music volume is 30% and SFX volume is 60%. Zero volume stops that audio
channel; unmuting resumes music. If no audio device is available, the game stays
playable silently and logs a warning.

Audio uses the JDK's Java Sound API; there is no extra dependency or online service.
All device access and playback run on a dedicated worker, and clips are closed on exit.
The bundled mono PCM16 WAVs are in `src/main/resources/audio`.
They were composed and synthesized specifically for this project with no external
samples. To regenerate the exact assets using Python's standard library:

```powershell
python scripts/generate_audio.py
```
