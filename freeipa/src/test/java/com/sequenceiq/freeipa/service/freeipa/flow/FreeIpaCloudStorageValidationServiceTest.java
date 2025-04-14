package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;

@ExtendWith(MockitoExtension.class)
class FreeIpaCloudStorageValidationServiceTest {

    private static final String STORAGE_LOCATION = "storage-location";

    @InjectMocks
    private FreeIpaCloudStorageValidationService underTest;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private Stack stack;

    @Mock
    private List<GatewayConfig> allGateways;

    @Mock
    private GatewayConfig primaryGatewayConfig;

    @Mock
    private Set<Node> allNodes;

    @BeforeEach
    void before() {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        when(gatewayConfigService.getGatewayConfigs(stack, instanceMetaDataSet)).thenReturn(allGateways);
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(allNodes);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(primaryGatewayConfig);
    }

    @Test
    void testValidateShouldNotThrowValidationError() throws CloudbreakOrchestratorException {
        underTest.validate(stack);

        verify(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());
    }

    @Test
    void testValidateShouldThrowValidationErrorWhenTheBackupObjectIsNull() throws CloudbreakOrchestratorException {
        doThrow(new CloudbreakOrchestratorFailedException("connection timeout"))
                .when(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());

        Exception exception = assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.validate(stack));

        assertEquals("Validating FreeIPA cloud storage permission for backup failed. " +
                "For more details please check '/var/log/ipabackup.log' on the instance.", exception.getMessage());
        verify(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());
    }

    @Test
    void testValidateShouldThrowAwsValidationError() throws CloudbreakOrchestratorException {
        Backup backup = createBackup();
        backup.setS3(createAwsParams());
        when(stack.getBackup()).thenReturn(backup);
        doThrow(new CloudbreakOrchestratorFailedException("connection timeout"))
                .when(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());

        Exception exception = assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.validate(stack));

        assertEquals("Validating FreeIPA cloud storage permission for backup failed. "
                + "The instance profile instance-profile did not have permission to write to storage-location. "
                + "If provisioning was done using the UI, then verify the log's instance profile and logs location base when provisioning in the UI. "
                + "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. "
                + "Specifically verify the logStorage.instanceProfile and logStorage.storageLocationBase. "
                + "Refer to Cloudera documentation at "
                + "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-aws/topics/mc-idbroker-minimum-setup.html#mc-idbroker-minimum-setup "
                + "for the required rights. For more details please check '/var/log/ipabackup.log' on the instance.", exception.getMessage());
        verify(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());
    }

    @Test
    void testValidateShouldThrowAzureValidationError() throws CloudbreakOrchestratorException {
        Backup backup = createBackup();
        backup.setAdlsGen2(createAzureParams());
        when(stack.getBackup()).thenReturn(backup);
        doThrow(new CloudbreakOrchestratorFailedException("connection timeout"))
                .when(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());

        Exception exception = assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.validate(stack));

        assertEquals("Validating FreeIPA cloud storage permission for backup failed. "
                + "The managed profile managed-identity did not have permission to write to storage-location. "
                + "If provisioning was done using the UI, then verify the logger identity and logs location base when provisioning in the UI. "
                + "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. "
                + "Specifically, verify the logStorage.managedIdentity and logStorage.storageLocationBase. Refer to Cloudera documentation at "
                + "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-azure/topics/mc-az-minimal-setup-for-cloud-storage.html"
                + "#mc-az-minimal-setup-for-cloud-storage for the required rights. "
                + "For more details please check '/var/log/ipabackup.log' on the instance.", exception.getMessage());
        verify(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());
    }

    @Test
    void testValidateShouldThrowGcpValidationError() throws CloudbreakOrchestratorException {
        Backup backup = createBackup();
        backup.setGcs(createGpParams());
        when(stack.getBackup()).thenReturn(backup);
        doThrow(new CloudbreakOrchestratorFailedException("connection timeout"))
                .when(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());

        Exception exception = assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.validate(stack));

        assertEquals("Validating FreeIPA cloud storage permission for backup failed. "
                + "The managed profile service-account-email did not have permission to write to storage-location. "
                + "If provisioning was done using the UI, then verify the logger identity and logs location base when provisioning in the UI. "
                + "If provisioning was done using the CLI, then verify the JSON which was provided when creating the environment. "
                + "Specifically, verify the logStorage.managedIdentity and logStorage.storageLocationBase. Refer to Cloudera documentation at "
                + "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-gcp/topics/mc-gcp_minimum_setup_for_cloud_storage.html"
                + "#mc-gcp_minimum_setup_for_cloud_storage for the required rights. "
                + "For more details please check '/var/log/ipabackup.log' on the instance.", exception.getMessage());
        verify(hostOrchestrator).validateCloudStorageBackup(eq(primaryGatewayConfig), eq(allGateways), eq(allNodes), any());
    }

    private Backup createBackup() {
        Backup backup = new Backup();
        backup.setStorageLocation(STORAGE_LOCATION);
        return backup;
    }

    private S3CloudStorageV1Parameters createAwsParams() {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = new S3CloudStorageV1Parameters();
        s3CloudStorageV1Parameters.setInstanceProfile("instance-profile");
        return s3CloudStorageV1Parameters;
    }

    private AdlsGen2CloudStorageV1Parameters createAzureParams() {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageV1Parameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageV1Parameters.setManagedIdentity("managed-identity");
        return adlsGen2CloudStorageV1Parameters;
    }

    private GcsCloudStorageV1Parameters createGpParams() {
        GcsCloudStorageV1Parameters gcsCloudStorageV1Parameters = new GcsCloudStorageV1Parameters();
        gcsCloudStorageV1Parameters.setServiceAccountEmail("service-account-email");
        return gcsCloudStorageV1Parameters;
    }

}