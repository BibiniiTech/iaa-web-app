/*
 * ============================================================================
 * PROPRIETARY AND CONFIDENTIAL PROPERTY OF CLEMENT KOFI ACHEAMPONG
 * (BIBINII TECH) - ALL RIGHTS RESERVED.
 *
 * Application: Internal Auditing Support & Statutory Reporting Platform (IAA App)
 * Author: Clement Kofi Acheampong (Internal Auditor & Software Engineer)
 * Copyright (c) 2024-2026 Clement Kofi Acheampong. All Rights Reserved.
 *
 * NOTICE: All information, source code, algorithms, and data designs contained
 * herein are protected under the Ghana Copyright Act, 2005 (Act 690) and
 * international intellectual property treaties. Unauthorized copying,
 * decompilation, reverse engineering, redistribution, or modification of this
 * file via any medium is strictly prohibited without prior written consent.
 * ============================================================================
 */

package com.bibiniitech.iaaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.createBitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object FileCompressionUtils {
    private const val TAG = "FileCompressionUtils"

    /**
     * Compresses a voting candidate/option image to a compact JPEG temp file (max 800x800, 75% quality).
     */
    suspend fun compressVotingImageToFile(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val compressedBytes = compressImageToBytes(
                context = context,
                uri = uri,
                maxDimension = 800,
                quality = 75
            ) ?: return@withContext null

            val tempFile = File(context.cacheDir, "compressed_voting_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { it.write(compressedBytes) }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress voting image", e)
            null
        }
    }

    /**
     * Compresses any file (PDF, image, Office doc, etc.) to a temporary file if compression reduces size.
     * Caller should delete the returned File (if non-null) after upload completes.
     */
    suspend fun compressUriToTempFile(context: Context, uri: Uri, fileName: String): File? = withContext(Dispatchers.IO) {
        try {
            val originalBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val compressedBytes = compressBytesByType(context, uri, fileName, originalBytes)
            if (compressedBytes.size < originalBytes.size) {
                val safeExt = fileName.substringAfterLast('.', "tmp").take(10)
                val tempFile = File(context.cacheDir, "upload_comp_${System.currentTimeMillis()}.$safeExt")
                FileOutputStream(tempFile).use { it.write(compressedBytes) }
                Log.d(TAG, "Compressed $fileName from ${originalBytes.size} to ${compressedBytes.size} bytes")
                tempFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing file to temp file: $fileName", e)
            null
        }
    }

    /**
     * Reads and automatically compresses attachment bytes (PDF, image, Office doc/ZIP) before uploading/emailing.
     */
    suspend fun getCompressedAttachmentBytes(
        context: Context,
        uri: Uri,
        fileName: String
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val originalBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            compressBytesByType(context, uri, fileName, originalBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read/compress attachment: $fileName", e)
            null
        }
    }

    private fun compressBytesByType(
        context: Context,
        uri: Uri,
        fileName: String,
        originalBytes: ByteArray
    ): ByteArray {
        if (originalBytes.isEmpty()) return originalBytes

        val lowerName = fileName.lowercase(Locale.ROOT)
        val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT) ?: ""

        return when {
            mimeType.startsWith("image/") ||
                lowerName.endsWith(".jpg") ||
                lowerName.endsWith(".jpeg") ||
                lowerName.endsWith(".png") ||
                lowerName.endsWith(".webp") ||
                lowerName.endsWith(".bmp") -> {
                val compressed = compressImageToBytes(context, uri, maxDimension = 1600, quality = 78)
                if (compressed != null && compressed.isNotEmpty() && compressed.size < originalBytes.size) {
                    compressed
                } else {
                    originalBytes
                }
            }

            mimeType == "application/pdf" || lowerName.endsWith(".pdf") -> {
                val compressed = compressPdfBytes(context, originalBytes)
                if (compressed != null && compressed.isNotEmpty() && compressed.size < originalBytes.size) {
                    compressed
                } else {
                    originalBytes
                }
            }

            lowerName.endsWith(".docx") ||
                lowerName.endsWith(".xlsx") ||
                lowerName.endsWith(".pptx") ||
                lowerName.endsWith(".zip") -> {
                val compressed = recompressZipBytes(originalBytes)
                if (compressed != null && compressed.isNotEmpty() && compressed.size < originalBytes.size) {
                    compressed
                } else {
                    originalBytes
                }
            }

            else -> originalBytes
        }
    }

    /**
     * Downscales and compresses an image URI into JPEG bytes while respecting EXIF orientation.
     */
    fun compressImageToBytes(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1600,
        quality: Int = 78
    ): ByteArray? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return null

            var inSampleSize = 1
            while ((srcWidth / inSampleSize) > maxDimension * 2 || (srcHeight / inSampleSize) > maxDimension * 2) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            val rotationDegrees = try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val exif = ExifInterface(input)
                    when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (_: Exception) {
                0f
            }

            val maxCurrentDim = max(decodedBitmap.width, decodedBitmap.height)
            val scale = if (maxCurrentDim > maxDimension) {
                maxDimension.toFloat() / maxCurrentDim.toFloat()
            } else {
                1f
            }

            val transformedBitmap = if (scale < 1f || rotationDegrees != 0f) {
                val matrix = Matrix().apply {
                    if (scale < 1f) postScale(scale, scale)
                    if (rotationDegrees != 0f) postRotate(rotationDegrees)
                }
                Bitmap.createBitmap(
                    decodedBitmap,
                    0,
                    0,
                    decodedBitmap.width,
                    decodedBitmap.height,
                    matrix,
                    true
                ).also {
                    if (it !== decodedBitmap) decodedBitmap.recycle()
                }
            } else {
                decodedBitmap
            }

            val out = ByteArrayOutputStream()
            transformedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            transformedBitmap.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Image compression failed", e)
            null
        }
    }

    /**
     * Compresses PDF bytes by re-rendering pages at an optimized resolution and JPEG-encoding page bitmaps.
     * Returns null if the PDF cannot be rendered or if compression does not reduce size.
     */
    private fun compressPdfBytes(context: Context, originalBytes: ByteArray): ByteArray? {
        // Skip tiny PDFs (< 150 KB) that are already compact text/vector documents
        if (originalBytes.size < 150 * 1024) return null

        val tempInput = File(context.cacheDir, "pdf_in_${System.currentTimeMillis()}.pdf")
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val newPdf = PdfDocument()

        return try {
            FileOutputStream(tempInput).use { it.write(originalBytes) }
            pfd = ParcelFileDescriptor.open(tempInput, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            val pageCount = renderer.pageCount
            // Avoid re-rendering very large book-length PDFs (> 120 pages) to prevent OOM/timeouts
            if (pageCount <= 0 || pageCount > 120) {
                return null
            }

            // If average page size is already < 60 KB/page, it is likely pure vector/text and won't shrink via rasterization
            val avgBytesPerPage = originalBytes.size / pageCount
            if (avgBytesPerPage < 60 * 1024) {
                return null
            }

            val paint = Paint(Paint.FILTER_BITMAP_FLAG)

            for (i in 0 until pageCount) {
                val page = renderer.openPage(i)
                val pageWidth = page.width
                val pageHeight = page.height

                // Render at max 1200px on longest side for crisp readability with small footprint
                val maxSide = max(pageWidth, pageHeight).coerceAtLeast(1)
                val renderScale = (1200f / maxSide.toFloat()).coerceIn(1.0f, 1.5f)
                val bitmapWidth = (pageWidth * renderScale).roundToInt().coerceAtLeast(1)
                val bitmapHeight = (pageHeight * renderScale).roundToInt().coerceAtLeast(1)

                val rawBitmap = createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
                Canvas(rawBitmap).drawColor(Color.WHITE)
                page.render(rawBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                // Compress page bitmap via JPEG (72% quality) and decode as RGB_565 to minimize PDF stream size
                val jpegOut = ByteArrayOutputStream()
                rawBitmap.compress(Bitmap.CompressFormat.JPEG, 72, jpegOut)
                rawBitmap.recycle()

                val jpegBytes = jpegOut.toByteArray()
                val rgb565Opts = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val compressedPageBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size, rgb565Opts)

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                val pdfPage = newPdf.startPage(pageInfo)
                if (compressedPageBitmap != null) {
                    val destRect = Rect(0, 0, pageWidth, pageHeight)
                    pdfPage.canvas.drawBitmap(compressedPageBitmap, null, destRect, paint)
                    compressedPageBitmap.recycle()
                }
                newPdf.finishPage(pdfPage)
            }

            val pdfOut = ByteArrayOutputStream()
            newPdf.writeTo(pdfOut)
            val resultBytes = pdfOut.toByteArray()

            if (resultBytes.isNotEmpty() && resultBytes.size < originalBytes.size) {
                resultBytes
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDF compression failed, keeping original PDF", e)
            null
        } finally {
            try { newPdf.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            try { tempInput.delete() } catch (_: Exception) {}
        }
    }

    /**
     * Re-compresses ZIP-based archives (.docx, .xlsx, .pptx, .zip) using Deflater.BEST_COMPRESSION.
     */
    private fun recompressZipBytes(originalBytes: ByteArray): ByteArray? {
        return try {
            val out = ByteArrayOutputStream()
            ZipInputStream(ByteArrayInputStream(originalBytes)).use { zis ->
                ZipOutputStream(out).use { zos ->
                    zos.setLevel(Deflater.BEST_COMPRESSION)
                    val buffer = ByteArray(8192)
                    var entry: ZipEntry? = zis.nextEntry
                    var entryCount = 0
                    while (entry != null) {
                        entryCount++
                        val newEntry = ZipEntry(entry.name)
                        if (entry.time != -1L) newEntry.time = entry.time
                        zos.putNextEntry(newEntry)
                        if (!entry.isDirectory) {
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                zos.write(buffer, 0, len)
                            }
                        }
                        zos.closeEntry()
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                    if (entryCount == 0) return null
                }
            }
            out.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "ZIP/Office recompression failed", e)
            null
        }
    }
}
