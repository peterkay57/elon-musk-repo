package com.example.ui.components

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.rememberAsyncImagePainter
import com.example.vault.VaultManager
import com.example.vault.data.HiddenFile
import java.io.File

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun VaultMediaViewer(
    hiddenFile: HiddenFile,
    vaultManager: VaultManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val decryptedFile = remember {
        val tempFile = File(context.cacheDir, "temp_view_${hiddenFile.fileName}")
        vaultManager.decryptFile(File(hiddenFile.encryptedPath), tempFile)
        tempFile
    }

    DisposableEffect(Unit) {
        onDispose {
            if (decryptedFile.exists()) decryptedFile.delete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(hiddenFile.fileName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (hiddenFile.fileType) {
                "Photos" -> {
                    Image(
                        painter = rememberAsyncImagePainter(decryptedFile),
                        contentDescription = "Viewed Photo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                "Videos" -> {
                    val exoPlayer = remember {
                        ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(decryptedFile.absolutePath))
                            prepare()
                        }
                    }
                    DisposableEffect(Unit) {
                        onDispose {
                            exoPlayer.release()
                        }
                    }
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text("File type ${hiddenFile.fileType} not supported in viewer.")
                }
            }
        }
    }
}
