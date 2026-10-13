package com.xperiaprocam.data.storage

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.xperiaprocam.domain.model.FileNameGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Saves captures into MediaStore under Pictures/XperiaProCam. No storage permission needed. */
@Singleton
class MediaStoreSaver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** @return content Uri of the saved JPEG, or null on failure. */
    fun saveJpeg(bytes: ByteArray, displayName: String = FileNameGenerator.jpeg()): Uri? = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/XperiaProCam")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        useStream(uri) { out -> out.write(bytes) }
        uri
    }.getOrNull()

    /** @return content Uri of the saved DNG RAW file, or null on failure. */
    fun saveDng(displayName: String = FileNameGenerator.dng(), writeBlock: (OutputStream) -> Unit): Uri? = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/XperiaProCam")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return null
        useStream(uri) { out -> writeBlock(out) }
        uri
    }.getOrNull()

    private inline fun useStream(uri: Uri, block: (OutputStream) -> Unit) {
        context.contentResolver.openOutputStream(uri)?.use { block(it) }
    }
}
