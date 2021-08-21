package com.sequenceiq.cloudbreak.reactor.handler.cluster.dr;

import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.DATABASE_BACKUP_POSTFIX;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.DISASTER_RECOVERY_KEY;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.OBJECT_STORAGE_URL_KEY;
import static com.sequenceiq.cloudbreak.reactor.handler.cluster.dr.BackupRestoreSaltConfigGenerator.RANGER_ADMIN_GROUP_KEY;
import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {BackupRestoreSaltConfigGenerator.class})
public class BackupRestoreSaltConfigGeneratorTest {

    public static final String RANGER_ADMIN_GROUP = "ranger-admin";

    private static final String BACKUP_ID = "backupId";

    @Inject
    private BackupRestoreSaltConfigGenerator saltConfigGenerator;

    @Test
    public void testCreateSaltConfig() throws URISyntaxException {
        String cloudPlatform = "aws";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, placeholderStack);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("s3a://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
        assertEquals(RANGER_ADMIN_GROUP, disasterRecoveryKeyValuePairs.get(RANGER_ADMIN_GROUP_KEY));
    }

    @Test
    public void testCreateSaltConfigWithHdfsLocation() throws URISyntaxException {
        // AWS platform
        String cloudPlatform = "aws";
        String location = "hdfs://test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, placeholderStack);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("hdfs://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
        assertEquals(RANGER_ADMIN_GROUP, disasterRecoveryKeyValuePairs.get(RANGER_ADMIN_GROUP_KEY));

        // Azure platform
        cloudPlatform = "azure";
        placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, placeholderStack);

        disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals("hdfs://test/backups/backupId_database_backup", disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testObjectStorageUrlIsPrefixedWithS3aForAwsCloudplatform() throws URISyntaxException {
        String cloudPlatform = "aws";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, placeholderStack);

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

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, placeholderStack);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals(location + "/"  + BACKUP_ID + DATABASE_BACKUP_POSTFIX, disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }

    @Test
    public void testObjectStorageUrlIsPrefixedWithAbfsForAzureCloudplatform() throws URISyntaxException {
        String cloudPlatform = "azure";
        String location = "/test/backups";
        Stack placeholderStack = new Stack();
        placeholderStack.setCloudPlatform(cloudPlatform);

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, placeholderStack);

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

        SaltConfig saltConfig = saltConfigGenerator.createSaltConfig(location, BACKUP_ID, RANGER_ADMIN_GROUP, true, placeholderStack);

        Map<String, Object> disasterRecoveryProperties = saltConfig.getServicePillarConfig().get("disaster-recovery").getProperties();
        Map<String, String> disasterRecoveryKeyValuePairs = (Map<String, String>) disasterRecoveryProperties.get(DISASTER_RECOVERY_KEY);

        assertEquals(location + "/"  + BACKUP_ID + DATABASE_BACKUP_POSTFIX, disasterRecoveryKeyValuePairs.get(OBJECT_STORAGE_URL_KEY));
    }
}
