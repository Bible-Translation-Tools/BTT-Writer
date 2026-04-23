package org.unfoldingword.door43client.db

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DriverFactory): Door43Database {
    val driver = driverFactory.createDriver()
    val database = Door43Database(driver)

    return database
}