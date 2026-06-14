package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.remote.SupabaseService
import com.example.data.repository.JimpitanRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object Injection {
    private var database: AppDatabase? = null
    private var repository: JimpitanRepository? = null

    fun provideRepository(context: Context): JimpitanRepository {
        if (repository == null) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, "jimpitan-db"
            )
                .fallbackToDestructiveMigration()
                .build()
            
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.SUPABASE_URL + "/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            val api = retrofit.create(SupabaseService::class.java)

            repository = JimpitanRepository(api, db.jimpitanDao(), BuildConfig.SUPABASE_ANON_KEY)
        }
        return repository!!
    }
}
