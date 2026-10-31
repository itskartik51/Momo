package com.personal.momo.UI_Screens.Chat.Media

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

object FirebaseMediaManager {

    private const val STORAGE_FOLDER = "chat_media"

    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private val storageRef: StorageReference by lazy {
        storage.reference.child(STORAGE_FOLDER)
    }

    suspend fun uploadEncryptedMedia(
        encryptedFile: File,
        fileId: String,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!encryptedFile.exists() || encryptedFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalArgumentException("File does not exist or is empty: ${encryptedFile.absolutePath}")
            )
        }

        val fileRef = storageRef.child("$fileId.enc")
        val fileUri = Uri.fromFile(encryptedFile)
        val uploadTask = fileRef.putFile(fileUri)

        suspendCancellableCoroutine { continuation ->
            uploadTask.addOnProgressListener { snapshot ->
                val totalBytes = snapshot.totalByteCount
                if (totalBytes > 0L) {
                    val progress = (snapshot.bytesTransferred.toFloat() / totalBytes).coerceIn(0f, 1f)
                    onProgress?.invoke(progress)
                }
            }

            uploadTask.addOnSuccessListener {
                if (continuation.isActive) {
                    continuation.resume(Result.success(fileId))
                }
            }

            uploadTask.addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resume(Result.failure(exception))
                }
            }

            continuation.invokeOnCancellation {
                uploadTask.cancel()
            }
        }
    }

    suspend fun downloadEncryptedMedia(
        fileId: String,
        targetFile: File,
        onProgress: ((Float) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val fileRef = storageRef.child("$fileId.enc")

        if (targetFile.parentFile?.exists() == false) {
            targetFile.parentFile?.mkdirs()
        }

        val downloadTask = fileRef.getFile(targetFile)

        suspendCancellableCoroutine { continuation ->
            downloadTask.addOnProgressListener { snapshot ->
                val totalBytes = snapshot.totalByteCount
                if (totalBytes > 0L) {
                    val progress = (snapshot.bytesTransferred.toFloat() / totalBytes).coerceIn(0f, 1f)
                    onProgress?.invoke(progress)
                }
            }

            downloadTask.addOnSuccessListener {
                if (continuation.isActive) {
                    continuation.resume(Result.success(targetFile))
                }
            }

            downloadTask.addOnFailureListener { exception ->
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (continuation.isActive) {
                    continuation.resume(Result.failure(exception))
                }
            }

            continuation.invokeOnCancellation {
                downloadTask.cancel()
                if (targetFile.exists()) {
                    targetFile.delete()
                }
            }
        }
    }

    suspend fun deleteMediaFromFirebase(fileId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val fileRef = storageRef.child("$fileId.enc")

        suspendCancellableCoroutine { continuation ->
            fileRef.delete()
                .addOnSuccessListener {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(true))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        }
    }
}
