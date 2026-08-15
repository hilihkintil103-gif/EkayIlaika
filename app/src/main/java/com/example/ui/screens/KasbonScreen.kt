package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PriceTextStyle
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.PosViewModel
import com.example.util.PdfReportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class KasbonFilter { Semua, BelumLunas, Lunas, Tunggakan7Hari }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasbonScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val kasbonList by viewModel.kasbonList.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(KasbonFilter.BelumLunas) }

    // Helper aging calculation
    fun getKasbonAgingDays(tanggalISO: String, tanggalStr: String): Long {
        val sdfISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateMs = try {
            sdfISO.parse(tanggalISO)?.time
        } catch (e: Exception) {
            null
        }
        if (dateMs == null) return 0L
        val diff = System.currentTimeMillis() - dateMs
        val days = diff / (1000 * 60 * 60 * 24)
        return days.coerceAtLeast(0L)
    }

    val totalBelumLunas = remember(kasbonList) { kasbonList.filter { it.status == "Belum Lunas" }.sumOf { it.total } }
    val totalLunas = remember(kasbonList) { kasbonList.filter { it.status == "Lunas" }.sumOf { it.total } }
    val countNunggak = remember(kasbonList) { kasbonList.count { it.status == "Belum Lunas" } }

    val filteredKasbonList = remember(kasbonList, searchQuery, selectedFilter) {
        kasbonList.filter { k ->
            val matchesSearch = searchQuery.isBlank() ||
                    k.pelanggan.contains(searchQuery, ignoreCase = true) ||
                    k.nomorMeja.contains(searchQuery, ignoreCase = true) ||
                    k.trxId.contains(searchQuery, ignoreCase = true) ||
                    k.tanggalStr.contains(searchQuery, ignoreCase = true)

            val agingDays = getKasbonAgingDays(k.tanggalISO, k.tanggalStr)
            val matchesFilter = when (selectedFilter) {
                KasbonFilter.Semua -> true
                KasbonFilter.BelumLunas -> k.status == "Belum Lunas"
                KasbonFilter.Lunas -> k.status == "Lunas"
                KasbonFilter.Tunggakan7Hari -> k.status == "Belum Lunas" && agingDays >= 7
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📒 Daftar Kasbon & Piutang", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)

            Button(
                onClick = {
                    PdfReportHelper.generateAndPrintKasbonReport(
                        context = context,
                        storeName = storeSettings.namaToko,
                        currentUser = currentUser?.nama ?: "Pemilik",
                        kasbonList = filteredKasbonList
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cetak PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Piutang Belum Lunas:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("Rp ${viewModel.formatRupiah(totalBelumLunas)}", style = PriceTextStyle.copy(fontSize = 16.sp, color = DangerRed))
                    Text("($countNunggak Pelanggan Tertunggak)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Divider(modifier = Modifier
                    .height(36.dp)
                    .width(1.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Kasbon Lunas:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("Rp ${viewModel.formatRupiah(totalLunas)}", style = PriceTextStyle.copy(fontSize = 14.sp, color = SuccessEmerald))
                }
            }
        }

        // Search & Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("🔍 Cari Nama Pelanggan / No. Meja / Trx...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == KasbonFilter.BelumLunas,
                onClick = { selectedFilter = KasbonFilter.BelumLunas },
                label = { Text("🔴 Belum Lunas ($countNunggak)", fontSize = 12.sp) },
                shape = RoundedCornerShape(16.dp)
            )
            FilterChip(
                selected = selectedFilter == KasbonFilter.Tunggakan7Hari,
                onClick = { selectedFilter = KasbonFilter.Tunggakan7Hari },
                label = { Text("⚠️ > 7 Hari", fontSize = 12.sp) },
                shape = RoundedCornerShape(16.dp)
            )
            FilterChip(
                selected = selectedFilter == KasbonFilter.Lunas,
                onClick = { selectedFilter = KasbonFilter.Lunas },
                label = { Text("✅ Lunas", fontSize = 12.sp) },
                shape = RoundedCornerShape(16.dp)
            )
            FilterChip(
                selected = selectedFilter == KasbonFilter.Semua,
                onClick = { selectedFilter = KasbonFilter.Semua },
                label = { Text("Semua Data", fontSize = 12.sp) },
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (filteredKasbonList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Tidak ada data kasbon yang sesuai pencarian/filter.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredKasbonList) { kasbon ->
                    val agingDays = getKasbonAgingDays(kasbon.tanggalISO, kasbon.tanggalStr)

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(kasbon.pelanggan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tgl: ${kasbon.tanggalStr} | Meja: ${kasbon.nomorMeja}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                                Spacer(modifier = Modifier.height(4.dp))

                                // Aging Badge
                                if (kasbon.status == "Belum Lunas") {
                                    val (badgeBg, badgeText, badgeColor) = when {
                                        agingDays <= 3 -> Triple(PrimaryIndigo.copy(alpha = 0.15f), "⏱️ $agingDays Hari (Lancar)", PrimaryIndigo)
                                        agingDays <= 14 -> Triple(WarningAmber.copy(alpha = 0.15f), "⚠️ Terlambat $agingDays Hari", WarningAmber)
                                        else -> Triple(DangerRed.copy(alpha = 0.15f), "🚨 Tunggakan $agingDays Hari (Prioritas Tagih)", DangerRed)
                                    }

                                    Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                                        Text(
                                            badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text("Total Kasbon: Rp ${viewModel.formatRupiah(kasbon.total)}", style = PriceTextStyle.copy(fontSize = 13.sp, color = DangerRed))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (kasbon.status == "Lunas") {
                                    Surface(shape = RoundedCornerShape(6.dp), color = SuccessEmerald.copy(alpha = 0.15f)) {
                                        Text("LUNAS", fontSize = 10.sp, color = SuccessEmerald, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                } else {
                                    Surface(shape = RoundedCornerShape(6.dp), color = WarningAmber.copy(alpha = 0.15f)) {
                                        Text("BELUM LUNAS", fontSize = 10.sp, color = WarningAmber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { viewModel.lunasiKasbon(kasbon) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                                    ) {
                                        Text("Lunasi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

