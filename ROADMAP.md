# Roadmap Teleknia: 1.2 sampai 1.10

Status: versi 1.2 sampai 1.5 sudah diimplementasikan. Versi game aktif adalah 1.5.
Versi 1.6-1.10 merupakan rencana; belum diimplementasikan. Rencana rilis besar sebelumnya dihapus sementara.
Target jumlah konten adalah usulan cakupan rilis, bukan fitur yang sudah tersedia.
Tanggal rilis ditentukan setelah implementasi dan pengujian setiap versi.

## Arah game

Teleknia dikembangkan sebagai RPG cerita desktop offline dengan combat bergiliran,
pilihan yang mengubah dunia, pixel art 64 x 64, dan musik offline. Campaign lokal
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
| 1.6 | Living Journeys | Quest companion lebih dalam dan dunia yang bereaksi |
| 1.7 | Atlas of the Borderlands | UI scroll RPG, minimap, dan perjalanan melalui peta |
| 1.8 | Paths of Mastery | Specialization dan strategi combat lanjutan |
| 1.9 | Echoes of the Crown | Kelanjutan campaign dan dungeon pascatamat |
| 1.10 | Traveler's Edition | Lokalisasi lengkap, polishing, dan distribusi Windows |

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
  diselesaikan bertahap hingga 1.10.

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

## 1.5 - Bonds and Embers (selesai)

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

## 1.6 - Living Journeys (rencana)

Tujuan: memperdalam hubungan dan eksplorasi yang sudah ada sebelum menambah sistem besar.

Target fitur:

- Animasi narasi/dialog berupa teks muncul satu per satu di panel biru pada
  sketsa pengguna. Klik di area panel biru menampilkan seluruh teks saat itu
  seketika. Klik percepat tidak menghabiskan giliran atau otomatis memilih jawaban.
- Jika percakapan dibagi beberapa halaman, klik berikutnya setelah teks selesai
  melanjutkan halaman; pilihan jawaban aktif setelah halaman terakhir selesai.
  Mempercepat teks tidak melewati keputusan atau memberi reward lebih awal.
- Keyboard menyediakan aksi tampilkan teks/lanjut halaman yang setara. Reduce
  animations menampilkan teks langsung. Riwayat menyimpan narasi lengkap, bukan
  substring animasi. Save/load tidak menggandakan halaman atau hasil keputusan.
- Tambah kelanjutan dialog untuk Mira, Sera, Orin, Marin, dan Ilyra: sebelum quest,
  saat quest berjalan, setelah resolusi, setelah ending, serta tanggapan companion
  aktif terhadap kejadian penting. Target minimal empat percakapan kontekstual
  baru per karakter (20 total), dengan cabang sesuai keputusan yang relevan.
- Konteks mencakup reputasi faksi, pilihan obat/peta/persediaan, hasil damai atau
  combat, affinity, dan quest pribadi. Dialog hanya muncul saat syaratnya cocok;
  pengulangan tetap tidak memberi affinity, material atau reward tambahan.
- Perluas tiga quest pribadi companion menjadi rangkaian minimal tiga tahap per
  companion: percakapan, tujuan lapangan, dan keputusan akhir. Penyelesaian quest
  pribadi 1.5 tetap dihormati; hadiah/bonus lama tidak dicabut atau diberikan ulang.
- Tambah percakapan kontekstual setelah milestone penting, dengan respons terhadap
  reputasi dan keputusan pemain. Dialog berulang tetap tidak memberi affinity.
- Tambah enam event eksplorasi sekali ambil di lokasi lama, dengan pilihan dan
  konsekuensi yang tercatat dalam jurnal.
- Camp menampilkan perubahan setelah rescue, perbaikan sinyal, distribusi obat,
  dan penyelesaian quest companion.
- Jurnal menampilkan tujuan berikutnya, lokasi, dan alasan quest masih terkunci.
- Pindahkan definisi companion, resep, dan challenge ke JSON tervalidasi agar
  pengembangan konten tidak terus menambah kondisi dalam kode.

Syarat selesai: seluruh cabang quest pribadi dapat diselesaikan, save 1.5 mendapat
akses tanpa kehilangan affinity/bonus, dan event tidak menggandakan material.
Uji save/load pada setiap tahap quest dan kegagalan transaksi reward. Animasi
teks dan klik cepat harus dapat dipakai tanpa melewati jawaban atau memicu aksi
ganda. Tombol travel lama tetap tersedia selama 1.6; pemindahan ke peta pada 1.7.

## 1.7 - Atlas of the Borderlands (rencana)

Tujuan: redesain layar RPG dan jadikan peta sebagai tempat khusus perjalanan,
sesuai dua sketsa referensi pengguna.

Komposisi layar utama (gambar pertama):

- Bingkai luar bertekstur scroll/perkamen tua, tepian usang dan ornamen RPG;
  warna kuning pada sketsa menunjukkan material kertas, bukan blok kuning polos.
- Panel besar atas (merah muda pada sketsa) menjadi adegan lokasi, karakter dan
  battle. Gunakan identitas visual lokasi saat ini.
- Panel biru di tengah adalah area narasi/dialog dengan animasi teks versi 1.6.
  Klik pada area ini mempercepat atau menuntaskan teks; bukan memilih aksi lain.
- Panel kiri bawah menjadi pilihan aksi/dialog/combat. Tidak memuat tombol
  perjalanan antar lokasi setelah navigasi peta baru tersedia.
- Panel kanan bawah menjadi minimap: gambar sesuai lokasi aktif, saturasi gambar
  diturunkan agar tulisan **MAP** di tengah tetap jelas. Tambahkan penanda lokasi
  aktif, border RPG dan petunjuk bahwa panel bisa diklik.

Peta terbuka (gambar kedua):

- Klik minimap membuka panel peta dengan animasi scroll/perkamen terbentang,
  lalu menampilkan jaringan lokasi di dalam kertas. Tombol tutup mengembalikan
  pemain ke adegan sebelumnya. Reduce animations menampilkan peta langsung.
- Setiap lokasi memakai ilustrasi sesuai wilayahnya di dalam tile berbingkai
  RPG (arah bentuk heksagonal seperti contoh), bukan satu gambar untuk semua node.
- Garis menghubungkan node sesuai rute dunia sebenarnya. Lokasi aktif diberi
  tanda yang berbeda dari lokasi lain yang telah dieksplorasi.
- Lokasi yang belum dikunjungi tetap berupa siluet hitam/fog; jangan membocorkan
  ilustrasi, nama, lore atau isi quest. Jalur yang belum diketahui ikut tersembunyi.
- Agar eksplorasi pertama tetap mungkin, rute bertetangga yang sudah terbuka
  menampilkan node misterius gelap yang dapat dipilih dengan label "Jelajahi".
  Identitas lokasi terungkap sesudah pemain benar-benar tiba di sana.
- Klik node yang terhubung dan memenuhi syarat memindahkan lokasi. Lokasi jauh
  yang sudah dikunjungi tidak otomatis mengizinkan teleport; navigasi mengikuti
  rute bertetangga dan gate quest yang berlaku.
- Node yang terkunci tidak dapat dipilih; tampilkan penjelasan syarat yang aman
  tanpa membocorkan isi cerita. Selama battle/percakapan, peta boleh dilihat,
  tetapi perjalanan dinonaktifkan dengan alasan yang jelas.
- Tombol aksi perjalanan dan filter Travel di area dialog dipindahkan ke peta;
  seluruh akses peta menggunakan aturan perjalanan yang sama. Keyboard dapat
  memfokuskan dan memilih node; warna bukan satu-satunya penanda status.

Inventory RPG (empat referensi inventory pengguna):

- Ganti daftar kartu vertikal dengan grid **5 kolom x 6 baris**, 30 slot terlihat
  per halaman. Item stack menampilkan jumlah di sudut; slot kosong tetap terlihat.
  Filter/sort tetap tersedia. Jika jenis item lebih dari 30, gunakan pagination,
  bukan menghapus item atau memberlakukan batas kapasitas baru pada save lama.
- Ikuti tata letak sketsa terakhir: panel biru di kiri menjadi pratinjau karakter
  full-body/paper doll dan equipment; panel hijau di kanan menjadi grid inventory.
  Warna sketsa adalah penanda area, bukan warna akhir. Gunakan perkamen tua,
  border berornamen dan slot bertekstur sesuai konsep RPG pada referensi ketiga.
- Slot equipment mengelilingi karakter; item yang dipasang muncul sebagai ikon
  pada slot yang sesuai. Untuk sistem sekarang tampilkan Weapon, Armor dan
  Accessory. Jangan menambahkan slot Head/Shoulder/dll yang belum punya mekanik.
  Equip/unequip memperbarui ikon, penanda item dan statistik langsung.
- Klik kanan item membuka context menu: Equip jika equipment belum dipakai,
  Unequip jika sedang dipakai, serta Remove. Consumable juga memiliki Use;
  item non-equipment tidak menampilkan Equip. Keyboard menyediakan context menu
  atau tombol aksi setara, tanpa bergantung pada mouse kanan saja.
- Hover item sekitar satu detik membuka popup detail: ikon/nama, tipe, rarity,
  deskripsi, jumlah, efek/statistik, status equipped, dan perbandingan terhadap
  equipment aktif. Popup tetap berada dalam layar dan hilang saat pointer pindah,
  halaman berubah atau item dihapus. Fokus keyboard memberi detail yang setara.
- Remove meminta konfirmasi dan jumlah untuk stack, menjelaskan bahwa item dibuang
  tanpa gold dan tidak bisa dikembalikan lewat aksi undo. Item quest/progres dan
  material yang dibutuhkan progres tidak boleh dibuang sehingga quest tidak buntu.
  Equipment aktif harus di-unequip sebelum dibuang; context menu menjelaskannya.
- Unequip mengosongkan slot dan mengembalikan statistik dasar tanpa menghapus item
  dari inventory. Tidak memberi heal/MP gratis. Pergantian equipment, penggunaan
  consumable dan pembuangan mengikuti pembatasan combat/ending yang berlaku;
  aksi inventory tidak boleh menjadi jalan melewati aturan giliran battle.
- Preview awal memakai sprite karakter kelas dan ikon item pixel 64x64. Equipment
  terpasang wajib terlihat pada slot paper doll; mengganti tampilan baju/senjata
  pada tubuh karakter memerlukan layer sprite tersendiri dan bukan render 3D
  otomatis dari gambar referensi. Pertahankan gaya pixel RPG yang konsisten.

Syarat selesai: seluruh lokasi dapat dijangkau melalui peta tanpa tombol travel
lama; fog tidak mengunci eksplorasi pertama atau membocorkan lokasi rahasia;
klik berulang selama animasi tidak memicu perjalanan ganda. Uji gate quest,
battle, percakapan, save/load visited, minimum 980x680, teks 120%, dan Reduce
animations. Semua node, garis dan border tetap berada di dalam perkamen.
Inventory juga diuji pada lebih dari 30 jenis item, stack, sort/filter, hover delay,
keyboard, equip/unequip, pembatalan Remove, proteksi quest/material, dan save/load.
Penghapusan/equipment disimpan atomik: kegagalan save tidak menghilangkan item.
Panel paper doll dan grid harus tetap terbaca pada ukuran minimum/teks 120%.


## 1.8 - Paths of Mastery (rencana)

Tujuan: memberi identitas build yang lebih kuat tanpa membuat companion wajib.

Target fitur:

- Enam specialization: Fighter memilih Guardian/Berserker, Rogue memilih
  Assassin/Ranger, Mage memilih Pyromancer/Arcanist.
- Satu specialization aktif setelah level 5 dan milestone guardian. Masing-masing
  membuka satu passive dan satu skill aktif; perubahan dilakukan di camp dengan
  biaya gold yang ditampilkan sebelum konfirmasi.
- Enam variasi encounter dengan intent dan kombinasi status berbeda, serta tiga
  challenge baru yang menguji build, bukan sekadar menaikkan HP.
- Preview damage, biaya, cooldown dan efek specialization di menu Build.
- Rebalance delapan resep dan support companion berdasarkan hasil pengujian;
  hindari skill yang meniadakan cooldown, fatigue atau seluruh incoming damage.

Syarat selesai: ketiga kelas dapat menyelesaikan campaign pada kedua difficulty,
baik tanpa companion maupun dengan companion. Tidak ada loop heal, stun atau
shield tanpa batas. Specialization tersimpan konsisten saat combat/save/load;
perubahan build tidak mengisi HP/MP secara gratis.

## 1.9 - Echoes of the Crown (rencana)

Tujuan: melanjutkan cerita dengan konsekuensi yang mengikuti pilihan sebelumnya.

Target konten tambahan:

- Dua bab lanjutan, sehingga total lima bab; enam lokasi baru di dua kawasan,
  sehingga target total 17 lokasi.
- Tiga boss baru dan empat side quest yang melibatkan companion serta dua faksi
  yang sudah ada. Pertahankan opsi penyelesaian damai bila konflik mendukungnya.
- Kelanjutan setelah tiga ending lama, masing-masing memakai kondisi dunia yang
  berbeda. Ending lama tetap tercatat; kelanjutan memerlukan pilihan eksplisit.
- Dua ending lanjutan sebagai entri galeri tambahan, tanpa mengganti tiga ending
  campaign awal. Sediakan akses kedua ending melalui pilihan yang terdokumentasi.
- Satu dungeon pascatamat dengan tiga lantai, checkpoint per lantai, modifier
  terlihat, serta pemisahan hadiah pertama dan replay.
- Portrait/sprite baru mengikuti sumber 64x64; musik dan efek mengikuti konteks
  lokasi/battle yang sudah dipakai.

Syarat selesai: semua cabang ending lama dapat memasuki kelanjutan, keputusan
lintas bab memiliki dampak nyata, dungeon tidak menulis ulang ending, dan reward
unik tidak bisa diulang melalui retreat atau checkpoint. Save tamat dari versi
lama tetap dapat dimainkan tanpa memulai ulang.

## 1.10 - Traveler's Edition (rencana)

Tujuan: membuat versi desktop nyaman dimainkan dan mudah dipasang.

Target fitur dan pekerjaan rilis:

- Bahasa Indonesia/Inggris lengkap untuk UI, narasi, quest, dialog, item dan lore.
  Nama karakter pemain tetap literal dan pilihan bahasa tidak mengubah flag quest.
- Polish layout, kontras, tooltip, animasi dan navigasi keyboard; uji ukuran minimum
  980x680, skala teks 90-120%, fullscreen dan Reduce animations.
- Opsi New Game+ setelah campaign lanjutan: mulai karakter baru dengan kosmetik,
  galeri dan achievement profil. Level, gold, inventory, material, quest, affinity,
  serta flag hadiah direset; tidak membawa bonus combat pada rilis awal fitur ini.
- Ekspor/impor backup campaign beserta profil dengan validasi versi/checksum dan
  preview slot yang akan diganti. Simpan backup sebelum impor, gabungkan unlock
  profil yang valid, dan jangan mengeksekusi isi file backup.
- Paket Windows portable dengan runtime Java bawaan, launcher dan petunjuk update.
  Installer menjadi target tambahan setelah paket portable lolos uji komputer
  tanpa JDK/Maven.
- Dokumentasi pemain, catatan migrasi, audit dependensi/aset distribusi, serta
  pengujian regresi campaign, companion, crafting dan arena.

Syarat selesai: paket portable berjalan pada Windows tanpa Java terpasang;
seluruh teks kedua bahasa dapat dibaca; save 1.1-1.9 dapat dimigrasi; ekspor/impor
serta rollback diuji. New Game+ tidak menggandakan item, reward atau bonus combat.

## Batas cakupan

- Tahapan ini tetap untuk desktop JavaFX offline. Port browser/GitHub Pages adalah
  pekerjaan terpisah, belum termasuk komitmen rilis 1.6-1.10.
- Tanggal rilis belum ditetapkan. Jumlah konten di atas adalah target perencanaan
  dan baru disebut tersedia setelah implementasi serta pengujian selesai.
- Musik yang dipakai secara lokal tidak otomatis menjadi aset distribusi publik;
  paket publik harus menggunakan aset yang hak distribusinya sudah jelas.

## Urutan prioritas dan aturan rilis

- Urutan pengerjaan: 1.6 -> 1.7 -> 1.8 -> 1.9 -> 1.10. Sistem yang dipakai rilis
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

Mulai 1.6 dengan memindahkan definisi companion/resep/challenge ke data tervalidasi,
lalu implementasikan animasi teks panel biru, kelanjutan dialog kontekstual,
quest pribadi dan jurnal. Desain minimap/perkamen dikerjakan pada 1.7. Pertahankan tes regresi 1.5 dan buat skenario
migrasi sebelum menambahkan tahap quest baru. Nomor versi game tetap 1.5 sampai
implementasi berikutnya benar-benar selesai.
