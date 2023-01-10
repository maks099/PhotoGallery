package com.example.photogallery.api

import android.net.Uri
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GalleryItem(
    val id: String,
    val title: String,
    @Json(name = "url_s")
    val url: String,
    val owner: String
) {
    val photoPageUri: Uri
        get() = Uri.parse("https://www.flickr.com/photos/")
            .buildUpon()
            .appendPath(owner)
            .appendPath(id)
            .build()
}
