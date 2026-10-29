# Catatan Rilis Teleknia

## 1.3 / Arts of Battle - 29 Oktober 2026

Nomor build Maven: `1.3.0`. Versi skema snapshot: `3`.

- Tiga skill khas per kelas: Shield Bash/Rally, Venom Cut/Evasion, Frost Bind/Mana
  Ward melengkapi skill awal. MP, cooldown, tooltip, dan preview rank tersedia.
- Skill point per level sejak level 1; rank 0-3; reset di camp seharga 20 gold
  tanpa mengubah HP/MP. Build terkunci saat combat.
- Burn, Poison, Stun, Shield dengan aturan durasi, non-stacking, dan cooldown yang
  tersimpan. Status dibersihkan ketika encounter berakhir.
- Wolf charge, Guardian defensive stance, Warden fase II, serta fatigue untuk
  mencegah loop Guard/heal tanpa batas. Intent terlihat sebelum aksi.
- Sentinel Plate, Duelist Charm, Warding Charm; satu slot aksesori aktif dan
  critical deterministik berdasarkan giliran.
- Difficulty Story/Standard saat pembuatan karakter dan di camp. Story mengurangi
  damage dan penalti gold tanpa mengurangi reward.
- Tab Skills & Build, statistik aksesori/critical, badge status, tombol cooldown,
  serta shortcut 1-9 dan 0 untuk sepuluh aksi combat.
- CombatService dipisahkan dari generator cerita; definisi skill/status memakai
  JSON. Skema 1/2 tetap dapat dibaca dengan default Standard dan point sesuai level;
  progres, HP musuh, serta giliran tidak direset.

Validasi: 217 pemeriksaan campaign, 75 essentials, 93 combat/build, dan 43 UI.
Pemeriksaan mencakup sembilan skill,
rank/reset, durasi/non-stacking, cooldown, intent, difficulty, replay, migrasi, dan
playthrough tiga kelas pada kedua difficulty. UI diperiksa pada 980 x 680 dan
teks 120%, termasuk upgrade/reset, perubahan difficulty, shield, burn, serta CD.

## 1.2 / Traveler's Essentials - 29 Oktober 2026

Nomor build Maven: `1.2.0`. Versi skema snapshot: `2`.

### Fitur baru

- Tiga slot save manual dan autosave terpisah, lengkap dengan metadata karakter,
  lokasi, level, dan waktu simpan.
- Backup valid per slot, checksum SHA-256, deteksi snapshot rusak, dan tombol
  pemulihan dengan konfirmasi. Save manual tidak ikut tertimpa oleh autosave.
- Migrasi save 1.1 ke manual slot 1 dan autosave tanpa mengubah progres/combat.
- Tutorial lima langkah yang bisa dilewati dan dibuka kembali melalui F1.
- Filter inventory, sorting nama/rarity/jumlah, dan preview statistik equipment.
- Settings bahasa UI Indonesia/Inggris, skala teks 90-120%, fullscreen, serta audio.
- Shortcut F1, F5, F9, F11, Esc, dan 1-8 untuk aksi cerita saat tidak mengetik.
- Penyimpanan preferensi UI/window/tutorial terpisah dari slot karakter.

### Fondasi dan perbaikan

- Backup dan primary ditulis atomik; data rusak tidak dipromosikan menjadi backup.
- Payload save divalidasi dan versi skema masa depan ditolak tanpa ditimpa.
- Definisi item/label aksi dan teks UI dipindahkan ke resource JSON yang terpisah.
- Navigasi Back bertumpuk untuk Saves, Help, dan Settings.
- Workflow CI headless untuk regresi campaign dan pemeriksaan fitur 1.2.
- Narasi, nama dunia, deskripsi item, dan lore belum diterjemahkan; cakupan bahasa
  1.2 adalah UI dan tutorial. Backup snapshot memakai tabel di file SQLite yang sama.

### Validasi

217 pemeriksaan regresi campaign dan 75 pemeriksaan integrasi essentials lulus.
34 pemeriksaan UI lulus untuk tutorial, save/load/recovery, filter, preview equipment, lokalisasi,
shortcut, fullscreen, dan jendela 980 x 680 dengan skala teks 120%.

## 1.1 / The Cinder Crown — 29 Oktober 2026

Versi pengembangan campaign setelah baseline 1.0. Nomor build Maven: `1.1.0`.

### Konten dan fitur baru

- Campaign tiga bab, tujuh lokasi, tiga encounter utama, dan tiga ending.
- Pembuatan karakter dengan nama sendiri dan kelas Fighter, Rogue, atau Mage.
- Combat bergiliran dengan skill kelas, Arcane Bolt, Guard, Mend, potion,
  mana draught, retreat, serta indikator heavy attack musuh.
- Equipment yang memengaruhi attack/defense dan market di Survivor Camp.
- Camp rest, penalti kalah, dan rough rest gratis untuk mencegah progres buntu.
- World Map, Quest Journal, milestone achievement, dan side quest locket Mira.
- Pilihan cerita berdasarkan lokasi serta milestone; hadiah penting sekali ambil.

### Perbaikan

- Snapshot SQLite atomik untuk karakter, inventory, world state, combat, dan log.
- Riwayat pembuka tersimpan dan pertarungan dapat dilanjutkan setelah load.
- Save gagal mempertahankan state sebelumnya pada disk dan memori.
- Aksi memakai ID eksplisit agar kata kunci tidak saling bertabrakan.
- Potion tidak dikonsumsi ketika HP/MP penuh; equipment terkunci saat combat.
- UI combat dua kolom, panel intent musuh, statistik equipment yang sesuai,
  navigasi Adventure, dan tampilan yang bisa digulir pada jendela kecil.
- Nomor versi muncul pada judul jendela dan halaman Home.

### Kompatibilitas save

Save 1.0 mempertahankan karakter, statistik, gold, inventory, dan chat saat migrasi.
Quest campaign baru dimulai dari Burned Village karena versi 1.0 belum menyimpan
milestone quest. Save campaign yang sudah ada tetap dapat dilanjutkan.
Nomor skema snapshot tetap `1`; nomor skema penyimpanan terpisah dari versi game.

### Validasi pengembangan campaign

217 pemeriksaan integrasi: semua kombinasi tiga kelas/tiga ending, transaksi,
combat, recovery, migrasi, dan rollback save. UI diperiksa sampai ukuran 980×680.
Runner: `scripts/check-game.ps1`.

## 1.0 / Baseline

Versi sebelum pengembangan campaign, ditetapkan sebagai baseline 1.0.

- Game RPG chat offline berbasis JavaFX dan narasi lokal sederhana.
- Home, Story, Inventory, Profile, statistik karakter, potion, serta save SQLite.
- Desain pixel dark fantasy dengan sprite sumber 64×64.
- Dua musik chiptune original, enam efek suara, kontrol volume, dan mute.
