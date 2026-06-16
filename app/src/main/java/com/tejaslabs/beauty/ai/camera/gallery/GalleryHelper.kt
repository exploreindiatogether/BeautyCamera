package com.tejaslabs.beauty.ai.camera.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

object GalleryHelper {
    private const val TAG = "GalleryHelper"

    data class PhotoItem(
        val id: Long,
        val uri: Uri,
        val albumName: String,
        val dateAdded: Long
    )

    data class AlbumItem(
        val name: String,
        val coverUri: Uri,
        val photoCount: Int
    )

    fun fetchPhotos(context: Context): List<PhotoItem> {
        val photos = mutableListOf<PhotoItem>()
        
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val albumName = cursor.getString(bucketColumn) ?: "Camera"
                    val dateAdded = cursor.getLong(dateColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    photos.add(PhotoItem(id, contentUri, albumName, dateAdded))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying media store photos", e)
        }

        return photos
    }

    fun fetchAlbums(context: Context): List<AlbumItem> {
        val photos = fetchPhotos(context)
        val albumMap = mutableMapOf<String, MutableList<Uri>>()

        for (photo in photos) {
            val list = albumMap.getOrPut(photo.albumName) { mutableListOf() }
            list.add(photo.uri)
        }

        return albumMap.map { (name, uris) ->
            AlbumItem(
                name = name,
                coverUri = uris.first(),
                photoCount = uris.size
            )
        }.sortedByDescending { it.photoCount }
    }
}
