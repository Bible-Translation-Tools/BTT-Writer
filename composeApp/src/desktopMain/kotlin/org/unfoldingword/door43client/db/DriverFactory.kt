package org.unfoldingword.door43client.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.bibletranslationtools.writer.DirectoryProvider
import java.util.Properties

actual class DriverFactory(private val directoryProvider: DirectoryProvider) {
    actual fun createDriver(): SqlDriver {
        directoryProvider.databaseFile.parentFile?.mkdirs()

        val driver: SqlDriver = JdbcSqliteDriver(
            "jdbc:sqlite:${directoryProvider.databaseFile.absolutePath}",
            Properties(),
            Door43Database.Schema
        )
        return driver
    }
}