package com.sequenceiq.datalake.cm;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class RangerCloudIdentityServiceTest {

    @Mock
    private ClouderaManagerRangerUtil clouderaManagerRangerUtil;

    @Mock
    private SdxService sdxService;

    @InjectMocks
    private RangerCloudIdentityService underTest;

    @Test
    public void testSetAzureCloudIdentityMapping() throws ApiException {
        SdxCluster cluster = mock(SdxCluster.class);
        when(cluster.getStackCrn()).thenReturn("stack-crn");
        when(sdxService.listSdxByEnvCrn(anyString())).thenReturn(List.of(cluster));
        Map<String, String> userMapping = Map.of("user", "val1");
        underTest.setAzureCloudIdentityMapping("env-crn", userMapping);
        verify(clouderaManagerRangerUtil, times(1)).setAzureCloudIdentityMapping(eq("stack-crn"), eq(userMapping));
    }

}