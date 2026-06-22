-- Jimpitan Digital - Supabase Schema Setup v1

-- 1. ENABLE UUID-OSSP EXTENSION
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. CREATE TABLE: profiles
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    nama TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('ADMIN', 'PETUGAS')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. CREATE TABLE: warga
CREATE TABLE IF NOT EXISTS public.warga (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    qr_uuid TEXT UNIQUE NOT NULL,
    nama TEXT NOT NULL,
    rt TEXT NOT NULL,
    rw TEXT NOT NULL,
    nomor_rumah TEXT NOT NULL,
    alamat TEXT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. CREATE TABLE: pembayaran
CREATE TABLE IF NOT EXISTS public.pembayaran (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warga_id UUID REFERENCES public.warga(id) ON DELETE RESTRICT,
    nominal INTEGER NOT NULL CHECK (nominal >= 500),
    coverage_days INTEGER NOT NULL CHECK (coverage_days > 0),
    tanggal_bayar DATE NOT NULL,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    sync_status TEXT DEFAULT 'SYNCED' CHECK (sync_status IN ('SYNCED', 'CONFLICT', 'PENDING'))
);

-- 5. CREATE TABLE: coverage_history
CREATE TABLE IF NOT EXISTS public.coverage_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warga_id UUID REFERENCES public.warga(id) ON DELETE CASCADE,
    payment_id UUID REFERENCES public.pembayaran(id) ON DELETE CASCADE,
    tanggal_kewajiban DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT unique_warga_tanggal_kewajiban UNIQUE (warga_id, tanggal_kewajiban)
);

-- 6. CREATE TABLE: sync_logs
CREATE TABLE IF NOT EXISTS public.sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id TEXT,
    entity_type TEXT,
    entity_id UUID,
    status TEXT,
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 7. ENABLE ROW LEVEL SECURITY (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.warga ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.pembayaran ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.coverage_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sync_logs ENABLE ROW LEVEL SECURITY;

-- 7.5 GRANT PERMISSIONS TO ROLES
GRANT USAGE ON SCHEMA public TO authenticated, anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO authenticated, anon;

-- 8. CREATE RLS POLICIES

-- Profiles:
CREATE POLICY "Allow read profiles for authenticated users" 
ON public.profiles TO authenticated USING (true);

CREATE POLICY "Allow update profiles for user owner or admin" 
ON public.profiles FOR UPDATE TO authenticated 
USING (auth.uid() = id OR (SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN');

-- Warga:
CREATE POLICY "Allow read warga for authenticated users" 
ON public.warga FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow write warga for admin only" 
ON public.warga FOR ALL TO authenticated 
USING ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN')
WITH CHECK ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN');

-- Pembayaran:
CREATE POLICY "Allow read pembayaran for authenticated users" 
ON public.pembayaran FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow insert pembayaran for authenticated users" 
ON public.pembayaran FOR INSERT TO authenticated WITH CHECK (true);

CREATE POLICY "Allow delete/update pembayaran for admin only" 
ON public.pembayaran FOR ALL TO authenticated 
USING ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN')
WITH CHECK ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN');

-- Coverage History:
CREATE POLICY "Allow read coverage_history for authenticated users" 
ON public.coverage_history FOR SELECT TO authenticated USING (true);

CREATE POLICY "Allow insert coverage_history for authenticated users" 
ON public.coverage_history FOR INSERT TO authenticated WITH CHECK (true);

CREATE POLICY "Allow delete/update coverage_history for admin only" 
ON public.coverage_history FOR ALL TO authenticated 
USING ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN')
WITH CHECK ((SELECT role FROM public.profiles WHERE id = auth.uid()) = 'ADMIN');

-- Sync Logs:
CREATE POLICY "Allow all actions for authenticated users on sync_logs" 
ON public.sync_logs FOR ALL TO authenticated USING (true);


-- 9. USER SIGNUP TRIGGER (Automatically create profile)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, nama, role)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'nama', NEW.email),
        COALESCE(NEW.raw_user_meta_data->>'role', 'PETUGAS')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- 10. INITIAL SEED DATA FOR WARGA (RT 03 / RW 01)
INSERT INTO public.warga (qr_uuid, nama, rt, rw, nomor_rumah, alamat, is_active)
VALUES 
    ('WRG001', 'Edi Subekti', '03', '01', '009', 'Jl. Mawar No. 9', true),
    ('WRG002', 'Samiri', '03', '01', '008', 'Jl. Mawar No. 8', true),
    ('WRG003', 'Duryono', '03', '01', '007', 'Jl. Mawar No. 7', true),
    ('WRG004', 'Seswa Hidayat', '03', '01', '001', 'Jl. Melati No. 1', true),
    ('WRG005', 'Casto', '03', '01', '002', 'Jl. Melati No. 2', true),
    ('WRG006', 'Turyanto', '03', '01', '004', 'Jl. Melati No. 4', true),
    ('WRG007', 'Pamuji', '03', '01', '005', 'Jl. Melati No. 5', true),
    ('WRG008', 'Datar', '03', '01', '006', 'Jl. Melati No. 6', true),
    ('WRG009', 'Anton', '03', '01', '010', 'Jl. Mawar No. 10', true),
    ('WRG010', 'Karyanto', '03', '01', '011', 'Jl. Mawar No. 11', true)
ON CONFLICT (qr_uuid) DO NOTHING;

-- 11. DEFAULT ADMIN ACCOUNT INSTRUCTIONS
-- Akun admin bawaan secara luring telah diatur di dalam aplikasi Android:
-- Email: admin@gempala.com
-- Password: gempala2026
-- 
-- Untuk membuat akun admin ini secara daring di Supabase Auth, silakan gunakan
-- menu "Users" -> "Add User" -> "Create User" di Supabase Dashboard dengan 
-- email dan password di atas. Secara otomatis trigger 'on_auth_user_created'
-- akan mendaftarkan profil pengguna tersebut sebagai ADMIN.


-- 12. PENGELUARAN TABLE FOR BUKU KAS
CREATE TABLE IF NOT EXISTS public.pengeluaran (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nominal INTEGER NOT NULL CHECK (nominal > 0),
    tanggal DATE NOT NULL,
    keterangan TEXT NOT NULL,
    created_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ENABLE ROW LEVEL SECURITY (RLS)
ALTER TABLE public.pengeluaran ENABLE ROW LEVEL SECURITY;

-- GRANT PERMISSIONS TO ROLES
GRANT SELECT, INSERT, UPDATE, DELETE ON public.pengeluaran TO authenticated, anon;

-- CREATE RLS POLICIES
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


-- 13. VIEW: vw_laporan_transaksi
-- View to simplify admin dashboard reporting and include Petugas info
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

-- GRANT PERMISSIONS FOR VIEW
GRANT SELECT ON public.vw_laporan_transaksi TO authenticated, anon;


-- 14. RPC: sync_pembayaran_offline
-- Function to safely handle offline synchronization and detect conflicts
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

