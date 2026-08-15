package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.CashExpenseEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.HoldOrderEntity
import com.example.data.model.HppHistoryEntity
import com.example.data.model.KasbonEntity
import com.example.data.model.ProductEntity
import com.example.data.model.RawMaterialEntity
import com.example.data.model.RecipeIngredient
import com.example.data.model.StoreSettings
import com.example.data.model.ToppingItem
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NavTab { Kasir, Produk, Laporan, Kasbon, Pengaturan, Tentang }
enum class ProductSubTab { DaftarMenu, StokProduk, BahanBaku }
enum class SettingsSubTab { ProfilToko, QrisBank, UserManagement, Panduan, BackupRestore }
enum class PaymentModalType { Tunai, QRIS, Transfer, Kasbon }

data class HppCalcState(
    val namaProduk: String = "",
    val kategori: String = "Makanan",
    val bahanList: List<HppIngredientInput> = emptyList(),
    val tenagaKerja: Double = 0.0,
    val overhead: Double = 0.0,
    val jumlahUnit: Int = 1,
    val targetFC: Double = 35.0,
    val calculatedResult: HppResult? = null
)

data class HppIngredientInput(
    val idBahan: Long? = null,
    val nama: String,
    val hargaPartai: Double,
    val isiPartai: Double,
    val pakai: Double,
    val waste: Double,
    val hppAktual: Double
)

data class HppResult(
    val totalBahan: Double,
    val totalBiayaLain: Double,
    val hppUnit: Double,
    val saranTargetFC: Double,
    val m35: Double,
    val m50: Double,
    val customMarginVal: Double = 40.0,
    val customHarga: Double = 0.0,
    val customUntung: Double = 0.0,
    val customFoodCostPct: Double = 0.0
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = PosRepository(database.posDao())
    }

    // DB Flows
    val users: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val products: StateFlow<List<ProductEntity>> = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val rawMaterials: StateFlow<List<RawMaterialEntity>> = repository.allRawMaterials.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val holdOrders: StateFlow<List<HoldOrderEntity>> = repository.allHoldOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val kasbonList: StateFlow<List<KasbonEntity>> = repository.allKasbon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashExpenses: StateFlow<List<CashExpenseEntity>> = repository.allCashExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hppHistoryList: StateFlow<List<HppHistoryEntity>> = repository.allHppHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Session & Theme
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        prefs.edit().putBoolean("is_dark_mode", isDark).apply()
    }

    private val _showLoginScreen = MutableStateFlow(true)
    val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

    fun navigateToLogin() {
        _showLoginScreen.value = true
    }

    fun proceedToLogin() {
        _showLoginScreen.value = true
    }

    fun logout() {
        _currentUser.value = null
        _showLoginScreen.value = true
    }

    fun login(u: String, p: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val matched = repository.getUserByUsername(u)
            if (matched != null && matched.password == p) {
                _currentUser.value = matched
                _showLoginScreen.value = false
                onResult(true, "Berhasil masuk!")
            } else {
                onResult(false, "Username atau Password salah!")
            }
        }
    }

    // Active Navigation
    private val _selectedTab = MutableStateFlow(NavTab.Kasir)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    private val _selectedProductSubTab = MutableStateFlow(ProductSubTab.DaftarMenu)
    val selectedProductSubTab: StateFlow<ProductSubTab> = _selectedProductSubTab.asStateFlow()

    fun selectProductSubTab(subTab: ProductSubTab) {
        _selectedProductSubTab.value = subTab
    }

    private val _selectedSettingsSubTab = MutableStateFlow(SettingsSubTab.QrisBank)
    val selectedSettingsSubTab: StateFlow<SettingsSubTab> = _selectedSettingsSubTab.asStateFlow()

    fun selectSettingsSubTab(subTab: SettingsSubTab) {
        _selectedSettingsSubTab.value = subTab
    }

    // POS Kasir Filter & Cart
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _selectedCategoryFilter = MutableStateFlow("Semua")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    fun selectCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    // Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _orderType = MutableStateFlow("Dine-In")
    val orderType: StateFlow<String> = _orderType.asStateFlow()
    fun setOrderType(type: String) { _orderType.value = type }

    private val _tableNo = MutableStateFlow("")
    val tableNo: StateFlow<String> = _tableNo.asStateFlow()
    fun setTableNo(no: String) { _tableNo.value = no }

    private val _discountVal = MutableStateFlow(0.0)
    val discountVal: StateFlow<Double> = _discountVal.asStateFlow()
    fun setDiscountVal(v: Double) { _discountVal.value = v }

    private val _discountType = MutableStateFlow("persen") // "persen" or "nominal"
    val discountType: StateFlow<String> = _discountType.asStateFlow()
    fun setDiscountType(t: String) { _discountType.value = t }

    private val _taxPercent = MutableStateFlow(0.0)
    val taxPercent: StateFlow<Double> = _taxPercent.asStateFlow()
    fun setTaxPercent(v: Double) { _taxPercent.value = v }

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()
    fun setCustomerName(name: String) { _customerName.value = name }

    private val _cashPaid = MutableStateFlow(0.0)
    val cashPaid: StateFlow<Double> = _cashPaid.asStateFlow()
    fun setCashPaid(amount: Double) { _cashPaid.value = amount }

    private fun loadStoreSettings(): StoreSettings {
        return StoreSettings(
            namaToko = prefs.getString("store_nama", "Toko POS Kuliner") ?: "Toko POS Kuliner",
            alamatToko = prefs.getString("store_alamat", "Jl. Raya Utama No. 123, Jakarta") ?: "Jl. Raya Utama No. 123, Jakarta",
            noTelpToko = prefs.getString("store_notelp", "0812-3456-7890") ?: "0812-3456-7890",
            pesanStruk = prefs.getString("store_pesan_struk", "Terima kasih atas kunjungan Anda!") ?: "Terima kasih atas kunjungan Anda!",
            qrisUrl = prefs.getString("store_qris_url", "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=TOKO_KASIGRATIS_DEFAULT") ?: "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=TOKO_KASIGRATIS_DEFAULT",
            bankNama = prefs.getString("store_bank_nama", "BCA") ?: "BCA",
            bankNoRek = prefs.getString("store_bank_norek", "1234567890") ?: "1234567890",
            bankPemilik = prefs.getString("store_bank_pemilik", "PT KasiGRatis Indonesia") ?: "PT KasiGRatis Indonesia",
            modalAwalLaci = java.lang.Double.longBitsToDouble(prefs.getLong("store_modal_laci", java.lang.Double.doubleToRawLongBits(0.0)))
        )
    }

    private fun persistStoreSettings(settings: StoreSettings) {
        prefs.edit()
            .putString("store_nama", settings.namaToko)
            .putString("store_alamat", settings.alamatToko)
            .putString("store_notelp", settings.noTelpToko)
            .putString("store_pesan_struk", settings.pesanStruk)
            .putString("store_qris_url", settings.qrisUrl)
            .putString("store_bank_nama", settings.bankNama)
            .putString("store_bank_norek", settings.bankNoRek)
            .putString("store_bank_pemilik", settings.bankPemilik)
            .putLong("store_modal_laci", java.lang.Double.doubleToRawLongBits(settings.modalAwalLaci))
            .apply()
    }

    // Store Settings
    private val _storeSettings = MutableStateFlow(loadStoreSettings())
    val storeSettings: StateFlow<StoreSettings> = _storeSettings.asStateFlow()

    fun updateStoreProfile(namaToko: String, alamatToko: String, noTelpToko: String, pesanStruk: String) {
        val updated = _storeSettings.value.copy(
            namaToko = namaToko,
            alamatToko = alamatToko,
            noTelpToko = noTelpToko,
            pesanStruk = pesanStruk
        )
        _storeSettings.value = updated
        persistStoreSettings(updated)
    }

    fun updateQrisUrl(url: String) {
        val updated = _storeSettings.value.copy(qrisUrl = url)
        _storeSettings.value = updated
        persistStoreSettings(updated)
    }

    fun updateBankInfo(bankNama: String, bankNoRek: String, bankPemilik: String) {
        val updated = _storeSettings.value.copy(
            bankNama = bankNama,
            bankNoRek = bankNoRek,
            bankPemilik = bankPemilik
        )
        _storeSettings.value = updated
        persistStoreSettings(updated)
    }

    fun updateModalAwalLaci(modal: Double) {
        val updated = _storeSettings.value.copy(modalAwalLaci = modal)
        _storeSettings.value = updated
        persistStoreSettings(updated)
    }

    // Modal / Dialog States
    private val _activeProductForOptions = MutableStateFlow<ProductEntity?>(null)
    val activeProductForOptions: StateFlow<ProductEntity?> = _activeProductForOptions.asStateFlow()

    fun openProductOptions(product: ProductEntity) {
        _activeProductForOptions.value = product
    }

    fun closeProductOptions() {
        _activeProductForOptions.value = null
    }

    private val _activePaymentModal = MutableStateFlow<PaymentModalType?>(null)
    val activePaymentModal: StateFlow<PaymentModalType?> = _activePaymentModal.asStateFlow()

    fun openPaymentModal(type: PaymentModalType) {
        _activePaymentModal.value = type
    }

    fun closePaymentModal() {
        _activePaymentModal.value = null
    }

    private val _activeReceipt = MutableStateFlow<TransactionEntity?>(null)
    val activeReceipt: StateFlow<TransactionEntity?> = _activeReceipt.asStateFlow()

    fun showReceipt(trx: TransactionEntity) {
        _activeReceipt.value = trx
    }

    fun closeReceipt() {
        _activeReceipt.value = null
    }

    private val _showHoldOrdersModal = MutableStateFlow(false)
    val showHoldOrdersModal: StateFlow<Boolean> = _showHoldOrdersModal.asStateFlow()

    fun toggleHoldOrdersModal(show: Boolean) {
        _showHoldOrdersModal.value = show
    }

    private val _showGuideModal = MutableStateFlow<String?>(null) // "owner" or "kasir"
    val showGuideModal: StateFlow<String?> = _showGuideModal.asStateFlow()

    fun openGuideModal(role: String) { _showGuideModal.value = role }
    fun closeGuideModal() { _showGuideModal.value = null }

    // HPP Calculator State
    private val _hppState = MutableStateFlow(HppCalcState())
    val hppState: StateFlow<HppCalcState> = _hppState.asStateFlow()

    fun updateHppNamaProduk(nama: String) { _hppState.value = _hppState.value.copy(namaProduk = nama) }
    fun updateHppKategori(kategori: String) { _hppState.value = _hppState.value.copy(kategori = kategori) }
    fun updateHppTenagaKerja(v: Double) { _hppState.value = _hppState.value.copy(tenagaKerja = v) }
    fun updateHppOverhead(v: Double) { _hppState.value = _hppState.value.copy(overhead = v) }
    fun updateHppJumlahUnit(v: Int) { _hppState.value = _hppState.value.copy(jumlahUnit = maxOf(1, v)) }
    fun updateHppTargetFC(v: Double) { _hppState.value = _hppState.value.copy(targetFC = v) }

    fun addHppIngredient(nama: String, hargaPartai: Double, isiPartai: Double, pakai: Double, waste: Double, idBahan: Long? = null) {
        val hPerUnit = hargaPartai / maxOf(1.0, isiPartai)
        val murni = hPerUnit * pakai
        val denganWaste = murni * (1.0 + (waste / 100.0))
        val item = HppIngredientInput(idBahan, nama, hargaPartai, isiPartai, pakai, waste, denganWaste)
        val newList = _hppState.value.bahanList + item
        _hppState.value = _hppState.value.copy(bahanList = newList)
    }

    fun removeHppIngredient(index: Int) {
        val list = _hppState.value.bahanList.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _hppState.value = _hppState.value.copy(bahanList = list)
        }
    }

    fun resetHppCalculator(defaultCategory: String = "Makanan") {
        _hppState.value = HppCalcState(kategori = defaultCategory)
    }

    fun calculateHppResult() {
        val st = _hppState.value
        val totalBahan = st.bahanList.sumOf { it.hppAktual }
        val totalBiayaLain = st.tenagaKerja + st.overhead
        val totalBiaya = totalBahan + totalBiayaLain
        val unit = maxOf(1, st.jumlahUnit)
        val hppUnit = (totalBiaya / unit)

        val targetFCDecimal = maxOf(0.01, st.targetFC / 100.0)
        // Standard F&B Target Selling Price based on Food Cost Target %
        val saranTargetFC = Math.round(hppUnit / targetFCDecimal).toDouble()
        // Profit Margin 35%: Price = HPP / (1 - 0.35)
        val m35 = Math.round(hppUnit / 0.65).toDouble()
        // Profit Margin 50%: Price = HPP / (1 - 0.50)
        val m50 = Math.round(hppUnit / 0.50).toDouble()

        val defaultHarga = saranTargetFC
        val untung = maxOf(0.0, defaultHarga - hppUnit)
        val fcPct = if (defaultHarga > 0) (hppUnit / defaultHarga) * 100.0 else 0.0

        val result = HppResult(
            totalBahan = totalBahan,
            totalBiayaLain = totalBiayaLain,
            hppUnit = Math.round(hppUnit).toDouble(),
            saranTargetFC = saranTargetFC,
            m35 = m35,
            m50 = m50,
            customHarga = defaultHarga,
            customUntung = Math.round(untung).toDouble(),
            customFoodCostPct = Math.round(fcPct * 10.0) / 10.0
        )
        _hppState.value = _hppState.value.copy(calculatedResult = result)
    }

    fun updateCustomHppPrice(price: Double) {
        val res = _hppState.value.calculatedResult ?: return
        val untung = price - res.hppUnit
        val fcPct = if (price > 0) (res.hppUnit / price) * 100.0 else 0.0
        val marginPct = if (price > 0) (untung / price) * 100.0 else 0.0
        _hppState.value = _hppState.value.copy(
            calculatedResult = res.copy(
                customHarga = price,
                customUntung = Math.round(untung).toDouble(),
                customFoodCostPct = Math.round(fcPct * 10.0) / 10.0,
                customMarginVal = Math.round(marginPct * 10.0) / 10.0
            )
        )
    }

    fun saveHppToArchiveAndMenu(onDone: () -> Unit) {
        val st = _hppState.value
        val res = st.calculatedResult ?: return
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date())

            val bahanJson = JSONArray().apply {
                st.bahanList.forEach { b ->
                    put(JSONObject().apply {
                        put("idBahan", b.idBahan)
                        put("nama", b.nama)
                        put("pakai", b.pakai)
                        put("hppAktual", b.hppAktual)
                    })
                }
            }.toString()

            val hppEntity = HppHistoryEntity(
                waktuStr = dateStr,
                namaProduk = st.namaProduk,
                hppFinal = res.hppUnit,
                saranTargetFC = res.saranTargetFC,
                m35 = res.m35,
                m50 = res.m50,
                jumlahUnitFinal = st.jumlahUnit,
                totalBahan = res.totalBahan,
                totalBiayaLain = res.totalBiayaLain,
                targetFCPersen = st.targetFC,
                bahanListJson = bahanJson
            )
            repository.insertHppHistory(hppEntity)

            // Also sync/update or insert product in Product Menu
            if (st.namaProduk.isNotBlank()) {
                val existingProduct = products.value.find { it.nama.equals(st.namaProduk, ignoreCase = true) }
                if (existingProduct != null) {
                    repository.updateProduct(
                        existingProduct.copy(
                            kategori = st.kategori,
                            modal = res.hppUnit,
                            jual = res.customHarga
                        )
                    )
                } else {
                    val newProduct = ProductEntity(
                        emoji = "🥤",
                        nama = st.namaProduk,
                        kategori = st.kategori,
                        modal = res.hppUnit,
                        jual = res.customHarga,
                        stok = st.jumlahUnit,
                        aktif = true
                    )
                    repository.insertProduct(newProduct)
                }
            }

            onDone()
        }
    }

    // Cart Management Methods
    fun addProductToCart(product: ProductEntity, varian: String = "", toppings: List<ToppingItem> = emptyList()) {
        val currentList = _cart.value.toMutableList()
        val toppingPriceSum = toppings.sumOf { it.harga }
        val toppingNamesSorted = toppings.map { it.nama }.sorted().joinToString(",")
        val cartKey = "${product.id}|$varian|$toppingNamesSorted"

        var namaLengkap = product.nama
        if (varian.isNotBlank()) namaLengkap += " ($varian)"
        if (toppings.isNotEmpty()) namaLengkap += " +[${toppings.joinToString(", ") { it.nama }}]"

        val existingIndex = currentList.indexOfFirst { it.cartKey == cartKey }
        val existingQty = if (existingIndex != -1) currentList[existingIndex].qty else 0

        if (product.stok < (existingQty + 1)) return

        if (existingIndex != -1) {
            val oldItem = currentList[existingIndex]
            val newQty = oldItem.qty + 1
            val effectiveUnit = if (oldItem.grosirMin > 0 && newQty >= oldItem.grosirMin) oldItem.grosirHarga else oldItem.jualDasar
            val newSubtotal = newQty * (effectiveUnit + oldItem.toppingPrice)
            currentList[existingIndex] = oldItem.copy(qty = newQty, subtotal = newSubtotal)
        } else {
            val effectiveUnit = if (product.grosirMin > 0 && 1 >= product.grosirMin) product.grosirHarga else product.jual
            val item = CartItem(
                cartKey = cartKey,
                id = product.id,
                nama = namaLengkap,
                modal = product.modal,
                jualDasar = product.jual,
                grosirMin = product.grosirMin,
                grosirHarga = product.grosirHarga,
                toppingPrice = toppingPriceSum,
                qty = 1,
                subtotal = effectiveUnit + toppingPriceSum
            )
            currentList.add(item)
        }
        _cart.value = currentList
    }

    fun updateCartQty(index: Int, delta: Int) {
        val currentList = _cart.value.toMutableList()
        if (index !in currentList.indices) return
        val item = currentList[index]
        val newQty = item.qty + delta
        if (newQty <= 0) {
            currentList.removeAt(index)
        } else {
            val effectiveUnit = if (item.grosirMin > 0 && newQty >= item.grosirMin) item.grosirHarga else item.jualDasar
            val newSubtotal = newQty * (effectiveUnit + item.toppingPrice)
            currentList[index] = item.copy(qty = newQty, subtotal = newSubtotal)
        }
        _cart.value = currentList
    }

    fun clearCart() {
        _cart.value = emptyList()
        _customerName.value = ""
        _tableNo.value = ""
        _cashPaid.value = 0.0
    }

    // Checkout / Payment Process
    fun checkout(metode: String) {
        val c = _cart.value
        if (c.isEmpty()) return

        val rawSubtotal = c.sumOf { it.subtotal }
        val dType = _discountType.value
        val dVal = _discountVal.value
        val nomDiskon = if (dType == "persen") (rawSubtotal * (dVal / 100.0)) else minOf(rawSubtotal, dVal)
        val setelahDiskon = rawSubtotal - nomDiskon
        val nomPajak = (setelahDiskon * (_taxPercent.value / 100.0))
        val totalAkhir = Math.round(setelahDiskon + nomPajak).toDouble()

        val bayar = if (metode == "tunai") maxOf(totalAkhir, _cashPaid.value) else totalAkhir
        val kembali = if (metode == "tunai") maxOf(0.0, bayar - totalAkhir) else 0.0

        val now = Date()
        val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(now)
        val trxId = "TRX-${System.currentTimeMillis().toString().takeLast(6)}"

        val itemsJsonArray = JSONArray().apply {
            c.forEach { item ->
                put(JSONObject().apply {
                    put("cartKey", item.cartKey)
                    put("id", item.id)
                    put("nama", item.nama)
                    put("modal", item.modal)
                    put("jualDasar", item.jualDasar)
                    put("toppingPrice", item.toppingPrice)
                    put("qty", item.qty)
                    put("subtotal", item.subtotal)
                })
            }
        }.toString()

        val totalModalSum = c.sumOf { it.modal * it.qty }
        val kasirNama = _currentUser.value?.nama ?: "Kasir Utama"

        val trx = TransactionEntity(
            id = trxId,
            waktu = now.time,
            tanggalISO = isoDate,
            tanggalStr = dateStr,
            kasir = kasirNama,
            pelanggan = _customerName.value.ifBlank { "Umum" },
            tipePesanan = _orderType.value,
            nomorMeja = _tableNo.value.ifBlank { "-" },
            metode = metode,
            subtotal = rawSubtotal,
            diskon = nomDiskon,
            pajak = nomPajak,
            totalPemasukan = totalAkhir,
            totalModal = totalModalSum,
            uangBayar = bayar,
            uangKembali = kembali,
            itemsJson = itemsJsonArray
        )

        viewModelScope.launch {
            if (metode == "kasbon") {
                val kasbon = KasbonEntity(
                    trxId = trxId,
                    tanggalISO = isoDate,
                    tanggalStr = dateStr,
                    pelanggan = _customerName.value.ifBlank { "Umum" },
                    tipePesanan = _orderType.value,
                    nomorMeja = _tableNo.value.ifBlank { "-" },
                    total = totalAkhir,
                    status = "Belum Lunas",
                    itemsJson = itemsJsonArray
                )
                repository.insertKasbon(kasbon)
            } else {
                repository.insertTransaction(trx)
            }

            // Deduct product stock & raw material inventory (BOM)
            c.forEach { item ->
                val p = repository.getProductById(item.id)
                if (p != null) {
                    val updatedStock = maxOf(0, p.stok - item.qty)
                    repository.updateProduct(p.copy(stok = updatedStock))

                    // If BOM resep is configured, parse and deduct raw materials
                    if (p.resepJson.isNotBlank() && p.resepJson != "[]") {
                        try {
                            val jsonArray = JSONArray(p.resepJson)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val idBahan = if (obj.has("idBahan")) obj.optLong("idBahan", -1) else -1
                                val pakai = obj.optDouble("pakai", 0.0)
                                if (idBahan > 0) {
                                    val rm = repository.getRawMaterialById(idBahan)
                                    if (rm != null) {
                                        val newRmStok = maxOf(0.0, rm.stok - (pakai * item.qty))
                                        repository.updateRawMaterial(rm.copy(stok = newRmStok))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            _activeReceipt.value = trx
            clearCart()
        }
    }

    // Held Orders Logic
    fun holdCurrentOrder() {
        val c = _cart.value
        if (c.isEmpty()) return
        val nowStr = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date())
        val cust = _customerName.value.ifBlank { "Pelanggan #${(holdOrders.value.size + 1)}" }
        val table = _tableNo.value.ifBlank { "Meja #${(holdOrders.value.size + 1)}" }

        val itemsJson = JSONArray().apply {
            c.forEach { item ->
                put(JSONObject().apply {
                    put("cartKey", item.cartKey)
                    put("id", item.id)
                    put("nama", item.nama)
                    put("modal", item.modal)
                    put("jualDasar", item.jualDasar)
                    put("grosirMin", item.grosirMin)
                    put("grosirHarga", item.grosirHarga)
                    put("toppingPrice", item.toppingPrice)
                    put("qty", item.qty)
                    put("subtotal", item.subtotal)
                })
            }
        }.toString()

        val entity = HoldOrderEntity(
            waktuStr = nowStr,
            pelanggan = cust,
            nomorMeja = table,
            tipePesanan = _orderType.value,
            itemsJson = itemsJson
        )

        viewModelScope.launch {
            repository.insertHoldOrder(entity)
            clearCart()
        }
    }

    fun restoreHoldOrder(holdOrder: HoldOrderEntity) {
        viewModelScope.launch {
            val list = mutableListOf<CartItem>()
            try {
                val jsonArr = JSONArray(holdOrder.itemsJson)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    list.add(
                        CartItem(
                            cartKey = obj.getString("cartKey"),
                            id = obj.getLong("id"),
                            nama = obj.getString("nama"),
                            modal = obj.getDouble("modal"),
                            jualDasar = obj.getDouble("jualDasar"),
                            grosirMin = obj.optInt("grosirMin", 0),
                            grosirHarga = obj.optDouble("grosirHarga", 0.0),
                            toppingPrice = obj.getDouble("toppingPrice"),
                            qty = obj.getInt("qty"),
                            subtotal = obj.getDouble("subtotal")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _cart.value = list
            _customerName.value = holdOrder.pelanggan
            _tableNo.value = holdOrder.nomorMeja
            _orderType.value = holdOrder.tipePesanan
            repository.deleteHoldOrder(holdOrder.id)
            _showHoldOrdersModal.value = false
        }
    }

    fun deleteHoldOrder(id: Long) {
        viewModelScope.launch {
            repository.deleteHoldOrder(id)
        }
    }

    // Kasbon Mark as Paid
    fun lunasiKasbon(kasbon: KasbonEntity) {
        viewModelScope.launch {
            val updated = kasbon.copy(status = "Lunas")
            repository.updateKasbon(updated)

            val now = Date()
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(now)
            val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)

            val trx = TransactionEntity(
                id = "LUNAS-${kasbon.trxId}",
                waktu = now.time,
                tanggalISO = isoDate,
                tanggalStr = dateStr,
                kasir = _currentUser.value?.nama ?: "Kasir Utama",
                pelanggan = kasbon.pelanggan,
                tipePesanan = kasbon.tipePesanan,
                nomorMeja = kasbon.nomorMeja,
                metode = "tunai",
                totalPemasukan = kasbon.total,
                itemsJson = kasbon.itemsJson
            )
            repository.insertTransaction(trx)
        }
    }

    // Cash Expenses (Kas Keluar)
    fun addCashExpense(ket: String, nominal: Double) {
        if (ket.isBlank() || nominal <= 0) return
        val now = Date()
        val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(now)
        val entity = CashExpenseEntity(
            tanggalISO = isoDate,
            tanggalStr = dateStr,
            kasir = _currentUser.value?.nama ?: "Kasir Utama",
            keterangan = ket,
            nominal = nominal
        )
        viewModelScope.launch {
            repository.insertCashExpense(entity)
        }
    }

    fun deleteCashExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteCashExpense(id)
        }
    }

    // Product CRUD Operations
    fun saveProduct(
        id: Long = 0,
        emoji: String,
        nama: String,
        kategori: String,
        modal: Double,
        jual: Double,
        grosirMin: Int,
        grosirHarga: Double,
        stok: Int,
        varianList: List<String>,
        toppingList: List<ToppingItem>,
        resepList: List<RecipeIngredient>
    ) {
        if (nama.isBlank() || jual <= 0) return
        val varianJson = JSONArray(varianList).toString()
        val toppingJson = JSONArray().apply {
            toppingList.forEach { t ->
                put(JSONObject().apply {
                    put("nama", t.nama)
                    put("harga", t.harga)
                })
            }
        }.toString()

        val resepJson = JSONArray().apply {
            resepList.forEach { r ->
                put(JSONObject().apply {
                    put("idBahan", r.idBahan)
                    put("nama", r.nama)
                    put("pakai", r.pakai)
                })
            }
        }.toString()

        val entity = ProductEntity(
            id = id,
            emoji = emoji.ifBlank { "📦" },
            nama = nama,
            kategori = kategori,
            modal = modal,
            jual = jual,
            grosirMin = grosirMin,
            grosirHarga = grosirHarga,
            varianJson = varianJson,
            toppingJson = toppingJson,
            stok = stok,
            resepJson = resepJson,
            aktif = true
        )

        viewModelScope.launch {
            if (id > 0) repository.updateProduct(entity) else repository.insertProduct(entity)
        }
    }

    fun toggleProductActive(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(aktif = !product.aktif))
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }

    fun adjustProductStock(productId: Long, delta: Int) {
        viewModelScope.launch {
            val p = repository.getProductById(productId) ?: return@launch
            val updated = maxOf(0, p.stok + delta)
            repository.updateProduct(p.copy(stok = updated))
        }
    }

    // Raw Material CRUD
    fun saveRawMaterial(id: Long = 0, nama: String, harga: Double, isi: Double, satuan: String, stok: Double? = null) {
        if (nama.isBlank() || harga <= 0 || isi <= 0) return
        viewModelScope.launch {
            if (id > 0) {
                val existing = repository.getRawMaterialById(id)
                val finalStok = stok ?: existing?.stok ?: isi
                val entity = RawMaterialEntity(
                    id = id,
                    nama = nama,
                    harga = harga,
                    isi = isi,
                    stok = finalStok,
                    satuan = satuan.ifBlank { "gram" }
                )
                repository.updateRawMaterial(entity)
            } else {
                val entity = RawMaterialEntity(
                    id = 0,
                    nama = nama,
                    harga = harga,
                    isi = isi,
                    stok = stok ?: isi,
                    satuan = satuan.ifBlank { "gram" }
                )
                repository.insertRawMaterial(entity)
            }
        }
    }

    fun adjustRawMaterialStock(rawMaterialId: Long, delta: Double) {
        viewModelScope.launch {
            val rm = repository.getRawMaterialById(rawMaterialId) ?: return@launch
            val updated = maxOf(0.0, rm.stok + delta)
            repository.updateRawMaterial(rm.copy(stok = updated))
        }
    }

    fun deleteRawMaterial(id: Long) {
        viewModelScope.launch {
            repository.deleteRawMaterial(id)
        }
    }

    // Category CRUD
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name))
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // User Management CRUD
    fun saveUser(id: Long = 0, nama: String, username: String, pass: String, role: String) {
        if (nama.isBlank() || username.isBlank() || pass.isBlank()) return
        val entity = UserEntity(id = id, nama = nama, username = username, password = pass, role = role)
        viewModelScope.launch {
            if (id > 0) repository.updateUser(entity) else repository.insertUser(entity)
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.deleteUser(id)
        }
    }

    fun resetUserPassword(username: String, newPass: String) {
        viewModelScope.launch {
            val u = repository.getUserByUsername(username)
            if (u != null) {
                repository.updateUser(u.copy(password = newPass))
            }
        }
    }

    fun deleteHppHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHppHistory(id)
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
        }
    }

    // ==========================================
    // BACKUP & RESTORE DATA JSON ENGINE
    // ==========================================

    suspend fun generateBackupJsonString(): String {
        val root = JSONObject()
        root.put("app", "KasiGR-atis POS")
        root.put("version", "6.7")
        root.put("schemaVersion", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Store settings
        val curSettings = _storeSettings.value
        val settingsObj = JSONObject().apply {
            put("namaToko", curSettings.namaToko)
            put("alamatToko", curSettings.alamatToko)
            put("noTelpToko", curSettings.noTelpToko)
            put("pesanStruk", curSettings.pesanStruk)
            put("qrisUrl", curSettings.qrisUrl)
            put("bankNama", curSettings.bankNama)
            put("bankNoRek", curSettings.bankNoRek)
            put("bankPemilik", curSettings.bankPemilik)
            put("modalAwalLaci", curSettings.modalAwalLaci)
        }
        root.put("storeSettings", settingsObj)

        // Categories
        val categories = repository.getAllCategoriesDirect()
        val catArray = JSONArray().apply {
            categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                })
            }
        }
        root.put("categories", catArray)

        // Products
        val products = repository.getAllProductsDirect()
        val prodArray = JSONArray().apply {
            products.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("emoji", p.emoji)
                    put("nama", p.nama)
                    put("kategori", p.kategori)
                    put("modal", p.modal)
                    put("jual", p.jual)
                    put("grosirMin", p.grosirMin)
                    put("grosirHarga", p.grosirHarga)
                    put("varianJson", p.varianJson)
                    put("toppingJson", p.toppingJson)
                    put("stok", p.stok)
                    put("resepJson", p.resepJson)
                    put("aktif", p.aktif)
                })
            }
        }
        root.put("products", prodArray)

        // Raw Materials
        val materials = repository.getAllRawMaterialsDirect()
        val matArray = JSONArray().apply {
            materials.forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id)
                    put("nama", m.nama)
                    put("harga", m.harga)
                    put("isi", m.isi)
                    put("stok", m.stok)
                    put("satuan", m.satuan)
                })
            }
        }
        root.put("rawMaterials", matArray)

        // Transactions
        val transactions = repository.getAllTransactionsDirect()
        val trxArray = JSONArray().apply {
            transactions.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("waktu", t.waktu)
                    put("tanggalISO", t.tanggalISO)
                    put("tanggalStr", t.tanggalStr)
                    put("kasir", t.kasir)
                    put("pelanggan", t.pelanggan)
                    put("tipePesanan", t.tipePesanan)
                    put("nomorMeja", t.nomorMeja)
                    put("metode", t.metode)
                    put("subtotal", t.subtotal)
                    put("diskon", t.diskon)
                    put("pajak", t.pajak)
                    put("totalPemasukan", t.totalPemasukan)
                    put("totalModal", t.totalModal)
                    put("uangBayar", t.uangBayar)
                    put("uangKembali", t.uangKembali)
                    put("itemsJson", t.itemsJson)
                })
            }
        }
        root.put("transactions", trxArray)

        // Kasbon
        val kasbons = repository.getAllKasbonDirect()
        val kasbonArray = JSONArray().apply {
            kasbons.forEach { k ->
                put(JSONObject().apply {
                    put("id", k.id)
                    put("trxId", k.trxId)
                    put("tanggalISO", k.tanggalISO)
                    put("tanggalStr", k.tanggalStr)
                    put("pelanggan", k.pelanggan)
                    put("tipePesanan", k.tipePesanan)
                    put("nomorMeja", k.nomorMeja)
                    put("total", k.total)
                    put("status", k.status)
                    put("itemsJson", k.itemsJson)
                })
            }
        }
        root.put("kasbon", kasbonArray)

        // Cash Expenses
        val expenses = repository.getAllCashExpensesDirect()
        val expArray = JSONArray().apply {
            expenses.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("tanggalISO", e.tanggalISO)
                    put("tanggalStr", e.tanggalStr)
                    put("kasir", e.kasir)
                    put("keterangan", e.keterangan)
                    put("nominal", e.nominal)
                })
            }
        }
        root.put("cashExpenses", expArray)

        // HPP History
        val hpps = repository.getAllHppHistoryDirect()
        val hppArray = JSONArray().apply {
            hpps.forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("waktuStr", h.waktuStr)
                    put("namaProduk", h.namaProduk)
                    put("hppFinal", h.hppFinal)
                    put("saranTargetFC", h.saranTargetFC)
                    put("m35", h.m35)
                    put("m50", h.m50)
                    put("jumlahUnitFinal", h.jumlahUnitFinal)
                    put("totalBahan", h.totalBahan)
                    put("totalBiayaLain", h.totalBiayaLain)
                    put("targetFCPersen", h.targetFCPersen)
                    put("bahanListJson", h.bahanListJson)
                    put("statusMargin", h.statusMargin)
                })
            }
        }
        root.put("hppHistory", hppArray)

        // Users
        val users = repository.getAllUsersDirect()
        val usersArray = JSONArray().apply {
            users.forEach { u ->
                put(JSONObject().apply {
                    put("id", u.id)
                    put("nama", u.nama)
                    put("username", u.username)
                    put("password", u.password)
                    put("role", u.role)
                })
            }
        }
        root.put("users", usersArray)

        return root.toString(2)
    }

    fun shareBackupFile(context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = generateBackupJsonString()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "KasiGRatis_Backup_$timestamp.json"

                val backupDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
                val file = File(backupDir, fileName)

                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos, "UTF-8").use { writer ->
                        writer.write(jsonString)
                        writer.flush()
                    }
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "Backup Data KasiGR-atis POS ($fileName)")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(sendIntent, "Bagikan File Backup").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)

                onResult(true, "File backup $fileName siap dibagikan!")
            } catch (e: Exception) {
                onResult(false, "Gagal membuat file backup: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun saveBackupToDownloads(context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = generateBackupJsonString()
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "KasiGRatis_Backup_$timestamp.json"

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { os ->
                            OutputStreamWriter(os, "UTF-8").use { writer ->
                                writer.write(jsonString)
                                writer.flush()
                            }
                        }
                        onResult(true, "✅ File backup berhasil disimpan di folder Download: $fileName")
                    } else {
                        onResult(false, "Gagal membuat file di folder Download")
                    }
                } else {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { fos ->
                        OutputStreamWriter(fos, "UTF-8").use { writer ->
                            writer.write(jsonString)
                            writer.flush()
                        }
                    }
                    onResult(true, "✅ File backup berhasil disimpan di folder Download: $fileName")
                }
            } catch (e: Exception) {
                onResult(false, "Gagal menyimpan file backup: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun writeBackupToUri(context: android.content.Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = generateBackupJsonString()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os, "UTF-8").use { writer ->
                        writer.write(jsonString)
                        writer.flush()
                    }
                }
                onResult(true, "✅ File backup berhasil disimpan ke lokasi memori yang Anda pilih!")
            } catch (e: Exception) {
                onResult(false, "Gagal menyimpan ke lokasi terpilih: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun copyBackupToClipboard(context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonString = generateBackupJsonString()
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("KasiGRatis Backup JSON", jsonString)
                clipboard.setPrimaryClip(clip)
                onResult(true, "Teks JSON Backup berhasil disalin ke Clipboard!")
            } catch (e: Exception) {
                onResult(false, "Gagal menyalin backup: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun readTextFromUri(context: android.content.Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, "UTF-8").use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun restoreBackupData(
        jsonString: String,
        replaceAll: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (jsonString.isBlank()) {
                    onResult(false, "File atau teks data JSON kosong.")
                    return@launch
                }

                val root = JSONObject(jsonString)
                if (!root.has("products") && !root.has("categories") && !root.has("transactions")) {
                    onResult(false, "Format file bukan cadangan data KasiGR-atis yang valid.")
                    return@launch
                }

                // If replaceAll, clear existing data tables
                if (replaceAll) {
                    repository.clearAllDataForRestore()
                }

                var prodCount = 0
                var matCount = 0
                var catCount = 0
                var trxCount = 0
                var kasbonCount = 0
                var expCount = 0
                var hppCount = 0
                var userCount = 0

                // 1. Restore Store Settings
                if (root.has("storeSettings")) {
                    val s = root.getJSONObject("storeSettings")
                    val restoredSettings = StoreSettings(
                        namaToko = s.optString("namaToko", _storeSettings.value.namaToko),
                        alamatToko = s.optString("alamatToko", _storeSettings.value.alamatToko),
                        noTelpToko = s.optString("noTelpToko", _storeSettings.value.noTelpToko),
                        pesanStruk = s.optString("pesanStruk", _storeSettings.value.pesanStruk),
                        qrisUrl = s.optString("qrisUrl", _storeSettings.value.qrisUrl),
                        bankNama = s.optString("bankNama", _storeSettings.value.bankNama),
                        bankNoRek = s.optString("bankNoRek", _storeSettings.value.bankNoRek),
                        bankPemilik = s.optString("bankPemilik", _storeSettings.value.bankPemilik),
                        modalAwalLaci = s.optDouble("modalAwalLaci", _storeSettings.value.modalAwalLaci)
                    )
                    _storeSettings.value = restoredSettings
                    persistStoreSettings(restoredSettings)
                }

                // 2. Restore Categories
                if (root.has("categories")) {
                    val arr = root.getJSONArray("categories")
                    val list = mutableListOf<CategoryEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(CategoryEntity(
                            id = if (replaceAll) obj.optLong("id", 0) else 0,
                            name = obj.optString("name", "Umum")
                        ))
                    }
                    if (list.isNotEmpty()) {
                        repository.insertCategories(list)
                        catCount = list.size
                    }
                }

                // 3. Restore Raw Materials
                if (root.has("rawMaterials")) {
                    val arr = root.getJSONArray("rawMaterials")
                    val list = mutableListOf<RawMaterialEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(RawMaterialEntity(
                            id = if (replaceAll) obj.optLong("id", 0) else 0,
                            nama = obj.optString("nama", "Bahan"),
                            harga = obj.optDouble("harga", 0.0),
                            isi = obj.optDouble("isi", 1.0),
                            stok = obj.optDouble("stok", 0.0),
                            satuan = obj.optString("satuan", "gram")
                        ))
                    }
                    if (list.isNotEmpty()) {
                        repository.insertRawMaterials(list)
                        matCount = list.size
                    }
                }

                // 4. Restore Products
                if (root.has("products")) {
                    val arr = root.getJSONArray("products")
                    val list = mutableListOf<ProductEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(ProductEntity(
                            id = if (replaceAll) obj.optLong("id", 0) else 0,
                            emoji = obj.optString("emoji", "📦"),
                            nama = obj.optString("nama", "Produk"),
                            kategori = obj.optString("kategori", "Umum"),
                            modal = obj.optDouble("modal", 0.0),
                            jual = obj.optDouble("jual", 0.0),
                            grosirMin = obj.optInt("grosirMin", 0),
                            grosirHarga = obj.optDouble("grosirHarga", 0.0),
                            varianJson = obj.optString("varianJson", "[]"),
                            toppingJson = obj.optString("toppingJson", "[]"),
                            stok = obj.optInt("stok", 0),
                            resepJson = obj.optString("resepJson", "[]"),
                            aktif = obj.optBoolean("aktif", true)
                        ))
                    }
                    if (list.isNotEmpty()) {
                        repository.insertProducts(list)
                        prodCount = list.size
                    }
                }

                // 5. Restore Transactions
                if (root.has("transactions")) {
                    val arr = root.getJSONArray("transactions")
                    val list = mutableListOf<TransactionEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(TransactionEntity(
                            id = obj.optString("id", "TRX-${System.currentTimeMillis()}-$i"),
                            waktu = obj.optLong("waktu", System.currentTimeMillis()),
                            tanggalISO = obj.optString("tanggalISO", "2026-01-01"),
                            tanggalStr = obj.optString("tanggalStr", ""),
                            kasir = obj.optString("kasir", "Kasir"),
                            pelanggan = obj.optString("pelanggan", "Umum"),
                            tipePesanan = obj.optString("tipePesanan", "Dine-In"),
                            nomorMeja = obj.optString("nomorMeja", "-"),
                            metode = obj.optString("metode", "tunai"),
                            subtotal = obj.optDouble("subtotal", 0.0),
                            diskon = obj.optDouble("diskon", 0.0),
                            pajak = obj.optDouble("pajak", 0.0),
                            totalPemasukan = obj.optDouble("totalPemasukan", 0.0),
                            totalModal = obj.optDouble("totalModal", 0.0),
                            uangBayar = obj.optDouble("uangBayar", 0.0),
                            uangKembali = obj.optDouble("uangKembali", 0.0),
                            itemsJson = obj.optString("itemsJson", "[]")
                        ))
                    }
                    if (list.isNotEmpty()) {
                        repository.insertTransactions(list)
                        trxCount = list.size
                    }
                }

                // 6. Restore Kasbon
                if (root.has("kasbon")) {
                    val arr = root.getJSONArray("kasbon")
                    val list = mutableListOf<KasbonEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(KasbonEntity(
                            id = if (replaceAll) obj.optLong("id", 0) else 0,
                            trxId = obj.optString("trxId", "KSB-${System.currentTimeMillis()}-$i"),
                            tanggalISO = obj.optString("tanggalISO", "2026-01-01"),
                            tanggalStr = obj.optString("tanggalStr", ""),
                            pelanggan = obj.optString("pelanggan", "Pelanggan"),
                            tipePesanan = obj.optString("tipePesanan", "Dine-In"),
                            nomorMeja = obj.optString("nomorMeja", "-"),
                            total = obj.optDouble("total", 0.0),
                            status = obj.optString("status", "Belum Lunas"),
                            itemsJson = obj.optString("itemsJson", "[]")
                        ))
                    }
                    if (list.isNotEmpty()) {
                        repository.insertKasbons(list)
                        kasbonCount = list.size
                    }
                }

                // 7. Restore Cash Expenses
                if (root.has("cashExpenses")) {
                    val arr = root.getJSONArray("cashExpenses")
                    val list = mutableListOf<CashExpenseEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(CashExpenseEntity(
                            id = if (replaceAll) obj.optLong("id", 0) else 0,
                            tanggalISO = obj.optString("tanggalISO", "2026-01-01"),
                            tanggalStr = obj.optString("tanggalStr", ""),
                            kasir = obj.optString("kasir", "Kasir"),
                            keterangan = obj.optString("keterangan", "Pengeluaran"),
                            nominal = obj.optDouble("nominal", 0.0)
                        ))
                    }
                    if (list.isNotEmpty()) {
                        repository.insertCashExpenses(list)
                        expCount = list.size
                    }
                }

                // 8. Restore HPP History
                if (root.has("hppHistory")) {
                    val arr = root.getJSONArray("hppHistory")
                    val list = mutableListOf<HppHistoryEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(HppHistoryEntity(
                            id = if (replaceAll) obj.optLong("id", 0) else 0,
                            waktuStr = obj.optString("waktuStr", ""),
                            namaProduk = obj.optString("namaProduk", "Produk HPP"),
                            hppFinal = obj.optDouble("hppFinal", 0.0),
                            saranTargetFC = obj.optDouble("saranTargetFC", 0.0),
                            m35 = obj.optDouble("m35", 0.0),
                            m50 = obj.optDouble("m50", 0.0),
                            jumlahUnitFinal = obj.optInt("jumlahUnitFinal", 1),
                            totalBahan = obj.optDouble("totalBahan", 0.0),
                            totalBiayaLain = obj.optDouble("totalBiayaLain", 0.0),
                            targetFCPersen = obj.optDouble("targetFCPersen", 35.0),
                            bahanListJson = obj.optString("bahanListJson", "[]"),
                            statusMargin = obj.optString("statusMargin", "✅ HPP Pulih")
                        ))
                    }
                    if (list.isNotEmpty()) {
                        repository.insertHppHistories(list)
                        hppCount = list.size
                    }
                }

                // 9. Restore Users (optional/if present)
                if (root.has("users")) {
                    val arr = root.getJSONArray("users")
                    val list = mutableListOf<UserEntity>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(UserEntity(
                            id = if (replaceAll) obj.optLong("id", 0) else 0,
                            nama = obj.optString("nama", "Pengguna"),
                            username = obj.optString("username", "user$i"),
                            password = obj.optString("password", "123456"),
                            role = obj.optString("role", "kasir")
                        ))
                    }
                    if (list.isNotEmpty()) {
                        repository.insertUsers(list)
                        userCount = list.size
                    }
                }

                val summary = StringBuilder("✅ Pemulihan Data Berhasil!\n")
                if (prodCount > 0) summary.append("• $prodCount Produk Menu\n")
                if (matCount > 0) summary.append("• $matCount Bahan Baku\n")
                if (catCount > 0) summary.append("• $catCount Kategori\n")
                if (trxCount > 0) summary.append("• $trxCount Riwayat Transaksi\n")
                if (kasbonCount > 0) summary.append("• $kasbonCount Kasbon\n")
                if (expCount > 0) summary.append("• $expCount Pengeluaran Kas\n")
                if (hppCount > 0) summary.append("• $hppCount Arsip HPP\n")
                if (userCount > 0) summary.append("• $userCount Akun Kasir/Owner\n")
                summary.append("• Profil Toko & Pengaturan dipulihkan.")

                onResult(true, summary.toString())
            } catch (e: Exception) {
                onResult(false, "Gagal memulihkan data: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    // Formatting utility
    fun formatRupiah(amount: Double): String {
        return try {
            val formatter = NumberFormat.getInstance(Locale("id", "ID"))
            formatter.format(Math.round(amount))
        } catch (e: Exception) {
            Math.round(amount).toString()
        }
    }
}
