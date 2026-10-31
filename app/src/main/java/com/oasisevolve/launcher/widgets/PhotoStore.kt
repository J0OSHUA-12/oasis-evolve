package com.oasisevolve.launcher.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.oasisevolve.launcher.storage.WidgetPhoto
import java.io.File
import kotlin.math.max

private const val MAX_PHOTO_SIDE = 1024

/** Guarda copias reducidas de las fotos del widget en el almacenamiento privado de la app. */
class PhotoStore(private val context: Context) {
    private val directory = File(context.filesDir, "widget_photos").apply { mkdirs() }

    fun path(file: String): File = File(directory, file)

    fun delete(file: String) {
        path(file).delete()
    }

    fun import(uri: Uri): WidgetPhoto? = runCatching {
        val bitmap = decode(uri)
        val name = "${System.nanoTime()}.jpg"
        path(name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        bitmap.recycle()
        WidgetPhoto(name, takenAt(uri))
    }.getOrNull()

    private fun decode(uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= 28) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val scale = MAX_PHOTO_SIDE.toFloat() / max(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_PHOTO_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("No se pudo leer la imagen")
    }

    /** Fecha real de la foto si la galería la informa. Si no, no se muestra ninguna. */
    private fun takenAt(uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_TAKEN), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0).takeIf { it > 0 } else null
        }
    }.getOrNull()
}
