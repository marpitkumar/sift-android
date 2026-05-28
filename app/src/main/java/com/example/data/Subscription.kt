package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serviceName: String,
    val category: String, // e.g. "Entertainment", "Utilities", "SaaS", "Fitness", "Other" or custom
    val billingAmount: Double,
    val currency: String, // 3-letter ISO code e.g. "USD", "INR"
    val billingCycle: String, // "Monthly", "Quarterly", "Annual", "One-Time"
    val nextRenewalDate: String, // YYYY-MM-DD
    val paymentMethod: String, // e.g. "UPI", "Credit Card", "Cash"
    val usageFrequency: String, // "High", "Medium", "Low", "Unused"
    val isAutoRenew: Boolean,
    val status: String // "Active", "Paused", "Cancelled"
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextRenewalDate ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Int)

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionById(id: Int): Subscription?

    // Custom categories operations
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Int)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesOnce(): List<Category>
}

@Database(entities = [Subscription::class, Category::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
}

