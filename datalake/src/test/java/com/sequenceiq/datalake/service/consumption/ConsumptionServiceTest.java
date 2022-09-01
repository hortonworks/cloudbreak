package com.sequenceiq.datalake.service.consumption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;
import com.sequenceiq.consumption.api.v1.consumption.model.request.StorageConsumptionRequest;
import com.sequenceiq.datalake.entity.SdxCluster;

@ExtendWith(MockitoExtension.class)
class ConsumptionServiceTest {

    private static final boolean CONSUMPTION_ENABLED = true;

    private static final boolean CONSUMPTION_DISABLED = false;

    private static final String ACCOUNT_ID = "accountId";

    private static final String INITIATOR_USER_CRN = "initiatorUserCrn";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String DATALAKE_CRN = "datalakeCrn";

    private static final String DATALAKE_NAME = "datalakeName";

    private static final String STORAGE_BASE_LOCATION = "s3a://foo/bar";

    private static final String EMPTY_STORAGE_BASE_LOCATION = "";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ConsumptionClientService consumptionClientService;

    @InjectMocks
    private ConsumptionService underTest;

    @Captor
    private ArgumentCaptor<StorageConsumptionRequest> storageConsumptionRequestCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "consumptionEnabled", CONSUMPTION_ENABLED);
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSkipWhenDeploymentFlagDisabled() {
        ReflectionTestUtils.setField(underTest, "consumptionEnabled", CONSUMPTION_DISABLED);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(sdxCluster());

        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionClientService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class), anyString());
    }

    private SdxCluster sdxCluster(String cloudStorageBaseLocation) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setAccountId(ACCOUNT_ID);
        sdxCluster.setCloudStorageBaseLocation(cloudStorageBaseLocation);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setCrn(DATALAKE_CRN);
        sdxCluster.setClusterName(DATALAKE_NAME);
        return sdxCluster;
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestSkipWhenEntitlementDisabled() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(false);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(sdxCluster());

        verify(consumptionClientService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class), anyString());
    }

    @ParameterizedTest(name = "cloudStorageBaseLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_BASE_LOCATION})
    @NullSource
    void scheduleStorageConsumptionCollectionIfNeededTestSkipWhenNoStorageBaseLocation(String cloudStorageBaseLocation) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);

        underTest.scheduleStorageConsumptionCollectionIfNeeded(sdxCluster(cloudStorageBaseLocation));

        verify(consumptionClientService, never()).scheduleStorageConsumptionCollection(anyString(), any(StorageConsumptionRequest.class), anyString());
    }

    @Test
    void scheduleStorageConsumptionCollectionIfNeededTestExecute() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.scheduleStorageConsumptionCollectionIfNeeded(sdxCluster()));

        verify(consumptionClientService).scheduleStorageConsumptionCollection(eq(ACCOUNT_ID), storageConsumptionRequestCaptor.capture(), anyString());
        verifyStorageConsumptionRequest(storageConsumptionRequestCaptor.getValue());
    }

    private void verifyStorageConsumptionRequest(StorageConsumptionRequest storageConsumptionRequest) {
        assertThat(storageConsumptionRequest).isNotNull();
        assertThat(storageConsumptionRequest.getEnvironmentCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(storageConsumptionRequest.getMonitoredResourceCrn()).isEqualTo(DATALAKE_CRN);
        assertThat(storageConsumptionRequest.getMonitoredResourceName()).isEqualTo(DATALAKE_NAME);
        assertThat(storageConsumptionRequest.getMonitoredResourceType()).isEqualTo(ResourceType.DATALAKE);
        assertThat(storageConsumptionRequest.getStorageLocation()).isEqualTo(STORAGE_BASE_LOCATION);
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSkipWhenDeploymentFlagDisabled() {
        ReflectionTestUtils.setField(underTest, "consumptionEnabled", CONSUMPTION_DISABLED);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(sdxCluster());

        verify(entitlementService, never()).isCdpSaasEnabled(anyString());
        verify(consumptionClientService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString(), anyString());
    }

    private SdxCluster sdxCluster() {
        return sdxCluster(STORAGE_BASE_LOCATION);
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestSkipWhenEntitlementDisabled() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(false);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(sdxCluster());

        verify(consumptionClientService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString(), anyString());
    }

    @ParameterizedTest(name = "cloudStorageBaseLocation={0}")
    @ValueSource(strings = {EMPTY_STORAGE_BASE_LOCATION})
    @NullSource
    void unscheduleStorageConsumptionCollectionIfNeededTestSkipWhenNoStorageBaseLocation(String cloudStorageBaseLocation) {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);

        underTest.unscheduleStorageConsumptionCollectionIfNeeded(sdxCluster(cloudStorageBaseLocation));

        verify(consumptionClientService, never()).unscheduleStorageConsumptionCollection(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void unscheduleStorageConsumptionCollectionIfNeededTestExecute() {
        when(entitlementService.isCdpSaasEnabled(ACCOUNT_ID)).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(INITIATOR_USER_CRN, () -> underTest.unscheduleStorageConsumptionCollectionIfNeeded(sdxCluster()));

        verify(consumptionClientService).unscheduleStorageConsumptionCollection(ACCOUNT_ID, DATALAKE_CRN, STORAGE_BASE_LOCATION, INITIATOR_USER_CRN);
    }

}