# Jimpitan Digital - Database Schema v1

## Project Overview

Jimpitan Digital adalah aplikasi pencatatan iuran harian warga berbasis Android dengan arsitektur Offline First.

Backend menggunakan Supabase.
Frontend menggunakan Kotlin + Jetpack Compose.

---

# Business Rules

## Aturan Dasar Jimpitan

Nilai wajib jimpitan: Rp500 per hari.

Warga dapat membayar lebih dari Rp500.

| Nominal | Coverage |
|----------|----------|
| Rp500 | 1 Hari |
| Rp1.000 | 2 Hari |
| Rp2.000 | 4 Hari |
| Rp5.000 | 10 Hari |

Formula:

coverage_days = nominal / 500

## Coverage System

Pembayaran dialokasikan ke hari yang belum lunas paling lama terlebih dahulu (FIFO).

Contoh:
- Tunggakan: 10,11,12,13,14 Juni
- Bayar Rp2.000
- Coverage = 4 hari

Maka:
- 10 Juni = Lunas
- 11 Juni = Lunas
- 12 Juni = Lunas
- 13 Juni = Lunas

14 Juni masih belum lunas.

## Future Coverage

Jika tidak ada tunggakan dan warga membayar Rp5.000:

Coverage = 10 hari

Maka tanggal hari ini hingga 9 hari ke depan dianggap lunas.

---

# Authentication

Menggunakan Supabase Auth.

Role:
- ADMIN
- PETUGAS

---

# Table: profiles

Fields:
- id (UUID)
- nama (TEXT)
- role (ADMIN | PETUGAS)
- created_at (TIMESTAMP)

---

# Table: warga

Fields:
- id (UUID)
- qr_uuid (TEXT)
- nama (TEXT)
- rt (TEXT)
- rw (TEXT)
- nomor_rumah (TEXT)
- alamat (TEXT)
- is_active (BOOLEAN)
- created_at (TIMESTAMP)
- updated_at (TIMESTAMP)

Rules:
- qr_uuid unik
- QR tidak boleh menyimpan data pribadi

Contoh QR:
JMP|WRG001

---

# Table: pembayaran

Fields:
- id (UUID)
- warga_id (UUID)
- nominal (INTEGER)
- coverage_days (INTEGER)
- tanggal_bayar (DATE)
- created_by (UUID)
- created_at (TIMESTAMP)

---

# Table: coverage_history

Fields:
- id (UUID)
- warga_id (UUID)
- payment_id (UUID)
- tanggal_kewajiban (DATE)
- created_at (TIMESTAMP)

Constraint:
UNIQUE(warga_id, tanggal_kewajiban)

---

# Offline First Architecture

Room Database:
- warga
- pembayaran
- coverage_history

Semua operasi dilakukan ke Room terlebih dahulu.

Sinkronisasi:
Room → WorkManager → Supabase

Status:
- PENDING
- SYNCED
- FAILED
- CONFLICT

---

# Petugas Flow

Login
→ Sync Data Warga
→ Dashboard
→ Scan QR
→ Cari Warga Lokal
→ Input Nominal
→ Hitung Coverage
→ Simpan Lokal
→ Status PENDING
→ Sync Otomatis

---

# Admin Dashboard

Fitur:
- Total Warga
- Total Pembayaran Hari Ini
- Total Pembayaran Bulan Ini
- Daftar Tunggakan
- Detail Riwayat Warga
- Laporan Rentang Tanggal

---

# Initial Seed Data

RT 03 / RW 01

| QR | Nama | Rumah |
|------|------|------|
| WRG001 | Edi Subekti | 009 |
| WRG002 | Samiri | 008 |
| WRG003 | Duryono | 007 |
| WRG004 | Seswa Hidayat | 001 |
| WRG005 | Casto | 002 |
| WRG006 | Turyanto | 004 |
| WRG007 | Pamuji | 005 |
| WRG008 | Datar | 006 |
| WRG009 | Anton | 010 |
| WRG010 | Karyanto | 011 |

---

# Tech Stack

Frontend:
- Kotlin
- Jetpack Compose
- CameraX
- Room Database
- WorkManager

Backend:
- Supabase Auth
- Supabase Postgres
- Supabase Storage

Source of Truth:
Supabase PostgreSQL
