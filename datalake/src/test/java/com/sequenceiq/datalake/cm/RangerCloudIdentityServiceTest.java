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

import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncState;
import com.sequenceiq.sdx.api.model.RangerCloudIdentitySyncStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RangerCloudIdentityServiceTest {

    @Mock
    private ClouderaManagerRangerUtil clouderaManagerRangerUtil;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private RangerCloudIdentityService underTest;

    @Test
    public void testSetAzureCloudIdentityMappingSuccessSync() throws ApiException {
        ApiCommand apiCommand = mock(ApiCommand.class);
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(apiCommand.getSuccess()).thenReturn(true);
        testSetAzureCloudIdentityMapping(Optional.of(apiCommand), RangerCloudIdentitySyncState.SUCCESS);
    }

    @Test
    public void testSetAzureCloudIdentityMappingActiveSync() throws ApiException {
        ApiCommand apiCommand = mock(ApiCommand.class);
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(apiCommand.getActive()).thenReturn(true);
        testSetAzureCloudIdentityMapping(Optional.of(apiCommand), RangerCloudIdentitySyncState.ACTIVE);
    }

    @Test
    public void testSetAzureCloudIdentityMappingFailSync() throws ApiException {
        ApiCommand apiCommand = mock(ApiCommand.class);
        when(apiCommand.getId()).thenReturn(BigDecimal.ONE);
        when(apiCommand.getSuccess()).thenReturn(false);
        testSetAzureCloudIdentityMapping(Optional.of(apiCommand), RangerCloudIdentitySyncState.FAILED);
    }

    @Test
    public void testSetAzureCloudIdentityMappingNoApiCommand() throws ApiException {
        testSetAzureCloudIdentityMapping(Optional.empty(), RangerCloudIdentitySyncState.SUCCESS);
    }

    @Test
    public void testSetAzureCloudIdentityMappingNoDatalake() throws ApiException {
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(Collections.emptyList());

        Map<String, String> userMapping = Map.of("user", "val1");
        RangerCloudIdentitySyncStatus status = underTest.setAzureCloudIdentityMapping("env-crn", userMapping);

        assertEquals(RangerCloudIdentitySyncState.NOT_APPLICABLE, status.getState());
        verify(clouderaManagerRangerUtil, never()).setAzureCloudIdentityMapping(eq("stack-crn"), eq(userMapping));
    }

    private void testSetAzureCloudIdentityMapping(Optional<ApiCommand> apiCommand, RangerCloudIdentitySyncState expectedStatus) throws ApiException {
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