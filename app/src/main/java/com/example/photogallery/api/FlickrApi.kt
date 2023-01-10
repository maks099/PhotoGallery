package com.example.photogallery.api

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FlickrApi {
    @GET("services/rest/?method=flickr.interestingness.getList")
    suspend fun fetchPhotos(@Query("page")  page: Int = 1): FlickrResponse

    @GET("services/rest/?method=flickr.photos.search")
    suspend fun searchPhotos(
        @Query("page")  page: Int = 1,
        @Query("text") query: String
    ): FlickrResponse
}