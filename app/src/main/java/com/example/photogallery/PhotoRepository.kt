package com.example.photogallery

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.photogallery.api.*
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create

private const val TAG1 = "PhotoRepository"
const val NETWORK_PAGE_SIZE = 25

class PhotoRepository {
    private val flickrApi: FlickrApi


    init {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(PhotoInterceptor())
            .build()

        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://api.flickr.com/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient)
            .build()

        flickrApi = retrofit.create<FlickrApi>()
    }

    fun fetchPhotos(): Flow<PagingData<GalleryItem>>{
       return getPager(REQUEST_TYPE.photos, null)
    }

    private fun getPager(requestType: REQUEST_TYPE, params: List<String>?): Flow<PagingData<GalleryItem>>{
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                FlickrResponsePagingSource(api = flickrApi, requestType, params)
            }
        ).flow
    }

    fun searchPhotos(query: String): Flow<PagingData<GalleryItem>>{
        Log.d(TAG1, "searchPhotos call: $query")
        return getPager(REQUEST_TYPE.search, listOf(query))
    }

    suspend fun searchPhotosAPI(query: String): FlickrResponse{
        return flickrApi.searchPhotos(query = query)
    }
}