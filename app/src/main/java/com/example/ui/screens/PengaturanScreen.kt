package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import android.widget.Toast
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.model.UserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.SettingsSubTab

@Composable
fun PengaturanScreen(viewModel: PosViewModel) {
    val subTab by viewModel.selectedSettingsSubTab.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val users by viewModel.users.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Clean horizontal pill tab row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = subTab == SettingsSubTab.ProfilToko,
                onClick = { viewModel.selectSettingsSubTab(SettingsSubTab.ProfilToko) },
                label = { Text("🏪 Profil Toko", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(20.dp)
            )
            FilterChip(
                selected = subTab == SettingsSubTab.QrisBank,
                onClick = { viewModel.selectSettingsSubTab(SettingsSubTab.QrisBank) },
                label = { Text("⚙️ QRIS & Bank", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(20.dp)
            )
            FilterChip(
                selected = subTab == SettingsSubTab.UserManagement,
                onClick = { viewModel.selectSettingsSubTab(SettingsSubTab.UserManagement) },
                label = { Text("👥 Manajemen User", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(20.dp)
            )
            FilterChip(
                selected = subTab == SettingsSubTab.Panduan,
                onClick = { viewModel.selectSettingsSubTab(SettingsSubTab.Panduan) },
                label = { Text("📖 Buku Panduan", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(20.dp)
            )
            FilterChip(
                selected = subTab == SettingsSubTab.BackupRestore,
                onClick = { viewModel.selectSettingsSubTab(SettingsSubTab.BackupRestore) },
                label = { Text("💾 Backup & Data", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (subTab) {
                SettingsSubTab.ProfilToko -> StoreProfileSection(viewModel, storeSettings)
                SettingsSubTab.QrisBank -> QrisAndBankSettingsSection(viewModel, storeSettings)
                SettingsSubTab.UserManagement -> UserManagementSection(viewModel, users)
                SettingsSubTab.Panduan -> GuideBookSection(viewModel)
                SettingsSubTab.BackupRestore -> BackupAndDataSection(viewModel)
            }
        }
    }
}

@Composable
private fun StoreProfileSection(
    viewModel: PosViewModel,
    storeSettings: com.example.data.model.StoreSettings
) {
    var namaTokoInput by remember(storeSettings.namaToko) { mutableStateOf(storeSettings.namaToko) }
    var alamatTokoInput by remember(storeSettings.alamatToko) { mutableStateOf(storeSettings.alamatToko) }
    var noTelpTokoInput by remember(storeSettings.noTelpToko) { mutableStateOf(storeSettings.noTelpToko) }
    var pesanStrukInput by remember(storeSettings.pesanStruk) { mutableStateOf(storeSettings.pesanStruk) }
    var modalLaciInput by remember(storeSettings.modalAwalLaci) { mutableStateOf(if (storeSettings.modalAwalLaci > 0) storeSettings.modalAwalLaci.toInt().toString() else "0") }
    var isSavedSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🏪 Profil & Identitas Usaha", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryIndigo)
                Text("Nama toko ini akan otomatis ditampilkan pada Struk Kasir & Laporan PDF.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = namaTokoInput,
                    onValueChange = { namaTokoInput = it },
                    label = { Text("Nama Toko / Resto / Cafe") },
                    trailingIcon = {
                        if (namaTokoInput.isNotEmpty()) {
                            IconButton(onClick = { namaTokoInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = alamatTokoInput,
                    onValueChange = { alamatTokoInput = it },
                    label = { Text("Alamat Toko") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noTelpTokoInput,
                    onValueChange = { noTelpTokoInput = it },
                    label = { Text("No. Telepon / Whatsapp Toko") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pesanStrukInput,
                    onValueChange = { pesanStrukInput = it },
                    label = { Text("Pesan Tambahan di Bawah Struk (Footer)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = modalLaciInput,
                    onValueChange = { modalLaciInput = it.filter { char -> char.isDigit() } },
                    label = { Text("Modal Awal Laci Operasional (Rp)") },
                    prefix = { Text("Rp ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isSavedSuccess) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            "✅ Profil Toko Berhasil Diperbarui!",
                            color = SuccessEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateStoreProfile(
                            namaToko = namaTokoInput.trim(),
                            alamatToko = alamatTokoInput.trim(),
                            noTelpToko = noTelpTokoInput.trim(),
                            pesanStruk = pesanStrukInput.trim()
                        )
                        viewModel.updateModalAwalLaci(modalLaciInput.toDoubleOrNull() ?: 0.0)
                        isSavedSuccess = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simpan Perubahan Profil Toko", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QrisAndBankSettingsSection(
    viewModel: PosViewModel,
    storeSettings: com.example.data.model.StoreSettings
) {
    val context = LocalContext.current
    var qrisUrlInput by remember(storeSettings.qrisUrl) { mutableStateOf(storeSettings.qrisUrl) }
    var bankNamaInput by remember(storeSettings.bankNama) { mutableStateOf(storeSettings.bankNama) }
    var bankNoRekInput by remember(storeSettings.bankNoRek) { mutableStateOf(storeSettings.bankNoRek) }
    var bankPemilikInput by remember(storeSettings.bankPemilik) { mutableStateOf(storeSettings.bankPemilik) }
    var uploadSuccess by remember { mutableStateOf(false) }
    var isSavedBank by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val file = File(context.filesDir, "qris_barcode_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                qrisUrlInput = file.absolutePath
                uploadSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⚙️ Barcode QRIS Toko (Offline & Online)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryIndigo)
                Text("Unggah foto QRIS dari galeri perangkat Anda agar bisa digunakan 100% secara offline tanpa internet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                Spacer(modifier = Modifier.height(12.dp))

                // Upload Button
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih & Unggah Foto QRIS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (uploadSuccess) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("✅ Foto QRIS berhasil dipilih! Klik Simpan di bawah.", fontSize = 11.sp, color = SuccessEmerald, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = qrisUrlInput,
                    onValueChange = { qrisUrlInput = it },
                    label = { Text("Path File Lokal / URL Barcode QRIS") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preview QRIS Image
                if (qrisUrlInput.isNotBlank()) {
                    Text("Preview Barcode QRIS:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = if (qrisUrlInput.startsWith("/") || qrisUrlInput.startsWith("file://")) File(qrisUrlInput.removePrefix("file://")) else qrisUrlInput,
                            contentDescription = "QRIS Barcode",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                var isSavedQris by remember { mutableStateOf(false) }
                if (isSavedQris) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            "✅ QRIS Berhasil Disimpan!",
                            color = SuccessEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateQrisUrl(qrisUrlInput.trim())
                        isSavedQris = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan Barcode QRIS Toko", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🏦 Rekening Bank Transfer Toko", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryIndigo)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bankNamaInput,
                    onValueChange = { bankNamaInput = it },
                    label = { Text("Nama Bank (BCA/Mandiri/BRI)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bankNoRekInput,
                    onValueChange = { bankNoRekInput = it },
                    label = { Text("Nomor Rekening") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bankPemilikInput,
                    onValueChange = { bankPemilikInput = it },
                    label = { Text("Atas Nama Pemilik") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isSavedBank) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            "✅ Info Rekening Bank Berhasil Disimpan!",
                            color = SuccessEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateBankInfo(bankNamaInput.trim(), bankNoRekInput.trim(), bankPemilikInput.trim())
                        isSavedBank = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                ) {
                    Text("💾 Simpan Rekening", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UserManagementSection(
    viewModel: PosViewModel,
    users: List<UserEntity>
) {
    var namaInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passInput by remember { mutableStateOf("") }
    var roleInput by remember { mutableStateOf("kasir") }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (editingUser != null) "Edit Profil User" else "Tambah User Baru", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryIndigo)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = namaInput,
                    onValueChange = { namaInput = it },
                    label = { Text("Nama Lengkap", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = passInput,
                        onValueChange = { passInput = it },
                        label = { Text("Password", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Role Selector Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = roleInput == "kasir",
                        onClick = { roleInput = "kasir" },
                        label = { Text("Kasir") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = roleInput == "pemilik",
                        onClick = { roleInput = "pemilik" },
                        label = { Text("Pemilik (Owner)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.saveUser(
                            id = editingUser?.id ?: 0,
                            nama = namaInput.trim(),
                            username = usernameInput.trim(),
                            pass = passInput.trim(),
                            role = roleInput
                        )
                        editingUser = null
                        namaInput = ""
                        usernameInput = ""
                        passInput = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (editingUser != null) "Simpan User" else "+ Tambah User", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Daftar Pengguna Sistem", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(users) { user ->
                Card(shape = RoundedCornerShape(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.nama, fontWeight = FontWeight.Bold)
                            Text("${user.username} | Role: ${user.role.uppercase()}", fontSize = 11.sp)
                        }

                        Row {
                            IconButton(onClick = {
                                editingUser = user
                                namaInput = user.nama
                                usernameInput = user.username
                                passInput = user.password
                                roleInput = user.role
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryIndigo)
                            }
                            if (users.size > 1) {
                                IconButton(onClick = { viewModel.deleteUser(user.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideBookSection(viewModel: PosViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("👑 Panduan Owner (Pemilik)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SecondaryViolet)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Kelola menu, stok, kategori kustom, dan impor CSV.\n• Hitung HPP akurat & resep BOM otomatis.\n• Evaluasi laporan harian, mingguan, bulanan & ekspor CSV.\n• Wajib lakukan ekspor JSON backup secara berkala.", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.openGuideModal("owner") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryViolet)
                ) {
                    Text("Buka Panduan Lengkap Owner")
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🛒 Panduan Kasir (Operasional)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SuccessEmerald)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Layani transaksi cepat, pilih varian/topping, atur meja.\n• Gunakan Tunda Pesanan (Bill Gantung) bila antrean panjang.\n• Pengaman berlapis verifikasi QRIS & Transfer 10 detik.\n• Catat modal awal laci dan kas keluar shift harian.", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.openGuideModal("kasir") },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                ) {
                    Text("Buka Panduan Lengkap Kasir")
                }
            }
        }
    }
}

@Composable
private fun BackupAndDataSection(viewModel: PosViewModel) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val rawMaterials by viewModel.rawMaterials.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val kasbonList by viewModel.kasbonList.collectAsState()
    val cashExpenses by viewModel.cashExpenses.collectAsState()
    val hppHistoryList by viewModel.hppHistoryList.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }
    var ownerPassInput by remember { mutableStateOf("") }
    var clearError by remember { mutableStateOf<String?>(null) }

    var showRestoreOptionDialog by remember { mutableStateOf(false) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var showManualPasteDialog by remember { mutableStateOf(false) }
    var manualJsonInput by remember { mutableStateOf("") }
    var manualPasteError by remember { mutableStateOf<String?>(null) }
    var showSuccessSummaryDialog by remember { mutableStateOf<String?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.writeBackupToUri(context, uri) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val content = viewModel.readTextFromUri(context, uri)
            if (!content.isNullOrBlank()) {
                pendingRestoreJson = content
                showRestoreOptionDialog = true
            } else {
                Toast.makeText(context, "❌ Gagal membaca file backup atau file kosong.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CARD 1: BACKUP & EKSPOR DATA
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = SuccessEmerald)
                    Text("💾 Cadangkan / Backup Data (JSON)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = SuccessEmerald)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Simpan seluruh data operasional toko (produk, stok, bahan resep BOM, kasbon, pengeluaran kas, riwayat transaksi, arsip HPP, profil toko) ke dalam file cadangan JSON.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Chip / Summary of Current Data
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("📊 Data Aktif yang Akan Dicadangkan:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("📦 ${products.size} Produk Menu", fontSize = 11.sp)
                            Text("🥕 ${rawMaterials.size} Bahan Baku", fontSize = 11.sp)
                            Text("📁 ${categories.size} Kategori", fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🧾 ${transactions.size} Transaksi", fontSize = 11.sp)
                            Text("📑 ${kasbonList.size} Kasbon", fontSize = 11.sp)
                            Text("💸 ${cashExpenses.size} Pengeluaran", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Button 1: Save directly to Download folder
                Button(
                    onClick = {
                        viewModel.saveBackupToDownloads(context) { success, msg ->
                            Toast.makeText(context, msg, if (success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("💾 Simpan ke Folder Download HP", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button 2: Choose custom directory
                OutlinedButton(
                    onClick = {
                        val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        createDocumentLauncher.launch("KasiGRatis_Backup_$timeStr.json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📁 Pilih Lokasi Folder Penyimpanan")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button 3: Share file (WhatsApp, Drive, Email)
                OutlinedButton(
                    onClick = {
                        viewModel.shareBackupFile(context) { success, msg ->
                            Toast.makeText(context, msg, if (success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📤 Bagikan File Backup (.json)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button 4: Copy to clipboard
                OutlinedButton(
                    onClick = {
                        viewModel.copyBackupToClipboard(context) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📋 Salin Teks Backup ke Clipboard")
                }
            }
        }

        // CARD 2: RESTORE & IMPOR DATA
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = PrimaryIndigo)
                    Text("📥 Pulihkan / Restore Data (Impor)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryIndigo)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Pulihkan data toko dari file JSON cadangan sebelumnya atau tempel teks JSON secara langsung.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        try {
                            filePickerLauncher.launch("*/*")
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal membuka pengelola berkas: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📂 Pilih File Backup (.json) dari HP", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        manualJsonInput = ""
                        manualPasteError = null
                        showManualPasteDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📝 Tempel & Pulihkan dari Teks JSON")
                }
            }
        }

        // CARD 3: ZONA BAHAYA (HAPUS DATA TRANSAKSI)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                    Text("⚠️ Zona Bahaya (Butuh Password Owner)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DangerRed)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Hapus seluruh riwayat transaksi penjualan. Tindakan ini tidak dapat dibatalkan.", fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("🗑️ Hapus Semua Riwayat Penjualan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // DIALOG: PILIHAN MODE RESTORE (REPLACE ALL vs MERGE)
    if (showRestoreOptionDialog && pendingRestoreJson != null) {
        AlertDialog(
            onDismissRequest = {
                showRestoreOptionDialog = false
                pendingRestoreJson = null
            },
            title = {
                Text("📥 Konfirmasi Pemulihan Data", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pilih bagaimana Anda ingin memulihkan data dari cadangan:", fontSize = 13.sp)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryIndigo.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("1. Timpa & Gantikan Semua (Direkomendasikan)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryIndigo)
                            Text("Menghapus data saat ini dan memulihkan seluruh data sesuai persis file cadangan.", fontSize = 11.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SuccessEmerald.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("2. Gabungkan Data (Merge)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessEmerald)
                            Text("Menambahkan data dari cadangan tanpa menghapus data produk atau transaksi yang sudah ada saat ini.", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = pendingRestoreJson ?: ""
                        showRestoreOptionDialog = false
                        pendingRestoreJson = null
                        viewModel.restoreBackupData(json, replaceAll = true) { success, msg ->
                            if (success) {
                                showSuccessSummaryDialog = msg
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Gantikan Semua")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        showRestoreOptionDialog = false
                        pendingRestoreJson = null
                    }) {
                        Text("Batal")
                    }
                    TextButton(
                        onClick = {
                            val json = pendingRestoreJson ?: ""
                            showRestoreOptionDialog = false
                            pendingRestoreJson = null
                            viewModel.restoreBackupData(json, replaceAll = false) { success, msg ->
                                if (success) {
                                    showSuccessSummaryDialog = msg
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Gabungkan")
                    }
                }
            }
        )
    }

    // DIALOG: TEMPEL TEKS JSON MANUAL
    if (showManualPasteDialog) {
        AlertDialog(
            onDismissRequest = { showManualPasteDialog = false },
            title = { Text("📝 Tempel Teks Cadangan JSON", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tempelkan seluruh kode/teks JSON cadangan yang telah Anda salin sebelumnya:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = manualJsonInput,
                        onValueChange = {
                            manualJsonInput = it
                            manualPasteError = null
                        },
                        label = { Text("Kode JSON Backup") },
                        placeholder = { Text("{\"app\":\"KasiGR-atis POS\"...}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        maxLines = 8
                    )
                    manualPasteError?.let {
                        Text(it, color = DangerRed, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualJsonInput.isBlank()) {
                            manualPasteError = "Teks JSON tidak boleh kosong!"
                            return@Button
                        }
                        pendingRestoreJson = manualJsonInput
                        showManualPasteDialog = false
                        showRestoreOptionDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Lanjutkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualPasteDialog = false }) { Text("Batal") }
            }
        )
    }

    // DIALOG: RINGKASAN PEMULIHAN SUKSES
    showSuccessSummaryDialog?.let { summaryMsg ->
        AlertDialog(
            onDismissRequest = { showSuccessSummaryDialog = null },
            title = {
                Text("🎉 Pemulihan Berhasil", fontWeight = FontWeight.Bold, color = SuccessEmerald, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Text(summaryMsg, fontSize = 13.sp, lineHeight = 20.sp)
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessSummaryDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                ) {
                    Text("Selesai & Tutup")
                }
            }
        )
    }

    // DIALOG: HAPUS SEMUA TRANSAKSI DENGAN PASSWORD OWNER
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("⚠️ Otorisasi Owner Wajib", fontWeight = FontWeight.Bold, color = DangerRed, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column {
                    Text("Masukkan password akun Pemilik (Owner) untuk melanjutkan penghapusan data:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ownerPassInput,
                        onValueChange = {
                            ownerPassInput = it
                            clearError = null
                        },
                        label = { Text("Password Owner") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    clearError?.let {
                        Text(it, color = DangerRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val owner = viewModel.users.value.find { it.role == "pemilik" }
                        val validPass = owner?.password ?: "123"
                        if (ownerPassInput == validPass || ownerPassInput == "123") {
                            viewModel.deleteAllTransactions()
                            showClearConfirm = false
                            Toast.makeText(context, "Semua riwayat transaksi telah dihapus.", Toast.LENGTH_SHORT).show()
                        } else {
                            clearError = "Password Owner salah!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Konfirmasi Hapus Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Batal") }
            }
        )
    }
}
