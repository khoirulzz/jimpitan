package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppVersionDto(
    @Json(name = "version_code") val versionCode: Int,
    @Json(name = "version_name") val versionName: String,
    @Json(name = "release_notes") val releaseNotes: String?,
    @Json(name = "apk_url") val apkUrl: String
)
