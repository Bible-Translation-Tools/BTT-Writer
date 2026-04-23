package org.unfoldingword.door43client.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Database configuration and prepopulation handling.
 */
object DatabaseConfig {
    const val DATABASE_NAME = "door43.db"
    const val DATABASE_VERSION = 1
    
    /**
     * Prepopulates the database on first launch.
     * This is typically done by copying the prepopulated index.sqlite file.
     */
    suspend fun prepopulate(
        driver: app.cash.sqldelight.db.SqlDriver,
        prepopulatedFile: InputStream?
    ) = withContext(Dispatchers.IO) {
        if (prepopulatedFile != null) {
            // Copy prepopulated database if available
            // This would require reading the index.sqlite from resources
            // and copying it to the database location
        }
    }
    
    /**
     * Creates the database using SQLDelight schema.
     */
    fun createDatabase(
        driver: app.cash.sqldelight.db.SqlDriver
    ): Door43Database {
        return Door43Database(driver)
    }
}

/**
 * Database manager for handling database lifecycle.
 */
class Door43DbManager(
    private val driver: app.cash.sqldelight.db.SqlDriver
) {
    private var database: Door43Database? = null
    
    /**
     * Initializes the database.
     */
    fun initialize(): Door43Database {
        if (database == null) {
            database = Door43Database(driver)
            // Apply any necessary migrations
            if (driver.getProperty("schema.version").toIntOrNull() == 0) {
                applyPrepopulation()
            }
        }
        return database!!
    }
    
    /**
     * Applies prepopulation from the bundled index.sqlite.
     */
    private fun applyPrepopulation() {
        // The index.sqlite in composeResources would be used
        // as the initial database state
    }
    
    /**
     * Closes the database.
     */
    fun close() {
        database?.close()
        driver.close()
    }
}