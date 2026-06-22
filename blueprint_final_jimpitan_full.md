# Blueprint Final Aplikasi Jimpitan Digital

## 1. Ringkasan Produk

Aplikasi ini adalah sistem jimpitan digital untuk RT/RW yang menggantikan pencatatan manual menjadi pencatatan berbasis QR.

Tujuan utama:

- Petugas cukup scan QR warga.
- Data warga tampil otomatis di aplikasi.
- Pembayaran dapat dicatat cepat saat offline.
- Data tetap sinkron ke server saat internet tersedia.
- Tidak boleh ada pembayaran ganda pada hari kewajiban yang sama.
- Admin dapat memantau laporan global, per warga, harian, mingguan, dan rentang waktu tertentu.

---

## 2. Prinsip Dasar Sistem

### 2.1 QR hanya berisi ID
QR tidak boleh berisi nama, alamat, RT, atau data pribadi.

Isi QR hanya ID acak, misalnya:

- `JMP|550e8400-e29b-41d4-a716-446655440000`

Saat scan menggunakan aplikasi jimpitan, aplikasi akan mengambil data warga dari database lokal atau server.

Saat scan menggunakan aplikasi lain, yang terlihat hanya ID acak.

---

### 2.2 Offline-first, online-sync
Aplikasi harus tetap bisa dipakai tanpa internet.

Alur operasional:

1. Data warga sudah tersimpan di lokal.
2. Petugas scan QR.
3. Data warga tampil dari database lokal.
4. Petugas input nominal.
5. Pembayaran disimpan ke lokal dulu.
6. Saat internet tersedia, data disinkronkan ke Supabase.

---

### 2.3 Sistem jimpitan memakai coverage hari
Aturan jimpitan:

- Rp500 = 1 hari
- Rp1.000 = 2 hari
- Rp2.000 = 4 hari
- Rp5.000 = 10 hari

Artinya pembayaran tidak lagi dianggap hanya untuk 1 hari, tetapi sebagai pembelian sejumlah hari kewajiban.

Kalau warga membayar lebih besar dari Rp500, maka sistem otomatis mengalokasikan pembayaran ke hari yang belum lunas mulai dari tunggakan tertua terlebih dahulu.

Contoh:

- Warga masih menunggak 4 hari.
- Ia membayar Rp2.000.
- Sistem melunasi 4 hari itu.

---

### 2.4 Pencegahan double bayar
Pencegahan double bayar harus dilakukan di dua level:

- **Level lokal**: mencegah input ganda pada HP petugas yang sama.
- **Level server**: mencegah konflik jika dua petugas mencatat warga yang sama secara offline.

Karena itu, validasi utama tetap harus dijaga oleh database server.

---

## 3. Role Pengguna

### 3.1 Petugas
Fokus petugas adalah operasional lapangan.

Bisa:

- Login
- Scan QR warga
- Melihat data warga
- Input nominal pembayaran
- Melihat transaksi hari ini
- Melihat status sync
- Melihat ringkasan warga yang sudah atau belum bayar

Tidak bisa:

- Menghapus warga
- Mengubah struktur data
- Mengakses laporan penuh semua warga secara bebas
- Menghapus transaksi historis

---

### 3.2 Admin
Fokus admin adalah monitoring dan pengelolaan penuh.

Bisa:

- Menambah dan mengubah data warga
- Menambah petugas
- Melihat laporan harian, mingguan, bulanan, dan rentang waktu
- Melihat detail per warga
- Melihat riwayat bayar dan tunggakan
- Melihat agregasi pemasukan
- Generate QR massal
- Mengelola status aktif/nonaktif warga

---

## 4. Stack yang Direkomendasikan

### 4.1 Frontend Android
- Kotlin
- Jetpack Compose
- CameraX untuk scanner QR
- Room untuk database lokal
- WorkManager untuk sinkronisasi background
- Supabase Kotlin SDK

### 4.2 Backend
- Supabase
- PostgreSQL
- Authentication
- Row Level Security
- Storage opsional untuk file PDF QR atau export laporan

### 4.3 Kenapa tanpa backend tambahan dulu
Untuk kebutuhan MVP jimpitan ini, aplikasi bisa langsung frontend Android ke Supabase.

Tidak perlu backend terpisah seperti Node.js, Spring Boot, atau NestJS, kecuali nanti ada kebutuhan tambahan seperti:

- Integrasi WhatsApp Gateway
- Payment gateway QRIS
- Logic bisnis yang sangat kompleks
- Multi-RT/Multi-RW skala besar dengan banyak aturan tambahan

---

## 5. Arsitektur Sistem

```text
Android App (Kotlin + Compose)
        │
        ├── Room Database (offline cache)
        │
        ├── CameraX (scan QR)
        │
        ├── WorkManager (sync otomatis)
        │
        ▼
      Supabase
        │
        ├── Authentication
        ├── PostgreSQL
        ├── RLS
        └── Storage (opsional)
```

---

## 6. Format QR

### 6.1 Format yang digunakan
Format QR yang direkomendasikan:

```text
JMP|UUID
```

Contoh:

```text
JMP|550e8400-e29b-41d4-a716-446655440000
```

### 6.2 Aturan
- QR hanya menyimpan ID acak.
- QR tidak menyimpan nama warga.
- QR tidak menyimpan nomor rumah.
- QR tidak menyimpan data sensitif.
- Aplikasi akan melakukan lookup ke database untuk menampilkan data lengkap.

---

## 7. Alur Penggunaan Petugas

## 7.1 Login
Petugas login menggunakan email dan password.

Setelah login:

1. Aplikasi mengecek role pengguna.
2. Aplikasi melakukan sinkronisasi data warga terbaru ke lokal.
3. Petugas masuk ke dashboard.

---

## 7.2 Dashboard Petugas
Dashboard petugas harus sangat sederhana.

Komponen utama:

- Tombol scanner besar di tengah
- Ringkasan transaksi hari ini
- Total nominal terkumpul hari ini
- Jumlah warga yang sudah bayar
- Jumlah warga yang belum bayar
- Status internet dan sinkronisasi

Contoh layout:

```text
┌──────────────────────────┐
│ Halo, Pak Slamet         │
│ Status: Online / Offline │
├──────────────────────────┤
│                          │
│        [ SCAN QR ]       │
│                          │
├──────────────────────────┤
│ Hari ini                 │
│ Sudah bayar: 78          │
│ Belum bayar: 22          │
│ Terkumpul: Rp95.500      │
├──────────────────────────┤
│ Sync: 3 data pending     │
└──────────────────────────┘
```

---

## 7.3 Scan QR
Saat petugas menekan scanner:

1. Kamera aktif.
2. QR dipindai.
3. Aplikasi membunyikan suara "Beep".
4. Aplikasi membaca `JMP|UUID`.
4. Aplikasi mencari warga di database lokal.
5. Jika ditemukan, tampilkan identitas warga.
6. Jika tidak ditemukan, tampilkan pesan “warga tidak ditemukan”.

---

## 7.4 Detail Warga setelah scan
Data yang muncul:

- Nama
- RT
- RW
- Nomor rumah
- Status coverage
- Tanggal coverage terakhir
- Tunggakan hari ini

Contoh:

```text
Nama: Budi Santoso
RT: 03
RW: 01
Rumah: A-12

Coverage aktif sampai:
20 Juni 2026

Status:
Lunas 6 hari ke depan
```

---

## 7.5 Input pembayaran
Setelah data warga muncul, petugas masuk ke menu pembayaran.

Komponen form:

- Field nominal manual
- Tombol nominal cepat:
  - 500
  - 1000
  - 2000
  - 5000
- Tombol simpan

Contoh alur:

1. Petugas scan warga.
2. Data muncul.
3. Petugas klik nominal `2000`.
4. Field nominal terisi otomatis `2000`.
5. Petugas klik simpan.

---

## 7.6 Simpan pembayaran
Saat tombol simpan ditekan:

1. Aplikasi menghitung coverage days:
   - `coverage_days = nominal / 500`
2. Aplikasi menyimpan transaksi ke Room.
3. Transaksi diberi status `PENDING`.
4. Jika ada internet, WorkManager mulai sinkronisasi.
5. Jika tidak ada internet, data tetap aman di lokal.

---

## 7.7 Setelah simpan
Setelah simpan sukses:

- Aplikasi membunyikan notifikasi suara berhasil.
- Tampil pesan berhasil.
- Aplikasi kembali ke scanner.
- Petugas bisa lanjut scan warga berikutnya tanpa banyak klik.

---

## 8. Alur Penggunaan Admin

## 8.1 Dashboard Admin
Dashboard admin lebih lengkap daripada petugas.

Komponen:

- Total warga aktif
- Total transaksi hari ini
- Total pemasukan hari ini
- Total pemasukan minggu ini
- Total pemasukan bulan ini
- Jumlah warga lunas
- Jumlah warga menunggak
- Grafik tren pemasukan

---

## 8.2 Menu admin
Menu utama admin:

- Dashboard
- Warga
- Transaksi
- Laporan
- Tunggakan
- Petugas
- Pengaturan

---

## 8.3 Detail per warga
Admin dapat klik satu warga dan melihat:

- Identitas lengkap
- Coverage aktif sampai tanggal tertentu
- Kalender pembayaran
- Riwayat transaksi
- Total nominal yang sudah dibayar
- Jumlah hari lunas
- Jumlah hari menunggak

Contoh:

```text
Budi Santoso
RT 03 / RW 01
Rumah A-12

Coverage sampai:
20 Juni 2026

Riwayat:
01 ✓
02 ✓
03 ✓
04 ✗
05 ✓
06 ✓
07 ✓
```

---

## 8.4 Laporan
Admin bisa melihat laporan berdasarkan:

- Harian
- Mingguan
- Bulanan
- Custom range

Contoh custom range:

- 1 Juni 2026 sampai 30 Juni 2026

Ringkasan:

- Total warga
- Total transaksi
- Total nominal terkumpul
- Total coverage hari yang terjual
- Total tunggakan tersisa

---

## 8.5 Tunggakan
Menu tunggakan menampilkan warga yang coverage-nya sudah habis.

Bisa difilter:

- RT
- RW
- Rentang waktu
- Status aktif/nonaktif

Contoh:

- Pak Budi: tunggakan 2 hari
- Pak Joko: tunggakan 7 hari
- Bu Siti: tunggakan 1 hari

---

## 9. Aturan Bisnis Coverage Hari

## 9.1 Aturan dasar
Nilai wajib:

- Rp500 per hari

Jika nominal lebih besar, sistem mengonversi menjadi jumlah hari.

Contoh:

- Rp500 = 1 hari
- Rp1000 = 2 hari
- Rp2000 = 4 hari
- Rp5000 = 10 hari

---

## 9.2 FIFO / oldest unpaid first
Saat pembayaran dilakukan, sistem harus mengisi hari yang belum lunas mulai dari tanggal tertua dulu.

Contoh:

Hari kewajiban yang belum lunas:

- 10 Juni
- 11 Juni
- 12 Juni
- 13 Juni
- 14 Juni

Warga membayar Rp2000.

Coverage = 4 hari.

Sistem mengalokasikan:

- 10 Juni = lunas
- 11 Juni = lunas
- 12 Juni = lunas
- 13 Juni = lunas

14 Juni tetap belum lunas.

---

## 9.3 Pembayaran di muka
Jika tidak ada tunggakan, coverage akan diarahkan ke hari-hari berikutnya.

Contoh:

Warga membayar Rp5000 pada 14 Juni.

Maka coverage menjadi:

- 14 Juni
- 15 Juni
- 16 Juni
- 17 Juni
- 18 Juni
- 19 Juni
- 20 Juni
- 21 Juni
- 22 Juni
- 23 Juni

---

## 9.4 Status warga
Status warga dihitung dari coverage terakhir.

Jika hari ini masih berada di dalam coverage, status adalah:

- Lunas

Jika coverage sudah lewat:

- Menunggak X hari

---

## 10. Flow Pembayaran Detail

### Kasus 1: Warga belum punya tunggakan
1. Petugas scan QR
2. Data muncul
3. Petugas input Rp2000
4. Sistem menghitung coverage 4 hari
5. Coverage dipakai untuk hari ini dan 3 hari berikutnya

---

### Kasus 2: Warga punya tunggakan 4 hari
1. Petugas scan QR
2. Sistem mendeteksi 4 hari menunggak
3. Petugas input Rp2000
4. Sistem melunasi 4 hari paling lama dulu
5. Status tunggakan menjadi 0

---

### Kasus 3: Warga bayar besar
1. Petugas scan QR
2. Petugas input Rp5000
3. Sistem menghitung 10 hari coverage
4. Jika ada tunggakan, lunasi tunggakan dulu
5. Sisa coverage menjadi hari-hari berikutnya

---

## 11. Mekanisme Anti Double Bayar

## 11.1 Level lokal
Di HP petugas yang sama, aplikasi harus mengecek apakah warga sudah punya coverage untuk tanggal yang sama.

Jika sudah, maka aplikasi tidak boleh menyimpan transaksi baru untuk tanggal kewajiban yang sama.

---

## 11.2 Level server
Di Supabase/PostgreSQL, harus ada constraint unik untuk mencegah data ganda pada tanggal kewajiban yang sama.

Yang unik bukan `pembayaran`, melainkan `coverage_history` untuk kombinasi:

- `warga_id`
- `tanggal_kewajiban`

Dengan begitu, jika ada dua petugas offline mencatat warga yang sama, saat sync hanya satu yang berhasil dan sisanya ditolak.

---

Jika sync gagal karena data sudah ada di server (misal: tanggal kewajiban tersebut sudah dilunasi oleh petugas lain secara offline):

- Konflik dideteksi secara Atomic menggunakan PostgreSQL RPC (`sync_pembayaran_offline`).
- Transaksi tetap disimpan di tabel `pembayaran` pada server, namun dengan `sync_status = 'CONFLICT'`.
- Transaksi konflik TIDAK dimasukkan ke tabel `coverage_history`.
- Di lokal Android, status transaksi menjadi `CONFLICT` dan petugas dapat melihat alasan konflik.
- Admin dapat mengecek laporan transaksi di dashboard dan melihat transaksi mana saja yang berstatus `CONFLICT`.

---

## 12. Model Data

## 12.1 Tabel `profiles`
Menyimpan data akun dan role.

Field:

- id
- nama
- role
- created_at

Role:

- ADMIN
- PETUGAS

---

## 12.2 Tabel `warga`
Menyimpan data warga.

Field:

- id
- qr_uuid
- nama
- rt
- rw
- nomor_rumah
- alamat
- is_active
- created_at
- updated_at

Catatan:
- `qr_uuid` harus unik
- QR akan memakai field ini

---

## 12.3 Tabel `pembayaran`
Menyimpan transaksi pembayaran.

Field:

- id
- warga_id
- tanggal_bayar
- nominal
- coverage_days
- created_by
- created_at
- sync_status

Contoh:
- nominal = 2000
- coverage_days = 4

---

## 12.4 Tabel `coverage_history`
Tabel ini sangat penting.

Fungsinya menyimpan alokasi hari kewajiban yang dilunasi oleh transaksi tertentu.

Field:

- id
- warga_id
- tanggal_kewajiban
- payment_id
- created_at

Contoh:
- 10 Juni → payment TX001
- 11 Juni → payment TX001
- 12 Juni → payment TX001
- 13 Juni → payment TX001

Constraint unik:

- `UNIQUE(warga_id, tanggal_kewajiban)`

Ini adalah penjaga utama anti double bayar.

---

## 12.5 Tabel `sync_logs`
Menyimpan log sinkronisasi.

Field:

- id
- device_id
- entity_type
- entity_id
- status
- message
- created_at

Status:
- PENDING
- SYNCED
- FAILED
- CONFLICT

---

## 13. Room Database Lokal

## 13.1 `warga_local`
Menyimpan cache data warga untuk offline scan.

Field:
- id
- qr_uuid
- nama
- rt
- rw
- nomor_rumah
- alamat
- is_active
- updated_at

---

## 13.2 `pembayaran_local`
Menyimpan transaksi lokal sebelum sync.

Field:
- id_local
- server_id
- warga_id
- tanggal_bayar
- nominal
- coverage_days
- sync_status
- created_at

Status:
- PENDING
- SYNCED
- FAILED
- CONFLICT

---

## 13.3 `coverage_local`
Menyimpan hasil coverage sementara untuk perhitungan cepat di aplikasi.

Field:
- id
- warga_id
- tanggal_kewajiban
- payment_id
- created_at

---

## 14. Sinkronisasi

## 14.1 Waktu sinkronisasi
Sinkronisasi dilakukan otomatis:

- Saat internet tersedia
- Saat aplikasi aktif
- Berjalan periodik via WorkManager

---

## 14.2 Urutan sinkronisasi
1. Sync data master warga
2. Sync transaksi pembayaran pending
3. Sync coverage_history
4. Update status transaksi lokal

---

## 14.3 Jika sync sukses
Status lokal:
- `PENDING` → `SYNCED`

---

## 14.4 Jika sync konflik
Status lokal:
- `PENDING` → `CONFLICT`

Contoh alasan:
- Tanggal kewajiban sudah terisi oleh transaksi lain
- Ada transaksi rangkap dari perangkat lain

---

## 15. Security dan Akses Data

## 15.1 Supabase SDK di frontend aman
Frontend Android boleh langsung memakai:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

Yang tidak boleh masuk APK:

- `service_role_key`

---

## 15.2 RLS wajib aktif
Semua tabel penting harus memakai Row Level Security.

Contoh aturan:

- Petugas boleh baca data warga
- Petugas boleh insert pembayaran
- Petugas tidak boleh delete warga
- Admin boleh CRUD lengkap

---

## 15.3 Validasi tetap di database
Jangan hanya mengandalkan UI.

Validasi penting harus ada di sisi database:

- Unique coverage per warga per tanggal
- Check nominal minimal
- Role-based access

---

## 16. UI Minimum Viable Product

## 16.1 Untuk Petugas
- Login
- Dashboard
- Scan QR
- Detail warga
- Input nominal
- Riwayat hari ini
- Status sync

---

## 16.2 Untuk Admin
- Login
- Dashboard global
- Daftar warga
- Detail per warga
- Laporan
- Tunggakan
- Kelola petugas
- Generate QR

---

## 17. Flow Layar Lengkap

## 17.1 Petugas
Login → Sync data warga → Dashboard → Scan QR → Detail warga → Input nominal → Simpan lokal → Sync otomatis → Kembali ke scanner

---

## 17.2 Admin
Login → Dashboard global → Pilih menu warga/laporan/tunggakan → Filter data → Lihat detail → Export jika perlu

---

## 18. Edge Cases yang Harus Ditangani

### 18.1 Scan QR tidak valid
Tampilkan:
- QR tidak dikenali

### 18.2 Warga belum ada di lokal
Jika offline:
- Tampilkan tidak ditemukan
- Catat untuk sinkronisasi berikutnya

Jika online:
- Coba ambil dari server

### 18.3 Nominal tidak kelipatan 500
Aturan yang disarankan:
- Tolak nominal yang bukan kelipatan 500
- Atau izinkan nominal bebas tetapi pembulatan/validasi harus jelas

Rekomendasi:
- Hanya terima kelipatan 500 agar aturan bisnis konsisten

### 18.4 Data sync konflik
Status:
- CONFLICT
- Ditampilkan ke petugas/admin untuk ditinjau

### 18.5 Internet putus saat sync
- Data tetap aman di lokal
- Status tetap `PENDING`
- Sync dilanjutkan otomatis saat internet kembali

---

## 19. Rekomendasi Implementasi MVP

### Sprint 1
- Login
- Role admin/petugas
- Data warga
- QR generator
- Scanner
- Detail warga
- Input nominal

### Sprint 2
- Room database
- Offline scan
- Sync ke Supabase
- WorkManager
- Konflik dan status sync

### Sprint 3
- Dashboard admin
- Laporan
- Tunggakan
- Detail per warga
- Export PDF/Excel jika perlu

---

## 20. Rekomendasi Keputusan Teknis Final

Untuk versi awal, pilihan paling cocok adalah:

- **Android Kotlin + Jetpack Compose**
- **Room untuk offline**
- **CameraX untuk scan**
- **WorkManager untuk sinkronisasi**
- **Supabase langsung dari frontend**
- **PostgreSQL untuk data utama**
- **RLS dan unique constraint untuk keamanan dan anti double bayar**

Tanpa backend tambahan dulu.

---

## 21. Ringkasan Aturan Paling Penting

1. QR hanya berisi ID acak.
2. Data warga tampil dari database lokal saat offline.
3. Pembayaran dicatat di lokal dulu.
4. Sinkronisasi ke Supabase dilakukan otomatis.
5. Rp500 = 1 hari.
6. Pembayaran lebih besar dihitung sebagai beberapa hari.
7. Alokasi hari dilakukan dari tunggakan tertua dulu.
8. Satu tanggal kewajiban tidak boleh dipakai dua kali.
9. Admin dan petugas punya dashboard yang berbeda.
10. Data sensitif tidak boleh bocor dari QR.

---

## 22. Definisi Sukses MVP

Aplikasi dianggap sukses untuk versi pertama jika:

- Petugas bisa scan dan mencatat pembayaran dengan cepat.
- Aplikasi tetap berjalan saat offline.
- Data warga tetap terbaca dari cache lokal.
- Pembayaran besar otomatis dihitung ke beberapa hari.
- Tidak ada double bayar pada hari yang sama.
- Admin bisa melihat laporan dan tunggakan dengan jelas.

---

## 23. Penutup

Blueprint ini adalah dasar untuk membangun aplikasi jimpitan digital yang sederhana dipakai di lapangan, kuat di data, aman dari double catatan, dan enak dipakai oleh petugas maupun admin.

Fokus utama bukan membuat fitur yang banyak, tetapi membuat alur yang cepat, jelas, dan tidak membingungkan orang yang mencatat jimpitan setiap hari.
