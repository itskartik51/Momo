package com.personal.momo.UI_Screens.Chat.Media

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class MediaEncryptionResult(
    val encryptedFile: File,
    val base64Key: String,
    val base64Iv: String
)

object MediaCrypto {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val AES_KEY_SIZE_BITS = 256
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val STREAM_BUFFER_SIZE_BYTES = 64 * 1024

    suspend fun encryptFile(
        context: Context,
        inputFile: File
    ): MediaEncryptionResult = withContext(Dispatchers.IO) {
        if (!inputFile.exists() || inputFile.length() == 0L) {
            throw IllegalArgumentException("Input file for encryption does not exist or is empty: ${inputFile.absolutePath}")
        }

        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE_BITS)
        val secretKey = keyGen.generateKey()

        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cacheFolder = File(context.cacheDir, "media_cache").apply {
            if (!exists()) mkdirs()
        }
        val encryptedFile = File(cacheFolder, "ENC_${UUID.randomUUID().toString().take(8)}.enc")

        try {
            FileInputStream(inputFile).use { inputStream ->
                FileOutputStream(encryptedFile).use { outputStream ->
                    val buffer = ByteArray(STREAM_BUFFER_SIZE_BYTES)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val updateBytes = cipher.update(buffer, 0, bytesRead)
                        if (updateBytes != null && updateBytes.isNotEmpty()) {
                            outputStream.write(updateBytes)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null && finalBytes.isNotEmpty()) {
                        outputStream.write(finalBytes)
                    }
                }
            }
        } catch (e: Exception) {
            if (encryptedFile.exists()) {
                encryptedFile.delete()
            }
            throw e
        }

        val base64Key = Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
        val base64Iv = Base64.encodeToString(iv, Base64.NO_WRAP)

        MediaEncryptionResult(
            encryptedFile = encryptedFile,
            base64Key = base64Key,
            base64Iv = base64Iv
        )
    }

    suspend fun decryptFile(
        context: Context,
        encryptedFile: File,
        base64Key: String,
        base64Iv: String,
        outputExtension: String
    ): File = withContext(Dispatchers.IO) {
        if (!encryptedFile.exists() || encryptedFile.length() == 0L) {
            throw IllegalArgumentException("Encrypted source file does not exist or is empty: ${encryptedFile.absolutePath}")
        }

        val keyBytes = Base64.decode(base64Key, Base64.NO_WRAP)
        val ivBytes = Base64.decode(base64Iv, Base64.NO_WRAP)

        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, ivBytes)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val mediaFolder = File(context.filesDir, "chat_media").apply {
            if (!exists()) mkdirs()
        }
        val cleanExtension = outputExtension.trimStart('.').ifEmpty { "dat" }
        val decryptedFile = File(mediaFolder, "MEDIA_${UUID.randomUUID().toString().take(8)}.$cleanExtension")

        try {
            FileInputStream(encryptedFile).use { inputStream ->
                FileOutputStream(decryptedFile).use { outputStream ->
                    val buffer = ByteArray(STREAM_BUFFER_SIZE_BYTES)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val updateBytes = cipher.update(buffer, 0, bytesRead)
                        if (updateBytes != null && updateBytes.isNotEmpty()) {
                            outputStream.write(updateBytes)
                        }
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null && finalBytes.isNotEmpty()) {
                        outputStream.write(finalBytes)
                    }
                }
            }
        } catch (e: Exception) {
            if (decryptedFile.exists()) {
                decryptedFile.delete()
            }
            throw e
        }

        decryptedFile
    }

    fun getLocalMediaFile(context: Context, fileName: String): File? {
        val file = File(File(context.filesDir, "chat_media"), fileName)
        return if (file.exists() && file.length() > 0L) file else null
    }

    fun deleteLocalMedia(context: Context, fileName: String): Boolean {
        return try {
            val file = File(File(context.filesDir, "chat_media"), fileName)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }
}
