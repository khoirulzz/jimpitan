package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class WargaDto(
    val id: String,
    val qr_uuid: String,
    val nama: String,
    val rt: String,
    val rw: String,
    val nomor_rumah: String,
    val alamat: String,
    val is_active: Boolean,
    val updated_at: String
)

@JsonClass(generateAdapter = true)
data class PembayaranDto(
    val id: String? = null,
    val warga_id: String,
    val nominal: Int,
    val coverage_days: Int,
    val tanggal_bayar: String,
    val created_by: String
)

@JsonClass(generateAdapter = true)
data class CoverageDto(
    val id: String? = null,
    val warga_id: String,
    val payment_id: String,
    val tanggal_kewajiban: String
)

@JsonClass(generateAdapter = true)
data class ProfileDto(
    val id: String,
    val nama: String,
    val role: String,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val access_token: String,
    val user: UserDto
)

@JsonClass(generateAdapter = true)
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>
)

interface SupabaseService {

    // ─── Auth ─────────────────────────────────────────────────────────────────

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Header("apikey") apiKey: String,
        @Body req: AuthRequest
    ): AuthResponse

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body req: SignUpRequest
    ): AuthResponse

    // ─── Profiles ─────────────────────────────────────────────────────────────

    @GET("rest/v1/profiles?select=*")
    suspend fun getProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): List<ProfileDto>

    @GET("rest/v1/profiles?select=*&role=eq.PETUGAS&order=created_at.asc")
    suspend fun getPetugas(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<ProfileDto>

    // ─── Warga ────────────────────────────────────────────────────────────────

    @GET("rest/v1/warga?select=*&is_active=eq.true&order=nama.asc")
    suspend fun getWarga(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<WargaDto>

    @POST("rest/v1/warga")
    suspend fun insertWarga(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body req: WargaDto
    ): List<WargaDto>

    // ─── Pembayaran ───────────────────────────────────────────────────────────

    @GET("rest/v1/pembayaran?select=*&order=tanggal_bayar.desc")
    suspend fun getPembayaran(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<PembayaranDto>

    @POST("rest/v1/pembayaran")
    suspend fun insertPembayaran(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body req: PembayaranDto
    ): List<PembayaranDto>

    // ─── Coverage History ─────────────────────────────────────────────────────

    @GET("rest/v1/coverage_history?select=*&order=tanggal_kewajiban.asc")
    suspend fun getCoverageHistory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<CoverageDto>

    @POST("rest/v1/coverage_history")
    suspend fun insertCoverage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body req: CoverageDto
    ): List<CoverageDto>

    // ─── OTA Updater ──────────────────────────────────────────────────────────

    @GET("rest/v1/app_versions?select=*&order=version_code.desc&limit=1")
    suspend fun getLatestVersion(
        @Header("apikey") apiKey: String
    ): List<AppVersionDto>
}
