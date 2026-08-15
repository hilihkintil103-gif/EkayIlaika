package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DangerRed
import com.example.ui.theme.KasiGratisTheme
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessEmerald
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.PosViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: PosViewModel) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showLoginScreen by viewModel.showLoginScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val showGuideRole by viewModel.showGuideModal.collectAsState()

    KasiGratisTheme(darkTheme = isDarkMode) {
        if (showLoginScreen || currentUser == null) {
            LoginScreen(
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode(it) }
            )
        } else {
            Scaffold(
                topBar = {
                    Surface(
                        shadowElevation = 4.dp,
                        tonalElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        "🛒 KasiGR-atis POS",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                    )
                                    Text(
                                        "👤 ${currentUser?.nama ?: ""} (${currentUser?.role?.uppercase()})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.toggleDarkMode(!isDarkMode) }) {
                                    Text(if (isDarkMode) "🌙" else "☀️", fontSize = 16.sp)
                                }

                                IconButton(onClick = { viewModel.logout() }) {
                                    Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = DangerRed)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                },
                bottomBar = {
                    Surface(
                        shadowElevation = 8.dp,
                        tonalElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp
                        ) {
                        val role = currentUser?.role ?: "kasir"

                        NavigationBarItem(
                            selected = selectedTab == NavTab.Kasir,
                            onClick = { viewModel.selectTab(NavTab.Kasir) },
                            icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                            label = { Text("Kasir", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                        )

                        if (role == "pemilik") {
                            NavigationBarItem(
                                selected = selectedTab == NavTab.Produk,
                                onClick = { viewModel.selectTab(NavTab.Produk) },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                label = { Text("Produk", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                            )
                        }

                        NavigationBarItem(
                            selected = selectedTab == NavTab.Laporan,
                            onClick = { viewModel.selectTab(NavTab.Laporan) },
                            icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                            label = { Text("Laporan", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                        )

                        NavigationBarItem(
                            selected = selectedTab == NavTab.Kasbon,
                            onClick = { viewModel.selectTab(NavTab.Kasbon) },
                            icon = { Icon(Icons.Default.Book, contentDescription = null) },
                            label = { Text("Kasbon", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                        )

                        if (role == "pemilik") {
                            NavigationBarItem(
                                selected = selectedTab == NavTab.Pengaturan,
                                onClick = { viewModel.selectTab(NavTab.Pengaturan) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Pengaturan", fontSize = 9.sp, maxLines = 1, softWrap = false) }
                            )
                        }

                        NavigationBarItem(
                            selected = selectedTab == NavTab.Tentang,
                            onClick = { viewModel.selectTab(NavTab.Tentang) },
                            icon = { Icon(Icons.Default.Info, contentDescription = null) },
                            label = { Text("Tentang", fontSize = 9.5.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Crossfade(targetState = selectedTab, label = "tabCrossfade") { tab ->
                        when (tab) {
                            NavTab.Kasir -> KasirScreen(viewModel = viewModel)
                            NavTab.Produk -> ProdukScreen(viewModel = viewModel)
                            NavTab.Laporan -> LaporanScreen(viewModel = viewModel)
                            NavTab.Kasbon -> KasbonScreen(viewModel = viewModel)
                            NavTab.Pengaturan -> PengaturanScreen(viewModel = viewModel)
                            NavTab.Tentang -> TentangScreen()
                        }
                    }

                    showGuideRole?.let { role ->
                        GuideBookDialog(role = role, onDismiss = { viewModel.closeGuideModal() })
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideBookDialog(role: String, onDismiss: () -> Unit) {
    val isOwner = role == "owner"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isOwner) "📖 Buku Panduan Operasional Owner" else "📖 Buku Panduan Operasional Kasir",
                fontWeight = FontWeight.Bold,
                color = PrimaryIndigo,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (isOwner) {
                    GuideItem("1. Pengaturan Toko & Bank", "Atur URL QRIS barcode toko dan nomor rekening bank transfer pada menu Pengaturan agar verifikasi pembayaran otomatis berjalan.")
                    GuideItem("2. Manajemen Pengguna", "Buat akun untuk kasir toko Anda dengan role 'Kasir' dan simpan password secara aman.")
                    GuideItem("3. Kelola Produk & Kategori", "Tambah produk baru, harga jual, harga grosir, varian, dan stok awal pada menu Produk.")
                    GuideItem("4. Hitung HPP & Resep BOM", "Gunakan Kalkulator HPP & BOM untuk menghitung biaya modal per unit produk secara presisi berdasarkan bahan baku (kulakan) dan persentase waste.")
                    GuideItem("5. Evaluasi Laporan Penjualan", "Pantau omset harian, estimasi modal, dan laba bersih riil pada menu Laporan.")
                    GuideItem("6. Backup & Restore JSON", "Unduh backup data JSON secara berkala pada menu Pengaturan untuk mencegah kehilangan data.")
                } else {
                    GuideItem("1. Login Akun Kasir", "Masuk dengan username dan password kasir yang telah disiapkan oleh Pemilik toko.")
                    GuideItem("2. Buka Shift & Modal Laci", "Masukkan nominal Modal Awal Laci di tab Laporan sebelum memulai transaksi pertama harian.")
                    GuideItem("3. Pemesanan & Kasir", "Pilih produk dari layar Kasir, pilih varian/topping opsional, dan tentukan tipe pesanan (Dine-In/Takeaway) serta nomor meja.")
                    GuideItem("4. Tunda Pesanan (Bill Gantung)", "Jika pelanggan menunda bayar, tekan tombol 'Tunda' untuk menyimpan pesanan dan memproses antrean berikutnya.")
                    GuideItem("5. Pembayaran Cepat", "Proses pembayaran Tunai, QRIS, atau Transfer Bank. Gunakan penghitung detik verifikasi QRIS/Transfer untuk memastikan saldo masuk.")
                    GuideItem("6. Catat Kas Keluar", "Catat pengeluaran kecil shift (seperti beli es batu) pada menu Laporan Kas Keluar.")
                    GuideItem("7. Serah Terima Shift", "Di akhir shift, periksa kecocokan uang fisik di laci dengan hitungan sistem pada menu Laporan.")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)) {
                Text("Paham & Tutup")
            }
        }
    )
}

@Composable
private fun GuideItem(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryIndigo)
        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}
