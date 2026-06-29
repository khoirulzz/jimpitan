const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');

class SupabaseStorageStore {
  constructor() {
    this.supabase = createClient(
      process.env.SUPABASE_URL,
      process.env.SUPABASE_SERVICE_ROLE_KEY
    );
    this.BUCKET = 'whatsapp-sessions';
    this.SESSION_FILE = 'session.zip';
  }

  async sessionExists({ session }) {
    const { data } = await this.supabase.storage
      .from(this.BUCKET)
      .list('', { search: this.SESSION_FILE });
    return !!(data && data.length > 0);
  }

  async save({ session, path }) {
    const fileBuffer = fs.readFileSync(path);
    await this.supabase.storage
      .from(this.BUCKET)
      .upload(this.SESSION_FILE, fileBuffer, {
        upsert: true,
        contentType: 'application/zip',
      });
  }

  async extract({ session, path }) {
    const { data, error } = await this.supabase.storage
      .from(this.BUCKET)
      .download(this.SESSION_FILE);
    if (error) throw error;
    const buffer = Buffer.from(await data.arrayBuffer());
    fs.writeFileSync(path, buffer);
  }

  async delete({ session }) {
    await this.supabase.storage
      .from(this.BUCKET)
      .remove([this.SESSION_FILE]);
  }
}

module.exports = SupabaseStorageStore;
