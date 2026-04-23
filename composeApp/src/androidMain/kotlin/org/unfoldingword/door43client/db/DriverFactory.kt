package org.unfoldingword.door43client.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.bibletranslationtools.writer.DirectoryProvider
import org.koin.mp.KoinPlatform.getKoin

actual class DriverFactory(private val directoryProvider: DirectoryProvider) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            Door43Database.Schema,
            getKoin().get(),
            directoryProvider.databaseFile.name
        )
    }
}
