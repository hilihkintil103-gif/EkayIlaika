package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.PosDao
import com.example.data.model.CashExpenseEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.HoldOrderEntity
import com.example.data.model.HppHistoryEntity
import com.example.data.model.KasbonEntity
import com.example.data.model.ProductEntity
import com.example.data.model.RawMaterialEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        RawMaterialEntity::class,
        TransactionEntity::class,
        HoldOrderEntity::class,
        KasbonEntity::class,
        CashExpenseEntity::class,
        HppHistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kasigratis_pos.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate default data in background
                            scope.launch(Dispatchers.IO) {
                                INSTANCE?.let { database ->
                                    populateDatabase(database.posDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDatabase(dao: PosDao) {
            try {
                // Default Users
                dao.insertUser(UserEntity(nama = "Pemilik Toko", username = "owner", password = "123", role = "pemilik"))
                dao.insertUser(UserEntity(nama = "Kasir Utama", username = "kasir", password = "123", role = "kasir"))

                // Default Categories
                dao.insertCategory(CategoryEntity(name = "Makanan"))
                dao.insertCategory(CategoryEntity(name = "Minuman"))
                dao.insertCategory(CategoryEntity(name = "Snack"))

                // Default Raw Materials
                dao.insertRawMaterial(RawMaterialEntity(nama = "Biji Kopi Arabika", harga = 120000.0, isi = 1000.0, stok = 5000.0, satuan = "gram"))
                dao.insertRawMaterial(RawMaterialEntity(nama = "Susu Fresh Milk", harga = 18000.0, isi = 1000.0, stok = 10000.0, satuan = "ml"))
                dao.insertRawMaterial(RawMaterialEntity(nama = "Gula Pasir", harga = 15000.0, isi = 1000.0, stok = 3000.0, satuan = "gram"))

                // Default Products
                val kopiVarian = "[\"Ice\", \"Hot\"]"
                val kopiTopping = "[{\"nama\":\"Extra Shot\",\"harga\":5000.0},{\"nama\":\"Boba\",\"harga\":3000.0}]"
                val kopiResep = "[{\"idBahan\":1,\"nama\":\"Biji Kopi Arabika\",\"pakai\":18.0},{\"idBahan\":2,\"nama\":\"Susu Fresh Milk\",\"pakai\":150.0}]"

                val rotiVarian = "[\"Cokelat\", \"Keju\"]"
                val rotiTopping = "[{\"nama\":\"Extra Keju\",\"harga\":3000.0}]"

                dao.insertProduct(
                    ProductEntity(
                        emoji = "☕",
                        nama = "Kopi Susu",
                        kategori = "Minuman",
                        modal = 5000.0,
                        jual = 12000.0,
                        grosirMin = 5,
                        grosirHarga = 10000.0,
                        varianJson = kopiVarian,
                        toppingJson = kopiTopping,
                        stok = 50,
                        resepJson = kopiResep,
                        aktif = true
                    )
                )

                dao.insertProduct(
                    ProductEntity(
                        emoji = "🍞",
                        nama = "Roti Bakar",
                        kategori = "Makanan",
                        modal = 4000.0,
                        jual = 10000.0,
                        grosirMin = 0,
                        grosirHarga = 0.0,
                        varianJson = rotiVarian,
                        toppingJson = rotiTopping,
                        stok = 30,
                        resepJson = "[]",
                        aktif = true
                    )
                )

                dao.insertProduct(
                    ProductEntity(
                        emoji = "🍟",
                        nama = "Keripik Singkong",
                        kategori = "Snack",
                        modal = 3000.0,
                        jual = 7000.0,
                        grosirMin = 10,
                        grosirHarga = 6000.0,
                        varianJson = "[]",
                        toppingJson = "[]",
                        stok = 15,
                        resepJson = "[]",
                        aktif = true
                    )
                )
            } catch (e: Exception) {
                // Ignore if already populated
            }
        }
    }
}
