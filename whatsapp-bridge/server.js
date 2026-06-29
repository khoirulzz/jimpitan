require('dotenv').config();
const express = require('express');
const { Client, RemoteAuth } = require('whatsapp-web.js');
const { createClient } = require('@supabase/supabase-js');
const SupabaseStorageStore = require('./SupabaseStorageStore');

const app = express();
app.use(express.json());

const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);

// ─── Jam Operasional ────────────────────────────────────────────────────────
function isWithinOperationalHours() {
  const wibHour = (new Date().getUTCHours() + 7) % 24;
  return wibHour >= 20 || wibHour < 3; // 20:00 - 03:00 WIB
}

// ─── WhatsApp Client ─────────────────────────────────────────────────────────
const store = new SupabaseStorageStore();
const client = new Client({
  authStrategy: new RemoteAuth({
    store,
    backupSyncIntervalMs: 300000, // backup setiap 5 menit
  }),
  puppeteer: {
    headless: true,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--single-process',
    ],
  },
});

let qrCodeData = null;
let isReady = false;

client.on('qr', (qr) => {
  qrCodeData = qr;
  isReady = false;
  console.log('[WA] QR code baru tersedia. Buka /qr untuk scan.');
});

client.on('ready', () => {
  isReady = true;
  qrCodeData = null;
  console.log('[WA] Client siap mengirim pesan!');
});

client.on('disconnected', (reason) => {
  isReady = false;
  console.log('[WA] Disconnected:', reason);
});

client.initialize();

app.get('/', (req, res) => {
  res.redirect('/qr');
});

// ─── Endpoint: Health Check (untuk keep-alive cron) ──────────────────────────
app.get('/health', (req, res) => {
  res.json({ status: 'ok', wa_ready: isReady });
});

// ─── Endpoint: QR Code ───────────────────────────────────────────────────────
app.get('/qr', async (req, res) => {
  if (isReady) return res.send('<h2>WhatsApp sudah terhubung ✅</h2>');
  if (!qrCodeData) return res.send('<h2>Menunggu QR code...</h2>');
  const QRCode = require('qrcode');
  const svg = await QRCode.toString(qrCodeData, { type: 'svg' });
  res.send(`<html><body>${svg}<p>Scan QR dengan WhatsApp</p></body></html>`);
});

// ─── Endpoint: Webhook Pembayaran ────────────────────────────────────────────
app.post('/webhook/payment', async (req, res) => {
  // Validasi secret key
  const secret = req.headers['x-webhook-secret'];
  if (secret !== process.env.WEBHOOK_SECRET) {
    return res.status(403).json({ error: 'Forbidden' });
  }

  // Selalu balas 200 dulu agar Supabase tidak retry
  res.status(200).json({ status: 'received' });

  // Cek jam operasional
  if (!isWithinOperationalHours()) {
    console.log('[WA] Di luar jam operasional, pesan tidak dikirim.');
    return;
  }

  if (!isReady) {
    console.warn('[WA] Client belum siap, pesan dibatalkan.');
    return;
  }

  const { warga_id, nominal, coverage_days, tanggal_bayar } = req.body;

  try {
    // Ambil data warga (termasuk no_wa) dari Supabase
    const { data: wargaData, error } = await supabase
      .from('warga')
      .select('nama, no_wa')
      .eq('id', warga_id)
      .single();

    if (error || !wargaData?.no_wa) {
      console.warn(`[WA] Warga ${warga_id} tidak punya nomor WA, skip.`);
      return;
    }

    const { nama, no_wa } = wargaData;
    const waNumber = `${no_wa}@c.us`;

    const message =
      `✅ *Konfirmasi Pembayaran Jimpitan*\n\n` +
      `Halo Bapak/Ibu *${nama}*, pembayaran jimpitan Anda telah tercatat:\n\n` +
      `📅 Tanggal: ${tanggal_bayar}\n` +
      `💰 Nominal: Rp${nominal.toLocaleString('id-ID')}\n` +
      `📆 Masa berlaku: ${coverage_days} hari\n\n` +
      `Terima kasih atas partisipasi Anda! 🙏\n` +
      `_- Pengurus RT 03 / RW 01_`;

    await client.sendMessage(waNumber, message);
    console.log(`[WA] Pesan terkirim ke ${nama} (${no_wa})`);
  } catch (err) {
    console.error('[WA] Gagal kirim pesan:', err.message);
  }
});

const PORT = process.env.PORT || 7860;
app.listen(PORT, '0.0.0.0', () => console.log(`[Server] Berjalan di port ${PORT}`));
