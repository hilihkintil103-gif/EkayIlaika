package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CashExpenseEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.HoldOrderEntity
import com.example.data.model.HppHistoryEntity
import com.example.data.model.KasbonEntity
import com.example.data.model.ProductEntity
import com.example.data.model.RawMaterialEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {

    // Users
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: Long)


    // Categories
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)


    // Products
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)


    // Raw Materials
    @Query("SELECT * FROM raw_materials ORDER BY id DESC")
    fun getAllRawMaterials(): Flow<List<RawMaterialEntity>>

    @Query("SELECT * FROM raw_materials WHERE id = :id LIMIT 1")
    suspend fun getRawMaterialById(id: Long): RawMaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawMaterial(rawMaterial: RawMaterialEntity): Long

    @Update
    suspend fun updateRawMaterial(rawMaterial: RawMaterialEntity)

    @Query("DELETE FROM raw_materials WHERE id = :id")
    suspend fun deleteRawMaterial(id: Long)


    // Transactions
    @Query("SELECT * FROM transactions ORDER BY waktu DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE tanggalISO = :isoDate ORDER BY waktu DESC")
    fun getTransactionsByDate(isoDate: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()


    // Hold Orders
    @Query("SELECT * FROM hold_orders ORDER BY id DESC")
    fun getAllHoldOrders(): Flow<List<HoldOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldOrder(holdOrder: HoldOrderEntity): Long

    @Query("DELETE FROM hold_orders WHERE id = :id")
    suspend fun deleteHoldOrder(id: Long)


    // Kasbon
    @Query("SELECT * FROM kasbon ORDER BY id DESC")
    fun getAllKasbon(): Flow<List<KasbonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKasbon(kasbon: KasbonEntity): Long

    @Update
    suspend fun updateKasbon(kasbon: KasbonEntity)


    // Cash Expenses
    @Query("SELECT * FROM cash_expenses ORDER BY id DESC")
    fun getAllCashExpenses(): Flow<List<CashExpenseEntity>>

    @Query("SELECT * FROM cash_expenses WHERE tanggalISO = :isoDate ORDER BY id DESC")
    fun getCashExpensesByDate(isoDate: String): Flow<List<CashExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashExpense(cashExpense: CashExpenseEntity): Long

    @Query("DELETE FROM cash_expenses WHERE id = :id")
    suspend fun deleteCashExpense(id: Long)


    // HPP History
    @Query("SELECT * FROM hpp_history ORDER BY id DESC")
    fun getAllHppHistory(): Flow<List<HppHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHppHistory(hpp: HppHistoryEntity): Long

    @Query("DELETE FROM hpp_history WHERE id = :id")
    suspend fun deleteHppHistory(id: Long)

    // Direct Queries for Backup & Restore
    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsersDirect(): List<UserEntity>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllCategoriesDirect(): List<CategoryEntity>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllProductsDirect(): List<ProductEntity>

    @Query("SELECT * FROM raw_materials ORDER BY id ASC")
    suspend fun getAllRawMaterialsDirect(): List<RawMaterialEntity>

    @Query("SELECT * FROM transactions ORDER BY waktu ASC")
    suspend fun getAllTransactionsDirect(): List<TransactionEntity>

    @Query("SELECT * FROM hold_orders ORDER BY id ASC")
    suspend fun getAllHoldOrdersDirect(): List<HoldOrderEntity>

    @Query("SELECT * FROM kasbon ORDER BY id ASC")
    suspend fun getAllKasbonDirect(): List<KasbonEntity>

    @Query("SELECT * FROM cash_expenses ORDER BY id ASC")
    suspend fun getAllCashExpensesDirect(): List<CashExpenseEntity>

    @Query("SELECT * FROM hpp_history ORDER BY id ASC")
    suspend fun getAllHppHistoryDirect(): List<HppHistoryEntity>

    // Batch Inserts for Restore
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawMaterials(materials: List<RawMaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKasbons(kasbons: List<KasbonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashExpenses(expenses: List<CashExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHppHistories(hpps: List<HppHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    // Clear queries for Clean Restore / Replace All
    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Query("DELETE FROM raw_materials")
    suspend fun deleteAllRawMaterials()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("DELETE FROM kasbon")
    suspend fun deleteAllKasbon()

    @Query("DELETE FROM cash_expenses")
    suspend fun deleteAllCashExpenses()

    @Query("DELETE FROM hpp_history")
    suspend fun deleteAllHppHistory()

    @Query("DELETE FROM hold_orders")
    suspend fun deleteAllHoldOrders()
}
