package shared.Utility

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

private const val BASE_URL = "https://7h6nr2h6n5amtaadd5db7gbu2i.appsync-api.us-west-2.amazonaws.com/"

private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

private val retrofit =
    Retrofit.Builder()
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .baseUrl(BASE_URL)
        .build()

interface ApiService {
    @POST("graphql")
    @Headers("Content-Type: application/json", "x-api-key: da2-gnn7q3s2izhrnis7hypn3zt7ue")
    fun query(@Body data: RequestBody): Deferred<String>;
}

object HTTP {
    val retrofitService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}