# Roadmap Teleknia: 1.2 sampai 2.0

Status: versi 1.2, 1.3, dan 1.4 sudah diimplementasikan. Versi game aktif adalah 1.4.
Bagian 1.5 sampai 2.0 tetap merupakan rencana pengembangan.
Target jumlah konten adalah usulan cakupan rilis, bukan fitur yang sudah tersedia.
Tanggal rilis ditentukan setelah implementasi dan pengujian setiap versi.

## Arah game

Teleknia dikembangkan sebagai RPG cerita desktop offline dengan combat bergiliran,
pilihan yang mengubah dunia, pixel art 64 x 64, dan musik chiptune. Campaign lokal
harus tetap bisa ditamatkan tanpa akun, API AI, atau internet. Multiplayer dan
integrasi AI tidak masuk cakupan roadmap ini.

Baseline 1.1: tiga kelas, tiga bab, tujuh lokasi, tiga encounter utama, tiga ending,
equipment, market, side quest Mira, peta, jurnal, dan satu slot save atomik.

| Versi | Tema | Hasil utama |
| --- | --- | --- |
| 1.2 | Traveler's Essentials | Save lebih aman, onboarding, dan kenyamanan bermain |
| 1.3 | Arts of Battle | Build karakter dan strategi combat yang lebih beragam |
| 1.4 | Beyond Emberwood | Area, NPC, quest bercabang, dan konsekuensi dunia |
| 1.5 | Bonds and Embers | Companion, crafting, serta aktivitas setelah tamat |
| 2.0 | The Age of Rekindling | Campaign lengkap, pilihan lintas bab, dan paket rilis mandiri |

## 1.2 - Traveler's Essentials (selesai)

Tujuan: pemain mudah memulai, memahami aturan, dan menjaga beberapa petualangan.

Fitur utama:

- Tiga slot save manual, satu autosave terpisah, dan backup terakhir yang valid.
  Tampilkan nama, kelas, level, lokasi, dan waktu simpan pada pemilihan slot.
- Save 1.1 diimpor sebagai slot pertama. Pemulihan save rusak menawarkan backup,
  tidak langsung mengganti data dengan karakter baru.
- Tutorial singkat yang bisa dilewati: aksi cerita, combat, equipment, camp, dan
  cara memilih quest. Tambahkan bantuan yang bisa dibuka kembali.
- Inventory dengan filter tipe, sorting nama/rarity, serta perbandingan statistik
  sebelum mengganti equipment.
- Pengaturan skala teks, window/fullscreen, dan shortcut keyboard untuk aksi serta
  navigasi. Audio yang sudah ada masuk ke panel pengaturan yang sama.
- Bahasa Indonesia dan Inggris untuk UI dan tutorial. Terjemahan campaign penuh
  diselesaikan bertahap menuju 2.0.

Fondasi implementasi:

- Pisahkan repository save per slot dari migrasi save lama dan sediakan migrasi
  berurutan berdasarkan versi skema.
- Mulai pisahkan definisi item, aksi, dan teks UI dari controller/service; ini
  memudahkan penambahan konten tanpa memperbanyak kondisi di satu file.
- Jadikan runner pemeriksaan campaign bagian dari workflow CI tanpa membutuhkan
  perangkat audio atau membuka jendela JavaFX.

Syarat selesai: setiap slot terisolasi, save/load saat combat tetap utuh, backup
bisa dipulihkan, dan UI dapat dimainkan dengan keyboard pada 980 x 680.

## 1.3 - Arts of Battle (selesai)

Tujuan: pilihan strategi menentukan hasil pertarungan dan identitas tiap kelas.

Fitur utama:

- Tambah dua skill per kelas sehingga setiap kelas memiliki tiga skill khas.
  Contoh: Fighter mendapat Shield Bash dan Rally; Rogue mendapat Venom Cut dan
  Evasion; Mage mendapat Frost Bind dan Mana Ward.
- Status effect awal: burn, poison, stun, dan shield dengan durasi serta indikator
  yang jelas. Tetapkan aturan stack dan kapan durasi berkurang.
- Skill point per level dan reset build berbiaya di camp. Tampilkan preview
  manfaat sebelum mengalokasikan point.
- Pola musuh lebih beragam: charge, defensive stance, dan fase boss. Intent musuh
  tetap terlihat agar pemain dapat merencanakan aksi.
- Armor dan aksesori dengan bonus berbeda; tooltip menjelaskan attack, defense,
  critical, dan efek tanpa pemain perlu membaca kode.
- Pilihan difficulty Story dan Standard. Story mengurangi hukuman dan damage
  musuh; Standard menjadi acuan balancing.

Fondasi implementasi:

- Pisahkan CombatService dari generator narasi dan buat definisi skill/status
  terstruktur. Jika memakai randomness, simpan seed/state agar save/load tidak
  mengubah hasil giliran yang sama.
- Pertahankan kejelasan biaya aksi: aksi gagal tidak menghabiskan resource atau
  memajukan giliran tanpa penjelasan.

Syarat selesai: semua kelas dapat menamatkan campaign pada kedua difficulty;
status, cooldown, dan giliran tersimpan konsisten; tidak ada kombinasi yang
menghasilkan invulnerability, heal, atau giliran tak terbatas.

## 1.4 - Beyond Emberwood (selesai)

Tujuan: memperluas eksplorasi dan membuat pilihan terasa berpengaruh.

Target konten tambahan di atas rilis sebelumnya:

- Dua kawasan baru, misalnya Frostveil Pass dan Drowned Coast, dengan sedikitnya
  empat lokasi baru secara keseluruhan.
- Enam side quest, empat NPC penting, dua boss opsional, dan sedikitnya 12 item
  baru. Quest memakai karakter dan konflik yang saling terhubung.
- Pilihan dialog bercabang dengan reputasi awal Survivor Camp dan Ember Keepers.
  Reputasi membuka dialog, harga, atau penyelesaian alternatif.
- Resolusi quest lewat negosiasi, penemuan petunjuk, atau combat bila masuk akal;
  tidak semua quest berakhir dengan membunuh musuh.
- Codex berisi monster, karakter, dan lore yang sudah ditemukan.
- Sprite sumber 64 x 64 untuk NPC, musuh, dan item penting baru; musik kawasan
  serta SFX yang tetap konsisten dengan tema 1.1.

Fondasi implementasi:

- Pindahkan definisi lokasi, dialog, quest, reward, dan syarat unlock ke data
  terstruktur. Validasi ID, koneksi peta, serta referensi item saat build.
- Save yang sedang berjalan mendapat akses ke konten baru melalui pintu masuk
  yang sesuai progresnya, tanpa wajib memulai ulang.

Syarat selesai: semua cabang quest dapat diselesaikan, reputasi benar-benar
memengaruhi sedikitnya satu hasil per faksi, dan hadiah unik tidak bisa digandakan.

## 1.5 - Bonds and Embers

Tujuan: memberi hubungan antarkarakter dan alasan kembali bermain setelah tamat.

Fitur utama:

- Tiga companion yang bisa direkrut; maksimal satu companion aktif dalam combat.
  Masing-masing punya peran, skill pendukung, dialog, dan quest pribadi.
- Affinity sederhana meningkat melalui keputusan quest yang relevan dan membuka
  interaksi atau kemampuan, bukan lewat mengulang dialog tanpa batas.
- Crafting di camp dengan delapan resep awal untuk consumable dan upgrade
  equipment. Material berasal dari quest atau encounter yang ditentukan.
- Aktivitas setelah tamat: challenge board dan arena dengan modifier yang jelas.
  Replay encounter tidak mengulang reward unik atau menulis ulang ending.
- Galeri ending dan achievement lintas save, disimpan terpisah dari progres
  campaign. Pemain tetap bisa memulai petualangan dengan kelas lain.
- Peningkatan presentasi: animasi pixel sederhana untuk attack, hit, heal, dan
  status, dengan opsi mengurangi animasi.

Fondasi implementasi:

- Tetapkan aturan companion saat kalah, retreat, load, dan pergantian anggota;
  companion tidak boleh menghapus tantangan kelas utama.
- Pisahkan state campaign yang sudah selesai dari progres challenge pascatamat.
- Harga crafting, pembelian, penjualan, dan reward diuji bersama agar tidak ada
  loop ekonomi dengan keuntungan tak terbatas.

Syarat selesai: semua companion dan resep bisa diperoleh melalui jalur yang
terdokumentasi; arena dapat dimainkan tanpa mengubah hasil cerita; affinity,
material, dan reward bertahan setelah save/load.

## 2.0 - The Age of Rekindling

Tujuan: rilis besar yang menyatukan sistem sebelumnya menjadi petualangan lengkap.

Target total konten, termasuk konten 1.x:

- Lima bab utama, sekitar 18-20 lokasi, dan 6-8 encounter boss utama/opsional.
- Sedikitnya lima ending yang mencerminkan keputusan penting lintas bab,
  reputasi, serta hubungan companion.
- Penutup quest dan arc karakter; setiap ending memberikan epilog yang sesuai
  kondisi dunia, bukan hanya mengganti paragraf terakhir.
- Enam specialization: masing-masing dua pilihan lanjutan untuk Fighter, Rogue,
  dan Mage. Contoh: Guardian/Berserker, Assassin/Ranger, Pyromancer/Arcanist.
- New Game+ opsional yang membawa bonus terpilih; kunci quest, item unik, dan flag
  cerita direset dengan aturan eksplisit agar campaign baru tidak rusak.
- UI dan seluruh campaign tersedia dalam bahasa Indonesia dan Inggris.
- Portrait, efek combat, musik area/boss, dan sprite 64 x 64 yang konsisten.
- Paket Windows dengan runtime Java bawaan dan launcher; pemain tidak perlu
  memasang JDK atau Maven. Sediakan portable build serta installer yang diuji.

Tahap menuju rilis:

1. Alpha: jalur campaign dan migrasi save sudah lengkap, lalu hentikan penambahan
   sistem besar sampai jalur tersebut stabil.
2. Beta: uji balancing, cabang cerita, aksesibilitas, performa, dan instalasi pada
   lingkungan Windows tanpa JDK.
3. Release candidate: perbaikan bug, audit seluruh aset, dan verifikasi migrasi
   dari save 1.1 sampai 1.5 sebelum memberi label 2.0.

Syarat selesai: setiap ending bisa dicapai melalui jalur sah; semua kelas dan
specialization dapat menamatkan game; New Game+ tidak menggandakan item unik;
paket mandiri bisa dipasang, dijalankan, diperbarui, dan dihapus dengan benar.

## Urutan prioritas dan aturan rilis

- Urutan pengerjaan: 1.2 -> 1.3 -> 1.4 -> 1.5 -> 2.0. Sistem yang dipakai rilis
  berikutnya harus stabil sebelum konten yang bergantung padanya dikembangkan.
- Untuk setiap versi, tuntaskan fitur utama dan syarat selesai sebelum menambah
  fitur tambahan. Pecah pekerjaan menjadi tiket kecil yang bisa diuji sendiri.
- Fitur tambahan yang memperbesar scope dipindah ke patch atau rilis berikutnya;
  jangan mengorbankan kompatibilitas save untuk mengejar jumlah fitur.
- Skema save memiliki versi sendiri. Backup dibuat sebelum migrasi dan save
  lama tidak dihapus ketika migrasi gagal.
- Pertahankan pengujian campaign 1.1 sebagai regresi; tambah skenario untuk fitur
  baru. Jumlah test saja tidak cukup tanpa cakupan jalur cerita dan kegagalan.
- Nomor versi UI, Maven, README, dan changelog diperbarui ketika rilis terkait
  benar-benar siap. Penyusunan roadmap ini tidak mengubah versi aplikasi.
- Tidak ada jadwal kalender yang dijanjikan. Estimasi dibuat setelah lingkup
  tiap rilis dipecah dan kebutuhan aset, balancing, serta pengujian dinilai.

## Langkah pertama yang disarankan

Lanjutkan ke 1.5 dengan merancang companion dan aturan pergantian anggota sebelum
mengembangkan quest pribadi serta crafting. Fondasi world/data/quest/codex 1.4 dan
UI RPG berbasis pilihan sudah tersedia; input chat custom sudah dihapus.
