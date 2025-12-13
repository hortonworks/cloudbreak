package com.sequenceiq.cloudbreak.structuredevent.converter;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
class ClusterToClusterDetailsConverterTest {

    @Mock
    private RdsConfigToRdsDetailsConverter rdsConfigToRdsDetailsConverter;

    @Mock
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @InjectMocks
    private ClusterToClusterDetailsConverter underTest;

    @Test
    void convertWithoutRDS() {
        when(rdsConfigWithoutClusterService.findByClusterId(any())).thenReturn(Set.of());
        ClusterDetails result = underTest.convert(getClusterView(), getStackView(), getGatewayView());
        assertAll(
                () -> assertEquals(1L, result.getId()),
                () -> assertEquals("name", result.getName()),
                () -> assertEquals("description", result.getDescription()),
                () -> assertEquals("AVAILABLE", result.getStatus()),
                () -> assertEquals("ok", result.getStatusReason()),
                () -> assertTrue(result.isRazEnabled()),
                () -> assertTrue(result.isRmsEnabled()),
                () -> assertTrue(result.getGatewayEnabled()),
                () -> assertEquals("CENTRAL", result.getGatewayType()),
                () -> assertEquals("SSO_PROVIDER", result.getSsoType()),
                () -> assertEquals("S3", result.getFileSystemType())
        );
    }

    @Test
    void convertWithoutRDSAndGateway() {
        when(rdsConfigWithoutClusterService.findByClusterId(any())).thenReturn(Set.of());
        ClusterDetails result = underTest.convert(getClusterView(), getStackView(), null);
        assertAll(
                () -> assertEquals(1L, result.getId()),
                () -> assertEquals("name", result.getName()),
                () -> assertEquals("description", result.getDescription()),
                () -> assertEquals("AVAILABLE", result.getStatus()),
                () -> assertEquals("ok", result.getStatusReason()),
                () -> assertTrue(result.isRazEnabled()),
                () -> assertTrue(result.isRmsEnabled()),
                () -> assertFalse(result.getGatewayEnabled()),
                () -> assertEquals("S3", result.getFileSystemType())
        );
    }

    private ClusterView getClusterView() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setCreationStarted(Instant.now().toEpochMilli());
        cluster.setCreationFinished(Instant.now().toEpochMilli());
        cluster.setUpSince(Instant.now().toEpochMilli());
        cluster.setAutoTlsEnabled(true);
        cluster.setFqdn("fqdn");
        cluster.setClusterManagerIp("127.0.0.1");
        cluster.setName("name");
        cluster.setDescription("description");
        cluster.setDatabaseServerCrn("crn");
        cluster.setRangerRazEnabled(true);
        cluster.setRangerRmsEnabled(true);
        cluster.setCertExpirationState(CertExpirationState.VALID);
        cluster.setEnvironmentCrn("crn");
        cluster.setProxyConfigCrn("");
        cluster.setVariant("");
        FileSystem fileSystem = new FileSystem();
        fileSystem.setType(FileSystemType.S3);
        cluster.setFileSystem(fileSystem);
        return cluster;
    }

    private StackView getStackView() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setRegion("us-west-2");
        stack.setResourceCrn("crn");
        stack.setName("name");
        stack.setGatewayPort(80);
        stack.setEnvironmentCrn("crn");
        stack.setType(StackType.DATALAKE);
        stack.setStackStatus(new StackStatus(stack, Status.AVAILABLE, "ok", DetailedStackStatus.AVAILABLE));
        return stack;
    }

    private GatewayView getGatewayView() {
        Gateway gateway = new Gateway();
        gateway.setGatewayPort(80);
        gateway.setGatewayType(GatewayType.CENTRAL);
        gateway.setSsoType(SSOType.SSO_PROVIDER);
        return gateway;
    }

}
