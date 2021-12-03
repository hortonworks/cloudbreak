package com.sequenceiq.cloudbreak.common.dbmigration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.ibatis.migration.ConnectionProvider;
import org.apache.ibatis.migration.DataSourceConnectionProvider;
import org.apache.ibatis.migration.FileMigrationLoader;
import org.apache.ibatis.migration.operations.PendingOperation;
import org.apache.ibatis.migration.operations.UpOperation;
import org.apache.ibatis.migration.options.DatabaseOperationOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.io.Files;

@Configuration
public class CommonDatabaseMigrationConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonDatabaseMigrationConfig.class);

    private static final String PENDING_OPERATION_WARNING_MSG = "WARNING: Running pending migrations out of order can create unexpected results.";

    @Value("${cb.schema.migration.auto:true}")
    private boolean schemaMigrationEnabled;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private List<SchemaLocationProvider> schemaLocationProviders;

    private File tempUpMigrationFolder;

    private File tempPendingMigrationFolder;

    @Bean
    @DependsOn("dataSource")
    public UpOperation databaseUpMigration(FileMigrationLoader upMigrationLoader, FileMigrationLoader pendingMigrationLoader) throws IOException {
        UpOperation upOperation = new UpOperation();
        if (schemaMigrationEnabled) {
            try {
                doDatabaseMigration(upMigrationLoader, pendingMigrationLoader, upOperation);
            } catch (Exception e) {
                throw new IllegalStateException("Something went wrong during database migration.", e);
            } finally {
                FileUtils.forceDelete(tempUpMigrationFolder);
                FileUtils.forceDelete(tempPendingMigrationFolder);
            }
        }
        return upOperation;
    }

    private void doDatabaseMigration(FileMigrationLoader upMigrationLoader,
            FileMigrationLoader pendingMigrationLoader, UpOperation upOperation) throws IOException {

        PendingOperation pendingOperation = new PendingOperation();
        ConnectionProvider dataSourceConnectionProvider = new DataSourceConnectionProvider(dataSource);
        DatabaseOperationOption operationOption = databaseOperationOption();
        try (ByteArrayOutputStream upOutStream = new ByteArrayOutputStream()) {
            try (ByteArrayOutputStream pendingOutStream = new ByteArrayOutputStream()) {
                upOperation.operate(dataSourceConnectionProvider, upMigrationLoader, operationOption, new PrintStream(upOutStream));
                pendingOperation.operate(dataSourceConnectionProvider, pendingMigrationLoader, operationOption, new PrintStream(pendingOutStream));
                String upMigrationResult = upOutStream.toString().trim();
                String pendingMigrationResult = pendingOutStream.toString().trim();
                if (upMigrationResult.isEmpty() && pendingMigrationResult.equals(PENDING_OPERATION_WARNING_MSG)) {
                    LOGGER.info("Schema is up to date. No migration necessary.");
                } else {
                    logMigrationResult(upMigrationResult, "up");
                    logMigrationResult(pendingMigrationResult, "pending");
                }
            }
        }
    }

    private DatabaseOperationOption databaseOperationOption() {
        DatabaseOperationOption operationOption = new DatabaseOperationOption();
        operationOption.setDelimiter(";");
        operationOption.setFullLineDelimiter(false);
        operationOption.setSendFullScript(true);
        operationOption.setAutoCommit(false);
        operationOption.setThrowWarning(false);
        return operationOption;
    }

    @Bean
    public FileMigrationLoader upMigrationLoader() throws IOException {
        tempUpMigrationFolder = Files.createTempDir();
        copyInternalFilesToTempFolder(tempUpMigrationFolder, schemaLocationProviders, SchemaLocationProvider::upSubfolder);
        return new FileMigrationLoader(tempUpMigrationFolder, "UTF-8", new Properties());
    }

    @Bean
    public FileMigrationLoader pendingMigrationLoader() throws IOException {
        tempPendingMigrationFolder = Files.createTempDir();
        copyInternalFilesToTempFolder(tempPendingMigrationFolder, schemaLocationProviders, SchemaLocationProvider::pendingSubfolder);
        return new FileMigrationLoader(tempPendingMigrationFolder, "UTF-8", new Properties());
    }

    private void copyInternalFilesToTempFolder(File tempFolder, List<SchemaLocationProvider> schemaLocationProviders,
            Function<SchemaLocationProvider, Optional<String>> subfolderFunction) throws IOException {
        Set<Resource> migrationResources = collectMigrationResources(tempFolder, schemaLocationProviders, subfolderFunction);
        copyResourcesToTempFolder(tempFolder, migrationResources);
    }

    private Set<Resource> collectMigrationResources(File tempFolder, List<SchemaLocationProvider> schemaLocationProviders,
            Function<SchemaLocationProvider, Optional<String>> subfolderFunction) {

        Set<Resource> migrationFiles = new HashSet<>();
        for (SchemaLocationProvider provider : schemaLocationProviders) {
            subfolderFunction.apply(provider).ifPresent(subfolder -> {
                String classpath = String.format("classpath:schema/%s/*.*", subfolder);
                try {
                    LOGGER.info("Copying internal migration files from: '{}' to '{}'", classpath, tempFolder.getAbsolutePath());
                    migrationFiles.addAll(Arrays.asList(applicationContext.getResources(classpath)));
                } catch (FileNotFoundException e) {
                    LOGGER.warn("Provided schema location '{}' was not found. Continuing execution. Detailed message: {}", classpath, e.getMessage());
                } catch (IOException io) {
                    throw new IllegalStateException(io);
                }
            });
        }
        return migrationFiles;
    }

    private void copyResourcesToTempFolder(File tempFolder, Set<Resource> migrationResources) throws IOException {
        for (Resource resource : migrationResources) {
            if (resource instanceof ClassPathResource) {
                File newFile = new File(tempFolder, resource.getFilename());
                boolean created = newFile.createNewFile();
                if (!created) {
                    throw new IllegalStateException("Failed to create new file for the classpath migration script: "
                            + ((ClassPathResource) resource).getPath());
                }
                try (InputStream inputStream = Channels.newInputStream(resource.readableChannel())) {
                    FileUtils.copyInputStreamToFile(inputStream, newFile);
                }
            } else {
                FileUtils.copyFileToDirectory(resource.getFile(), tempFolder);
            }
        }
    }

    private void logMigrationResult(String migrationResult, String operation) {
        if (!migrationResult.isEmpty()) {
            LOGGER.debug("Migration result of '{}' operation:\n{}", operation, migrationResult);
        }
    }
}
