package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.CashExpenseEntity
import com.example.data.model.KasbonEntity
import com.example.data.model.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportHelper {


    fun generateAndPrintHtmlReport(
        context: Context,
        storeName: String,
        periodName: String,
        currentUser: String,
        transactions: List<TransactionEntity>,
        expenses: List<CashExpenseEntity>,
        topProducts: List<Triple<String, Int, Double>>
    ) {
        val htmlContent = buildHtmlReport(
            storeName = storeName,
            periodName = periodName,
            currentUser = currentUser,
            transactions = transactions,
            expenses = expenses,
            topProducts = topProducts
        )

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "${storeName.replace(" ", "_")}_Laporan_${System.currentTimeMillis()}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    fun buildHtmlReport(
        storeName: String,
        periodName: String,
        currentUser: String,
        transactions: List<TransactionEntity>,
        expenses: List<CashExpenseEntity>,
        topProducts: List<Triple<String, Int, Double>>
    ): String {
        val nowStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val numberFormat = NumberFormat.getInstance(Locale("id", "ID"))

        fun fmt(num: Double): String = numberFormat.format(Math.round(num))

        val totalOmset = transactions.sumOf { it.totalPemasukan }
        val totalModal = transactions.sumOf { it.totalModal }
        val totalExpenses = expenses.sumOf { it.nominal }
        val labaKotor = totalOmset - totalModal
        val labaBersih = labaKotor - totalExpenses
        val profitMargin = if (totalOmset > 0) (labaBersih / totalOmset) * 100.0 else 0.0
        val countTrx = transactions.size
        val avgBasket = if (countTrx > 0) totalOmset / countTrx else 0.0

        val omsetTunai = transactions.filter { it.metode == "tunai" }.sumOf { it.totalPemasukan }
        val omsetQris = transactions.filter { it.metode == "qris" }.sumOf { it.totalPemasukan }
        val omsetTransfer = transactions.filter { it.metode == "transfer" }.sumOf { it.totalPemasukan }
        val omsetKasbon = transactions.filter { it.metode == "kasbon" }.sumOf { it.totalPemasukan }

        val dineInCount = transactions.count { it.tipePesanan == "Dine-In" }
        val takeawayCount = transactions.count { it.tipePesanan != "Dine-In" }

        val topProductsRows = StringBuilder()
        topProducts.forEachIndexed { idx, p ->
            topProductsRows.append("""
                <tr>
                    <td style="text-align:center;">${idx + 1}</td>
                    <td><strong>${p.first}</strong></td>
                    <td style="text-align:center;">${p.second} Porsi</td>
                    <td style="text-align:right;">Rp ${fmt(p.third)}</td>
                </tr>
            """.trimIndent())
        }

        val expenseRows = StringBuilder()
        if (expenses.isEmpty()) {
            expenseRows.append("""<tr><td colspan="4" style="text-align:center; color:#888;">Tidak ada catatan kas keluar pada periode ini.</td></tr>""")
        } else {
            expenses.forEachIndexed { idx, e ->
                expenseRows.append("""
                    <tr>
                        <td style="text-align:center;">${idx + 1}</td>
                        <td>${e.tanggalISO}</td>
                        <td>${e.keterangan} (Kasir: ${e.kasir})</td>
                        <td style="text-align:right; color:#c0392b; font-weight:bold;">- Rp ${fmt(e.nominal)}</td>
                    </tr>
                """.trimIndent())
            }
        }

        val transactionRows = StringBuilder()
        if (transactions.isEmpty()) {
            transactionRows.append("""<tr><td colspan="7" style="text-align:center; color:#888;">Tidak ada transaksi penjualan pada periode ini.</td></tr>""")
        } else {
            transactions.take(50).forEach { t ->
                val profitTrx = t.totalPemasukan - t.totalModal
                transactionRows.append("""
                    <tr>
                        <td><strong>${t.id}</strong></td>
                        <td>${t.tanggalStr}</td>
                        <td>${t.kasir}</td>
                        <td>${t.tipePesanan}</td>
                        <td style="text-transform:uppercase;">${t.metode}</td>
                        <td style="text-align:right;">Rp ${fmt(t.totalPemasukan)}</td>
                        <td style="text-align:right; color:#27ae60; font-weight:bold;">Rp ${fmt(profitTrx)}</td>
                    </tr>
                """.trimIndent())
            }
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Laporan Penjualan - $storeName</title>
                <style>
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        color: #333;
                        margin: 20px;
                        padding: 0;
                        font-size: 12px;
                    }
                    .header {
                        border-bottom: 3px solid #2c3e50;
                        padding-bottom: 10px;
                        margin-bottom: 20px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 22px;
                        color: #2c3e50;
                        text-transform: uppercase;
                    }
                    .header p {
                        margin: 3px 0;
                        color: #555;
                        font-size: 11px;
                    }
                    .grid-2 {
                        display: flex;
                        gap: 15px;
                        margin-bottom: 20px;
                    }
                    .card {
                        flex: 1;
                        background: #f8f9fa;
                        border: 1px solid #e9ecef;
                        border-radius: 8px;
                        padding: 12px;
                    }
                    .card h3 {
                        margin-top: 0;
                        margin-bottom: 10px;
                        font-size: 13px;
                        color: #2c3e50;
                        border-bottom: 1px solid #ddd;
                        padding-bottom: 5px;
                    }
                    .stat-row {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 6px;
                    }
                    .stat-row .label { color: #555; }
                    .stat-row .val { font-weight: bold; }
                    .highlight-green { color: #27ae60; font-size: 14px; }
                    .highlight-red { color: #c0392b; }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                    }
                    th {
                        background: #2c3e50;
                        color: #fff;
                        text-align: left;
                        padding: 8px;
                        font-size: 11px;
                        text-transform: uppercase;
                    }
                    td {
                        padding: 7px 8px;
                        border-bottom: 1px solid #eee;
                        font-size: 11px;
                    }
                    tr:nth-child(even) { background-color: #fcfcfc; }
                    .section-title {
                        font-size: 14px;
                        font-weight: bold;
                        color: #2c3e50;
                        margin-top: 15px;
                        margin-bottom: 8px;
                    }
                    .footer {
                        margin-top: 30px;
                        text-align: center;
                        font-size: 10px;
                        color: #888;
                        border-top: 1px solid #ddd;
                        padding-top: 10px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$storeName</h1>
                    <p><strong>DOKUMEN EVALUASI PENJUALAN & KEUANGAN TOKO</strong></p>
                    <p>Periode Evaluasi: <strong>$periodName</strong> | Dicetak Pada: $nowStr | Operator/Role: $currentUser</p>
                </div>

                <div class="grid-2">
                    <div class="card">
                        <h3>📊 Ringkasan Eksekutif Keuangan</h3>
                        <div class="stat-row"><span class="label">Total Transaksi Selesai:</span><span class="val">$countTrx Trx</span></div>
                        <div class="stat-row"><span class="label">Total Omset Kotor (Gross Sales):</span><span class="val">Rp ${fmt(totalOmset)}</span></div>
                        <div class="stat-row"><span class="label">Total Modal HPP Produk:</span><span class="val highlight-red">- Rp ${fmt(totalModal)}</span></div>
                        <div class="stat-row"><span class="label">Total Kas Keluar Operasional:</span><span class="val highlight-red">- Rp ${fmt(totalExpenses)}</span></div>
                        <hr style="border: 0; border-top: 1px dashed #ccc; margin: 8px 0;">
                        <div class="stat-row"><span class="label">Laba Bersih Riil (Net Profit):</span><span class="val highlight-green">Rp ${fmt(labaBersih)}</span></div>
                        <div class="stat-row"><span class="label">Profit Margin Ratio (%):</span><span class="val">${String.format("%.1f", profitMargin)}%</span></div>
                        <div class="stat-row"><span class="label">Rata-rata Pembelian (AOV):</span><span class="val">Rp ${fmt(avgBasket)}</span></div>
                    </div>

                    <div class="card">
                        <h3>💳 Rincian Metode & Layanan</h3>
                        <div class="stat-row"><span class="label">Tunai / Cash Laci:</span><span class="val">Rp ${fmt(omsetTunai)}</span></div>
                        <div class="stat-row"><span class="label">Pembayaran QRIS:</span><span class="val">Rp ${fmt(omsetQris)}</span></div>
                        <div class="stat-row"><span class="label">Transfer Bank Direct:</span><span class="val">Rp ${fmt(omsetTransfer)}</span></div>
                        <div class="stat-row"><span class="label">Kasbon / Piutang Pelanggan:</span><span class="val highlight-red">Rp ${fmt(omsetKasbon)}</span></div>
                        <hr style="border: 0; border-top: 1px dashed #ccc; margin: 8px 0;">
                        <div class="stat-row"><span class="label">Porsi Makan di Tempat (Dine-In):</span><span class="val">$dineInCount Transaksi</span></div>
                        <div class="stat-row"><span class="label">Porsi Bungkus (Takeaway):</span><span class="val">$takeawayCount Transaksi</span></div>
                    </div>
                </div>

                <div class="section-title">🔥 Top Menu Terlaris (Product Performance)</div>
                <table>
                    <thead>
                        <tr>
                            <th style="width:40px; text-align:center;">#</th>
                            <th>Nama Menu / Produk</th>
                            <th style="text-align:center;">Jumlah Terjual</th>
                            <th style="text-align:right;">Total Omset Ditorehkan</th>
                        </tr>
                    </thead>
                    <tbody>
                        $topProductsRows
                    </tbody>
                </table>

                <div class="section-title">💸 Catatan Pengeluaran Kas Operasional</div>
                <table>
                    <thead>
                        <tr>
                            <th style="width:40px; text-align:center;">#</th>
                            <th style="width:100px;">Tanggal</th>
                            <th>Keterangan / Pengeluaran</th>
                            <th style="text-align:right;">Nominal (Rp)</th>
                        </tr>
                    </thead>
                    <tbody>
                        $expenseRows
                    </tbody>
                </table>

                <div class="section-title">📋 Riwayat Sampel Transaksi Penjualan (Maksimal 50 Terakhir)</div>
                <table>
                    <thead>
                        <tr>
                            <th>No. Trx</th>
                            <th>Tanggal & Jam</th>
                            <th>Kasir</th>
                            <th>Tipe</th>
                            <th>Metode</th>
                            <th style="text-align:right;">Omset</th>
                            <th style="text-align:right;">Est. Laba</th>
                        </tr>
                    </thead>
                    <tbody>
                        $transactionRows
                    </tbody>
                </table>

                <div class="footer">
                    * Laporan ini dibuat secara otomatis oleh sistem Kasir & Point of Sales Toko pada $nowStr.
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun generateAndPrintShiftReport(
        context: Context,
        storeName: String,
        kasirName: String,
        modalLaci: Double,
        transactions: List<TransactionEntity>,
        expenses: List<CashExpenseEntity>
    ) {
        val nowStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val numberFormat = NumberFormat.getInstance(Locale("id", "ID"))
        fun fmt(num: Double): String = numberFormat.format(Math.round(num))

        val omsetTunai = transactions.filter { it.metode == "tunai" }.sumOf { it.totalPemasukan }
        val omsetQris = transactions.filter { it.metode == "qris" }.sumOf { it.totalPemasukan }
        val omsetTransfer = transactions.filter { it.metode == "transfer" }.sumOf { it.totalPemasukan }
        val omsetKasbon = transactions.filter { it.metode == "kasbon" }.sumOf { it.totalPemasukan }
        val totalOmset = transactions.sumOf { it.totalPemasukan }
        val totalExpenses = expenses.sumOf { it.nominal }
        val expectedPhysicalCash = modalLaci + omsetTunai - totalExpenses

        val expenseRows = StringBuilder()
        if (expenses.isEmpty()) {
            expenseRows.append("<tr><td colspan='3' style='text-align:center; color:#888;'>Tidak ada pengeluaran kas.</td></tr>")
        } else {
            expenses.forEachIndexed { idx, e ->
                expenseRows.append("<tr><td>${idx+1}</td><td>${e.keterangan}</td><td style='text-align:right; color:#c0392b;'>- Rp ${fmt(e.nominal)}</td></tr>")
            }
        }

        val trxRows = StringBuilder()
        if (transactions.isEmpty()) {
            trxRows.append("<tr><td colspan='5' style='text-align:center; color:#888;'>Belum ada transaksi shift ini.</td></tr>")
        } else {
            transactions.forEach { t ->
                trxRows.append("<tr><td>${t.id}</td><td>${t.tanggalStr}</td><td style='text-transform:uppercase;'>${t.metode}</td><td>${t.tipePesanan}</td><td style='text-align:right;'>Rp ${fmt(t.totalPemasukan)}</td></tr>")
            }
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Laporan Shift Kasir - $kasirName</title>
                <style>
                    body { font-family: sans-serif; margin: 20px; font-size: 12px; color: #333; }
                    .header { border-bottom: 2px solid #2c3e50; padding-bottom: 8px; margin-bottom: 16px; }
                    .header h2 { margin: 0; color: #2c3e50; }
                    .card { background: #f8f9fa; border: 1px solid #ddd; border-radius: 6px; padding: 12px; margin-bottom: 16px; }
                    .row { display: flex; justify-content: space-between; margin-bottom: 6px; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
                    th { background: #2c3e50; color: white; padding: 6px; text-align: left; }
                    td { padding: 6px; border-bottom: 1px solid #eee; }
                    .bold { font-weight: bold; }
                    .green { color: #27ae60; font-weight: bold; }
                    .red { color: #c0392b; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>$storeName</h2>
                    <p style="margin: 4px 0;"><strong>LAPORAN SERAH TERIMA SHIFT KASIR</strong></p>
                    <p style="margin: 2px 0;">Kasir Operator: <strong>$kasirName</strong> | Waktu Cetak: $nowStr</p>
                </div>

                <div class="card">
                    <div class="row"><span>Modal Awal Laci Kasir:</span><span class="bold">Rp ${fmt(modalLaci)}</span></div>
                    <div class="row"><span>Total Pemasukan Tunai (Cash):</span><span class="green">+ Rp ${fmt(omsetTunai)}</span></div>
                    <div class="row"><span>Total Kas Keluar Shift:</span><span class="red">- Rp ${fmt(totalExpenses)}</span></div>
                    <hr style="border: 0; border-top: 1px dashed #ccc; margin: 8px 0;">
                    <div class="row" style="font-size: 14px;"><span>💵 Uang Fisik Wajib Ada di Laci:</span><span class="green">Rp ${fmt(expectedPhysicalCash)}</span></div>
                </div>

                <div class="card">
                    <div class="row"><span>Omset QRIS Digital:</span><span class="bold">Rp ${fmt(omsetQris)}</span></div>
                    <div class="row"><span>Omset Transfer Bank Direct:</span><span class="bold">Rp ${fmt(omsetTransfer)}</span></div>
                    <div class="row"><span>Omset Kasbon Pelanggan:</span><span class="red">Rp ${fmt(omsetKasbon)}</span></div>
                    <div class="row" style="font-size: 13px;"><span>TOTAL OMSET SHIFT:</span><span class="bold">Rp ${fmt(totalOmset)}</span></div>
                </div>

                <h3>💸 Catatan Kas Keluar Shift</h3>
                <table>
                    <thead><tr><th>#</th><th>Keterangan</th><th style="text-align:right;">Nominal</th></tr></thead>
                    <tbody>$expenseRows</tbody>
                </table>

                <h3>📋 Transaksi Shift Kasir (${transactions.size} Trx)</h3>
                <table>
                    <thead><tr><th>No. Trx</th><th>Waktu</th><th>Metode</th><th>Tipe</th><th style="text-align:right;">Nominal</th></tr></thead>
                    <tbody>$trxRows</tbody>
                </table>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "${storeName.replace(" ", "_")}_Shift_${kasirName}_${System.currentTimeMillis()}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun printReceiptHtml(
        context: Context,
        storeName: String,
        storeAddress: String,
        storePhone: String,
        receiptFooter: String,
        transaction: TransactionEntity
    ) {
        val numberFormat = NumberFormat.getInstance(Locale("id", "ID"))
        fun fmt(num: Double): String = numberFormat.format(Math.round(num))

        val items = try {
            val arr = org.json.JSONArray(transaction.itemsJson)
            List(arr.length()) {
                val obj = arr.getJSONObject(it)
                Triple(obj.getString("nama"), obj.getInt("qty"), obj.getDouble("subtotal"))
            }
        } catch (e: Exception) { emptyList() }

        val itemsRows = StringBuilder()
        items.forEach { (nama, qty, subtotal) ->
            itemsRows.append("""
                <tr>
                    <td style="text-align:left; padding: 3px 0;">$nama <span style="font-size:10px; color:#555;">x$qty</span></td>
                    <td style="text-align:right; padding: 3px 0;">Rp ${fmt(subtotal)}</td>
                </tr>
            """.trimIndent())
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Struk #${transaction.id}</title>
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace, sans-serif;
                        width: 280px;
                        margin: 0 auto;
                        padding: 12px;
                        font-size: 11px;
                        color: #000;
                        background: #fff;
                    }
                    .text-center { text-align: center; }
                    .text-right { text-align: right; }
                    .bold { font-weight: bold; }
                    .title { font-size: 16px; font-weight: bold; margin-bottom: 2px; }
                    .divider { border-top: 1px dashed #000; margin: 8px 0; }
                    table { width: 100%; border-collapse: collapse; }
                    td { vertical-align: top; }
                </style>
            </head>
            <body>
                <div class="text-center">
                    <div class="title">${if (storeName.isBlank()) "Toko POS Kuliner" else storeName}</div>
                    ${if (storeAddress.isNotBlank()) "<div style='font-size:10px;'>$storeAddress</div>" else ""}
                    ${if (storePhone.isNotBlank()) "<div style='font-size:10px;'>Telp: $storePhone</div>" else ""}
                </div>

                <div class="divider"></div>

                <div><strong>No:</strong> #${transaction.id}</div>
                <div><strong>Tgl:</strong> ${transaction.tanggalStr}</div>
                <div><strong>Kasir:</strong> ${transaction.kasir}</div>
                <div><strong>Pelanggan:</strong> ${transaction.pelanggan} (${transaction.nomorMeja})</div>

                <div class="divider"></div>

                <table>
                    <tbody>
                        $itemsRows
                    </tbody>
                </table>

                <div class="divider"></div>

                <table>
                    <tr>
                        <td>Subtotal:</td>
                        <td class="text-right">Rp ${fmt(transaction.subtotal)}</td>
                    </tr>
                    ${if (transaction.diskon > 0) """
                    <tr>
                        <td>Diskon:</td>
                        <td class="text-right">- Rp ${fmt(transaction.diskon)}</td>
                    </tr>
                    """ else ""}
                    ${if (transaction.pajak > 0) """
                    <tr>
                        <td>Pajak:</td>
                        <td class="text-right">+ Rp ${fmt(transaction.pajak)}</td>
                    </tr>
                    """ else ""}
                    <tr class="bold" style="font-size: 12px;">
                        <td>TOTAL:</td>
                        <td class="text-right">Rp ${fmt(transaction.totalPemasukan)}</td>
                    </tr>
                    <tr>
                        <td>Metode:</td>
                        <td class="text-right">${transaction.metode.uppercase()}</td>
                    </tr>
                    <tr>
                        <td>Bayar:</td>
                        <td class="text-right">Rp ${fmt(transaction.uangBayar)}</td>
                    </tr>
                    <tr>
                        <td>Kembali:</td>
                        <td class="text-right">Rp ${fmt(transaction.uangKembali)}</td>
                    </tr>
                </table>

                <div class="divider"></div>

                <div class="text-center" style="margin-top: 6px;">
                    ${if (receiptFooter.isNotBlank()) "<div>$receiptFooter</div>" else "<div>Terima kasih atas kunjungan Anda!</div>"}
                </div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "Struk_${transaction.id}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A6).build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun printHppReport(
        context: Context,
        storeName: String,
        currentUser: String,
        namaProduk: String,
        jumlahUnit: Int,
        totalBahan: Double,
        totalBiayaLain: Double,
        tenagaKerja: Double,
        overhead: Double,
        hppUnit: Double,
        targetFcPct: Double,
        saranTargetFc: Double,
        m35: Double,
        m50: Double,
        customPrice: Double,
        bahanListJson: String
    ) {
        val nowStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val numberFormat = NumberFormat.getInstance(Locale("id", "ID"))
        fun fmt(num: Double): String = numberFormat.format(Math.round(num))

        val items = try {
            val arr = org.json.JSONArray(bahanListJson)
            List(arr.length()) {
                val obj = arr.getJSONObject(it)
                val nama = obj.optString("nama", "Bahan")
                val pakai = obj.optDouble("pakai", 0.0)
                val subtotal = obj.optDouble("subtotal", 0.0)
                val waste = obj.optDouble("waste", 0.0)
                Triple(nama, pakai, subtotal)
            }
        } catch (e: Exception) { emptyList() }

        val bahanRows = StringBuilder()
        if (items.isEmpty()) {
            bahanRows.append("<tr><td colspan='3' style='text-align:center; color:#888;'>Tidak ada rincian bahan.</td></tr>")
        } else {
            items.forEachIndexed { idx, (nama, pakai, sub) ->
                bahanRows.append("""
                    <tr>
                        <td style='text-align:center;'>${idx + 1}</td>
                        <td>$nama</td>
                        <td style='text-align:center;'>${pakai}g/ml/pcs</td>
                        <td style='text-align:right;'>Rp ${fmt(sub)}</td>
                    </tr>
                """.trimIndent())
            }
        }

        val totalModalBatch = totalBahan + totalBiayaLain
        val effectiveCustomPrice = if (customPrice > 0) customPrice else saranTargetFc
        val projectedRevenue = effectiveCustomPrice * jumlahUnit
        val projectedProfitBatch = projectedRevenue - totalModalBatch
        val projectedMarginPct = if (projectedRevenue > 0) (projectedProfitBatch / projectedRevenue) * 100.0 else 0.0
        val breakEvenUnits = if (effectiveCustomPrice > 0) Math.ceil(totalModalBatch / effectiveCustomPrice) else 0.0

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Laporan Costing HPP - $namaProduk</title>
                <style>
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        color: #333;
                        margin: 20px;
                        padding: 0;
                        font-size: 12px;
                    }
                    .header {
                        border-bottom: 3px solid #2c3e50;
                        padding-bottom: 10px;
                        margin-bottom: 20px;
                    }
                    .header h1 { margin: 0; font-size: 20px; color: #2c3e50; text-transform: uppercase; }
                    .header p { margin: 3px 0; color: #555; font-size: 11px; }
                    .grid-2 { display: flex; gap: 15px; margin-bottom: 20px; }
                    .card {
                        flex: 1;
                        background: #f8f9fa;
                        border: 1px solid #e9ecef;
                        border-radius: 8px;
                        padding: 12px;
                    }
                    .card h3 {
                        margin-top: 0;
                        margin-bottom: 10px;
                        font-size: 13px;
                        color: #2c3e50;
                        border-bottom: 1px solid #ddd;
                        padding-bottom: 5px;
                    }
                    .stat-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
                    .stat-row .label { color: #555; }
                    .stat-row .val { font-weight: bold; }
                    .green { color: #27ae60; }
                    .red { color: #c0392b; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                    th { background: #2c3e50; color: #fff; text-align: left; padding: 8px; font-size: 11px; text-transform: uppercase; }
                    td { padding: 7px 8px; border-bottom: 1px solid #eee; font-size: 11px; }
                    tr:nth-child(even) { background-color: #fcfcfc; }
                    .section-title { font-size: 14px; font-weight: bold; color: #2c3e50; margin-top: 15px; margin-bottom: 8px; }
                    .footer { margin-top: 30px; text-align: center; font-size: 10px; color: #888; border-top: 1px solid #ddd; padding-top: 10px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$storeName</h1>
                    <p><strong>ANALISIS HPP (HARGA POKOK PRODUKSI) & SIMULASI PRODUKSI</strong></p>
                    <p>Nama Produk: <strong>$namaProduk</strong> | Jumlah Batch: <strong>$jumlahUnit Porsi/Unit</strong> | Dicetak: $nowStr | Operator: $currentUser</p>
                </div>

                <div class="grid-2">
                    <div class="card">
                        <h3>📊 Ringkasan Komponen HPP</h3>
                        <div class="stat-row"><span class="label">Total Bahan Baku (BOM):</span><span class="val">Rp ${fmt(totalBahan)}</span></div>
                        <div class="stat-row"><span class="label">Tenaga Kerja / SDM:</span><span class="val">Rp ${fmt(tenagaKerja)}</span></div>
                        <div class="stat-row"><span class="label">Biaya Overhead / Utilitas:</span><span class="val">Rp ${fmt(overhead)}</span></div>
                        <div class="stat-row"><span class="label">Total Modal Produksi Batch:</span><span class="val red">Rp ${fmt(totalModalBatch)}</span></div>
                        <hr style="border:0; border-top:1px dashed #ccc; margin:8px 0;">
                        <div class="stat-row" style="font-size:13px;"><span class="label">HPP Bersih per Unit/Porsi:</span><span class="val red">Rp ${fmt(hppUnit)}</span></div>
                    </div>

                    <div class="card">
                        <h3>💡 Rekomendasi Harga Jual</h3>
                        <div class="stat-row"><span class="label">Saran Target Food Cost (${targetFcPct.toInt()}%):</span><span class="val green">Rp ${fmt(saranTargetFc)}</span></div>
                        <div class="stat-row"><span class="label">Margin Keuntungan 35%:</span><span class="val">Rp ${fmt(m35)}</span></div>
                        <div class="stat-row"><span class="label">Margin Keuntungan 50%:</span><span class="val">Rp ${fmt(m50)}</span></div>
                        <hr style="border:0; border-top:1px dashed #ccc; margin:8px 0;">
                        <div class="stat-row"><span class="label">Harga Jual Simulasi Dipilih:</span><span class="val green" style="font-size:13px;">Rp ${fmt(effectiveCustomPrice)}</span></div>
                    </div>
                </div>

                <div class="card" style="background: #eef9f2; border-color: #a3e4d7; margin-bottom: 20px;">
                    <h3 style="color: #1e8449;">🎯 Simulasi Balik Modal & Proyeksi Produksi (Batch $jumlahUnit Unit)</h3>
                    <div class="grid-2" style="margin-bottom:0;">
                        <div>
                            <div class="stat-row"><span class="label">Total Proyeksi Omset Penjualan:</span><span class="val">Rp ${fmt(projectedRevenue)}</span></div>
                            <div class="stat-row"><span class="label">Total Modal Produksi Dikeluarkan:</span><span class="val red">- Rp ${fmt(totalModalBatch)}</span></div>
                            <div class="stat-row"><span class="label">Proyeksi Laba Bersih Batch:</span><span class="val green" style="font-size:14px;">+ Rp ${fmt(projectedProfitBatch)}</span></div>
                        </div>
                        <div>
                            <div class="stat-row"><span class="label">Proyeksi Margin Profit (%):</span><span class="val">${String.format("%.1f", projectedMarginPct)}%</span></div>
                            <div class="stat-row"><span class="label">Titik Impas (BEP Unit Terjual):</span><span class="val">${breakEvenUnits.toInt()} Porsi dari $jumlahUnit</span></div>
                            <div class="stat-row"><span class="label">Status Kelayakan Usaha:</span><span class="val green">Sangat Layak & Profitable</span></div>
                        </div>
                    </div>
                </div>

                <div class="section-title">🥘 Rincian Bahan Baku & Takaran Resep (Bill of Materials)</div>
                <table>
                    <thead>
                        <tr>
                            <th style="width:40px; text-align:center;">#</th>
                            <th>Nama Bahan Baku</th>
                            <th style="text-align:center;">Takaran / Pakai</th>
                            <th style="text-align:right;">Subtotal Biaya</th>
                        </tr>
                    </thead>
                    <tbody>
                        $bahanRows
                    </tbody>
                </table>

                <div class="footer">
                    * Laporan Analisis Costing HPP ini dihasilkan secara profesional oleh Sistem POS & Kalkulator Kuliner pada $nowStr.
                </div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "HPP_${namaProduk.replace(" ", "_")}_${System.currentTimeMillis()}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun generateAndPrintKasbonReport(
        context: Context,
        storeName: String,
        currentUser: String,
        kasbonList: List<KasbonEntity>
    ) {
        val nowStr = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID")).format(Date())
        val numberFormat = NumberFormat.getInstance(Locale("id", "ID"))
        fun fmt(num: Double): String = numberFormat.format(Math.round(num))

        val totalBelumLunas = kasbonList.filter { it.status == "Belum Lunas" }.sumOf { it.total }
        val totalLunas = kasbonList.filter { it.status == "Lunas" }.sumOf { it.total }
        val countBelumLunas = kasbonList.count { it.status == "Belum Lunas" }

        val kasbonRows = StringBuilder()
        if (kasbonList.isEmpty()) {
            kasbonRows.append("<tr><td colspan='7' style='text-align:center; color:#888;'>Tidak ada data kasbon/piutang.</td></tr>")
        } else {
            kasbonList.forEachIndexed { idx, k ->
                val sdfISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateMs = try { sdfISO.parse(k.tanggalISO)?.time } catch (e: Exception) { null }
                val daysMs = if (dateMs != null) (System.currentTimeMillis() - dateMs) / (1000 * 60 * 60 * 24) else 0
                val agingText = if (k.status == "Lunas") "-" else if (daysMs <= 3) "$daysMs Hari (Lancar)" else if (daysMs <= 14) "⚠️ $daysMs Hari (Terlambat)" else "🚨 $daysMs Hari (Prioritas Tagih)"

                val statusColor = if (k.status == "Lunas") "#27ae60" else "#c0392b"
                kasbonRows.append("""
                    <tr>
                        <td style='text-align:center;'>${idx + 1}</td>
                        <td><strong>${k.pelanggan}</strong></td>
                        <td>${k.tanggalStr}</td>
                        <td>${k.nomorMeja} (${k.tipePesanan})</td>
                        <td style='color:$statusColor; font-weight:bold;'>${k.status.uppercase()}</td>
                        <td style='font-size:11px;'>$agingText</td>
                        <td style='text-align:right; font-weight:bold;'>Rp ${fmt(k.total)}</td>
                    </tr>
                """.trimIndent())
            }
        }

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Laporan Kasbon & Piutang - $storeName</title>
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; margin: 20px; font-size: 12px; color: #333; }
                    .header { border-bottom: 3px solid #2c3e50; padding-bottom: 10px; margin-bottom: 20px; }
                    .header h1 { margin: 0; font-size: 20px; color: #2c3e50; text-transform: uppercase; }
                    .header p { margin: 3px 0; color: #555; font-size: 11px; }
                    .card { background: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; padding: 12px; margin-bottom: 20px; }
                    .stat-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
                    .stat-row .label { color: #555; }
                    .stat-row .val { font-weight: bold; }
                    .red { color: #c0392b; }
                    .green { color: #27ae60; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                    th { background: #2c3e50; color: #fff; text-align: left; padding: 8px; font-size: 11px; text-transform: uppercase; }
                    td { padding: 7px 8px; border-bottom: 1px solid #eee; font-size: 11px; }
                    tr:nth-child(even) { background-color: #fcfcfc; }
                    .footer { margin-top: 30px; text-align: center; font-size: 10px; color: #888; border-top: 1px solid #ddd; padding-top: 10px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>$storeName</h1>
                    <p><strong>LAPORAN PIUTANG & KASBON PELANGGAN</strong></p>
                    <p>Waktu Dicetak: $nowStr | Operator/Role: $currentUser</p>
                </div>

                <div class="card">
                    <h3>📑 Ringkasan Piutang Usaha</h3>
                    <div class="stat-row"><span class="label">Total Piutang Belum Lunas:</span><span class="val red" style="font-size:14px;">Rp ${fmt(totalBelumLunas)} ($countBelumLunas Pelanggan)</span></div>
                    <div class="stat-row"><span class="label">Total Piutang Terbayar (Lunas):</span><span class="val green">Rp ${fmt(totalLunas)}</span></div>
                </div>

                <h3>📋 Daftar Rincian Kasbon & Aging Umur Tunggakan</h3>
                <table>
                    <thead>
                        <tr>
                            <th style='width:30px; text-align:center;'>#</th>
                            <th>Nama Pelanggan</th>
                            <th>Tanggal Trx</th>
                            <th>Detail / Meja</th>
                            <th>Status</th>
                            <th>Umur Tunggakan</th>
                            <th style='text-align:right;'>Total Kasbon</th>
                        </tr>
                    </thead>
                    <tbody>
                        $kasbonRows
                    </tbody>
                </table>

                <div class="footer">
                    * Dokumen Laporan Piutang Kasbon dihasilkan secara otomatis oleh sistem $storeName pada $nowStr.
                </div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "Kasbon_Piutang_${System.currentTimeMillis()}"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun generateAndPrintReleaseNotesPdf(context: Context) {
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
        val nowStr = sdf.format(Date())

        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: sans-serif; margin: 25px; color: #2d3748; line-height: 1.5; }
                    .header { text-align: center; border-bottom: 2px solid #4f46e5; padding-bottom: 15px; margin-bottom: 20px; }
                    .header h1 { margin: 0; font-size: 24px; color: #4f46e5; text-transform: uppercase; letter-spacing: 1px; }
                    .header p { margin: 4px 0; color: #718096; font-size: 12px; }
                    .badge { display: inline-block; background: #e0e7ff; color: #3730a3; padding: 4px 12px; border-radius: 20px; font-weight: bold; font-size: 13px; margin-top: 8px; }
                    .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 15px; margin-bottom: 16px; }
                    .section-title { font-size: 15px; font-weight: bold; color: #312e81; border-bottom: 1px solid #cbd5e1; padding-bottom: 6px; margin-bottom: 10px; }
                    ul { margin: 0; padding-left: 20px; font-size: 12px; color: #334155; }
                    li { margin-bottom: 6px; }
                    .highlight { font-weight: bold; color: #1e1b4b; }
                    .footer { margin-top: 30px; text-align: center; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 12px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>KASI GR-ATIS POS</h1>
                    <p><strong>CATATAN RILIS APLIKASI (RELEASE NOTES)</strong></p>
                    <div class="badge">Versi v6.7.0 — Official Public Release</div>
                    <p style="margin-top:8px;">Tanggal Rilis: 12 Agustus 2026 | Developer: Eka Ilaika</p>
                </div>

                <div class="card">
                    <div class="section-title">🚀 Ringkasan Rilis v6.7.0</div>
                    <p style="font-size:12px; margin:0;">Selamat datang di rilis perdana umum <strong>KasiGR-atis POS</strong>! Aplikasi kasir pintar dan manajemen keuangan toko 100% GRATIS yang dirancang khusus untuk memajukan UMKM Indonesia.</p>
                </div>

                <div class="card">
                    <div class="section-title">✨ Fitur Utama & Keunggulan Rilis Publik</div>
                    <ul>
                        <li><span class="highlight">🛒 Modul Kasir POS Modern</span>: Transaksi cepat, pencetakan struk printer thermal Bluetooth, dukungan berbagai metode bayar (Tunai, QRIS, Transfer, Kasbon), serta kalkulator kembalian otomatis.</li>
                        <li><span class="highlight">📈 Laporan Keuangan & HPP Akurat</span>: Analisis Omset Kotor, Estimasi Modal HPP, Laba Bersih, Rata-rata Basket (AOV), Filter Metode Pembayaran, serta Rincian Transaksi dengan sistem Pagination (15 data per halaman).</li>
                        <li><span class="highlight">📝 Pencatatan Kasbon & Aging Piutang</span>: Kelola hutang pelanggan dengan penanda umur tunggakan, riwayat pelunasan, dan rekapitulasi piutang lunas / belum lunas.</li>
                        <li><span class="highlight">🍔 Manajemen Produk & Resep Modal HPP</span>: Hitung modal bahan baku per resep menu secara presisi, peringatan margin tipis (Low Margin Alert), dan arsip resep.</li>
                        <li><span class="highlight">📄 Ekspor Laporan Dokumen PDF</span>: Fitur cetak PDF resmi untuk Laporan Shift Kasir, Evaluasi Finansial Pemilik, Daftar Piutang Kasbon, dan Catatan Rilis.</li>
                        <li><span class="highlight">🔒 Keamanan Multirole (Pemilik & Kasir)</span>: Mode tampilan terpisah antara Kasir (fokus laci kas) dan Pemilik Toko (akses analisis laba bersih berproteksi PIN).</li>
                    </ul>
                </div>

                <div class="card">
                    <div class="section-title">ℹ️ Informasi Penggunaan & Pengikatan Data</div>
                    <ul>
                        <li>Aplikasi berjalan 100% offline-first di perangkat pengguna, menjamin kecepatan transaksi tanpa ketergantungan sinyal internet.</li>
                        <li>Gunakan fitur <strong>Cetak PDF</strong> untuk mengarsipkan laporan keuangan harian / bulanan ke penyimpanan HP atau Google Drive.</li>
                    </ul>
                </div>

                <div class="footer">
                    Dokumen resmi Catatan Rilis KasiGR-atis POS v6.7.0 • Dibuat oleh Eka Ilaika (#UMKMBangkit Indonesia 🇮🇩) • $nowStr
                </div>
            </body>
            </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val jobName = "Release_Notes_KasiGratis_v6.7.0"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager?.print(
                    jobName,
                    printAdapter,
                    PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}


