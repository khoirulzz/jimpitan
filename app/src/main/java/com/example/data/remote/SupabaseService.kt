package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
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
    val role: String
)

@JsonClass(generateAdapter = true)
data class AuthRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    val id: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val access_token: String,
    val user: UserDto
)

interface SupabaseService {

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Header("apikey") apiKey: String,
        @Body req: AuthRequest
    ): AuthResponse

    @GET("rest/v1/profiles?select=*")
    suspend fun getProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("id") idFilter: String
    ): List<ProfileDto>

    @GET("rest/v1/warga?select=*")
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

    @POST("rest/v1/pembayaran")
    suspend fun insertPembayaran(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body req: PembayaranDto
    ): List<PembayaranDto>

    @POST("rest/v1/coverage_history")
    suspend fun insertCoverage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body req: CoverageDto
    ): List<CoverageDto>
}
