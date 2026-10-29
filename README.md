# Teleknia: Ashes of the Northern Gate

Game RPG desktop dark fantasy berbasis chat. Petualangan offline terdiri dari
**3 bab, 7 lokasi, 3 encounter utama, dan 3 ending**. Cerita memakai aturan dan
state lokal; tidak membutuhkan API AI atau koneksi internet saat dimainkan.

## Menjalankan

Butuh **JDK 21** dan Maven. Pastikan `java -version` dan `mvn -version` sama-sama
menunjukkan Java 21, lalu jalankan dari folder proyek:

```powershell
mvn clean javafx:run
```

Maven mengunduh dependensi pada build pertama. Folder `target` otomatis dibuat.

## Memulai petualangan

1. Pilih **Start New Story**, isi nama (1-24 karakter), lalu pilih Fighter, Rogue, atau Mage.
2. Cari persediaan dan selamatkan Mira di Burned Village.
3. Pergi ke Northern Gate dan hadapi Ash Wolf untuk membuka jalan ke Emberwood.
4. Kunjungi Survivor Camp untuk istirahat, berdagang, dan berbicara dengan Mira.
5. Telusuri shrine, arsip di Sunken Ruins, dan Cinder Tower untuk menuntaskan misteri Crown.

Memulai cerita baru menggantikan satu slot campaign aktif; dialog memberitahukan
hal tersebut sebelum karakter dibuat. **Continue Story** memuat progres terakhir,
termasuk HP musuh dan giliran jika berhenti saat bertarung.

## Fitur

- **Karakter:** nama bebas, tiga kelas dengan statistik dan skill berbeda.
- **Fighter:** 100 HP, 30 MP, Power Strike berbasis attack dan armor musuh.
- **Rogue:** 88 HP, 30 MP, Shadow Strike berbasis attack dan agility; critical pada
  serangan di setiap giliran tempur keempat jika agility minimal 10.
- **Mage:** 80 HP, 55 MP, Ember Surge berbasis intelligence dan mengabaikan armor.
- **Combat:** attack, class skill (6 MP), Arcane Bolt (10 MP), Guard, Mend (8 MP),
  potion, mana draught, dan retreat. Musuh membalas setiap aksi tempur yang valid.
- **Heavy attack:** setiap giliran musuh ketiga. Guard membagi dua damage (minimum 1)
  dan memulihkan hingga 6 MP. Intent musuh terlihat di panel combat.
- **Pemulihan:** Small Potion +35 HP, Mana Draught +25 MP. Tidak dikonsumsi saat
  resource penuh. Di combat, meminum potion menggunakan satu giliran.
- **Kalah:** kembali ke camp dengan setengah HP/MP, kehilangan 20% gold, tanpa hadiah.
  Retreat kehilangan hingga 5 gold. Musuh pulih saat encounter dimulai lagi.
- **Level up:** setiap 100 EXP; menaikkan statistik dan memulihkan HP/MP.
- **Equipment:** sword dan armor benar-benar memengaruhi damage/defense; pilih Equip
  di Inventory. Equipment dan penggunaan item bebas tidak tersedia selama combat.
- **Market:** empat barang dijual di Survivor Camp; transaksi memeriksa gold,
  kepemilikan equipment, lokasi, dan status combat.
- **Rest:** istirahat pertama gratis, berikutnya 8 gold untuk pemulihan penuh.
  Jika gold kurang, rough rest gratis memulihkan hingga setengah maksimum HP/MP.
- **World Map:** tujuh lokasi, status kunjungan, dan travel untuk rute yang terbuka.
- **Quest Journal:** milestone utama, side quest locket Mira, field notes, panduan
  combat, penghitung encounter/lokasi, dan achievement.
- **Ending:** Dawnkeeper, The Silent Watch, atau Sovereign of Ash. Purification
  membutuhkan Sun Sigil dan archive. Clue yang terlewat masih bisa diambil setelah
  Warden dikalahkan, sebelum memilih ending. Setelah ending, dunia menjadi epilog.
- **Hadiah sekali ambil:** pencarian, quest, dan encounter tidak bisa diulang untuk
  menggandakan gold, EXP, atau item. Aksi tak dikenal tidak memberi hadiah.

Klik **World** dari Story, Inventory, atau Profile untuk membuka tab **World Map**,
**Quest Journal**, dan **Camp Market**. Input bebas mendukung ID aksi serta alias
terbatas seperti `serang`, `bertahan`, `lari`, `sembuh`, `istirahat`, dan `help`.
Hanya aksi yang tersedia pada kondisi saat ini yang dijalankan.

## Penyimpanan dan kompatibilitas

Save SQLite: `data/rpg_chat_game.db`, relatif terhadap folder aplikasi dijalankan.
Tabel `campaign_save` berisi snapshot versioned lengkap: karakter, inventory,
equipment, flags, lokasi, combat, pilihan, jurnal, ending, dan seluruh riwayat chat.
Satu transaksi menyimpan semuanya. State memori baru diterapkan setelah commit
berhasil; kegagalan menyimpan tidak menimpa progres sebelumnya.

Save MVP lama dibaca sekali dari tabel legacy. Karakter, statistik, item, gold,
dan riwayat chat dipertahankan. Karena MVP tidak memiliki milestone quest yang
tersimpan, alur quest baru dimulai dari Burned Village. Tabel legacy tetap ada.

## Pixel art dan audio

Enam belas sprite PNG sumber **64 x 64** berada di `src/main/resources/sprites`.
`PixelAssets` merender pada skala integer dengan smoothing dimatikan. Beberapa
item/lokasi baru menggunakan sprite kategori yang sama. Prompt dan asal aset
tercatat di `sprites/GENERATION.md`.

Dua musik chiptune original (`embers`, `journey`) dan enam SFX diputar menggunakan
Java Sound API. Playback berjalan di worker terpisah; audio dilepas saat keluar.
Jika perangkat audio tidak tersedia, game tetap berjalan tanpa suara.
Kontrol Music, SFX, dan Mute ada di semua halaman; pengaturan tersimpan di
`data/audio.properties`. Semua audio offline dan tanpa sampel pihak ketiga.

Regenerasi WAV dengan Python standard library:

```powershell
python scripts/generate_audio.py
```

## Pemeriksaan otomatis

Dengan JDK 21 dan dependensi Maven yang sudah terunduh:

```powershell
./scripts/check-game.ps1
```

Runner ini mengompilasi main/test source dan menjalankan `CampaignChecks` tanpa
JUnit/Surefire tambahan. Pemeriksaan menjalankan semua kombinasi kelas/ending,
batas transaksi, combat, recovery, quest gates, save/load pertarungan, migrasi
legacy, dan kegagalan commit memakai trigger SQLite. Database uji terisolasi di
`target/campaign-checks`; save pemain tidak disentuh.

## Struktur

- `controller/`: Home, Story, Inventory, Profile, Adventure (map/journal/market).
- `model/`: Character, CampaignState, Enemy, inventory, choices, story logs/responses.
- `service/`: GameService, stateful LocalStoryGeneratorService, ItemCatalog,
  CharacterService, InventoryService, AudioService.
- `repository/`: snapshot atomik dan repository legacy untuk migrasi.
- `ui/`: PixelAssets, StatusPanel, AudioControls.
- `src/main/resources/`: CSS, sprite 64px, dan WAV.
- `src/test/java/`: integration checks tanpa dependensi test tambahan.

`OpenAiStoryGeneratorService` tetap placeholder dan tidak dipakai runtime.
