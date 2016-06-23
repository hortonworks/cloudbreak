package com.sequenceiq.cloudbreak.conf

import org.apache.commons.io.Charsets
import org.apache.ibatis.migration.DataSourceConnectionProvider
import org.apache.ibatis.migration.FileMigrationLoader
import org.apache.ibatis.migration.operations.PendingOperation
import org.apache.ibatis.migration.operations.UpOperation
import org.apache.ibatis.migration.options.DatabaseOperationOption
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn

import javax.inject.Inject
import javax.sql.DataSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.Properties

@Configuration
class DatabaseMigrationConfig {

    @Value("${cb.schema.scripts.location:" + DEFAULT_SCHEMA_LOCATION_IN_SOURCE + "} ")
    private val schemaLocation: String? = null

    @Value("${cb.schema.migration.auto:true}")
    private val schemaMigrationEnabled: Boolean = false

    @Inject
    private val dataSource: DataSource? = null

    @Bean
    @DependsOn("dataSource")
    fun databaseUpMigration(): UpOperation {
        var upOperation = UpOperation()
        val pendingOperation = PendingOperation()
        if (schemaMigrationEnabled) {
            val dataSourceConnectionProvider = DataSourceConnectionProvider(dataSource)
            val operationOption = DatabaseOperationOption()
            operationOption.delimiter = ";"
            operationOption.isFullLineDelimiter = false
            operationOption.isSendFullScript = true
            operationOption.isAutoCommit = false
            val upOutStream = ByteArrayOutputStream()
            val pendingOutStream = ByteArrayOutputStream()
            val upMigrationLoader = upMigrationLoader()
            upOperation = upOperation.operate(dataSourceConnectionProvider, upMigrationLoader, operationOption, PrintStream(upOutStream))
            val pendingMigrationLoader = pendingMigrationLoader()
            pendingOperation.operate(dataSourceConnectionProvider, pendingMigrationLoader, operationOption, PrintStream(pendingOutStream))
            val upMigrationResult = upOutStream.toString().trim { it <= ' ' }
            val pendingMigrationResult = pendingOutStream.toString().trim { it <= ' ' }
            if (upMigrationResult.isEmpty() && pendingMigrationResult == PENDING_OPERATION_WARNING_MSG) {
                LOGGER.info("Schema is up to date. No migration necessary.")
            } else {
                logMigrationResult(upMigrationResult, "up")
                logMigrationResult(pendingMigrationResult, "pending")
            }
        }
        return upOperation
    }

    @Bean
    fun upMigrationLoader(): FileMigrationLoader {
        var schemaLoc: String = schemaLocation
        if (SCHEMA_IN_CONTAINER == schemaLocation) {
            schemaLoc = DEFAULT_SCHEMA_LOCATION
        }
        schemaLoc = schemaLoc + UP_OPERATION_SUBFOLDER
        LOGGER.info("Creating up operation migration loader for location: '{}'.....", schemaLoc)
        val scriptsDir = File(schemaLoc)
        val emptyProperties = Properties()
        val charset = Charsets.UTF_8.displayName()
        return FileMigrationLoader(scriptsDir, charset, emptyProperties)
    }

    @Bean
    fun pendingMigrationLoader(): FileMigrationLoader {
        var schemaLoc: String = schemaLocation
        if (SCHEMA_IN_CONTAINER == schemaLocation) {
            schemaLoc = DEFAULT_SCHEMA_LOCATION
        }
        schemaLoc = schemaLoc + PENDING_OPERATION_SUBFOLDER
        LOGGER.info("Creating pending operation migration loader for location: '{}'.....", schemaLoc)
        val scriptsDir = File(schemaLoc)
        val emptyProperties = Properties()
        val charset = Charsets.UTF_8.displayName()
        return FileMigrationLoader(scriptsDir, charset, emptyProperties)
    }

    private fun logMigrationResult(migrationResult: String, operation: String) {
        if (!migrationResult.isEmpty()) {
            LOGGER.warn("Migration result of '{}' operation:\n{}", operation, migrationResult)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DatabaseMigrationConfig::class.java)
        private val DEFAULT_SCHEMA_LOCATION = "/schema"
        private val SCHEMA_IN_CONTAINER = "container"
        private val DEFAULT_SCHEMA_LOCATION_IN_SOURCE = "core/src/main/resources/schema"
        private val PENDING_OPERATION_WARNING_MSG = "WARNING: Running pending migrations out of order can create unexpected results."
        private val UP_OPERATION_SUBFOLDER = "/mybatis"
        private val PENDING_OPERATION_SUBFOLDER = "/app"
    }
}
