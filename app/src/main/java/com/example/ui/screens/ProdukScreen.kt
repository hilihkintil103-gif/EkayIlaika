package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.data.model.RawMaterialEntity
import com.example.data.model.ToppingItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.HppIngredientInput
import com.example.ui.viewmodel.PosViewModel
import com.example.ui.viewmodel.ProductSubTab
import com.example.util.PdfReportHelper
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukScreen(viewModel: PosViewModel) {
    val subTab by viewModel.selectedProductSubTab.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val rawMaterials by viewModel.rawMaterials.collectAsState()

    var showHppModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scroll hint for swipeable sub-menu tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "💡 Geser tab ke samping",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = { showHppModal = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "🧮 Kalkulator HPP",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Sub Navigation Tab Row
        SecondaryScrollableTabRow(selectedTabIndex = subTab.ordinal) {
            Tab(
                selected = subTab == ProductSubTab.DaftarMenu,
                onClick = { viewModel.selectProductSubTab(ProductSubTab.DaftarMenu) },
                text = { Text("📦 Menu & Kategori", fontSize = 12.sp) }
            )
            Tab(
                selected = subTab == ProductSubTab.StokProduk,
                onClick = { viewModel.selectProductSubTab(ProductSubTab.StokProduk) },
                text = { Text("📋 Stok Produk", fontSize = 12.sp) }
            )
            Tab(
                selected = subTab == ProductSubTab.BahanBaku,
                onClick = { viewModel.selectProductSubTab(ProductSubTab.BahanBaku) },
                text = { Text("🌾 Bahan Baku & BOM", fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (subTab) {
            ProductSubTab.DaftarMenu -> MenuAndCategorySection(viewModel, categories, products)
            ProductSubTab.StokProduk -> StockAdjustmentSection(viewModel, products)
            ProductSubTab.BahanBaku -> RawMaterialsAndBomSection(viewModel, rawMaterials, onOpenHppModal = { showHppModal = true })
        }
    }

    if (showHppModal) {
        HppCalculatorDialog(viewModel, categories, onDismiss = { showHppModal = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuAndCategorySection(
    viewModel: PosViewModel,
    categories: List<com.example.data.model.CategoryEntity>,
    products: List<ProductEntity>
) {
    var newCategoryInput by remember { mutableStateOf("") }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    // Form fields
    var emojiInput by remember { mutableStateOf("☕") }
    var nameInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Makanan") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var modalInput by remember { mutableStateOf("5000") }
    var jualInput by remember { mutableStateOf("12000") }
    var grosirMinInput by remember { mutableStateOf("0") }
    var grosirHargaInput by remember { mutableStateOf("0") }
    var stokInput by remember { mutableStateOf("50") }
    var varianInput by remember { mutableStateOf("") }
    var toppingNameInput by remember { mutableStateOf("") }
    var toppingPriceInput by remember { mutableStateOf("") }
    val toppingListState = remember { mutableStateListOf<ToppingItem>() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Kategori Management
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🏷️ Kategori Produk Kustom", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCategoryInput,
                            onValueChange = { newCategoryInput = it },
                            placeholder = { Text("Kategori Baru", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.addCategory(newCategoryInput.trim())
                                newCategoryInput = ""
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Kategori", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            AssistChip(
                                onClick = {},
                                label = { Text(cat.name, fontSize = 11.sp) },
                                trailingIcon = {
                                    if (categories.size > 1) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.deleteCategory(cat.id) }
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Form Add/Edit Product
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (editingProduct != null) "Edit Produk" else "Tambah Produk Baru",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PrimaryIndigo
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emojiInput,
                            onValueChange = { emojiInput = it },
                            label = { Text("Emoji", fontSize = 11.sp) },
                            colors = appTextFieldColors(),
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nama Produk", fontSize = 11.sp) },
                            trailingIcon = {
                                if (nameInput.isNotEmpty()) {
                                    IconButton(onClick = { nameInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = categoryInput.ifBlank { "Pilih Kategori..." },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Produk", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            colors = appTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            val catList = categories.map { it.name }.ifEmpty { listOf("Makanan", "Minuman", "Snack", "Lainnya") }
                            catList.forEach { catName ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            catName,
                                            fontSize = 12.sp,
                                            fontWeight = if (catName == categoryInput) FontWeight.Bold else FontWeight.Normal,
                                            color = if (catName == categoryInput) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        categoryInput = catName
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = modalInput,
                            onValueChange = { modalInput = it },
                            label = { Text("Modal (Rp)", fontSize = 11.sp) },
                            prefix = { Text("Rp ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = jualInput,
                            onValueChange = { jualInput = it },
                            label = { Text("Harga Jual (Rp)", fontSize = 11.sp) },
                            prefix = { Text("Rp ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = stokInput,
                            onValueChange = { stokInput = it },
                            label = { Text("Stok", fontSize = 11.sp) },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = grosirMinInput,
                            onValueChange = { grosirMinInput = it },
                            label = { Text("Min Grosir", fontSize = 11.sp) },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = grosirHargaInput,
                            onValueChange = { grosirHargaInput = it },
                            label = { Text("Harga Grosir", fontSize = 11.sp) },
                            prefix = { Text("Rp ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = varianInput,
                        onValueChange = { varianInput = it },
                        label = { Text("Varian (Pisah koma: Ice, Hot)", fontSize = 11.sp) },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("➕ Varian Topping / Add-ons & Harga", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryIndigo)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = toppingNameInput,
                            onValueChange = { toppingNameInput = it },
                            placeholder = { Text("Nama Topping (cth: Extra Keju)", fontSize = 11.sp) },
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = toppingPriceInput,
                            onValueChange = { toppingPriceInput = it },
                            placeholder = { Text("Harga (Rp)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(0.8f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                val name = toppingNameInput.trim()
                                val price = toppingPriceInput.toDoubleOrNull() ?: 0.0
                                if (name.isNotBlank()) {
                                    toppingListState.add(ToppingItem(name, price))
                                    toppingNameInput = ""
                                    toppingPriceInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("+ Tambah", fontSize = 11.sp)
                        }
                    }

                    if (toppingListState.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            toppingListState.forEachIndexed { index, topping ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text("${topping.nama} (Rp ${topping.harga.toInt()})", fontSize = 11.sp) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { toppingListState.removeAt(index) }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val varianList = varianInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            viewModel.saveProduct(
                                id = editingProduct?.id ?: 0,
                                emoji = emojiInput,
                                nama = nameInput.trim(),
                                kategori = categoryInput,
                                modal = modalInput.toDoubleOrNull() ?: 0.0,
                                jual = jualInput.toDoubleOrNull() ?: 0.0,
                                grosirMin = grosirMinInput.toIntOrNull() ?: 0,
                                grosirHarga = grosirHargaInput.toDoubleOrNull() ?: 0.0,
                                stok = stokInput.toIntOrNull() ?: 0,
                                varianList = varianList,
                                toppingList = toppingListState.toList(),
                                resepList = emptyList()
                            )
                            editingProduct = null
                            nameInput = ""
                            categoryInput = categories.firstOrNull()?.name ?: "Makanan"
                            toppingListState.clear()
                            varianInput = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Icon(
                            imageVector = if (editingProduct != null) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (editingProduct != null) "Simpan Perubahan" else "Tambah ke Menu", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List Products Table
        items(products) { product ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(product.emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Rp ${viewModel.formatRupiah(product.jual)}",
                                style = PriceTextStyle.copy(fontSize = 12.sp, color = PrimaryIndigo),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = SecondaryViolet.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        product.kategori.ifBlank { "Umum" },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SecondaryViolet,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (product.stok <= 5) DangerRed.copy(alpha = 0.15f) else SuccessEmerald.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "Stok: ${product.stok}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (product.stok <= 5) DangerRed else SuccessEmerald,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { editingProduct = product },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Produk", tint = PrimaryIndigo, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { viewModel.toggleProductActive(product) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (product.aktif) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (product.aktif) SuccessEmerald else DangerRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteProduct(product.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(editingProduct) {
        editingProduct?.let { p ->
            emojiInput = p.emoji
            nameInput = p.nama
            categoryInput = p.kategori
            modalInput = p.modal.toInt().toString()
            jualInput = p.jual.toInt().toString()
            grosirMinInput = p.grosirMin.toString()
            grosirHargaInput = p.grosirHarga.toInt().toString()
            stokInput = p.stok.toString()
            try {
                val arr = JSONArray(p.varianJson)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getString(i))
                }
                varianInput = list.joinToString(", ")
            } catch (e: Exception) {
                varianInput = ""
            }
            try {
                val arr = JSONArray(p.toppingJson)
                toppingListState.clear()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    toppingListState.add(ToppingItem(obj.getString("nama"), obj.getDouble("harga")))
                }
            } catch (e: Exception) {
                toppingListState.clear()
            }
        } ?: run {
            emojiInput = "☕"
            nameInput = ""
            categoryInput = categories.firstOrNull()?.name ?: "Makanan"
            modalInput = "5000"
            jualInput = "12000"
            grosirMinInput = "0"
            grosirHargaInput = "0"
            stokInput = "50"
            varianInput = ""
            toppingListState.clear()
        }
    }
}

@Composable
private fun StockAdjustmentSection(
    viewModel: PosViewModel,
    products: List<ProductEntity>
) {
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(products.firstOrNull()) }
    var adjustAmount by remember { mutableStateOf("10") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📦 Kelola Stok Produk Jadi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InfoCyan)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = adjustAmount,
                    onValueChange = { adjustAmount = it },
                    label = { Text("Jumlah Penyesuaian") },
                    trailingIcon = {
                        if (adjustAmount.isNotEmpty()) {
                            IconButton(onClick = { adjustAmount = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            selectedProduct?.let { p ->
                                val delta = adjustAmount.toIntOrNull() ?: 0
                                viewModel.adjustProductStock(p.id, delta)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            selectedProduct?.let { p ->
                                val delta = adjustAmount.toIntOrNull() ?: 0
                                viewModel.adjustProductStock(p.id, -delta)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kurangi", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Peringatan Stok Menipis (<= 5)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        val lowStockProducts = products.filter { it.stok <= 5 }
        if (lowStockProducts.isEmpty()) {
            Text("Semua stok produk jadi aman.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(lowStockProducts) { p ->
                    Card(shape = RoundedCornerShape(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${p.emoji} ${p.nama}", fontWeight = FontWeight.Bold)
                            Text("Sisa: ${p.stok}", fontWeight = FontWeight.ExtraBold, color = DangerRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RawMaterialsAndBomSection(
    viewModel: PosViewModel,
    rawMaterials: List<RawMaterialEntity>,
    onOpenHppModal: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var hargaInput by remember { mutableStateOf("120000") }
    var isiInput by remember { mutableStateOf("1000") }
    var satuanInput by remember { mutableStateOf("gram") }

    var editingRawMaterial by remember { mutableStateOf<RawMaterialEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onOpenHppModal,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryViolet)
        ) {
            Text("🧮 Buka Kalkulator HPP & Resep BOM V6.7", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🌾 Inventaris Bahan Baku (Kulakan)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nama Bahan Baku", fontSize = 11.sp) },
                    trailingIcon = {
                        if (nameInput.isNotEmpty()) {
                            IconButton(onClick = { nameInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hargaInput,
                        onValueChange = { hargaInput = it },
                        label = { Text("Harga Partai (Rp)", fontSize = 11.sp) },
                        prefix = { Text("Rp ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                        colors = appTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = isiInput,
                        onValueChange = { isiInput = it },
                        label = { Text("Isi Total", fontSize = 11.sp) },
                        colors = appTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = satuanInput,
                        onValueChange = { satuanInput = it },
                        label = { Text("Satuan", fontSize = 11.sp) },
                        placeholder = { Text("gram/ml/pcs", fontSize = 10.sp) },
                        colors = appTextFieldColors(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.saveRawMaterial(
                            nama = nameInput.trim(),
                            harga = hargaInput.toDoubleOrNull() ?: 0.0,
                            isi = isiInput.toDoubleOrNull() ?: 1000.0,
                            satuan = satuanInput.ifBlank { "gram" }
                        )
                        nameInput = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Bahan Baku", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Master Stok Bahan Baku", fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rawMaterials) { rm ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(rm.nama, fontWeight = FontWeight.Bold)
                            Text(
                                "Rp ${viewModel.formatRupiah(rm.harga)} / ${rm.isi} ${rm.satuan}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Stok: ${rm.stok} ${rm.satuan}", fontWeight = FontWeight.Bold, color = SuccessEmerald, fontSize = 12.sp)
                            }
                            IconButton(onClick = { editingRawMaterial = rm }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Bahan Baku", tint = PrimaryIndigo)
                            }
                            IconButton(onClick = { viewModel.deleteRawMaterial(rm.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus Bahan Baku", tint = DangerRed)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Edit Bahan Baku
    editingRawMaterial?.let { item ->
        var editNama by remember(item) { mutableStateOf(item.nama) }
        var editHarga by remember(item) { mutableStateOf(if (item.harga % 1.0 == 0.0) item.harga.toLong().toString() else item.harga.toString()) }
        var editIsi by remember(item) { mutableStateOf(if (item.isi % 1.0 == 0.0) item.isi.toLong().toString() else item.isi.toString()) }
        var editSatuan by remember(item) { mutableStateOf(item.satuan) }
        var editStok by remember(item) { mutableStateOf(if (item.stok % 1.0 == 0.0) item.stok.toLong().toString() else item.stok.toString()) }

        AlertDialog(
            onDismissRequest = { editingRawMaterial = null },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text("✏️ Edit Bahan Baku", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryIndigo, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editNama,
                        onValueChange = { editNama = it },
                        label = { Text("Nama Bahan Baku", fontSize = 11.sp) },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editHarga,
                            onValueChange = { editHarga = it },
                            label = { Text("Harga Partai (Rp)", fontSize = 11.sp) },
                            prefix = { Text("Rp ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editIsi,
                            onValueChange = { editIsi = it },
                            label = { Text("Isi Total", fontSize = 11.sp) },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editSatuan,
                            onValueChange = { editSatuan = it },
                            label = { Text("Satuan", fontSize = 11.sp) },
                            placeholder = { Text("gram/ml/pcs", fontSize = 10.sp) },
                            colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editStok,
                            onValueChange = { editStok = it },
                            label = { Text("Stok Saat Ini", fontSize = 11.sp) },
                            colors = appTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveRawMaterial(
                            id = item.id,
                            nama = editNama.trim(),
                            harga = editHarga.toDoubleOrNull() ?: item.harga,
                            isi = editIsi.toDoubleOrNull() ?: item.isi,
                            satuan = editSatuan.ifBlank { item.satuan },
                            stok = editStok.toDoubleOrNull() ?: item.stok
                        )
                        editingRawMaterial = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingRawMaterial = null }) {
                    Text("Batal", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HppCalculatorDialog(
    viewModel: PosViewModel,
    categories: List<com.example.data.model.CategoryEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val hppState by viewModel.hppState.collectAsState()
    val rawMaterials by viewModel.rawMaterials.collectAsState()

    LaunchedEffect(Unit) {
        val defaultCat = categories.firstOrNull()?.name ?: "Makanan"
        viewModel.resetHppCalculator(defaultCat)
    }

    var isManualMode by remember { mutableStateOf(false) }
    var selectedBahan by remember { mutableStateOf<RawMaterialEntity?>(rawMaterials.firstOrNull()) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Manual input fields
    var manualNama by remember { mutableStateOf("") }
    var manualHarga by remember { mutableStateOf("") }
    var manualIsi by remember { mutableStateOf("") }

    var pakaiInput by remember { mutableStateOf("18") }
    var wasteInput by remember { mutableStateOf("0") }

    var tkInput by remember { mutableStateOf(hppState.tenagaKerja.toInt().toString().let { if (it == "0") "" else it }) }
    var ovhInput by remember { mutableStateOf(hppState.overhead.toInt().toString().let { if (it == "0") "" else it }) }
    var unitInput by remember { mutableStateOf(hppState.jumlahUnit.toString()) }
    var targetFcInput by remember { mutableStateOf(hppState.targetFC.toInt().toString()) }

    var planPriceInput by remember { mutableStateOf("") }
    var simulatorMode by remember { mutableStateOf("Manual") } // "Manual", "FoodCost", "Margin"
    var simInputVal by remember { mutableStateOf("") }
    var showFormulaCard by remember { mutableStateOf(false) }

    // Interactive tooltip dialog state
    var activeTooltip by remember { mutableStateOf<Pair<String, String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧮 Kalkulator HPP", style = MaterialTheme.typography.titleLarge, color = PrimaryIndigo)
                IconButton(
                    onClick = {
                        activeTooltip = "📖 Petunjuk" to "Kalkulator ini membantu menghitung HPP berdasarkan Resep (BOM), Biaya Tenaga Kerja, Kemasan, dan Operasional untuk mendapatkan saran harga jual ideal."
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = "Bantuan", tint = PrimaryIndigo)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hppState.namaProduk,
                        onValueChange = { viewModel.updateHppNamaProduk(it) },
                        label = { Text("Nama Menu Target", style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = {
                            if (hppState.namaProduk.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateHppNamaProduk("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    IconButton(
                        onClick = {
                            activeTooltip = "Nama & Kategori Produk" to "• Nama Menu Target: Ketik nama produk/menu jadi yang akan dijual (misal: Nasi Goreng Spesial).\n• Kategori Produk: Pilih pengelompokan produk (Makanan, Minuman, Snack, dll) agar otomatis tersusun rapi di menu kasir."
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "Info Nama", tint = PrimaryIndigo)
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = hppState.kategori.ifBlank { "Pilih Kategori..." },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori Produk", style = MaterialTheme.typography.labelMedium) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        val catList = categories.map { it.name }.ifEmpty { listOf("Makanan", "Minuman", "Snack", "Dessert") }
                        catList.forEach { catName ->
                            DropdownMenuItem(
                                text = { Text(catName, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.updateHppKategori(catName)
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Collapsible Formula & Explanation Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryIndigo.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFormulaCard = !showFormulaCard }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📐", fontSize = 18.sp)
                                Column {
                                    Text(
                                        "Rumus & Cara Perhitungan HPP",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryIndigo
                                    )
                                    Text(
                                        if (showFormulaCard) "Ketuk untuk menyembunyikan rumus" else "Ketuk untuk melihat rumus & contoh perhitungan",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showFormulaCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showFormulaCard) "Tutup" else "Buka",
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = showFormulaCard) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = PrimaryIndigo.copy(alpha = 0.2f))

                                // Point 1: HPP Bahan
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("1️⃣ HPP Bahan Baku per Porsi:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryIndigo)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Text(
                                            "HPP Bahan = (Harga Kulakan / Isi Total) × Takaran Pakai × (1 + Waste %)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                // Point 2: Total HPP Batch
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("2️⃣ Total HPP 1 Batch Produksi:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryIndigo)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Text(
                                            "Total HPP Batch = Total Semua HPP Bahan + Tenaga Kerja + Kemasan & Gas",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                // Point 3: HPP Bersih Unit
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("3️⃣ HPP Bersih per Unit / Porsi:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryIndigo)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Text(
                                            "HPP Bersih Unit = Total HPP Batch / Jumlah Porsi Hasil (Yield)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                // Point 4: Rekomendasi Harga Jual
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("4️⃣ Rekomendasi Harga Jual (Target Food Cost %):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryIndigo)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Text(
                                            "Rekomendasi Harga Jual = HPP Bersih Unit / (Target Food Cost % / 100)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                // Point 5: Margin Keuntungan
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("5️⃣ Rasio Margin Keuntungan (%):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryIndigo)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Text(
                                            "Margin % = ((Harga Jual - HPP Unit) / Harga Jual) × 100%",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                // Point 6: Cara Menghitung Biaya Overhead (Tenaga Kerja, Kemasan, Gas & Listrik)
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("6️⃣ Cara Menghitung Biaya Overhead & Operasional:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryIndigo)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "👷 Tenaga Kerja: (Gaji Harian / Target Porsi per Hari)\n" +
                                                "• Contoh: Gaji Koki Rp 100.000 / 100 porsi = Rp 1.000/porsi.",
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                            Text(
                                                "📦 Kemasan: Total item sekali pakai per porsi\n" +
                                                "• Contoh: Paper bowl Rp 500 + Sendok Rp 100 + Kantong Rp 150 = Rp 750/porsi.",
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                            Text(
                                                "🔥 Gas & Listrik: (Total Tagihan Bulanan / Total Porsi Terjual Sebulan)\n" +
                                                "• Contoh: Gas 3kg + Listrik Rp 300.000 / 1.500 porsi = Rp 200/porsi.",
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }

                                Text(
                                    "💡 Contoh Total HPP: HPP Bahan Rp 3.150 + Kemasan Rp 750 + Tenaga Kerja Rp 1.000 + Gas & Listrik Rp 200 = HPP Bersih Rp 5.100. Jika target Food Cost 35%, maka Harga Jual Ideal = Rp 5.100 / 0.35 = Rp 14.571 (dibulatkan Rp 14.500/15.000).",
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Section 1: Bahan Baku
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1. Bahan Baku (BOM)", style = MaterialTheme.typography.titleMedium, color = PrimaryIndigo)
                        IconButton(
                            onClick = {
                                activeTooltip = "Panduan Bahan Baku (BOM)" to "• Master Bahan: Ambil bahan mentah yang sudah terdaftar di gudang.\n• Input Manual: Masukkan nama bahan baru beserta harga kulakan total dan isi total kemasannya.\n• Takaran Pakai: Jumlah bahan yang dipakai untuk membuat produk ini.\n• Susut/Waste %: Persentase bahan yang terbuang saat proses memasak (misal kulit terbuang, susut air) sehingga HPP bahan dihitung lebih akurat."
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = "Info Bahan", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isManualMode,
                            onClick = { isManualMode = false },
                            label = { Text("Master Bahan", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !isManualMode,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = PrimaryIndigo
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = isManualMode,
                            onClick = { isManualMode = true },
                            label = { Text("Input Manual", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryIndigo,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isManualMode,
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = PrimaryIndigo
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (!isManualMode) {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedBahan?.let { "${it.nama} (Rp ${viewModel.formatRupiah(it.harga)}/${it.isi}${it.satuan})" } ?: "Pilih Bahan...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Bahan Baku", style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            colors = appTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            rawMaterials.forEach { rm ->
                                DropdownMenuItem(
                                    text = { Text("${rm.nama} - Rp ${viewModel.formatRupiah(rm.harga)} / ${rm.isi} ${rm.satuan}", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedBahan = rm
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = manualNama,
                        onValueChange = { manualNama = it },
                        label = { Text("Nama Bahan", style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            if (manualNama.isNotEmpty()) {
                                IconButton(onClick = { manualNama = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = manualHarga,
                            onValueChange = { manualHarga = it },
                            label = { Text("Harga Kulakan (Rp)", style = MaterialTheme.typography.labelSmall) },
                            prefix = { Text("Rp ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                            colors = appTextFieldColors(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = manualIsi,
                            onValueChange = { manualIsi = it },
                            label = { Text("Isi Total (g/ml)", style = MaterialTheme.typography.labelSmall) },
                            colors = appTextFieldColors(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pakaiInput,
                        onValueChange = { pakaiInput = it },
                        label = { Text("Takaran Pakai", style = MaterialTheme.typography.labelSmall) },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = wasteInput,
                        onValueChange = { wasteInput = it },
                        label = { Text("Susut/Waste %", style = MaterialTheme.typography.labelSmall) },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Button(
                    onClick = {
                        if (!isManualMode) {
                            selectedBahan?.let { rm ->
                                viewModel.addHppIngredient(
                                    idBahan = rm.id,
                                    nama = rm.nama,
                                    hargaPartai = rm.harga,
                                    isiPartai = rm.isi,
                                    pakai = pakaiInput.toDoubleOrNull() ?: 0.0,
                                    waste = wasteInput.toDoubleOrNull() ?: 0.0
                                )
                            }
                        } else {
                            if (manualNama.isNotBlank()) {
                                viewModel.addHppIngredient(
                                    idBahan = null,
                                    nama = manualNama,
                                    hargaPartai = manualHarga.toDoubleOrNull() ?: 0.0,
                                    isiPartai = manualIsi.toDoubleOrNull() ?: 1.0,
                                    pakai = pakaiInput.toDoubleOrNull() ?: 0.0,
                                    waste = wasteInput.toDoubleOrNull() ?: 0.0
                                )
                                manualNama = ""
                                manualHarga = ""
                                manualIsi = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah ke Resep", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                
                // Section 2: Bahan List
                if (hppState.bahanList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Daftar Resep:", style = MaterialTheme.typography.titleSmall)
                        hppState.bahanList.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("• ${item.nama} (${item.pakai}${if(item.waste>0) " +${item.waste}% waste" else ""})", style = MaterialTheme.typography.bodySmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Rp ${viewModel.formatRupiah(item.hppAktual)}", style = MaterialTheme.typography.labelMedium)
                                    IconButton(onClick = { viewModel.removeHppIngredient(index) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Section 3: Biaya Operasional
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2. Operasional & Batch", style = MaterialTheme.typography.titleMedium, color = PrimaryIndigo)
                    IconButton(
                        onClick = {
                            activeTooltip = "Panduan Operasional & Porsi" to "• Tenaga Kerja (Rp): Total upah/gaji pekerja atau jasa pembuatan per satu kali batch produksi.\n• Kemasan & Gas (Rp): Biaya kemasan (cup, box, plastik), gas, listrik, atau air per batch produksi.\n• Porsi (Yield): Jumlah total porsi atau unit produk jadi yang dihasilkan dari 1 kali pembuatan resep ini.\n• Target FoodCost %: Persentase ideal biaya bahan baku terhadap harga jual (Standar F&B: 30% - 35%)."
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "Info Operasional", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tkInput,
                        onValueChange = {
                            tkInput = it
                            viewModel.updateHppTenagaKerja(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Tenaga Kerja (Rp)", style = MaterialTheme.typography.labelSmall) },
                        prefix = { Text("Rp ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = ovhInput,
                        onValueChange = {
                            ovhInput = it
                            viewModel.updateHppOverhead(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Kemasan & Gas (Rp)", style = MaterialTheme.typography.labelSmall) },
                        prefix = { Text("Rp ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo) },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unitInput,
                        onValueChange = {
                            unitInput = it
                            viewModel.updateHppJumlahUnit(it.toIntOrNull() ?: 1)
                        },
                        label = { Text("Porsi (Yield)", style = MaterialTheme.typography.labelSmall) },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = targetFcInput,
                        onValueChange = {
                            targetFcInput = it
                            viewModel.updateHppTargetFC(it.toDoubleOrNull() ?: 35.0)
                        },
                        label = { Text("Target FoodCost %", style = MaterialTheme.typography.labelSmall) },
                        colors = appTextFieldColors(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.calculateHppResult() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hitung HPP & Rekomendasi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Calculation Result Section / Container (Always bordered and structured)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val res = hppState.calculatedResult
                        if (res == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = PrimaryIndigo.copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        "Tekan tombol Hitung di atas untuk melihat\nanalisis HPP dan rekomendasi harga.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("📊 Hasil Analisis Costing HPP:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryIndigo)
                                IconButton(
                                    onClick = {
                                        activeTooltip = "Panduan Hasil HPP & Simulator Harga" to "• HPP Bersih per Unit: Total biaya bahan baku (setelah waste) ditambah operasional dibagi jumlah porsi.\n• Saran Harga Jual: Rekomendasi harga otomatis berdasarkan target Food Cost.\n• Simulator Harga: Coba masukkan target margin atau harga jual untuk melihat proyeksi keuntungan bersih, rasio food cost %, dan margin %."
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Outlined.Info, contentDescription = "Info Hasil", tint = PrimaryIndigo, modifier = Modifier.size(18.dp))
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Bahan Baku (BOM):", fontSize = 12.sp)
                                Text("Rp ${viewModel.formatRupiah(res.totalBahan)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Overhead & Operasional:", fontSize = 12.sp)
                                Text("Rp ${viewModel.formatRupiah(res.totalBiayaLain)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("HPP Bersih per Unit/Porsi:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Rp ${viewModel.formatRupiah(res.hppUnit)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DangerRed)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("💡 Rekomendasi Harga Jual:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SecondaryViolet)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Target FC ${hppState.targetFC.toInt()}%:", fontSize = 12.sp)
                                Text("Rp ${viewModel.formatRupiah(res.saranTargetFC)}", fontWeight = FontWeight.Bold, color = SuccessEmerald, fontSize = 13.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Margin Keuntungan 35%:", fontSize = 12.sp)
                                Text("Rp ${viewModel.formatRupiah(res.m35)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Margin Keuntungan 50%:", fontSize = 12.sp)
                                Text("Rp ${viewModel.formatRupiah(res.m50)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Text("🎯 Simulator Rencana Harga Jual:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = simulatorMode == "Manual",
                                    onClick = {
                                        simulatorMode = "Manual"
                                        simInputVal = ""
                                        viewModel.updateCustomHppPrice(res.saranTargetFC)
                                    },
                                    label = { Text("Harga Rp", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = simulatorMode == "Manual",
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = PrimaryIndigo
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = simulatorMode == "FoodCost",
                                    onClick = {
                                        simulatorMode = "FoodCost"
                                        simInputVal = "35"
                                        val price = res.hppUnit / (35.0 / 100.0)
                                        viewModel.updateCustomHppPrice(price)
                                    },
                                    label = { Text("Food Cost %", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = simulatorMode == "FoodCost",
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = PrimaryIndigo
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = simulatorMode == "Margin",
                                    onClick = {
                                        simulatorMode = "Margin"
                                        simInputVal = "50"
                                        val price = res.hppUnit / (1.0 - (50.0 / 100.0))
                                        viewModel.updateCustomHppPrice(price)
                                    },
                                    label = { Text("Margin %", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryIndigo,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = simulatorMode == "Margin",
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = PrimaryIndigo
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = simInputVal,
                                onValueChange = {
                                    simInputVal = it
                                    val num = it.toDoubleOrNull() ?: 0.0
                                    val calculatedPrice = when (simulatorMode) {
                                        "FoodCost" -> {
                                            val fc = maxOf(1.0, num)
                                            res.hppUnit / (fc / 100.0)
                                        }
                                        "Margin" -> {
                                            val mg = minOf(95.0, maxOf(0.0, num))
                                            if (mg < 100.0) res.hppUnit / (1.0 - (mg / 100.0)) else res.hppUnit * 2.0
                                        }
                                        else -> num
                                    }
                                    viewModel.updateCustomHppPrice(calculatedPrice)
                                },
                                label = {
                                    Text(
                                        when (simulatorMode) {
                                            "FoodCost" -> "Masukkan Target Food Cost % (misal: 35)"
                                            "Margin" -> "Masukkan Margin Keuntungan % (misal: 50)"
                                            else -> "Masukkan Harga Jual (Rp)"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                colors = appTextFieldColors(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (simInputVal.isNotBlank() || simulatorMode != "Manual") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Untung bersih per porsi:", fontSize = 12.sp)
                                    Text("Rp ${viewModel.formatRupiah(res.customUntung)}", fontWeight = FontWeight.Bold, color = SuccessEmerald)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Rasio Food Cost %:", fontSize = 12.sp)
                                    Text("${res.customFoodCostPct}%", fontWeight = FontWeight.Bold, color = if (res.customFoodCostPct <= 35) SuccessEmerald else DangerRed)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Margin Keuntungan %:", fontSize = 12.sp)
                                    Text("${res.customMarginVal}%", fontWeight = FontWeight.Bold, color = SecondaryViolet)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Batal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (hppState.calculatedResult != null) {
                    OutlinedButton(
                        onClick = {
                            val res = hppState.calculatedResult!!
                            val bahanJson = org.json.JSONArray().apply {
                                hppState.bahanList.forEach { b ->
                                    put(org.json.JSONObject().apply {
                                        put("nama", b.nama)
                                        put("pakai", b.pakai)
                                        put("subtotal", b.hppAktual)
                                    })
                                }
                            }.toString()
                            PdfReportHelper.printHppReport(
                                context = context,
                                storeName = storeSettings.namaToko,
                                currentUser = currentUser?.nama ?: "Admin",
                                namaProduk = if (hppState.namaProduk.isBlank()) "Produk Tanpa Nama" else hppState.namaProduk,
                                jumlahUnit = hppState.jumlahUnit,
                                totalBahan = res.totalBahan,
                                totalBiayaLain = res.totalBiayaLain,
                                tenagaKerja = hppState.tenagaKerja,
                                overhead = hppState.overhead,
                                hppUnit = res.hppUnit,
                                targetFcPct = hppState.targetFC,
                                saranTargetFc = res.saranTargetFC,
                                m35 = res.m35,
                                m50 = res.m50,
                                customPrice = res.customHarga,
                                bahanListJson = bahanJson
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PrimaryIndigo),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigo),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Cetak PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveHppToArchiveAndMenu {
                            onDismiss()
                        }
                    },
                    enabled = hppState.calculatedResult != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        disabledContainerColor = PrimaryIndigo.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Simpan Arsip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {}
    )

    activeTooltip?.let { (title, desc) ->
        AlertDialog(
            onDismissRequest = { activeTooltip = null },
            title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryIndigo, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
            text = { Text(desc, fontSize = 12.sp, lineHeight = 18.sp) },
            confirmButton = {
                Button(
                    onClick = { activeTooltip = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Mengerti & Paham")
                }
            }
        )
    }
}

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    focusedBorderColor = PrimaryIndigo,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = PrimaryIndigo,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPrefixColor = PrimaryIndigo,
    unfocusedPrefixColor = PrimaryIndigo
)
