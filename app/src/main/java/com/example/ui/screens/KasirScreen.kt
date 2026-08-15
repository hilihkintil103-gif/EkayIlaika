package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PaymentModalType
import com.example.ui.viewmodel.PosViewModel
import com.example.util.PdfReportHelper
import kotlinx.coroutines.delay
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val holdOrders by viewModel.holdOrders.collectAsState()
    val activeProductForOptions by viewModel.activeProductForOptions.collectAsState()
    val activePaymentModal by viewModel.activePaymentModal.collectAsState()
    val activeReceipt by viewModel.activeReceipt.collectAsState()
    val showHoldOrdersModal by viewModel.showHoldOrdersModal.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    val focusManager = LocalFocusManager.current
    var isCartExpanded by remember { mutableStateOf(false) }

    // Filter products
    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchActive = p.aktif
            val matchCategory = selectedCategory == "Semua" || p.kategori == selectedCategory
            val matchQuery = p.nama.contains(searchQuery, ignoreCase = true)
            matchActive && matchCategory && matchQuery
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (cart.isNotEmpty()) 80.dp else 0.dp)
        ) {

            // Top Search Bar & Held Orders Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Cari menu...", fontSize = 13.sp) },
                    colors = appTextFieldColors(),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryIndigo)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                Surface(
                    onClick = { viewModel.toggleHoldOrdersModal(true) },
                    shape = RoundedCornerShape(12.dp),
                    color = WarningAmber,
                    contentColor = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⏳ Tunda (${holdOrders.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Cashier SOP Guide Button (for Cashier role)
            if (currentUser?.role == "kasir") {
                Surface(
                    onClick = { viewModel.openGuideModal("kasir") },
                    shape = RoundedCornerShape(10.dp),
                    color = InfoCyan,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Book, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📖 Buka Buku Panduan Kasir", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Category Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == "Semua",
                    onClick = { viewModel.selectCategoryFilter("Semua") },
                    label = { Text("Semua") }
                )

                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = { viewModel.selectCategoryFilter(cat.name) },
                        label = { Text(cat.name) }
                    )
                }
            }

            // Products Grid
            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Produk '$searchQuery' tidak ditemukan" else "Belum ada produk di kategori '$selectedCategory'",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Coba ubah kata kunci pencarian atau pilih kategori lain.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductGridCard(
                            product = product,
                            onClick = {
                                val hasVarian = try { JSONArray(product.varianJson).length() > 0 } catch (e: Exception) { false }
                                val hasTopping = try { JSONArray(product.toppingJson).length() > 0 } catch (e: Exception) { false }
                                if (hasVarian || hasTopping) {
                                    viewModel.openProductOptions(product)
                                } else {
                                    viewModel.addProductToCart(product)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Bottom Cart Bar
        if (cart.isNotEmpty()) {
            val totalItemCount = cart.sumOf { it.qty }
            val rawSubtotal = cart.sumOf { it.subtotal }
            val discountVal by viewModel.discountVal.collectAsState()
            val discountType by viewModel.discountType.collectAsState()
            val taxPercent by viewModel.taxPercent.collectAsState()

            val nomDiskon = if (discountType == "persen") (rawSubtotal * (discountVal / 100.0)) else minOf(rawSubtotal, discountVal)
            val setelahDiskon = rawSubtotal - nomDiskon
            val nomPajak = (setelahDiskon * (taxPercent / 100.0))
            val grandTotal = Math.round(setelahDiskon + nomPajak).toDouble()

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 12.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCartExpanded = !isCartExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryIndigo,
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "$totalItemCount",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Keranjang Belanja",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Rp ${viewModel.formatRupiah(grandTotal)}",
                                style = PriceTextStyle.copy(
                                    fontSize = 16.sp,
                                    color = DangerRed
                                )
                            )
                            Icon(
                                imageVector = if (isCartExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null
                            )
                        }
                    }

                    // Expanded Cart Details
                    AnimatedVisibility(
                        visible = isCartExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(top = 12.dp)
                        ) {
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Cart Items Table
                            cart.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.nama,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Rp ${viewModel.formatRupiah(item.subtotal)}",
                                            style = PriceTextStyle.copy(
                                                fontSize = 12.sp,
                                                color = PrimaryIndigo
                                            )
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.updateCartQty(index, -1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }

                                        Text(
                                            "${item.qty}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(
                                            onClick = { viewModel.updateCartQty(index, 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Order Configuration Inputs
                            val orderType by viewModel.orderType.collectAsState()
                            val tableNo by viewModel.tableNo.collectAsState()
                            val customerName by viewModel.customerName.collectAsState()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Order Type Selector
                                var expandedType by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1.2f)) {
                                    OutlinedButton(
                                        onClick = { expandedType = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(orderType, fontSize = 11.sp, maxLines = 1)
                                    }
                                    DropdownMenu(
                                        expanded = expandedType,
                                        onDismissRequest = { expandedType = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("🍽️ Dine-In") },
                                            onClick = {
                                                viewModel.setOrderType("Dine-In")
                                                expandedType = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("🥡 Takeaway") },
                                            onClick = {
                                                viewModel.setOrderType("Takeaway")
                                                expandedType = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("🛵 Online") },
                                            onClick = {
                                                viewModel.setOrderType("Online")
                                                expandedType = false
                                            }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = tableNo,
                                    onValueChange = { viewModel.setTableNo(it) },
                                    placeholder = { Text("Meja / Lokasi", fontSize = 11.sp) },
                                    colors = appTextFieldColors(),
                                    trailingIcon = {
                                        if (tableNo.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.setTableNo("") }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { viewModel.setCustomerName(it) },
                                placeholder = { Text("Nama Pelanggan / No HP (Opsional)", fontSize = 11.sp) },
                                colors = appTextFieldColors(),
                                trailingIcon = {
                                    if (customerName.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setCustomerName("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Payment Trigger Buttons Grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.openPaymentModal(PaymentModalType.Tunai) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                                    ) {
                                        Text("💸 Tunai", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.openPaymentModal(PaymentModalType.QRIS) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = InfoCyan)
                                    ) {
                                        Text("📱 QRIS", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.openPaymentModal(PaymentModalType.Transfer) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                                    ) {
                                        Text("💳 Transfer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.checkout("kasbon") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                                    ) {
                                        Text("📒 Kasbon", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.holdCurrentOrder() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("⏳ Tunda Pesanan / Simpan Meja", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal / Sheet for Product Options (Variants & Toppings)
    if (activeProductForOptions != null) {
        val prod = activeProductForOptions!!
        val varianList = remember(prod) {
            try {
                val arr = JSONArray(prod.varianJson)
                List(arr.length()) { arr.getString(it) }
            } catch (e: Exception) { emptyList<String>() }
        }
        val toppingList = remember(prod) {
            try {
                val arr = JSONArray(prod.toppingJson)
                List(arr.length()) {
                    val obj = arr.getJSONObject(it)
                    ToppingItem(obj.getString("nama"), obj.getDouble("harga"))
                }
            } catch (e: Exception) { emptyList<ToppingItem>() }
        }

        var selectedVarian by remember { mutableStateOf(varianList.firstOrNull() ?: "") }
        val selectedToppings = remember { mutableStateListOf<ToppingItem>() }

        AlertDialog(
            onDismissRequest = { viewModel.closeProductOptions() },
            title = { Text("Opsi: ${prod.nama}", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (varianList.isNotEmpty()) {
                        Text("Pilih Varian:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        varianList.forEach { v ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedVarian = v }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(selected = selectedVarian == v, onClick = { selectedVarian = v })
                                Text(v, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (toppingList.isNotEmpty()) {
                        Text("Pilih Topping / Tambahan:", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        toppingList.forEach { t ->
                            val isChecked = selectedToppings.contains(t)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedToppings.remove(t) else selectedToppings.add(t)
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        if (it) selectedToppings.add(t) else selectedToppings.remove(t)
                                    }
                                )
                                Text(t.nama, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Text("+Rp ${viewModel.formatRupiah(t.harga)}", fontSize = 11.sp, color = PrimaryIndigo, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addProductToCart(prod, selectedVarian, selectedToppings.toList())
                        viewModel.closeProductOptions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                ) {
                    Text("+ Masukkan Keranjang")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeProductOptions() }) {
                    Text("Batal")
                }
            }
        )
    }

    // Payment Verification Dialogs (Tunai, QRIS, Transfer)
    if (activePaymentModal != null) {
        val type = activePaymentModal!!
        val cartList by viewModel.cart.collectAsState()
        val rawSubtotal = cartList.sumOf { it.subtotal }
        val discountVal by viewModel.discountVal.collectAsState()
        val discountType by viewModel.discountType.collectAsState()
        val taxPercent by viewModel.taxPercent.collectAsState()

        val nomDiskon = if (discountType == "persen") (rawSubtotal * (discountVal / 100.0)) else minOf(rawSubtotal, discountVal)
        val setelahDiskon = rawSubtotal - nomDiskon
        val nomPajak = (setelahDiskon * (taxPercent / 100.0))
        val grandTotal = Math.round(setelahDiskon + nomPajak).toDouble()

        when (type) {
            PaymentModalType.Tunai -> {
                var cashInput by remember { mutableStateOf("") }
                val cashAmount = cashInput.toDoubleOrNull() ?: 0.0
                val changeAmount = maxOf(0.0, cashAmount - grandTotal)

                AlertDialog(
                    onDismissRequest = { viewModel.closePaymentModal() },
                    title = { Text("💸 Pembayaran Tunai", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    text = {
                        Column {
                            Text("Total Tagihan: Rp ${viewModel.formatRupiah(grandTotal)}", fontWeight = FontWeight.ExtraBold, color = DangerRed)
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = cashInput,
                                onValueChange = { cashInput = it },
                                label = { Text("Uang Diterima (Rp)") },
                                colors = appTextFieldColors(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Quick Cash Amount Buttons
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = { cashInput = grandTotal.toInt().toString() }, modifier = Modifier.weight(1f)) {
                                    Text("Pas", fontSize = 10.sp)
                                }
                                OutlinedButton(onClick = { cashInput = "50000" }, modifier = Modifier.weight(1f)) {
                                    Text("50k", fontSize = 10.sp)
                                }
                                OutlinedButton(onClick = { cashInput = "100000" }, modifier = Modifier.weight(1f)) {
                                    Text("100k", fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Kembalian: Rp ${viewModel.formatRupiah(changeAmount)}", fontWeight = FontWeight.Bold, color = SuccessEmerald)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.setCashPaid(cashAmount)
                                viewModel.checkout("tunai")
                                viewModel.closePaymentModal()
                            },
                            enabled = cashAmount >= grandTotal,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                        ) {
                            Text("Selesaikan Bayar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closePaymentModal() }) { Text("Batal") }
                    }
                )
            }

            PaymentModalType.QRIS -> {
                var secondsLeft by remember { mutableStateOf(10) }
                var isTimerRunning by remember { mutableStateOf(true) }
                var showConfirmationDialog by remember { mutableStateOf(false) }
                val storeSettings by viewModel.storeSettings.collectAsState()

                LaunchedEffect(isTimerRunning) {
                    if (isTimerRunning) {
                        while (secondsLeft > 0) {
                            delay(1000L)
                            secondsLeft--
                        }
                    }
                }

                AlertDialog(
                    onDismissRequest = { viewModel.closePaymentModal() },
                    title = { Text("📱 Verifikasi QRIS", fontWeight = FontWeight.Bold, color = PrimaryIndigo, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Minta pelanggan memindai barcode di bawah ini. Pastikan saldo sudah berbunyi/masuk.", textAlign = TextAlign.Center, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Total: Rp ${viewModel.formatRupiah(grandTotal)}", fontWeight = FontWeight.ExtraBold, color = DangerRed)

                            Spacer(modifier = Modifier.height(12.dp))
                            // Barcode Box
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (storeSettings.qrisUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = if (storeSettings.qrisUrl.startsWith("/") || storeSettings.qrisUrl.startsWith("file://")) File(storeSettings.qrisUrl.removePrefix("file://")) else storeSettings.qrisUrl,
                                        contentDescription = "QRIS Barcode",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("📊 Barcode QRIS Belum Diset", textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            if (secondsLeft > 0) {
                                Text("⏳ Mohon Cek Saldo... (${secondsLeft}s)", fontSize = 12.sp, color = WarningAmber, fontWeight = FontWeight.Bold)
                            } else {
                                Text("✅ Barcode Terverifikasi! Siapkan konfirmasi.", fontSize = 12.sp, color = SuccessEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmationDialog = true
                            },
                            enabled = secondsLeft == 0,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                        ) {
                            Text("✅ Dana Masuk & Cetak")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closePaymentModal() }) { Text("Batal") }
                    }
                )

                if (showConfirmationDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmationDialog = false },
                        icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(36.dp)) },
                        title = {
                            Text("Konfirmasi Akhir", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        },
                        text = {
                            Text(
                                "Apakah Anda sudah memastikan saldo benar-benar masuk ke rekening/akun?",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showConfirmationDialog = false
                                    viewModel.checkout("qris")
                                    viewModel.closePaymentModal()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald, contentColor = Color.White)
                            ) {
                                Text("Ya, Sudah Masuk", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showConfirmationDialog = false },
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                            ) {
                                Text("Belum", color = DangerRed, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }

            PaymentModalType.Transfer -> {
                var secondsLeft by remember { mutableStateOf(10) }
                var showConfirmationDialog by remember { mutableStateOf(false) }
                val storeSettings by viewModel.storeSettings.collectAsState()

                LaunchedEffect(Unit) {
                    while (secondsLeft > 0) {
                        delay(1000L)
                        secondsLeft--
                    }
                }

                AlertDialog(
                    onDismissRequest = { viewModel.closePaymentModal() },
                    title = { Text("💳 Transfer Bank", fontWeight = FontWeight.Bold, color = PrimaryIndigo, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                    text = {
                        Column {
                            Text("Informasikan nomor rekening berikut kepada pelanggan. Pastikan mutasi/saldo sudah masuk.", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Bank Purpose: ${storeSettings.bankNama}", fontWeight = FontWeight.Bold)
                                    Text("No. Rekening: ${storeSettings.bankNoRek}", fontWeight = FontWeight.ExtraBold, color = PrimaryIndigo)
                                    Text("Atas Nama: ${storeSettings.bankPemilik}", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Total Tagihan: Rp ${viewModel.formatRupiah(grandTotal)}", fontWeight = FontWeight.Bold, color = DangerRed)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            if (secondsLeft > 0) {
                                Text("⏳ Mohon Cek Mutasi... (${secondsLeft}s)", fontSize = 12.sp, color = WarningAmber, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmationDialog = true
                            },
                            enabled = secondsLeft == 0,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                        ) {
                            Text("✅ Saldo Masuk & Cetak")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.closePaymentModal() }) { Text("Batal") }
                    }
                )

                if (showConfirmationDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmationDialog = false },
                        icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(36.dp)) },
                        title = {
                            Text("Konfirmasi Akhir", fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        },
                        text = {
                            Text(
                                "Apakah Anda sudah memastikan mutasi/dana benar-benar masuk ke rekening/akun?",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showConfirmationDialog = false
                                    viewModel.checkout("transfer")
                                    viewModel.closePaymentModal()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald, contentColor = Color.White)
                            ) {
                                Text("Ya, Sudah Masuk", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showConfirmationDialog = false },
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                            ) {
                                Text("Belum", color = DangerRed, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }

            PaymentModalType.Kasbon -> {
                viewModel.checkout("kasbon")
                viewModel.closePaymentModal()
            }
        }
    }

    // Receipt Modal
    if (activeReceipt != null) {
        val trx = activeReceipt!!
        var waPhoneNumber by remember(trx) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.closeReceipt() },
            title = { Text("📄 Struk Penjualan", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Text(storeSettings.namaToko.ifEmpty { "Toko POS Kuliner" }, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        if (storeSettings.alamatToko.isNotEmpty()) {
                            Text(storeSettings.alamatToko, fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                        if (storeSettings.noTelpToko.isNotEmpty()) {
                            Text("Telp: ${storeSettings.noTelpToko}", fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

                        Text("No: ${trx.id}", fontSize = 11.sp, color = Color.Black)
                        Text("Tgl: ${trx.tanggalStr}", fontSize = 11.sp, color = Color.Black)
                        Text("Kasir: ${trx.kasir}", fontSize = 11.sp, color = Color.Black)
                        Text("Pelanggan: ${trx.pelanggan} (${trx.nomorMeja})", fontSize = 11.sp, color = Color.Black)
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)

                        // Items list
                        val items = remember(trx) {
                            try {
                                val arr = JSONArray(trx.itemsJson)
                                List(arr.length()) {
                                    val obj = arr.getJSONObject(it)
                                    Triple(obj.getString("nama"), obj.getInt("qty"), obj.getDouble("subtotal"))
                                }
                            } catch (e: Exception) { emptyList() }
                        }

                        items.forEach { (nama, qty, subtotal) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("$nama x$qty", fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
                                Text("Rp ${viewModel.formatRupiah(subtotal)}", fontSize = 11.sp, color = Color.Black)
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 11.sp, color = Color.Black)
                            Text("Rp ${viewModel.formatRupiah(trx.subtotal)}", fontSize = 11.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            Text("Rp ${viewModel.formatRupiah(trx.totalPemasukan)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Metode Bayar:", fontSize = 11.sp, color = Color.Black)
                            Text(trx.metode.uppercase(), fontSize = 11.sp, color = Color.Black)
                        }

                        if (storeSettings.pesanStruk.isNotEmpty()) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray)
                            Text(storeSettings.pesanStruk, fontSize = 11.sp, color = Color.DarkGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = waPhoneNumber,
                        onValueChange = { waPhoneNumber = it },
                        label = { Text("📱 Nomor WA Pelanggan (Opsional)", fontSize = 11.sp) },
                        placeholder = { Text("Contoh: 08123456789", fontSize = 11.sp) },
                        colors = appTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                PdfReportHelper.printReceiptHtml(
                                    context = context,
                                    storeName = storeSettings.namaToko,
                                    storeAddress = storeSettings.alamatToko,
                                    storePhone = storeSettings.noTelpToko,
                                    receiptFooter = storeSettings.pesanStruk,
                                    transaction = trx
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cetak / PDF", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val itemsSummary = try {
                                    val arr = JSONArray(trx.itemsJson)
                                    List(arr.length()) {
                                        val obj = arr.getJSONObject(it)
                                        "- ${obj.getString("nama")} x${obj.getInt("qty")} = Rp ${viewModel.formatRupiah(obj.getDouble("subtotal"))}"
                                    }.joinToString("%0A")
                                } catch (e: Exception) { "" }

                                val sName = Uri.encode(storeSettings.namaToko.ifEmpty { "STRUK PENJUALAN" })
                                val footer = Uri.encode(storeSettings.pesanStruk.ifEmpty { "Terima kasih atas kunjungan Anda!" })
                                val text = "*$sName*%0ANo: ${trx.id}%0ATgl: ${trx.tanggalStr}%0APelanggan: ${trx.pelanggan}%0A--------------------------%0A$itemsSummary%0A--------------------------%0A*Total: Rp ${viewModel.formatRupiah(trx.totalPemasukan)}*%0ABayar: Rp ${viewModel.formatRupiah(trx.uangBayar)}%0AKembali: Rp ${viewModel.formatRupiah(trx.uangKembali)}%0A%0A$footer"

                                val digitsOnly = waPhoneNumber.filter { it.isDigit() }
                                val formattedPhone = when {
                                    digitsOnly.startsWith("0") -> "62" + digitsOnly.substring(1)
                                    digitsOnly.startsWith("8") -> "62" + digitsOnly
                                    else -> digitsOnly
                                }

                                val waUrl = if (formattedPhone.isNotEmpty()) {
                                    "https://wa.me/$formattedPhone?text=$text"
                                } else {
                                    "https://wa.me/?text=$text"
                                }

                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💬 Kirim WA", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.closeReceipt() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selesai")
                    }
                }
            }
        )
    }

    // Held Orders Modal
    if (showHoldOrdersModal) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleHoldOrdersModal(false) },
            title = { Text("⏳ Pesanan Ditunda / Simpan Meja", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (holdOrders.isEmpty()) {
                        Text("Tidak ada pesanan ditunda.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    } else {
                        holdOrders.forEach { hold ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${hold.pelanggan} (${hold.nomorMeja})", fontWeight = FontWeight.Bold)
                                        Text("Waktu: ${hold.waktuStr} | Tipe: ${hold.tipePesanan}", fontSize = 11.sp)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { viewModel.restoreHoldOrder(hold) }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SuccessEmerald)
                                        }
                                        IconButton(onClick = { viewModel.deleteHoldOrder(hold.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleHoldOrdersModal(false) }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun ProductGridCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    val isSoldOut = product.stok <= 0
    val isStockLow = product.stok in 1..5
    val hasBom = try { JSONArray(product.resepJson).length() > 0 } catch (e: Exception) { false }

    Card(
        onClick = onClick,
        enabled = !isSoldOut,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSoldOut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSoldOut) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSoldOut) 0.dp else 4.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji avatar container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSoldOut) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(product.emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Product Name
            Text(
                text = product.nama,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Price
            Text(
                text = "Rp ${formatRupiah(product.jual)}",
                style = PriceTextStyle.copy(
                    fontSize = 12.sp,
                    color = PrimaryIndigo
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Fixed height slot for Badges so stock label is always aligned
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product.grosirMin > 0) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFECFDF5)) {
                            Text(
                                "Grosir",
                                fontSize = 9.sp,
                                color = SuccessEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (hasBom) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE0F2FE)) {
                            Text(
                                "BOM",
                                fontSize = 9.sp,
                                color = InfoCyan,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stock Badge at exact bottom position
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSoldOut) {
                    Surface(shape = RoundedCornerShape(4.dp), color = DangerRed) {
                        Text(
                            "HABIS",
                            fontSize = 9.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isStockLow) {
                    Text(
                        "${product.stok} (Menipis)",
                        fontSize = 10.sp,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "Stok: ${product.stok}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun formatRupiah(amount: Double): String {
    return try {
        java.text.NumberFormat.getInstance(java.util.Locale("id", "ID")).format(Math.round(amount))
    } catch (e: Exception) {
        Math.round(amount).toString()
    }
}
