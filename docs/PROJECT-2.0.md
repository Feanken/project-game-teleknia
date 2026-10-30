# Project 2.0 — tahap 1: fondasi

Status: tahap 1 diimplementasikan. Build pengembangan `2.0.0-alpha.1`, judul
`2.0-dev.1`. Ini belum rilis gameplay 2D. UI 1.8 tetap dapat dimainkan untuk
memeriksa regresi selama fondasi eksplorasi dibuat. JDK 21 + JavaFX tetap dipakai.

## Baseline dan pemulihan

- Baseline 1.8: commit `9ac60f6c0d28f04d4ffd176e2cacb392956eee88`, working tree bersih saat diarsipkan.
- Backup lokal: `backups/project-2.0-stage-1/teleknia-1.8-source.zip`.
- Save: `backups/project-2.0-stage-1/rpg_chat_game.db`, disalin memakai SQLite backup API,
  bukan copy mentah database terbuka. `PRAGMA integrity_check` menghasilkan `ok`.
- Preferensi audio/UI ikut disalin. SHA-256 arsip, DB dan preferensi ada di
  `backups/project-2.0-stage-1/manifest.json`. Folder ini diabaikan Git, tetap ada
  saat `mvn clean`, dan tidak memuat hasil build sementara.
- Pemulihan source: ekstrak ZIP ke folder terpisah lalu jalankan dengan JDK 21.
  Untuk memakai save backup, tutup game dahulu, salin backup DB ke `data/` pada
  folder hasil ekstraksi. Simpan DB aktif ke nama lain sebelum penggantian.
  Jangan mengekstrak baseline menimpa pengerjaan Project 2.0.

## Batas kepemilikan state

```text
UI eksplorasi / UI battle / menu
             ↓ command (action ID)
        GameService (facade)
             ↓
        CampaignEngine.advance(snapshot, action)
             ↓ candidate + response + feedback
        SaveGameRepository.save(candidate)
             ↓ jika commit berhasil
        publish currentState → refresh UI / audio
```

`CampaignEngine` sekarang menjalankan aturan aksi, perubahan resource, loot,
level, quest, choices dan log pada salinan state. Tidak mengimpor JavaFX, SQL
atau repository, dan tidak menulis file. Candidate boleh dibuang tanpa mengubah
state aktif. `GameService.performAction` memakai engine ini dan tetap menyimpan
sebelum memublikasikan hasil. Kegagalan save tidak menerapkan kandidat.

`GameService.snapshot()` adalah salinan mendalam untuk layar baru. Controller
lama masih memakai `getCurrentState()` sebagai jembatan kompatibilitas; jangan
memakainya untuk mutasi dari controller baru. Engine awal/new game dan operasi
inventory/build tetap melalui facade GameService. Refactor ini bukan perubahan
seluruh API menjadi immutable: model save dan respons masih mutable, tetapi
candidate/snapshot tidak berbagi objek dengan state aktif.

`GameplayMode` berasal dari state: BATTLE jika enemy aktif, EPILOGUE jika ending
ditutup, DIALOGUE jika percakapan aktif, selain itu EXPLORATION. Menu jeda adalah
overlay UI, bukan mode campaign yang ditulis ke save. Generator choices lama
sudah menggunakan mode ini; layar baru wajib mengikuti sumber yang sama.

## Sistem yang digunakan kembali

| Sistem | Pemilik/aturan yang dipertahankan | Integrasi 2D berikutnya |
| --- | --- | --- |
| Campaign, cabang, ending | LocalStoryGeneratorService, WorldService, WorldContent | NPC/objek mengirim action ID yang sudah divalidasi |
| Quest, reputasi, discovery | CampaignState flags, journal, codex | Tampilan map membaca flags, tidak memberi reward sendiri |
| Combat, status, intent | CombatService | Battle screen hanya memilih aksi dan menampilkan hasil |
| Specialization, skill | MasteryService, SkillCatalog | Skill dan biaya tetap dari katalog |
| Inventory/equipment | InventoryService, ItemCatalog, facade GameService | Menu overlay menggunakan transaksi yang sama |
| Companion/crafting/arena | BondService, LivingService | Camp/encounter menjadi titik akses, gate tetap berlaku |
| Save/backup/galeri | SaveGameRepository, skema 6 | Posisi eksplorasi ditambah dengan migrasi pada tahap save berikutnya |
| Wilayah/rute | WorldContent, AtlasService | Map ID memakai ID lokasi; portal tetap memeriksa gate rute |
| Audio | AudioService | UI merutekan musik sesuai region/enemy; engine tidak memutar suara |

Hindari duplikasi EXP, gold, HP, flags atau inventory ke state khusus renderer.
Posisi interpolasi, kamera, tombol ditekan, efek transisi dan animasi adalah state
sementara presentasi; progress dunia tetap pada campaign.

## Kontrak eksplorasi tahap 2

- Top-down empat arah; grid logis, bukan free movement diagonal.
- Tile dan aset native 64×64; satu koordinat dunia adalah satu tile.
  `(0,0)` kiri atas; X ke kanan, Y ke bawah. `WorldRules.Tile` dan `Direction`
  menyediakan koordinat UI-independent.
- Kecepatan awal empat tile/detik (`STEP_MILLIS=250`). Interpolasi visual memakai
  delta waktu; hasil gerak dikomit ke tile tujuan setelah langkah selesai.
- Renderer JavaFX Canvas untuk lapisan map; kontrol/dialog/menu tetap JavaFX nodes.
  Kamera mengikuti pemain, diklem di batas map. Sampling nearest-neighbor,
  skala integer, posisi render dibulatkan; map kecil dipusatkan.
- Viewport mengikuti ruang tersedia; pada minimum 980×680 target awal area dunia
  960×512 (15×8 tile pada 1×), HUD/dialog sebagai overlay. Ukuran bukan syarat
  minimum map. Renderer dan kamera baru dikerjakan di tahap 2.
- Layer: ground → objects → aktor menurut posisi kaki/Y → foreground → HUD.
- Collision statis memakai grid `blocked`; NPC/musuh menambah occupancy dinamis.
  Keluar batas dianggap terhalang. Jangan menyimpulkan collision dari warna sprite.
- Gerak dinonaktifkan saat dialog, menu jeda, battle atau transisi. Satu langkah
  dan satu interaksi aktif sekaligus; input tertahan tidak menggandakan transaksi.

| Input rencana | Konteks |
| --- | --- |
| WASD / panah | Berjalan dan arah menghadap pada eksplorasi |
| E / Enter | Interaksi tile di depan; lanjut/konfirmasi dialog |
| Esc | Menu jeda; kembali/tutup overlay sesuai konteks |
| M / I | Peta / inventory saat eksplorasi |
| Mouse | Menu dan pilihan; klik-untuk-berjalan dapat ditambah setelah pathfinding siap |
| F1 / F5 / F9 / F11 | Help / save / load / fullscreen dipertahankan |

Kontrak ini belum mengubah shortcut layar legacy. WASD tidak boleh memicu game
ketika fokus ada pada input teks. Saat kembali dari battle, gunakan tile aman
sebelum encounter, hindari spawn di collider musuh dan retrigger langsung.

## Data map

`world/MapDefinition` adalah record JSON-ready tervalidasi dan defensively copied.
Format awal 1; dimensi maksimum 256 per sumbu dan total 16.384 tile. Tile layers
berisi ID atlas integer nonnegatif; registry atlas/aset dan loader map dibangun di
tahap 2. Setiap daftar flat wajib sepanjang `width * height`, indeks `y*width+x`.
ID 0 adalah sel kosong/transparan. Collision tidak bergantung pada ID atlas.

```json
{
  "format": 1, "id": "village", "width": 3, "height": 2,
  "ground": [1,1,1,1,1,1], "objects": [0,0,0,0,0,2],
  "foreground": [0,0,0,0,0,0], "blocked": [false,false,false,false,false,true],
  "spawns": [{"id":"entry","tile":{"x":0,"y":0},"facing":"DOWN"}],
  "portals": [{"id":"to_gate","tile":{"x":2,"y":0},"targetMap":"gate","targetSpawn":"entry"}],
  "interactions": [{"id":"mira","kind":"NPC","tile":{"x":1,"y":1},"action":"npc_mira","sprite":"mira"}]
}
```

Ini contoh kontrak, bukan map desa playable. Jenis interaction: NPC, CHEST,
INSPECT, ENCOUNTER. Binding memakai action ID existing, bukan Java class/script.
Spawn/portal wajib berada pada tile yang walkable; ID unik per jenis. Interaksi
dapat berada pada tile terhalang karena diperiksa dari tile di depannya.
Validasi lintas map (target spawn, atlas ID, sprite, action dan gate quest) menjadi
tanggung jawab loader/registry tahap 2; constructor saat ini memvalidasi bentuk,
ukuran, koordinat dan identifier. Pemicu portal tidak boleh melewati AtlasService.

Skema save tetap 6 dalam tahap 1. Belum ada posisi default baru, reset campaign,
atau migrasi otomatis ke map yang belum tersedia. Saat sistem posisi masuk tahap
berikutnya, save lama dipetakan ke spawn aman per lokasi dengan validasi dan backup.

## Checklist Project 2.0

- [x] Tahap 1: baseline + backup, peta sistem, pemisahan pemrosesan aksi,
  snapshot/mode dan kontrak map/kontrol.
- [ ] Tahap 2: renderer, animasi gerak, collision, kamera dan portal.
- [ ] Tahap 3: interaksi NPC/objek dan dialog overlay.
- [ ] Tahap 4: encounter dan battle screen tersendiri, weakness/stagger.
- [ ] Tahap 5: Burned Village → Mira → battle pertama → camp yang bereaksi.
- [ ] Tahap 6: menu overlay, HUD, posisi save dan migrasi eksplorasi.
- [ ] Tahap 7: seluruh 11 lokasi dan integrasi konten.
- [ ] Tahap 8: regresi, playtest, performa dan dokumentasi rilis.

Validasi: `scripts/check-game.ps1` mencakup suite regresi 1.8 dan FoundationChecks
(isolasi candidate/snapshot, mode, kontrak map, round trip JSON dan input invalid).
`scripts/check-ui.ps1` memeriksa UI legacy yang tetap berjalan pada build fondasi.

Hasil tahap 1: 1.311 pemeriksaan regresi game + 23 fondasi + 107 UI lulus.
Backup source dan save juga lolos verifikasi ulang SHA-256, CRC ZIP dan SQLite
integrity check. Suite berjalan pada database terisolasi di `target/`.
