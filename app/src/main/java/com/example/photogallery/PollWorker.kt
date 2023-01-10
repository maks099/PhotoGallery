package com.example.photogallery

import android.app.PendingIntent
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.paging.map
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.photogallery.api.GalleryItem
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first

private const val TAG = "PollWorker"

// class for work in background
class PollWorker(
    private val context: Context ,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters){

    override suspend fun doWork(): Result {
        val preferencesRepository = PreferencesRepository.get()
        val photoRepository = PhotoRepository()

        val query = preferencesRepository.storedQuery.first()
        val lastId = preferencesRepository.lastResultId.first()

        if(query.isEmpty()){
            Log.i(TAG, "No saved query, finishing early")
            return Result.success()
        }

        return try {
            val list = photoRepository.searchPhotosAPI("query").photos.galleryItems
            if (list.isNotEmpty()) {
                val newResultId = list.first().id
                if (newResultId == lastId) {
                    Log.i(TAG , "Still have the same result: $newResultId")
                } else {
                    Log.i(TAG , "Got a new result: $newResultId")
                    preferencesRepository.setLastResultId(newResultId)
                    notifyUser()
                }

            }


                Result.success()
            } catch (ex: Exception){
                Log.e(TAG , "Backround task failed" , ex)
                Result.failure()
            }

    }

    private fun notifyUser() {
        val intent = MainActivity.newIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val resources = context.resources
        val notification = NotificationCompat
            .Builder(context, NOTIFICATION_CHANNEL_ID)
            .setTicker(resources.getString(R.string.new_pictures_title))
            .setSmallIcon(android.R.drawable.ic_menu_report_image)
            .setContentTitle(resources.getString(R.string.new_pictures_title))
            .setContentText(resources.getString(R.string.new_pictures_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(0, notification)
    }
}