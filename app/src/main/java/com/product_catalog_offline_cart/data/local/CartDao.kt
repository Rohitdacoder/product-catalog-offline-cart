package com.product_catalog_offline_cart.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Quantity changes are single SQL statements, so concurrent taps can't lose an update
 * (no read-modify-write in Kotlin).
 */
@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY added_at ASC")
    fun observeAll(): Flow<List<CartEntity>>

    /** Returns the new row ID, or -1 if the product is already in the cart. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(item: CartEntity): Long

    /** Returns the number of rows updated (0 if the product isn't in the cart). */
    @Query("UPDATE cart_items SET quantity = quantity + 1 WHERE product_id = :productId")
    suspend fun incrementQuantity(productId: Int): Int

    /** Decrements only while quantity > 1, so it can never reach 0 or below. Returns rows updated. */
    @Query("UPDATE cart_items SET quantity = quantity - 1 WHERE product_id = :productId AND quantity > 1")
    suspend fun decrementQuantityAboveOne(productId: Int): Int

    @Query("DELETE FROM cart_items WHERE product_id = :productId")
    suspend fun delete(productId: Int): Int

    @Query("DELETE FROM cart_items")
    suspend fun clear()
}
