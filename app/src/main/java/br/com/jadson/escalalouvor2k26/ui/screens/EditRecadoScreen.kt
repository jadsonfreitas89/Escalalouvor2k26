package br.com.jadson.escalalouvor2k26.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import androidx.compose.material.icons.filled.Delete
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
import androidx.core.content.FileProvider
import br.com.jadson.escalalouvor2k26.ui.theme.PrimaryOrange
import br.com.jadson.escalalouvor2k26.ui.theme.SurfaceDark
import br.com.jadson.escalalouvor2k26.ui.viewmodel.EscalaViewModel
import br.com.jadson.escalalouvor2k26.ui.viewmodel.UiState
import coil3.compose.AsyncImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecadoScreen(
    recadoId: String,
    navController: androidx.navigation.NavController,
    viewModel: EscalaViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var titulo by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var imagemUrl by remember { mutableStateOf("") }
    var ativo by remember { mutableStateOf("SIM") }
    
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isNewImageSelected by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val inputStream: InputStream? = context.contentResolver.openInputStream(it)
            bitmap = BitmapFactory.decodeStream(inputStream)
            isNewImageSelected = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            capturedImageUri?.let {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                bitmap = BitmapFactory.decodeStream(inputStream)
                isNewImageSelected = true
            }
        }
    }

    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }
    
    var dataLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success && !dataLoaded) {
            val recado = (uiState as UiState.Success).data.recados.find { it.id == recadoId }
            recado?.let {
                titulo = it.titulo
                mensagem = it.mensagem
                imagemUrl = it.imagemUrl ?: ""
                ativo = it.ativo
                dataLoaded = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Recado", fontWeight = FontWeight.Bold) },
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
            
            if (isNewImageSelected && bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Nova imagem",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { bitmap = null; isNewImageSelected = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.White)
                    }
                }
            } else if (!imagemUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                ) {
                    AsyncImage(
                        model = imagemUrl,
                        contentDescription = "Imagem atual",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imagemUrl = ""; isNewImageSelected = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color.White)
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
                            val file = createImageFile()
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            capturedImageUri = uri
                            cameraLauncher.launch(uri)
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
            
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(
                    checked = ativo.uppercase() == "SIM",
                    onCheckedChange = { ativo = if (it) "SIM" else "NAO" },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryOrange)
                )
                Text("Recado Ativo", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (titulo.isBlank() || mensagem.isBlank()) {
                        Toast.makeText(context, "Preencha título e mensagem.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    var base64Image: String? = null
                    if (isNewImageSelected && bitmap != null) {
                        val outputStream = ByteArrayOutputStream()
                        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        val byteArray = outputStream.toByteArray()
                        base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT)
                    }

                    viewModel.updateRecado(
                        id = recadoId,
                        titulo = titulo,
                        mensagem = mensagem,
                        imagemUrl = imagemUrl,
                        ativo = ativo,
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
                Text("SALVAR ALTERAÇÕES", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            
            TextButton(
                onClick = {
                    viewModel.updateRecado(
                        id = recadoId,
                        titulo = titulo,
                        mensagem = mensagem,
                        imagemUrl = imagemUrl,
                        ativo = "NAO",
                        onSuccess = {
                            Toast.makeText(context, "Recado excluído com sucesso.", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Excluir Recado", color = Color.Red)
            }
        }
    }
}
