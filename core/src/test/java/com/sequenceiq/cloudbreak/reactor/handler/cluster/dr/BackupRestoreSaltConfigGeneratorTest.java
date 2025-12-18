package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.BACKUP_RESTORE_CONFIG;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.COMPRESSION_LEVEL;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.DATABASE_BACKUP_POSTFIX;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.DATABASE_NAMES_FOR_DRY_RUN;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.DATABASE_NAMES_FOR_SIZING;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.DATABASE_NAMES_KEY;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.DISASTER_RECOVERY_KEY;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.OBJECT_STORAGE_URL_KEY;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.RANGER_ADMIN_GROUP_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;

@ExtendWith(MockitoExtension.class)
public class BackupRestoreSaltConfigGeneratorTest {

    public static final String RANGER_ADMIN_GROUP = "ranger-admin";

    private static final String BACKUP_ID = "backupId";

    private static final String TEST_ACCOUNT_ID = "test-account-id";

    private static final String EXPECTED_DATABASE_NAMES = "hive ranger profiler_agent profiler_metric";

    private static final String EXPECTED_DATABASE_NAMES_WITH_KNOX = "hive ranger profiler_agent profiler_metric knox_gateway";

    private static final String EXPECTED_DRY_RUN_DATABASE_NAMES = "hive ranger";

    private static final String EXPECTED_DRY_RUN_DATABASE_NAMES_WITH_KNOX = "hive ranger knox_gateway";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private BackupRestoreSaltConfigGenerator saltConfigGenerator;

    private MockedStatic<ThreadBasedUserCrnProvider> mockedUserCrnProvider;

    @BeforeEach
    public void setUp() {
        when(entitlementService.isDatalakeKnoxGatewayDbDrEnabled(anyString())).thenReturn(false);
        mockedUserCrnProvider = mockStatic(ThreadBasedUserCrnProvider.class);
        mockedUserCrnProvider.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(TEST_ACCOUNT_ID);
    }

    @AfterEach
    public void tearDown() {
        if (mockedUserCrnProvider != null) {
            mockedUserCrnProvider.close();
        }
    }

    @Test
    public void testCreateSaltConfig() throws URISyntaxException {
        String cloudPlatform = "aws";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("s3a://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
        assertEquals(RANGER_ADMIN_GROUP, disasterRecoveryKeyValuePairs.get(RANGER_ADMIN_GROUP_KEY));
        assertEquals(EXPECTED_DATABASE_NAMES, disasterRecoveryKeyValuePairs.get(DATABASE_NAMES_KEY));
    }

    @Test
    public void testCreateSaltConfigWithKnoxGatewayEntitlement() throws URISyntaxException {
        when(entitlementService.isDatalakeKnoxGatewayDbDrEnabled(anyString())).thenReturn(true);

        String cloudPlatform = "aws";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("s3a://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
        assertEquals(RANGER_ADMIN_GROUP, disasterRecoveryKeyValuePairs.get(RANGER_ADMIN_GROUP_KEY));
        assertEquals(EXPECTED_DATABASE_NAMES_WITH_KNOX, disasterRecoveryKeyValuePairs.get(DATABASE_NAMES_KEY));
    }

    @Test
    public void testCreateSaltConfigWithHdfsLocation() throws URISyntaxException {
        // AWS platform
        String cloudPlatform = "aws";
        String location = "hdfs://test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("hdfs://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
        assertEquals(RANGER_ADMIN_GROUP, disasterRecoveryKeyValuePairs.get(RANGER_ADMIN_GROUP_KEY));

        // Azure platform
        cloudPlatform = "azure";
        placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("hdfs://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testCreateSaltConfigWithSkipDatabaseNames() throws URISyntaxException {
        String cloudPlatform = "aws";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, List.of("hive"),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("s3a://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
        assertEquals(RANGER_ADMIN_GROUP, disasterRecoveryKeyValuePairs.get(RANGER_ADMIN_GROUP_KEY));
        assertEquals("ranger profiler_agent profiler_metric", disasterRecoveryKeyValuePairs.get(DATABASE_NAMES_KEY));

    }

    @Test
    public void testObjectStorageUrlIsPrefixedWithS3aForAwsCloudplatform() throws URISyntaxException {
        String cloudPlatform = "aws";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("s3a://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testS3aSchemeIsPassedThroughUnchanged() throws Exception {
        String cloudPlatform = "aws";
        String location = "s3a://eng-sdx-daily-datalake/bderriso-provo/data/backup01";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals(location + "/" + BACKUP_ID + DATABASE_BACKUP_POSTFIX, disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testObjectStorageUrlIsPrefixedWithAbfsForAzureCloudplatform() throws URISyntaxException {
        String cloudPlatform = "azure";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("abfs://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testObjectStorageUrlIsNotChangedForCorrectAbfsAzurePrefix() throws URISyntaxException {
        String cloudPlatform = "azure";
        String location = "abfs://eng-sdx-daily-datalake/bderriso-provo/data/backup01";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals(location + "/" + BACKUP_ID + DATABASE_BACKUP_POSTFIX, disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testObjectStorageUrlIsPrefixedWithGSForGCPCloudplatform() throws URISyntaxException {
        String cloudPlatform = "gcp";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("gs://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testGSSchemeIsPassedThroughUnchanged() throws Exception {
        String cloudPlatform = "gcp";
        String location = "gs://eng-sdx-daily-datalake/bderriso-provo/data/backup01";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals(location + "/" + BACKUP_ID + DATABASE_BACKUP_POSTFIX, disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testEnableCompressionFlagPropagated() throws URISyntaxException {
        String cloudPlatform = "azure";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                true, placeholderStack, false);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("1", disasterRecoveryKeyValuePairs.get(COMPRESSION_LEVEL));

        saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, Collections.emptyList(),
                false, placeholderStack, false);

        disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("0", disasterRecoveryKeyValuePairs.get(COMPRESSION_LEVEL));
    }

    @Test
    public void testCreateSaltConfigWithTempDirsWithoutKnoxEntitlement() {
        String tempBackupDir = "/custom/backup";
        String tempRestoreDir = "/custom/restore";
        SaltConfig inputSaltConfig = new SaltConfig();

        SaltConfig resultSaltConfig = saltConfigGenerator.createSaltConfig(inputSaltConfig, tempBackupDir, tempRestoreDir);

        Map<String, Object> backupRestoreProperties = resultSaltConfig.getServicePillarConfig().get("backup-restore-config").getProperties();
        Map<String, String> backupRestoreKeyValuePairs = (Map<String, String>) backupRestoreProperties.get(BACKUP_RESTORE_CONFIG);

        assertEquals(tempBackupDir, backupRestoreKeyValuePairs.get("temp_backup_dir"));
        assertEquals(tempRestoreDir, backupRestoreKeyValuePairs.get("temp_restore_dir"));
        assertEquals(EXPECTED_DATABASE_NAMES, backupRestoreKeyValuePairs.get(DATABASE_NAMES_FOR_SIZING));
        assertEquals(EXPECTED_DRY_RUN_DATABASE_NAMES, backupRestoreKeyValuePairs.get(DATABASE_NAMES_FOR_DRY_RUN));
    }

    @Test
    public void testCreateSaltConfigWithTempDirsWithKnoxEntitlement() {
        when(entitlementService.isDatalakeKnoxGatewayDbDrEnabled(anyString())).thenReturn(true);

        String tempBackupDir = "/custom/backup";
        String tempRestoreDir = "/custom/restore";
        SaltConfig inputSaltConfig = new SaltConfig();

        SaltConfig resultSaltConfig = saltConfigGenerator.createSaltConfig(inputSaltConfig, tempBackupDir, tempRestoreDir);

        Map<String, Object> backupRestoreProperties = resultSaltConfig.getServicePillarConfig().get("backup-restore-config").getProperties();
        Map<String, String> backupRestoreKeyValuePairs = (Map<String, String>) backupRestoreProperties.get(BACKUP_RESTORE_CONFIG);

        assertEquals(tempBackupDir, backupRestoreKeyValuePairs.get("temp_backup_dir"));
        assertEquals(tempRestoreDir, backupRestoreKeyValuePairs.get("temp_restore_dir"));
        assertEquals(EXPECTED_DATABASE_NAMES_WITH_KNOX, backupRestoreKeyValuePairs.get(DATABASE_NAMES_FOR_SIZING));
        assertEquals(EXPECTED_DRY_RUN_DATABASE_NAMES_WITH_KNOX, backupRestoreKeyValuePairs.get(DATABASE_NAMES_FOR_DRY_RUN));
    }

}
