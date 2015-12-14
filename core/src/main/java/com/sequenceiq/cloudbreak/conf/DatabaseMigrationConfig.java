package com.sequenceiq.cloudbreak.conf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.Properties;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.commons.io.Charsets;
import org.apache.ibatis.migration.DataSourceConnectionProvider;
import org.apache.ibatis.migration.FileMigrationLoader;
import org.apache.ibatis.migration.operations.UpOperation;
import org.apache.ibatis.migration.options.DatabaseOperationOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class DatabaseMigrationConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    private static final String DEFAULT_SCHEMA_LOCATION = "/schema";
    private static final String SCHEMA_IN_CONTAINER = "container";
    private static final String DEFAULT_SCHEMA_LOCATION_IN_SOURCE = "core/src/main/resources/schema";

    @Value("${cb.schema.scripts.location:" + DEFAULT_SCHEMA_LOCATION_IN_SOURCE + "}")
    private String schemaLocation;

    @Value("${cb.schema.migration.auto:true}")
    private boolean schemaMigrationEnabled;

    @Inject
    private DataSource dataSource;

    @Bean
    @DependsOn("dataSource")
    public UpOperation databaseUpMigration() {
        UpOperation upOperation = new UpOperation();
        if (schemaMigrationEnabled) {
            DataSourceConnectionProvider dataSourceConnectionProvider = new DataSourceConnectionProvider(dataSource);
            DatabaseOperationOption operationOption = new DatabaseOperationOption();
            operationOption.setRemoveCRs(true);
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            FileMigrationLoader migrationsLoader = fileMigrationLoader();
            LOGGER.info("Applying the necessary database migration scripts from location: '{}'....", schemaLocation);
            upOperation = upOperation.operate(dataSourceConnectionProvider, migrationsLoader, operationOption, new PrintStream(outStream));
            String migrationResult = outStream.toString().trim();
            if (migrationResult.isEmpty()) {
                LOGGER.info("Schema is up to date. No migration necessary.");
            } else {
                LOGGER.warn("Migration result:\n{}", migrationResult);
            }
        }
        return upOperation;
    }


    @Bean
    public FileMigrationLoader fileMigrationLoader() {
        if (SCHEMA_IN_CONTAINER.equals(schemaLocation)) {
            schemaLocation = DEFAULT_SCHEMA_LOCATION;
        }
        File scriptsDir = new File(schemaLocation);
        Properties emptyProperties = new Properties();
        String charset = Charsets.UTF_8.displayName();
        return new FileMigrationLoader(scriptsDir, charset, emptyProperties);
    }

}
