package com.sequenceiq.datalake.cm;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncState;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;

@ExtendWith(MockitoExtension.class)
class RangerCloudIdentityServiceTest {

    @Mock
    private ClouderaManagerRangerUtil clouderaManagerRangerUtil;

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private RangerCloudIdentityService underTest;

    private SdxStatusEntity mockSdxStatus(DatalakeStatusEnum status) {
        SdxStatusEntity sdxStatusEntity = mock(SdxStatusEntity.class);
        when(sdxStatusEntity.getStatus()).thenReturn(status);
        return sdxStatusEntity;
    }

    @Test
    public void testSetAzureCloudIdentityMappingSuccessSync() throws ApiException {
        ApiCommand apiCommand = mock(ApiCommand.class);
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(apiCommand.isSuccess()).thenReturn(true);
        SdxStatusEntity sdxStatus = mockSdxStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatus);
        testSetAzureCloudIdentityMapping(List.of(apiCommand), RangerCloudIdentitySyncState.SUCCESS);
    }

    @Test
    public void testSetAzureCloudIdentityMappingActiveSync() throws ApiException {
        ApiCommand apiCommand = mock(ApiCommand.class);
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(apiCommand.isActive()).thenReturn(true);
        SdxStatusEntity sdxStatus = mockSdxStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatus);
        testSetAzureCloudIdentityMapping(List.of(apiCommand), RangerCloudIdentitySyncState.ACTIVE);
    }

    @Test
    public void testSetAzureCloudIdentityMappingFailSync() throws ApiException {
        ApiCommand apiCommand = mock(ApiCommand.class);
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(apiCommand.isSuccess()).thenReturn(false);
        SdxStatusEntity sdxStatus = mockSdxStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatus);
        testSetAzureCloudIdentityMapping(List.of(apiCommand), RangerCloudIdentitySyncState.FAILED);
    }

    @Test
    public void testSetAzureCloudIdentityMappingNoApiCommand() throws ApiException {
        SdxStatusEntity sdxStatus = mockSdxStatus(DatalakeStatusEnum.RUNNING);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatus);
        testSetAzureCloudIdentityMapping(List.of(), RangerCloudIdentitySyncState.SUCCESS);
    }

    @Test
    public void testSetAzureCloudIdentityMappingNoDatalake() throws ApiException {
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(Collections.emptyList());

        Map<String, String> userMapping = Map.of("user", "val1");
        RangerCloudIdentitySyncStatus status = underTest.setAzureCloudIdentityMapping("env-crn", userMapping);

        assertEquals(RangerCloudIdentitySyncState.NOT_APPLICABLE, status.getState());
        verify(clouderaManagerRangerUtil, never()).setAzureCloudIdentityMapping(eq("stack-crn"), eq(userMapping));
    }

    @Test
    public void testSetAzureCloudIdentityMappingDatalakNotRunning() throws ApiException {
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of(mock(SdxCluster.class)));
        SdxStatusEntity sdxStatus = mockSdxStatus(DatalakeStatusEnum.PROVISIONING_FAILED);
        when(sdxStatusService.getActualStatusForSdx(any(SdxCluster.class))).thenReturn(sdxStatus);

        Map<String, String> userMapping = Map.of("user", "val1");
        RangerCloudIdentitySyncStatus status = underTest.setAzureCloudIdentityMapping("env-crn", userMapping);

        assertEquals(RangerCloudIdentitySyncState.NOT_APPLICABLE, status.getState());
        verify(clouderaManagerRangerUtil, never()).setAzureCloudIdentityMapping(eq("stack-crn"), eq(userMapping));
    }

    private void testSetAzureCloudIdentityMapping(List<ApiCommand> apiCommand, RangerCloudIdentitySyncState expectedStatus) throws ApiException {
        SdxCluster cluster = mock(SdxCluster.class);
        when(cluster.getStackCrn()).thenReturn("stack-crn");
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of(cluster));
        when(clouderaManagerRangerUtil.isCloudIdMappingSupported(any())).thenReturn(true);
        when(clouderaManagerRangerUtil.setAzureCloudIdentityMapping(any(), any())).thenReturn(apiCommand);

        Map<String, String> userMapping = Map.of("user", "val1");
        RangerCloudIdentitySyncStatus status = underTest.setAzureCloudIdentityMapping("env-crn", userMapping);

        assertEquals(expectedStatus, status.getState());
        verify(clouderaManagerRangerUtil, times(1)).setAzureCloudIdentityMapping(eq("stack-crn"), eq(userMapping));
    }

}