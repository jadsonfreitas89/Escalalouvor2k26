package br.com.jadson.escalalouvor2k26.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    recadoId: String,
    navController: NavController,
    viewModel: EscalaViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val recado = remember(uiState) {
        if (uiState is UiState.Success) {
            (uiState as UiState.Success).data.recados.find { it.id == recadoId }
        } else null
    }

    val imageUrl = recado?.imagemUrl

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visualizar Imagem", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    if (!imageUrl.isNullOrBlank() && !isError) {
                        IconButton(onClick = {
                            scope.launch {
                                saveImageToDevice(context, imageUrl)
                            }
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "Salvar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNullOrBlank()) {
                Text("Imagem não encontrada.", color = Color.White)
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale *= zoom
                                scale = scale.coerceIn(1f, 5f)
                                offset += pan
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit,
                    onState = { state ->
                        isLoading = state is coil3.compose.AsyncImagePainter.State.Loading
                        isError = state is coil3.compose.AsyncImagePainter.State.Error
                    }
                )

                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                }

                if (isError) {
                    Text("Não foi possível carregar a imagem.", color = Color.White)
                }
            }
        }
    }
}

private suspend fun saveImageToDevice(context: Context, url: String) {
    withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(url)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = result.image.toBitmap()
                val saved = saveBitmap(context, bitmap)
                withContext(Dispatchers.Main) {
                    if (saved) {
                        Toast.makeText(context, "Imagem salva no dispositivo.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Não foi possível salvar a imagem.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Não foi possível carregar a imagem.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun saveBitmap(context: Context, bitmap: Bitmap): Boolean {
    val filename = "recado_${System.currentTimeMillis()}.jpg"
    var outputStream: OutputStream? = null
    var success = false

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                outputStream = context.contentResolver.openOutputStream(imageUri)
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (!imagesDir.exists()) imagesDir.mkdirs()
            val image = java.io.File(imagesDir, filename)
            outputStream = java.io.FileOutputStream(image)
        }

        outputStream?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            success = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return success
}
