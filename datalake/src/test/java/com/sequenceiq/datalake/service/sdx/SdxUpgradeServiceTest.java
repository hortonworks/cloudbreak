package com.sequenceiq.datalake.service.sdx;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
public class SdxUpgradeServiceTest {

    @InjectMocks
    private SdxUpgradeService underTest;

    @Mock
    private SdxService sdxService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Captor
    private ArgumentCaptor<SdxCluster> sdxClusterArgumentCaptor;

    private SdxCluster sdxCluster;

    private SdxCluster detachedSdxCluster;

    @BeforeEach
    public void setUp() {
        sdxCluster = getValidSdxCluster();
        detachedSdxCluster = getDetachedSdxCluster();
    }

    @Test
    @DisplayName("Test if the runtime is properly updated")
    public void testUpdateRuntimeVersionFromCloudbreak() {
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        StackV4Response stackV4Response = getStackV4Response();
        when(stackV4Endpoint.get(eq(0L), eq("test-sdx-cluster"), eq(Set.of()), anyString()))
                .thenReturn(stackV4Response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.updateRuntimeVersionFromCloudbreak(1L);

        verify(sdxService, times(1)).updateRuntimeVersionFromStackResponse(eq(sdxCluster), eq(stackV4Response));

        when(sdxService.getById(1L)).thenReturn(detachedSdxCluster);
        stackV4Response = getStackV4Response();
        when(stackV4Endpoint.get(eq(0L), eq("test-sdx-cluster"), eq(Set.of()), anyString()))
                .thenReturn(stackV4Response);

        underTest.updateRuntimeVersionFromCloudbreak(1L);
        verify(sdxService, times(1)).updateRuntimeVersionFromStackResponse(eq(detachedSdxCluster), eq(stackV4Response));
    }

    @Test
    @DisplayName("Test if the runtime cannot be updated when there is no CDH product installed")
    public void testUpdateRuntimeVersionFromCloudbreakWithoutCDH() {
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        StackV4Response stackV4Response = getStackV4Response();
        ClouderaManagerProductV4Response spark3 = new ClouderaManagerProductV4Response();
        spark3.setName("SPARK3");
        spark3.setVersion("3.0.0.2.99.7110.0-18-1.p0.3525631");
        stackV4Response.getCluster().getCm().setProducts(List.of(spark3));
        when(stackV4Endpoint.get(eq(0L), eq("test-sdx-cluster"), eq(Set.of()), anyString()))
                .thenReturn(stackV4Response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.updateRuntimeVersionFromCloudbreak(1L);

        verify(sdxService, times(0)).save(any());
    }

    @Test
    @DisplayName("Test if the runtime cannot be updated when there is no CM installed")
    public void testUpdateRuntimeVersionFromCloudbreakWithoutCM() {
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        StackV4Response stackV4Response = getStackV4Response();
        stackV4Response.getCluster().setCm(null);
        when(stackV4Endpoint.get(eq(0L), eq("test-sdx-cluster"), eq(Set.of()), anyString()))
                .thenReturn(stackV4Response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.updateRuntimeVersionFromCloudbreak(1L);

        verify(sdxService, times(0)).save(any());
    }

    @Test
    @DisplayName("Test if the runtime cannot be updated when there is no cluster")
    public void testUpdateRuntimeVersionFromCloudbreakWithoutCluster() {
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        StackV4Response stackV4Response = getStackV4Response();
        stackV4Response.setCluster(null);
        when(stackV4Endpoint.get(eq(0L), eq("test-sdx-cluster"), eq(Set.of()), anyString()))
                .thenReturn(stackV4Response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.updateRuntimeVersionFromCloudbreak(1L);

        verify(sdxService, times(0)).save(any());
    }

    @Test
    @DisplayName("Test if the runtime cannot be updated when there is no CDP version specified")
    public void testUpdateRuntimeVersionFromCloudbreakWithoutCDHVersion() {
        when(sdxService.getById(1L)).thenReturn(sdxCluster);
        StackV4Response stackV4Response = getStackV4Response();
        ClouderaManagerProductV4Response cdp = new ClouderaManagerProductV4Response();
        cdp.setName("CDH");
        stackV4Response.getCluster().getCm().setProducts(List.of(cdp));
        when(stackV4Endpoint.get(eq(0L), eq("test-sdx-cluster"), eq(Set.of()), anyString()))
                .thenReturn(stackV4Response);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        underTest.updateRuntimeVersionFromCloudbreak(1L);

        verify(sdxService, times(0)).save(any());
    }

    private StackV4Response getStackV4Response() {
        ClouderaManagerProductV4Response cdp = new ClouderaManagerProductV4Response();
        cdp.setName("CDH");
        cdp.setVersion("7.2.1-1.cdh7.2.0.p0.3758356");

        ClouderaManagerProductV4Response cfm = new ClouderaManagerProductV4Response();
        cfm.setName("CFM");
        cfm.setVersion("2.0.0.0");

        ClouderaManagerProductV4Response spark3 = new ClouderaManagerProductV4Response();
        spark3.setName("SPARK3");
        spark3.setVersion("3.0.0.2.99.7110.0-18-1.p0.3525631");

        ClouderaManagerV4Response cm = new ClouderaManagerV4Response();
        cm.setProducts(List.of(cdp, cfm, spark3));

        ClusterV4Response clusterV4Response = new ClusterV4Response();
        clusterV4Response.setCm(cm);

        StackV4Response stackV4Response = new StackV4Response();
        stackV4Response.setName("test-sdx-cluster");
        stackV4Response.setCluster(clusterV4Response);

        return stackV4Response;
    }

    private SdxCluster getValidSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setRuntime("7.2.0");
        sdxCluster.setId(1L);
        sdxCluster.setAccountId("accountid");
        sdxCluster.setDetached(false);
        return sdxCluster;
    }

    private SdxCluster getDetachedSdxCluster() {
        SdxCluster sdxCluster = getValidSdxCluster();
        sdxCluster.setDetached(true);
        return sdxCluster;
    }
}
