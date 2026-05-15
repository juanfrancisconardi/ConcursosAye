package ar.gov.entrerios.cge.concursos.di

import ar.gov.entrerios.cge.concursos.BuildConfig
import ar.gov.entrerios.cge.concursos.core.network.CgeApi
import ar.gov.entrerios.cge.concursos.core.network.HtmlStringConverter
import ar.gov.entrerios.cge.concursos.core.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logger = HttpLoggingInterceptor { message -> Timber.tag("OkHttp").v(message) }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        val uaInterceptor = okhttp3.Interceptor { chain ->
            val req = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    "ConcursosCGE/1.0 (Android; +https://cge.entrerios.gov.ar)"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "es-AR,es;q=0.9")
                .build()
            chain.proceed(req)
        }

        return OkHttpClient.Builder()
            .addInterceptor(uaInterceptor)
            .addInterceptor(logger)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(HtmlStringConverter())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideCgeApi(retrofit: Retrofit): CgeApi = retrofit.create(CgeApi::class.java)
}
