-- =========================================================================
-- SQL UPDATE JIMPITAN DIGITAL (KHUSUS FITUR BARU)
-- Silakan jalankan seluruh query ini di SQL Editor Supabase
-- =========================================================================

-- 1. TAMBAH KOLOM sync_status DI TABEL pembayaran (Jika belum ada)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'pembayaran' AND column_name = 'sync_status') THEN
        ALTER TABLE public.pembayaran ADD COLUMN sync_status TEXT DEFAULT 'SYNCED' CHECK (sync_status IN ('SYNCED', 'CONFLICT', 'PENDING'));
    END IF;
END $$;


-- 2. CREATE TABLE: pengeluaran (Untuk Fitur Buku Kas)
CREATE TABLE IF NOT EXISTS public.pengeluaran (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nominal INTEGER NOT NULL CHECK (nominal > 0),
    tanggal DATE NOT NULL,
    keterangan TEXT NOT NULL,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- RLS untuk tabel pengeluaran
ALTER TABLE public.pengeluaran ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.pengeluaran TO authenticated, anon;

DROP POLICY IF EXISTS "Allow read pengeluaran for authenticated users" ON public.pengeluaran;
CREATE POLICY "Allow read pengeluaran for authenticated users" 
ON public.pengeluaran FOR SELECT TO authenticated USING (true);

DROP POLICY IF EXISTS "Allow insert pengeluaran for admin only" ON public.pengeluaran;
CREATE POLICY "Allow insert pengeluaran for admin only" 
ON public.pengeluaran FOR INSERT TO authenticated 
WITH CHECK ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN');

DROP POLICY IF EXISTS "Allow update/delete pengeluaran for admin only" ON public.pengeluaran;
CREATE POLICY "Allow update/delete pengeluaran for admin only" 
ON public.pengeluaran FOR ALL TO authenticated 
USING ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN')
WITH CHECK ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN');


-- 3. CREATE TABLE: sync_logs (Untuk Log Sinkronisasi)
CREATE TABLE IF NOT EXISTS public.sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT,
    entity_type TEXT,
    entity_id UUID,
    status TEXT,
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE public.sync_logs ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON public.sync_logs TO authenticated, anon;

DROP POLICY IF EXISTS "Allow all actions for authenticated users on sync_logs" ON public.sync_logs;
CREATE POLICY "Allow all actions for authenticated users on sync_logs" 
ON public.sync_logs FOR ALL TO authenticated USING (true);


-- 4. VIEW: vw_laporan_transaksi (Menambahkan nama petugas)
CREATE OR REPLACE VIEW public.vw_laporan_transaksi AS
SELECT 
    p.id AS payment_id,
    w.qr_uuid AS id_warga,
    w.nama AS nama_warga,
    p.nominal,
    p.coverage_days,
    p.tanggal_bayar,
    p.created_at,
    p.sync_status,
    pr.id AS id_petugas,
    pr.nama AS nama_petugas
FROM public.pembayaran p
JOIN public.warga w ON p.warga_id = w.id
JOIN public.profiles pr ON p.created_by = pr.id;

GRANT SELECT ON public.vw_laporan_transaksi TO authenticated, anon;


-- 5. RPC: sync_pembayaran_offline (Logika Anti-Conflict)
CREATE OR REPLACE FUNCTION public.sync_pembayaran_offline(
    p_id UUID,
    p_warga_id UUID,
    p_nominal INTEGER,
    p_coverage_days INTEGER,
    p_tanggal_bayar DATE,
    p_created_by UUID,
    p_created_at TIMESTAMP WITH TIME ZONE,
    p_coverage_dates DATE[]
) RETURNS TEXT AS $$
DECLARE
    v_conflict BOOLEAN := false;
    v_date DATE;
BEGIN
    -- Check if any of the coverage dates already exist for this warga
    FOREACH v_date IN ARRAY p_coverage_dates
    LOOP
        IF EXISTS (SELECT 1 FROM public.coverage_history WHERE warga_id = p_warga_id AND tanggal_kewajiban = v_date) THEN
            v_conflict := true;
            EXIT;
        END IF;
    END LOOP;

    IF v_conflict THEN
        -- Store as CONFLICT, do not insert into coverage_history
        INSERT INTO public.pembayaran (id, warga_id, nominal, coverage_days, tanggal_bayar, created_by, created_at, sync_status)
        VALUES (p_id, p_warga_id, p_nominal, p_coverage_days, p_tanggal_bayar, p_created_by, p_created_at, 'CONFLICT');
        RETURN 'CONFLICT';
    ELSE
        -- Store as SYNCED
        INSERT INTO public.pembayaran (id, warga_id, nominal, coverage_days, tanggal_bayar, created_by, created_at, sync_status)
        VALUES (p_id, p_warga_id, p_nominal, p_coverage_days, p_tanggal_bayar, p_created_by, p_created_at, 'SYNCED');
        
        -- Insert coverage dates
        FOREACH v_date IN ARRAY p_coverage_dates
        LOOP
            INSERT INTO public.coverage_history (warga_id, payment_id, tanggal_kewajiban)
            VALUES (p_warga_id, p_id, v_date);
        END LOOP;
        
        RETURN 'SYNCED';
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;


-- 6. TAMBAH KOLOM no_wa DI TABEL warga
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'warga' AND column_name = 'no_wa') THEN
        ALTER TABLE public.warga ADD COLUMN no_wa TEXT;
    END IF;
END $$;

-- 7. AKTIFKAN EXTENSION pg_net
CREATE EXTENSION IF NOT EXISTS pg_net;

-- 8. BUAT TRIGGER WEBHOOK
CREATE OR REPLACE FUNCTION public.notify_whatsapp_bridge()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.sync_status = 'SYNCED' THEN
    PERFORM net.http_post(
      url := 'https://YOUR_WHATSAPP_BRIDGE_URL/webhook/payment',
      headers := '{"Content-Type": "application/json", "x-webhook-secret": "YOUR_SECRET_KEY"}'::jsonb,
      body := jsonb_build_object(
        'payment_id', NEW.id,
        'warga_id', NEW.warga_id,
        'nominal', NEW.nominal,
        'coverage_days', NEW.coverage_days,
        'tanggal_bayar', NEW.tanggal_bayar,
        'created_by', NEW.created_by
      )
    );
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS trigger_whatsapp_notification ON public.pembayaran;
CREATE TRIGGER trigger_whatsapp_notification
AFTER INSERT ON public.pembayaran
FOR EACH ROW
EXECUTE FUNCTION public.notify_whatsapp_bridge();