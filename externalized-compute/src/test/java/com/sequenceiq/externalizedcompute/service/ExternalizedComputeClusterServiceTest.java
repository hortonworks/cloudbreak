package com.sequenceiq.externalizedcompute.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagGenerationRequest;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;
import com.sequenceiq.externalizedcompute.api.model.ExternalizedComputeClusterRequest;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeCluster;
import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterFlowManager;
import com.sequenceiq.externalizedcompute.repository.ExternalizedComputeClusterRepository;

@ExtendWith(MockitoExtension.class)
class ExternalizedComputeClusterServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Spy
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private CrnUserDetailsService crnUserDetailsService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CostTagging costTagging;

    @Mock
    private AccountTagService accountTagService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ExternalizedComputeClusterRepository externalizedComputeClusterRepository;

    @Mock
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Mock
    private ExternalizedComputeClusterFlowManager externalizedComputeClusterFlowManager;

    @InjectMocks
    private ExternalizedComputeClusterService clusterService;

    @Test
    public void testPrepareComputeClusterCreation() throws TransactionExecutionException {
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "partition", "cdp");
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "region", "us-west-1");
        doAnswer(invocation -> invocation.getArgument(0, Supplier.class).get()).when(transactionService).required(any(Supplier.class));

        ExternalizedComputeClusterRequest externalizedComputeClusterRequest = new ExternalizedComputeClusterRequest();
        externalizedComputeClusterRequest.setName("cluster");
        externalizedComputeClusterRequest.setEnvironmentCrn("envCrn");
        externalizedComputeClusterRequest.setTags(Map.of("key", "value"));

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setTags(new TagResponse());
        detailedEnvironmentResponse.setCrn("envCrn");
        detailedEnvironmentResponse.setCloudPlatform("AWS");
        when(environmentEndpoint.getByCrn(externalizedComputeClusterRequest.getEnvironmentCrn())).thenReturn(detailedEnvironmentResponse);

        Crn userCrn = Crn.fromString(USER_CRN);

        CrnUser crnUser = mock(CrnUser.class);
        when(crnUser.getUsername()).thenReturn("perdos@cloudera.com");
        when(crnUserDetailsService.loadUserByUsername(USER_CRN)).thenReturn(crnUser);

        ExternalizedComputeCluster savedCluster = new ExternalizedComputeCluster();
        when(externalizedComputeClusterRepository.save(any())).thenReturn(savedCluster);

        ArgumentCaptor<CDPTagGenerationRequest> cdpTagGenerationRequestArgumentCaptor = ArgumentCaptor.forClass(CDPTagGenerationRequest.class);
        when(costTagging.prepareDefaultTags(cdpTagGenerationRequestArgumentCaptor.capture())).thenReturn(Map.of());

        when(accountTagService.list()).thenReturn(Map.of("account", "tag"));

        when(entitlementService.internalTenant("1234")).thenReturn(true);

        clusterService.prepareComputeClusterCreation(externalizedComputeClusterRequest, true, userCrn);

        ArgumentCaptor<ExternalizedComputeCluster> clusterArgumentCaptor = ArgumentCaptor.forClass(
                ExternalizedComputeCluster.class);
        verify(externalizedComputeClusterRepository).save(clusterArgumentCaptor.capture());

        ExternalizedComputeCluster cluster = clusterArgumentCaptor.getValue();
        assertEquals(externalizedComputeClusterRequest.getName(), cluster.getName());
        assertEquals(externalizedComputeClusterRequest.getEnvironmentCrn(), cluster.getEnvironmentCrn());
        assertEquals("1234", cluster.getAccountId());
        assertTrue(cluster.isDefaultCluster());
        assertNull(cluster.getDeleted());

        CDPTagGenerationRequest tags = cdpTagGenerationRequestArgumentCaptor.getValue();
        assertThat(tags.getUserDefinedTags()).containsExactlyInAnyOrderEntriesOf(Map.of("key", "value"));
        assertEquals("envCrn", tags.getEnvironmentCrn());
        assertEquals(USER_CRN, tags.getCreatorCrn());
        assertEquals("AWS", tags.getPlatform());
        assertEquals("perdos@cloudera.com", tags.getUserName());
        assertEquals("1234", tags.getAccountId());
        assertTrue(tags.isInternalTenant());

        verify(externalizedComputeClusterStatusService).setStatus(savedCluster, ExternalizedComputeClusterStatusEnum.CREATE_IN_PROGRESS,
                "Cluster provision initiated");
    }

    @Test
    public void testGetLiftieClusterCrn() {
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "partition", "cdp");
        ReflectionTestUtils.setField(regionAwareCrnGenerator, "region", "us-west-1");

        ExternalizedComputeCluster cluster = new ExternalizedComputeCluster();
        cluster.setName("cluster");
        cluster.setEnvironmentCrn("envCrn");
        cluster.setLiftieName("liftie1");
        cluster.setAccountId("account");

        String liftieClusterCrn = clusterService.getLiftieClusterCrn(cluster);
        assertEquals("crn:cdp:compute:us-west-1:account:cluster:liftie1", liftieClusterCrn);
    }

}