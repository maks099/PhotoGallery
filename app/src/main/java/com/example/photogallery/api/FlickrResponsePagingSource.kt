package com.example.photogallery.api

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.photogallery.NETWORK_PAGE_SIZE
import okhttp3.Request
import retrofit2.HttpException
import java.io.IOException

enum class REQUEST_TYPE {photos, search}

private val TMDB_STARTING_PAGE_INDEX: Int = 1
const val TAG = "FlickrResponsePagingSource"

class FlickrResponsePagingSource(val api: FlickrApi, val requestType: REQUEST_TYPE, val paramsData: List<String>?) : PagingSource<Int, GalleryItem>() {

    override fun getRefreshKey(state: PagingState<Int, GalleryItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, GalleryItem> {
        val pageIndex = params.key ?: TMDB_STARTING_PAGE_INDEX
        return try {
            Log.d(TAG, "Current page: $pageIndex")
            val response = when(requestType){
                REQUEST_TYPE.search -> api.searchPhotos(pageIndex, paramsData!![0])
                else -> api.fetchPhotos(pageIndex)
            }
            val photos = response.photos.galleryItems
            val nextKey =
                if (photos.isEmpty()) {
                    null
                } else {
                    // By default, initial load size = 3 * NETWORK PAGE SIZE
                    // ensure we're not requesting duplicating items at the 2nd request
                    pageIndex + (1)
                }
            Log.d(TAG, "nextKey: ${params.loadSize / NETWORK_PAGE_SIZE}")
            LoadResult.Page(
                data = photos,
                prevKey = if (pageIndex == TMDB_STARTING_PAGE_INDEX) null else pageIndex,
                nextKey = nextKey
            )
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}