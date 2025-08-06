package com.sequenceiq.cloudbreak.reactor.handler.cluster.certrotate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCMCARotationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCMCARotationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate.ClusterCertificatesRotationFailed;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class ClusterCMCARotationHandlerTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private SecretRotationSaltService saltService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private ClusterCMCARotationHandler underTest;

    @Test
    void testHandlerWhenCMCARotationNotNeeded() {
        underTest.doAccept(new HandlerEvent<>(new Event<>(new ClusterCMCARotationRequest(1L, CertificateRotationType.HOST_CERTS))));

        verifyNoInteractions(stackDtoService, uncachedSecretServiceForRotation, clusterHostServiceRunner, saltService);
    }

    @Test
    void testCMCARotation() throws Exception {
        mockCmcaRotation();
        doNothing().when(saltService).executeSaltState(any(), any(), any());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(new ClusterCMCARotationRequest(1L, CertificateRotationType.ALL))));

        assertEquals(selectable.getClass(), ClusterCMCARotationSuccess.class);
        verify(uncachedSecretServiceForRotation, times(2)).update(any(), any());
        verify(saltService).updateSaltPillar(any(), any());
        verify(saltService).executeSaltState(any(), any(), any());
    }

    @Test
    void testFailedCMCARotation() throws Exception {
        mockCmcaRotation();
        doThrow(CloudbreakServiceException.class).when(saltService).executeSaltState(any(), any(), any());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(new ClusterCMCARotationRequest(1L, CertificateRotationType.ALL))));

        assertEquals(selectable.getClass(), ClusterCertificatesRotationFailed.class);
        verify(uncachedSecretServiceForRotation, times(2)).update(any(), any());
        verify(saltService).updateSaltPillar(any(), any());
        verify(saltService).executeSaltState(any(), any(), eq(List.of("cloudera.manager.rotate.cmca-renewal")));
    }

    private void mockCmcaRotation() throws Exception {
        when(uncachedSecretServiceForRotation.update(any(), any())).thenReturn("secret");
        doNothing().when(saltService).updateSaltPillar(any(), any());
        when(clusterHostServiceRunner.getClouderaManagerAutoTlsPillarProperties(any())).thenReturn(new SaltPillarProperties(null, null));
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);
        InstanceMetadataView imdView = mock(InstanceMetadataView.class);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(clusterService.getCluster(any())).thenReturn(cluster);
        when(cluster.getId()).thenReturn(1L);
        when(cluster.getKeyStorePwdSecret()).thenReturn(new Secret("raw", "secret1"));
        when(cluster.getTrustStorePwdSecret()).thenReturn(new Secret("raw", "secret2"));
        doNothing().when(cluster).setTrustStorePwdSecret(any());
        doNothing().when(cluster).setKeyStorePwdSecret(any());
        when(clusterService.save(cluster)).thenReturn(cluster);
        when(stackDto.getPrimaryGatewayInstance()).thenReturn(imdView);
        when(imdView.getDiscoveryFQDN()).thenReturn("host");
        when(stackDtoService.getById(any())).thenReturn(stackDto);
    }
}
