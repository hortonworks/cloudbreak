package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.CLUSTER_NAME;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.USER_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.getSdxCluster;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

@ExtendWith(MockitoExtension.class)
class DistroxServiceTest {
    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private DistroXV1Endpoint distroXV1Endpoint;

    @Mock
    private SdxService sdxService;

    @Mock
    private AccountIdService accountIdService;

    @InjectMocks
    private DistroxService underTest;

    @BeforeEach
    void setUp() {
        Field attempt = ReflectionUtils.findField(DistroxService.class, "attempt");
        ReflectionUtils.makeAccessible(attempt);
        ReflectionUtils.setField(attempt, underTest, 1);
        Field sleeptime = ReflectionUtils.findField(DistroxService.class, "sleeptime");
        ReflectionUtils.makeAccessible(sleeptime);
        ReflectionUtils.setField(sleeptime, underTest, 5);
    }

    @Test
    void refreshDatahubsWithoutName() {
        SdxCluster sdxCluster = getSdxCluster();
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        Collection<StackViewV4Response> responses = new ArrayList<>();
        StackViewV4Response stackViewV4Response = new StackViewV4Response();
        stackViewV4Response.setStatus(Status.AVAILABLE);
        stackViewV4Response.setCrn(UUID.randomUUID().toString());
        responses.add(stackViewV4Response);
        stackViewV4Responses.setResponses(responses);
        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setStatus(Status.AVAILABLE);
        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setStatus(Status.AVAILABLE);
        stackV4Response.setCluster(clusterV4Response);

        ArgumentCaptor<String> crnArgCaptor = ArgumentCaptor.forClass(String.class);
        when(sdxService.getByNameInAccountAllowDetached(crnArgCaptor.capture(), anyString()))
                .thenReturn(sdxCluster);
        when(distroXV1Endpoint.list(eq(null), eq(sdxCluster.getEnvCrn()))).thenReturn(stackViewV4Responses);
        when(distroXV1Endpoint.getByCrn(any(), any())).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.refreshDataHub(CLUSTER_NAME, null));

        verify(distroXV1Endpoint, times(2)).list(null, sdxCluster.getEnvCrn());
        verify(distroXV1Endpoint, times(1)).restartClusterServicesByCrns(any(), any());
        assertEquals("crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com", crnArgCaptor.getValue());
    }

    @Test
    void refreshDatahubsWithName() {
        SdxCluster sdxCluster = getSdxCluster();

        ArgumentCaptor<String> crnArgCaptor = ArgumentCaptor.forClass(String.class);
        when(sdxService.getByNameInAccountAllowDetached(crnArgCaptor.capture(), anyString()))
                .thenReturn(sdxCluster);
        StackV4Response stackV4Response = mock(StackV4Response.class);
        when(stackV4Response.getCrn()).thenReturn(
                CrnTestUtil.getEnvironmentCrnBuilder()
                        .setResource(UUID.randomUUID().toString())
                        .setAccountId(UUID.randomUUID().toString())
                        .build().toString());
        when(distroXV1Endpoint.getByName(anyString(), eq(null))).thenReturn(stackV4Response);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.refreshDataHub(CLUSTER_NAME, "datahubName"));

        verify(distroXV1Endpoint, times(1)).getByName(anyString(), eq(null));
        verify(distroXV1Endpoint, times(1)).restartClusterServicesByCrns(any(), any());
        assertEquals("crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com", crnArgCaptor.getValue());
    }
}