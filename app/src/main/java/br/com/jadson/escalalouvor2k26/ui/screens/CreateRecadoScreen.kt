package br.com.jadson.escalalouvor2k26.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecadoScreen(navController: androidx.navigation.NavController, viewModel: EscalaViewModel) {
    val context = LocalContext.current
    var titulo by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    fun resizeBitmap(source: Bitmap, maxSide: Int): Bitmap {
        if (source.width <= maxSide && source.height <= maxSide) return source
        val ratio: Float = source.width.toFloat() / source.height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (source.width > source.height) {
            targetWidth = maxSide
            targetHeight = (maxSide / ratio).toInt()
        } else {
            targetHeight = maxSide
            targetWidth = (maxSide * ratio).toInt()
        }
        Log.d("CreateRecado", "Redimensionando imagem: ${source.width}x${source.height} -> ${targetWidth}x${targetHeight}")
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            bitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            imageUri = capturedImageUri
            capturedImageUri?.let {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                bitmap = BitmapFactory.decodeStream(inputStream)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val file = createImageFile()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            capturedImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "A permissão da câmera é necessária para tirar fotos.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escrever Recado", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Título do Recado:", color = PrimaryOrange, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ex: Ensaio no sábado", color = Color.DarkGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color.DarkGray
                )
            )

            Text("Mensagem:", color = PrimaryOrange, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = mensagem,
                onValueChange = { mensagem = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("Digite o recado aqui...", color = Color.DarkGray) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color.DarkGray
                )
            )

            Text("Imagem do Recado:", color = PrimaryOrange, fontWeight = FontWeight.Bold)
            
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Imagem selecionada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { bitmap = null; imageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Remover", tint = Color.White)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f).height(100.dp).clickable { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = PrimaryOrange)
                            Text("Galeria", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    Card(
                        modifier = Modifier.weight(1f).height(100.dp).clickable {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                val file = createImageFile()
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                capturedImageUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryOrange)
                            Text("Câmera", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (titulo.isBlank() || mensagem.isBlank()) {
                        Toast.makeText(context, "Preencha título e mensagem.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    var base64Image: String? = null
                    bitmap?.let {
                        try {
                            val resizedBitmap = resizeBitmap(it, 1280)
                            val outputStream = ByteArrayOutputStream()
                            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                            val byteArray = outputStream.toByteArray()
                            base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)
                            Log.d("CreateRecado", "Payload Base64 gerado: ${base64Image?.length} caracteres. JPEG size: ${byteArray.size} bytes.")
                        } catch (e: Exception) {
                            Log.e("CreateRecado", "Erro ao processar imagem", e)
                        }
                    }

                    viewModel.createRecado(
                        titulo = titulo,
                        mensagem = mensagem,
                        imagemUrl = "",
                        imageBase64 = base64Image,
                        onSuccess = {
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("PUBLICAR RECADO", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
