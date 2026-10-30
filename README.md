# Teleknia 1.7: Atlas of the Borderlands

Game RPG desktop dark fantasy berbasis pilihan, dengan adegan lokasi dan portrait
yang bisa diklik. Petualangan offline terdiri dari **3 bab utama, 11 lokasi,
5 encounter (2 opsional), 6 side quest baru, dan 3 ending utama**. Cerita memakai aturan dan
state lokal; tidak membutuhkan API AI atau koneksi internet saat dimainkan.

Versi saat ini: **1.7** (build Maven `1.7.0`). Versi sebelum pengembangan campaign
ditetapkan sebagai **1.0**. Rincian perubahan ada di [CHANGELOG.md](CHANGELOG.md).
Rencana versi 1.2 sampai 1.10 tersedia di [ROADMAP.md](ROADMAP.md).

## Menjalankan

Butuh **JDK 21** dan Maven. Pastikan `java -version` dan `mvn -version` sama-sama
menunjukkan Java 21, lalu jalankan dari folder proyek:

```powershell
mvn clean javafx:run
```

Maven mengunduh dependensi pada build pertama. Folder `target` otomatis dibuat.

## Memulai petualangan

1. Pilih **New Journey / Petualangan Baru**, isi nama (1-24 karakter), lalu pilih Fighter, Rogue, atau Mage.
2. Cari persediaan dan selamatkan Mira di Burned Village.
3. Pergi ke Northern Gate dan hadapi Ash Wolf untuk membuka jalan ke Emberwood.
4. Kunjungi Survivor Camp untuk istirahat, berdagang, dan berbicara dengan Mira.
5. Telusuri shrine, arsip di Sunken Ruins, dan Cinder Tower untuk menuntaskan misteri Crown.

Memulai cerita baru hanya menggantikan autosave; tiga slot manual tetap aman.
**Continue Story** memuat autosave terakhir, termasuk HP musuh dan giliran saat
berhenti bertarung. Gunakan **Menu > Saves** atau **F5/F9** untuk memilih checkpoint manual.
Tutorial lima langkah bisa ditampilkan saat membuat karakter, dilewati, atau
dibuka lagi kapan saja melalui **Help / F1**.

## Fitur

- **Karakter:** nama bebas, tiga kelas dengan statistik dan skill berbeda.
- **Fighter:** 100 HP, 30 MP, Power Strike berbasis attack dan armor musuh.
- **Rogue:** 88 HP, 30 MP, Shadow Strike berbasis attack dan agility; critical pada
  serangan di setiap giliran tempur keempat jika agility minimal 10.
- **Mage:** 80 HP, 55 MP, Ember Surge berbasis intelligence dan mengabaikan armor.
- **Combat:** attack, tiga skill per kelas, Arcane Bolt (10 MP), Guard, Mend (8 MP, cooldown 2),
  potion, mana draught, dan retreat. Aksi valid menjalankan pola musuh; charge, bertahan, atau stun dapat melewati serangannya.
- **Heavy attack:** setiap giliran musuh ketiga. Guard membagi dua damage (minimum 1)
  dan memulihkan hingga 6 MP. Intent musuh terlihat di panel combat.
- **Pemulihan:** Small Potion +35 HP, Mana Draught +25 MP. Tidak dikonsumsi saat
  resource penuh. Di combat, meminum potion menggunakan satu giliran.
- **Kalah:** kembali ke camp dengan setengah HP/MP, kehilangan 20% gold pada Standard atau 10% pada Story, tanpa hadiah.
  Retreat kehilangan hingga 5 gold pada Standard atau 2 pada Story. Musuh pulih saat encounter dimulai lagi.
- **Level up:** setiap 100 EXP; menaikkan statistik dan memulihkan HP/MP.
- **Equipment:** sword, armor, dan aksesori benar-benar memengaruhi damage/defense; pilih Equip
  di Inventory. Equipment dan penggunaan item bebas tidak tersedia selama combat.
- **Market:** tujuh barang dijual di Survivor Camp; transaksi memeriksa gold,
  kepemilikan equipment, lokasi, dan status combat.
- **Rest:** istirahat pertama gratis, berikutnya 8 gold untuk pemulihan penuh.
  Jika gold kurang, rough rest gratis memulihkan hingga setengah maksimum HP/MP.
- **World Map:** sebelas lokasi, status kunjungan, dan travel untuk rute yang terbuka.
- **Quest Journal:** milestone utama, side quest locket Mira, field notes, panduan
  combat, penghitung encounter/lokasi, dan achievement.
- **Ending:** Dawnkeeper, The Silent Watch, atau Sovereign of Ash. Purification
  membutuhkan Sun Sigil dan archive. Clue yang terlewat masih bisa diambil setelah
  Warden dikalahkan, sebelum memilih ending. Setelah ending, dunia menjadi epilog.
- **Hadiah sekali ambil:** pencarian, quest, dan encounter tidak bisa diulang untuk
  menggandakan gold, EXP, atau item. Aksi tak dikenal tidak memberi hadiah.

Klik **Menu** untuk membuka inventory, peta, jurnal, market, build, arsip, kronik, companion, arena, dan galeri.
Halaman bermain tidak lagi memakai antarmuka chat atau input aksi bebas. Klik
portrait NPC untuk dialog, portrait musuh untuk Attack, atau **MAP** di kanan bawah untuk
membuka peta. Pilih kartu aksi dan gunakan filter All/Interact atau
All/Skills/Items saat combat. **Read scene** membuka narasi lengkap tanpa memakai
giliran. Tombol 1-9 dan 0 memilih kartu yang terlihat; Tab/Enter tetap didukung.

## Campfire & Purpose - penyempurnaan 1.7

### Start yang lebih atmosferik

Layar pembuka memakai ilustrasi campfire khusus Teleknia, judul besar dan menu
vertikal: Continue Story, New Journey, Load Game, Settings, Quit. Hover atau fokus
pada Continue menampilkan nama, kelas, level, lokasi dan waktu autosave (WIB), tanpa
memuat atau mengubah karakter. Jika autosave hilang/rusak, Continue dinonaktifkan;
gunakan Load Game untuk checkpoint manual atau pemulihan backup. New Journey
hanya mengganti autosave yang valid; checkpoint manual tetap aman.

Gunakan panah atas/bawah dan Enter, atau mouse. Ember/fade kecil dimatikan oleh
Reduce animations. Nama pemain tetap literal. Ilustrasi berada di
`src/main/resources/art/title-campfire.png`; prompt dan metode built-in imagegen
dicatat dalam `src/main/resources/art/GENERATION.md`.

### Menu, header dan footer

Menu dikelompokkan menjadi Adventure, Character dan System. Panel karakter
menampilkan statistik serta tujuan berikutnya dan akses jurnal. Kembali/Esc dari
Menu mengembalikan pemain ke layar asal petualangan atau Start, bukan selalu Home.

Header memakai judul section dan konteks yang relevan: wilayah pada Story, gold
pada Market/Crafting, jumlah quest aktif atau jenis item pada jurnal/inventory.
Tombol Menu tersedia saat bermain; di dalam section tombol Kembali menuju Menu.
HP/MP tetap pada HUD karakter, tidak diulang di header.

Footer hanya berisi petunjuk kontrol sesuai layar, status save singkat dan mute.
Slider musik/SFX berada di Settings. Shortcut F1, F5, F9 dan F11 tetap tersedia.
Indikator Saved/Tersimpan muncul setelah penulisan snapshot berhasil; kegagalan
save menampilkan Not saved dan tidak mengubah progres aktif. Memilih kartu atau
membaca preview tidak membuat save baru. Hasil transaksi tampil sementara dekat
panel aksi; pada layar lain, pesan singkat memakai notifikasi yang tidak menerima
klik. Pesan lama dibersihkan ketika berpindah layar.

### Persiapan arena

Challenges memakai daftar vertikal tiga trial. Detail menampilkan preview lawan,
HP/Attack/Defense, modifier, ikon hadiah pertama, ketentuan replay, HP/MP serta
companion aktif. Checklist menjelaskan syarat cerita selesai, borderlands dibuka
kembali, lokasi camp, tidak dalam battle/percakapan, dan gold minimal 10. Langkah
pertama yang belum terpenuhi tertulis di atas tombol masuk. Tombol dan biaya
berada pada area tetap; persiapan panjang dapat digulir. Build dan Inventory bisa
dibuka untuk persiapan. Semua data mencerminkan aturan arena 1.5 yang sudah ada:
biaya/reward/modifier/difficulty tidak ditambah atau diubah.

Panel Market, Build, Crafting dan companion juga menampilkan alasan tombol yang
belum tersedia (milestone, resource, rank, camp, atau battle). Perubahan ini tetap
versi 1.7 dan skema save 5, bukan rilis konten baru.

## Penyempurnaan tampilan 1.7 - Traveler interface

Seluruh layar memakai tipografi judul serif, teks isi yang lebih ringan, angka
statistik monospace, panel navy transparan, latar lokasi yang diredupkan, dan garis ivory tipis, terinspirasi
susunan UI Octopath Traveler. Aset Teleknia tetap dipakai. Bingkai kuning tebal
sebelumnya diganti; perkamen dipertahankan khusus untuk peta.

Layout Story mengikuti pilihan pertama: adegan di kiri, dialog tinggi di kanan,
kartu aksi di kiri bawah dan minimap di kanan bawah. Ringkasan level/HP/MP berada
di atas. Header menampilkan judul layar dan satu tombol **Menu**. Inventory dan
semua section game tersedia di dalam Menu; tidak ada tombol Inventory terpisah
pada header. Inventory juga ada pada navigasi section.

Narasi sekarang **satu teks utuh**, bukan halaman 1/2 atau 2/2. Paragraf dipertahankan,
teks panjang dapat di-scroll. Saat masih mengetik, satu klik utama di mana saja
pada layar bermain langsung menampilkan seluruh narasi. Gesture skip dikonsumsi
sebelum tombol menerima klik, sehingga tidak sekaligus memilih jawaban, menyerang,
atau membuka Menu. Klik berikutnya bekerja normal. Space/Enter saat fokus pada
teks juga menampilkan semua. Reduce animations menampilkan teks lengkap langsung.
Scroll, skip dan membaca tidak mengubah giliran, reward atau isi save.

## Fitur baru 1.7 - Atlas of the Borderlands

### Minimap dan peta perkamen

Layar Story memakai panel klasik navy, adegan di kiri atas, dialog di kanan atas,
aksi di kiri bawah, dan minimap kanan bawah. Gambar minimap mengikuti lokasi aktif
dengan saturasi rendah dan tulisan **MAP**. Klik untuk membuka animasi perkamen
terbentang; Close/Esc menutup peta. Reduce animations melewati animasi buka.

Peta yang sama tersedia melalui **Menu > World Map**. Tile heksagonal memakai
ilustrasi tiap lokasi, garis rute, dan penanda emas untuk posisi aktif. Lokasi
belum dikunjungi menjadi node hitam tanpa nama/lore. Frontier yang sudah terbuka
berlabel Explore/Jelajahi: klik untuk kunjungan pertama. Area yang jauh tetap
tersembunyi sampai jaringan eksplorasi mendekatinya. Klik node bertetangga yang
terbuka langsung bepergian. Lokasi yang sudah dikenal tetapi tidak bersebelahan
tidak memberi teleport. Saat battle/percakapan, peta bisa dilihat tetapi travel
ditolak. Tombol travel dan filter Travel tidak lagi tampil pada kartu dialog.

### Inventory grid dan paper doll

- Dua kategori: **All** dan **Quest Items** (termasuk material crafting bertipe quest).
- Grid 5 kolom x 6 baris, 30 slot per halaman; halaman berikutnya menampung jenis
  item tambahan tanpa menghapus item save lama atau menerapkan batas kapasitas.
  Pada jendela minimum/teks besar, panel dapat digulir agar ikon tetap tajam.
- Karakter full-body di kiri dengan slot Weapon/Armor/Accessory. Item dipasang
  melalui detail atau klik kanan, lalu ikonnya muncul pada slot equipment.
  Sprite kelas dipakai kembali; baju/senjata pada tubuh belum berubah per item.
- Hover satu detik menampilkan nama, rarity, deskripsi, jumlah, efek dan preview
  perbandingan. Fokus keyboard juga membuka detail; klik kartu hanya memilih.
- Klik kanan / context-menu keyboard: Equip atau Unequip, Use untuk consumable,
  serta Remove. Tombol setara tersedia pada panel detail untuk keyboard.
- Remove meminta konfirmasi dan jumlah stack. Tidak menghasilkan gold. Item
  quest/material tidak dapat dibuang; equipment aktif harus dilepas dahulu.
- Unequip mengembalikan statistik dasar tanpa menghapus item atau memulihkan HP/MP.
  Pengubahan inventory dikunci saat battle dan epilog yang belum dibuka kembali.

### Menu dan section game

Navigasi kategori di kiri menggantikan tab panjang. Semua section memakai daftar
atau kartu, pilihan berbingkai emas, dan panel detail terpisah. Pola browsing
terinspirasi menu koleksi RPG modern; artwork/aset game lain tidak disalin.

| Section | Cara penggunaan |
| --- | --- |
| Inventory | Grid item, paper doll dan aksi equipment/item |
| World Map | Graph perkamen yang sama dengan minimap |
| Quest Journal | Active/Completed, tahap berikutnya dan Track quest |
| Camp Market | Pilih barang, lihat harga/diskon/perbandingan, atur jumlah lalu Buy |
| Skills & Build | Pilih skill, lihat rank/MP/cooldown/preview, Upgrade/Reset/Difficulty |
| Archive | Koleksi Locations/Characters/Enemies, menggantikan section lore bernama Codex |
| Chronicle | Baca sampai 150 catatan terbaru, tanpa replay aksi |
| Camp & Bonds | Kategori Companions dan Crafting; semua aksi melalui panel detail |
| Challenges | Modifier, statistik, biaya dan status clear sebelum Enter |
| Gallery | Endings/Achievements lintas save; entri terkunci tidak membocorkan epilog |

Pilihan kartu tidak langsung membeli, craft, upgrade, atau masuk arena. Kategori,
pilihan entri dan halaman inventory diingat selama sesi aplikasi. Semua operasi
memakai validasi service dan save atomik. Pembelian consumable sejumlah 1-99
menjadi satu transaksi, bukan beberapa transaksi parsial.

Skema save tetap **5**; save 1.6 dan sebelumnya tetap terbaca, termasuk visited,
equipment, companion, dialog dan galeri. Backend mempertahankan ID travel untuk
kompatibilitas save/tests; UI perjalanan hanya melalui peta. Musik tetap memakai
sepuluh MP3 yang sudah disediakan. Kontrol baru mendukung ID/EN; narasi masih Inggris.

Build Maven memakai `target/maven-classes` dan `target/maven-test-classes` agar
tidak berbenturan dengan bytecode incremental IDE di `target/classes`. Perintah
menjalankan game tetap `mvn clean javafx:run` (JDK 21).

## Riwayat 1.6 - Living Journeys (paging diganti pada 1.7)

- Narasi dan dialog tampil huruf demi huruf di panel biru. Klik panel untuk
  menampilkan halaman penuh; klik berikutnya melanjutkan halaman. Pilihan aksi
  aktif setelah halaman terakhir selesai. Fokuskan teks dengan Tab, gunakan
  Space/Enter untuk fungsi yang sama. Reduce animations menampilkan semua teks
  langsung. Membaca tidak memajukan giliran atau memilih jawaban otomatis.
- 34 percakapan kontekstual tambahan untuk Mira, Sera, Orin, Marin dan Ilyra.
  Buka percakapan NPC lalu pilih topik yang tersedia. Topik mengikuti milestone,
  pilihan karavan/peta, reputasi, hasil damai/combat dan ending (setelah membuka
  borderlands). Dialog dan lore masih berbahasa Inggris.
- Quest companion tiga tahap: Talk di Camp & Bonds, aktifkan companion, lalu
  kunjungi lokasi dan pilih Investigate with ... di Story. Mira: Burned Village;
  Sera: Frostveil Pass; Orin: Emberwood Shrine. Kembali ke camp dengan affinity
  minimal dua untuk memilih promise home/watch di Story, atau menyelesaikan bond
  dari Camp & Bonds. Dua promise saling eksklusif. Bonus support versi 1.5 tidak
  dihapus; companion lama juga dapat mengikuti kelanjutan tanpa hadiah ganda.
- Enam event sekali ambil: desa (setelah rescue), gate/trail (wolf), shrine
  (guardian), Frostveil (q_signal), coast (q_chart). Pilih Help (4 gold) atau
  Remember (catatan tanpa gold). Save/load tidak mengulang reward.
- Deskripsi camp dan jurnal mengikuti perubahan dunia. Jurnal/Camp & Bonds
  menunjukkan langkah berikutnya untuk setiap companion. Companion aktif juga
  berkomentar sekali ketika kembali ke tempat tujuan pribadinya.
- Definisi companion, delapan resep, tiga challenge, topik dialog dan event kini
  berupa JSON tervalidasi di `src/main/resources/definitions`.

Skema save tetap 5: progres baru menggunakan flag campaign yang sudah tersimpan.
Save lama tetap terbaca. Animasi baca mulai ulang dari halaman pertama ketika
layar dibuka kembali; narasi lengkap tetap tersimpan di Chronicle. Tidak ada
reward pada pergantian halaman. Musik 1.4.1 dan fitur 1.5 tetap tersedia.
Desain minimap/perkamen dan inventory All/Quest Items tersedia mulai 1.7.

## Fitur baru 1.5 - Bonds and Embers

Buka World, lalu tab **Camp & Bonds**, **Challenges**, atau **Gallery**.
Rekrut/switch/crafting memerlukan Survivor Camp di luar battle dan percakapan.

| Companion | Rekrut setelah | Affinity dari milestone | Support |
| --- | --- | --- | --- |
| Mira | survivors | survivors, locket_returned, q_medicine | Heal 8 HP |
| Sera | q_signal | q_signal, q_caravan, rime_resolved | Damage 8 |
| Orin | q_oath | q_oath, archive, q_chart | Shield 8 |

Satu companion aktif. Support memakai satu giliran, tanpa MP, cooldown tiga aksi
lain. Affinity 2/3 membuka penyelesaian quest pribadi di camp: A Home Worth
Returning To (Mira), A Road Without Fear (Sera), The Living Oath (Orin).
Setelah quest pribadi, kekuatan support menjadi 12. Mengulang dialog tidak memberi
affinity. Companion tetap aktif setelah kalah/retreat; cooldown direset.

Material diberikan sekali per milestone, termasuk untuk progres save lama:

- Ember Shard x4: wolf, guardian, warden.
- Frost Thread x4: q_signal, q_oath, rime_resolved.
- Tide Pearl x4: q_chart, q_captain.

| Resep | Blueprint dari | Material | Gold |
| --- | --- | --- | --- |
| Small Potion | wolf | 1 Ember Shard | 3 |
| Mana Potion | wolf | 1 Ember Shard | 3 |
| Greater Potion (65 HP) | q_signal | 1 Frost Thread | 6 |
| Focus Draught (45 MP) | q_chart | 1 Tide Pearl | 6 |
| Tempered Blade (+12 Attack) | guardian | 3 Ember Shard | 15 |
| Woven Ward (+8 Defense) | q_oath | 3 Frost Thread | 15 |
| Bond Charm (+5 Defense) | q_chart | 3 Tide Pearl | 15 |
| Awakened Frost (+17 Attack) | rime_resolved | 4 Frost Thread | 20 |

Consumable bisa dipakai di Inventory atau melalui kartu battle. Crafting tidak
memajukan giliran. Bahan/gold diambil hanya jika semua syarat terpenuhi.
Equipment tidak bisa dibuat ketika sudah dimiliki. Tidak ada penjualan material.

Setelah tamat pilih **Explore the borderlands**, kembali ke camp, buka Challenges.
Trial of Ash memiliki attack +4; Trial of Ice defense +7; Trial of Tides HP +80.
Stat final terlihat sebelum masuk. Biaya tiap masuk 10 gold tanpa refund, penalti
retreat/defeat biasa tetap berlaku. Clear pertama memberi dua material wilayah;
replay tanpa gold/EXP/loot. Ending, reputasi, quest dan loot unik tidak berubah.

Gallery menyimpan ending dan enam achievement lintas slot/karakter, di tabel profil
terpisah dalam database. New Game atau restore save tidak menghapus unlock.
Penyimpanan profil atomik bersama snapshot. Menghapus database menghapus profil.
Save skema 1-4 tetap terbaca, companion/arena mendapat default aman (skema 5).

Settings > Reduce animations menonaktifkan animasi attack/hit/heal/status dan
fade layar. Musik MP3 versi 1.4.1 tetap dipakai. Portrait NPC dan ikon kategori
pixel 64x64 dipakai kembali. Kontrol utama baru tersedia dalam ID/EN; narasi dan
lore masih Inggris; lokalisasi penuh direncanakan pada 1.10. Game tetap desktop JavaFX; versi browser belum dibuat.

## Fitur baru 1.4 - Beyond Emberwood

- **Frostveil Pass dan Glacier Sanctum:** Sera, Brother Orin, suar pegunungan,
  karavan, tablet sumpah, serta Rime Sentinel.
- **Drowned Coast dan Tidewatch Lighthouse:** Marin, Keeper Ilyra, pengiriman obat,
  peta bintang, lonceng pelabuhan, dan Drowned Captain.
- **Enam side quest baru:** A Light Through Snow, What the Mountain Kept,
  The Sentinel's Promise, Salt and Mercy, Stars Above the Tide, The Last Harbor Bell.
- **Dialog bercabang:** tanyakan petunjuk, pilih siapa yang menerima persediaan
  atau peta, dan selesaikan kedua boss opsional lewat combat atau diplomasi.
- **Reputasi 0-10:** Survivor Camp 3 memberi diskon market 20% (dibulatkan ke bawah,
  minimum 1 gold); Camp 4 membuka parley captain jika petunjuk/bell tersedia.
  Ember Keepers 3 membuka parley sentinel setelah tablet diterjemahkan.
- **12 item baru:** enam item quest dan enam equipment, termasuk Sentinel's Edge,
  Glacier Mail, Rime Sigil, dan Harbor Oath Ring. Hadiah mengikuti cabang yang
  dipilih dan tidak bisa diambil lagi lewat cabang alternatif.
- **Codex:** lokasi, NPC, dan monster terbuka sesuai penemuan. Jurnal punya 13
  milestone dan tombol Track quest yang memperbarui tracker pada HUD.
- **UI RPG:** panel karakter/HP/MP, adegan berbasis sprite, portrait interaktif,
  intent/HP musuh, kartu aksi, filter, hasil aksi, dan pembaca narasi lengkap.
  Riwayat lama tetap tersimpan sebagai Chronicle yang hanya bisa dibaca.
- **32 sprite baru bersumber 64x64**, termasuk figur kelas, seluruh lokasi,
  encounter lama, NPC/boss baru, dan ikon item penting. Semua dirender tanpa
  smoothing pada skala integer.
- **Musik/SFX:** dua loop kawasan baru dan efek discovery/quest-complete; total
  musik per wilayah dan delapan SFX offline (musik diperbarui pada 1.4.1).

Akses Frostveil dari Northern Gate atau camp setelah Ash Wolf dikalahkan. Akses
Drowned Coast dari Sunken Ruins atau camp setelah Hollow Guardian dikalahkan.
Selesaikan pertanyaan lewat dialog NPC; Esc meninggalkan percakapan. Di akhir
campaign lama, pilih **Explore the borderlands** untuk membuka eksplorasi opsional.
Ending yang sudah dipilih tetap dipertahankan.

Definisi lokasi/rute, quest, NPC, dialog, aksi, event/reward, serta monster berada
di `src/main/resources/definitions`. Validasi memeriksa koneksi peta, ID referensi,
syarat unlock, item konsumsi/reward, dan syarat sekali ambil untuk aksi berhadiah.

## Fitur 1.3 - Arts of Battle

| Kelas | Skill awal | Skill tambahan |
| --- | --- | --- |
| Fighter | Power Strike (6 MP) | Shield Bash (8 MP, CD 3), Rally (8 MP, CD 4) |
| Rogue | Shadow Strike (6 MP) | Venom Cut (8 MP, CD 2), Evasion (7 MP, CD 3) |
| Mage | Ember Surge (6 MP) | Frost Bind (9 MP, CD 3), Mana Ward (7 MP, CD 3) |

Buka **World -> Skills & Build** atau **Profile -> Manage skills**. Semua skill
tersedia pada rank 0. Dapatkan satu skill point untuk setiap level, termasuk
level 1; tingkatkan hingga rank 3 dengan preview sebelum/sesudah. Upgrade menambah
kekuatan, bukan durasi atau pengurangan cooldown. Reset di camp berbiaya 20 gold,
mengembalikan point tanpa memulihkan HP/MP. Build terkunci selama combat.

- **Burn/Poison:** damage berkala di akhir giliran valid, termasuk giliran aplikasi,
  lalu durasi berkurang. Menembus armor dan shield. Venom Cut memberi Poison;
  Ember Surge memberi Burn. Heavy attack Warden fase II juga memberi Burn.
- **Stun:** Shield Bash/Frost Bind melewati aksi musuh pada giliran aplikasi,
  kemudian stun kedaluwarsa. Cooldown menghalangi stun beruntun tanpa jeda.
- **Shield:** menyerap damage langsung sampai kapasitas habis atau durasi selesai.
  Evasion/Mana Ward bertahan dua giliran termasuk giliran aplikasi.
- Efek sejenis memperbarui durasi dan mempertahankan potensi tertinggi, tidak
  menjumlahkan stack. Menang, kalah, atau retreat membersihkan efek dan cooldown.
- **Cooldown N** berarti lakukan N aksi valid LAIN sebelum skill siap kembali.
  Aksi gagal dan `help` tidak memajukan cooldown, efek, atau giliran.
- Wolf punya charge tanpa serangan langsung; Guardian memasang shield untuk dua
  giliran berikutnya; Warden memasuki fase II saat HP separuh. Intent dipilih
  sebelum aksi pemain sehingga perpindahan fase tidak mengejutkan pada giliran itu.
- **Fatigue:** setelah giliran 12, attack musuh bertambah 1 setiap dua giliran.
  Guard/heal tidak dapat mempertahankan pertarungan tanpa batas.
- **Story difficulty:** damage masuk dikalikan 65% dan dibulatkan ke atas sebelum
  Guard/Shield; penalti kalah 10%, retreat hingga 2 gold. Standard memakai damage
  normal, penalti 20%, retreat hingga 5 gold. Hadiah sama pada kedua difficulty.
  Pilih saat karakter dibuat atau ubah di camp melalui halaman build.
- Market menambah Sentinel Plate (+8 Defense), Duelist Charm (+2 Attack dan
  critical pada giliran ketiga), serta Warding Charm (+3 Defense). Satu aksesori
  aktif; bonus lama dilepas saat diganti. Critical biasa pada setiap giliran
  keempat membutuhkan Agility 10; Duelist Charm menggantinya menjadi ketiga.
- Combat deterministik: giliran, status, cooldown, ranks, equipment, dan difficulty
  disimpan. Tidak ada randomness tersembunyi yang berubah setelah load.
- Tombol **1-9, lalu 0** memilih seluruh sepuluh aksi combat. UI menampilkan MP,
  cooldown, intent, dan status aktif; aksi yang tidak bisa dilakukan dinonaktifkan.

## Fitur 1.2 - Traveler's Essentials

- **Tiga slot manual + satu autosave** dengan nama, kelas, level, lokasi, dan waktu
  simpan. Aksi game memperbarui autosave, bukan checkpoint manual.
- **Backup per slot** menyimpan snapshot valid sebelumnya. Checksum SHA-256 dan
  validasi isi mendeteksi snapshot bermasalah. Layar Saves menawarkan pemulihan
  eksplisit; save rusak tidak diam-diam diganti karakter baru.
- **Tutorial** lima langkah: aksi cerita, combat, equipment, camp, serta quest/save.
- **Inventory:** filter tipe, sorting nama/rarity/jumlah, dan perbandingan attack
  atau defense sebelum equip. Preview tidak mengubah statistik karakter.
- **Settings:** bahasa UI Indonesia/Inggris, teks 90-120%, fullscreen, dan audio.
  Bahasa narasi, nama dunia, deskripsi item, dan lore tetap Inggris pada 1.4.
- **Keyboard:** F1 bantuan, F5 slot simpan, F9 slot muat, F11 fullscreen; Esc keluar
  fullscreen, kembali dari halaman utilitas, atau membuka Settings. Tombol 1-9 dan 0
  memilih kartu aksi yang tampil. Tab/Enter menavigasi kontrol.

Pengaturan UI/window/tutorial tersimpan di `data/ui.properties`, terpisah dari
karakter. Pengaturan audio tetap di `data/audio.properties`.

## Penyimpanan dan kompatibilitas

Save SQLite: `data/rpg_chat_game.db`, relatif terhadap folder aplikasi dijalankan.
Tabel `save_slots` menyimpan slot 0 (auto) serta 1-3 (manual). `save_backups`
menyimpan backup per slot. Snapshot skema 5 mencakup karakter, inventory,
equipment, flags, combat, skill ranks, cooldown, status, difficulty, quest, ending,
reputasi, codex, dialog aktif, quest yang dipantau, dan seluruh riwayat pilihan.

Penulisan primary dan backup dilakukan dalam transaksi yang sama. Data primary
harus lolos validasi sebelum dijadikan backup. Kegagalan commit mempertahankan
save sebelumnya, backup, dan state aktif. Save manual yang bermasalah harus
pulih dari backup sebelum dapat ditimpa. Pemulihan backup menjaga checkpoint
manual lain; hasil pemulihan yang dimuat menjadi autosave aktif.

Save skema 1/2/3/4 tetap terbaca. Data lama mendapat default fitur combat/world yang
belum ada, dengan difficulty Standard jika belum tersimpan. Reputasi awal berasal
dari milestone yang sudah selesai dan hanya dihitung sekali. Default combat lama: Standard, ranks kosong, dan efek/cooldown
kosong. HP musuh dan giliran aktif dipertahankan; level menentukan point tersedia.
Slot diperbarui ke skema 5 saat ditulis atau dipulihkan; backup asal tetap tersedia.

Save 1.1 diimpor sekali ke **slot manual 1 dan autosave**, termasuk kondisi combat.
Payload asli `campaign_save` dipertahankan. Save 1.0 tetap mempertahankan karakter,
statistik, item, gold, dan chat, lalu memulai quest baru dari Burned Village.
Migrasi gagal tidak menghapus data asal. Versi skema save terpisah dari versi game.
Backup snapshot berada di database SQLite yang sama; pemulihan kerusakan seluruh
file database memerlukan salinan file dari luar aplikasi.

## Pixel art dan audio

Empat puluh delapan sprite PNG sumber **64 x 64** berada di `src/main/resources/sprites`.
`PixelAssets` merender pada skala integer dengan smoothing dimatikan. Beberapa
item/lokasi baru menggunakan sprite kategori yang sama. Prompt dan asal aset
tercatat di `sprites/GENERATION.md`.

Sepuluh lagu MP3 yang disediakan di `src/main/resources/audio_frieren` menggantikan
seluruh musik bawaan lama. JavaFX Media memutar musik langsung; delapan SFX WAV
menggunakan Java Sound. Musik dan SFX memiliki volume terpisah serta mute; settings
disimpan di `data/audio.properties`. Pergantian wilayah/battle mengganti tema,
kemudian kembali ke tema wilayah setelah battle. Semua playback offline.
Pemetaan lengkap ada di [panduan audio](src/main/resources/audio/README.md).

Regenerasi efek suara (tidak membuat ulang musik lama):

```powershell
python scripts/generate_audio.py
```

## Pemeriksaan otomatis

Dengan JDK 21 dan dependensi Maven yang sudah terunduh:

```powershell
./scripts/check-game.ps1
```

Runner ini mengompilasi main/test source dan menjalankan `CampaignChecks` serta
`EssentialsChecks`, `BattleChecks`, dan `WorldChecks` tanpa
JUnit/Surefire tambahan. Pemeriksaan menjalankan semua kombinasi kelas/ending,
batas transaksi, combat, recovery, quest gates, save/load pertarungan, migrasi
legacy, isolasi slot, checksum rusak, backup, settings, lokalisasi, filter/sorting,
status/cooldown, semua kelas pada kedua difficulty, replay deterministik, serta
kegagalan commit memakai trigger SQLite. Database uji terisolasi di
`target/campaign-checks`, `target/essentials-checks`, `target/battle-checks`, serta
`target/world-checks`; save pemain tidak disentuh.
Workflow `.github/workflows/game-checks.yml` menjalankan pemeriksaan ini pada push
atau pull request menggunakan JDK 21, tanpa membuka UI atau memakai perangkat audio.

Pemeriksaan UI desktop opsional (membuka jendela sementara, audio dibisukan):

```powershell
./scripts/check-ui.ps1
```

Script UI memakai folder kerja terisolasi `target/ui-checks`, bukan save pemain,
dan menyimpan screenshot hasil pemeriksaan di dalam folder tersebut. Script ini
memeriksa tutorial, save/load/recovery, filter, preview, dua bahasa, shortcut,
navigasi Back bertumpuk, fullscreen, serta ukuran jendela minimum.

## Struktur

- `controller/`: Home, Story, Inventory, Profile, Adventure, Saves, Settings, Tutorial.
- `model/`: Character, CampaignState, Enemy, inventory, choices, story logs/responses.
- `service/`: GameService, stateful LocalStoryGeneratorService, ItemCatalog,
  CharacterService, InventoryService, AudioService.
- `repository/`: slot/backup atomik, checksum, migrasi, dan repository legacy.
- `ui/`: PixelAssets, StatusPanel, AudioControls.
- `src/main/resources/`: CSS, sprite 64px, WAV, definisi item/aksi JSON, dan teks UI bilingual.
- `src/test/java/`: integration checks tanpa dependensi test tambahan.

`OpenAiStoryGeneratorService` tetap placeholder dan tidak dipakai runtime.

### Musik update 1.4.1

Empat wilayah memiliki musik eksplorasi sendiri. Battle memakai tema wilayah atau
musuh khusus, lalu kembali ke musik eksplorasi. Semua 10 MP3 di `audio_frieren`
digunakan; musik WAV lama dihapus. Tidak perlu menyalin ke `data/music`.
Lihat [pemetaan lagu](src/main/resources/audio/README.md).

Untuk memeriksa decoding semua MP3 di desktop tanpa suara:

```powershell
./scripts/check-audio.ps1
```
