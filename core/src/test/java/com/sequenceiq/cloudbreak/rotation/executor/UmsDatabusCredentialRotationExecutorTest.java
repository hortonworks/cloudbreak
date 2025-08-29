package com.sequenceiq.cloudbreak.rotation.executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

@ExtendWith(MockitoExtension.class)
public class UmsDatabusCredentialRotationExecutorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private UmsDatabusCredentialRotationExecutor underTest;

    @Test
    void testRotate() throws Exception {
        mockStack();
        when(grpcUmsClient.generateAccessSecretKeyPair(any(), any(), any())).thenReturn(new AltusCredential("", "".toCharArray()));
        when(altusMachineUserService.getDataBusCredential(any(), any(), any())).thenReturn(new DataBusCredential());
        when(uncachedSecretServiceForRotation.putRotation(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.rotate(new RotationContext(null));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(grpcUmsClient).generateAccessSecretKeyPair(any(), eq("user"), any());
    }

    @Test
    void testFinalizeRotation() throws Exception {
        mockStack();
        mockDeletion();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.finalizeRotation(new RotationContext(null));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(grpcUmsClient).deleteAccessKey(eq("oldAccessKey"), any());
        verify(uncachedSecretServiceForRotation).update(any(), contains("newAccessKey"));
    }

    @Test
    void testRollback() throws Exception {
        mockStack();
        mockDeletion();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.rollback(new RotationContext(null));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(grpcUmsClient).deleteAccessKey(eq("newAccessKey"), any());
        verify(uncachedSecretServiceForRotation).update(any(), contains("oldAccessKey"));
    }

    private void mockDeletion() throws Exception {
        String secret = "{\"machineUserName\":\"user\",\"accessKey\":\"newAccessKey\"}";
        String backup = "{\"machineUserName\":\"backup\",\"accessKey\":\"oldAccessKey\"}";
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(secret, backup));
        doNothing().when(grpcUmsClient).deleteAccessKey(any(), any());
        when(uncachedSecretServiceForRotation.update(any(), any())).thenReturn("");
    }

    private void mockStack() {
        StackDto stack = mock(StackDto.class);
        Cluster cluster = mock(Cluster.class);
        String secret = "{\"machineUserName\":\"user\"}";
        lenient().when(cluster.getDatabusCredential()).thenReturn(secret);
        lenient().when(stack.getStack()).thenReturn(mock(StackView.class));
        when(cluster.getDatabusCredentialSecret()).thenReturn(new Secret(secret, "path"));
        when(stack.getCluster()).thenReturn(cluster);
        when(stackDtoService.getByCrn(any())).thenReturn(stack);
        when(clusterService.getCluster(any())).thenReturn(cluster);
        when(clusterService.save(any())).thenReturn(cluster);
    }
}
