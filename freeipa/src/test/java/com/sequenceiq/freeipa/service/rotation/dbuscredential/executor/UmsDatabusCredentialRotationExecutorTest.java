package com.sequenceiq.freeipa.service.rotation.dbuscredential.executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryConfigService;

@ExtendWith(MockitoExtension.class)
public class UmsDatabusCredentialRotationExecutorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:12474ddc-6e44-4f4c-806a-b197ef12cbb8";

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private StackService stackService;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private TelemetryConfigService telemetryConfigService;

    @InjectMocks
    private UmsDatabusCredentialRotationExecutor underTest;

    @Mock
    private Stack stack;

    @Test
    void testRotate() throws Exception {
        mockStack();
        when(grpcUmsClient.generateAccessSecretKeyPair(any(), any(), any())).thenReturn(new AltusCredential("", "".toCharArray()));
        when(altusMachineUserService.getDataBusCredential(any(), any(), any())).thenReturn(new DataBusCredential());
        when(uncachedSecretServiceForRotation.putRotation(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.rotate(new RotationContext(ENV_CRN));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(grpcUmsClient).generateAccessSecretKeyPair(any(), eq("user"), any());
        verify(uncachedSecretServiceForRotation).putRotation(any(), any());
    }

    @Test
    void testRotateNullPreviously() throws Exception {
        mockStack();
        when(stack.getDatabusCredential()).thenReturn(null);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.rotate(new RotationContext(ENV_CRN));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        verify(uncachedSecretServiceForRotation, never()).putRotation(any(), any());
        verify(altusMachineUserService).getOrCreateDataBusCredentialIfNeeded((Stack) any(), any());
    }

    @Test
    void testFinalizeRotation() throws Exception {
        mockStack();
        mockDeletion();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.finalizeRotation(new RotationContext(ENV_CRN));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(grpcUmsClient).deleteAccessKey(eq("oldAccessKey"), any());
        verify(uncachedSecretServiceForRotation).update(any(), contains("newAccessKey"));
    }

    @Test
    void testFinalizeRotationNoRotation() throws Exception {
        mockStack();
        mockDeletion();
        String secret = "{\"machineUserName\":\"user\",\"accessKey\":\"newAccessKey\"}";
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(secret, null));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.finalizeRotation(new RotationContext(ENV_CRN));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(grpcUmsClient, never()).deleteAccessKey(eq("oldAccessKey"), any());
        verify(uncachedSecretServiceForRotation, never()).update(any(), contains("newAccessKey"));
    }

    @Test
    void testRollback() throws Exception {
        mockStack();
        mockDeletion();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.rollback(new RotationContext(ENV_CRN));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(grpcUmsClient).deleteAccessKey(eq("newAccessKey"), any());
        verify(uncachedSecretServiceForRotation).update(any(), contains("oldAccessKey"));
    }

    @Test
    void testRollbackNoRotation() throws Exception {
        mockStack();
        mockDeletion();
        String secret = "{\"machineUserName\":\"user\",\"accessKey\":\"newAccessKey\"}";
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(secret, null));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.rollback(new RotationContext(ENV_CRN));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        verify(grpcUmsClient, never()).deleteAccessKey(eq("newAccessKey"), any());
        verify(uncachedSecretServiceForRotation, never()).update(any(), contains("oldAccessKey"));
    }

    private void mockDeletion() throws Exception {
        String secret = "{\"machineUserName\":\"user\",\"accessKey\":\"newAccessKey\"}";
        String backup = "{\"machineUserName\":\"backup\",\"accessKey\":\"oldAccessKey\"}";
        when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(secret, backup));
        lenient().doNothing().when(grpcUmsClient).deleteAccessKey(any(), any());
        lenient().when(uncachedSecretServiceForRotation.update(any(), any())).thenReturn("");
    }

    private void mockStack() {
        String secret = "{\"machineUserName\":\"user\"}";
        lenient().when(stack.getDatabusCredential()).thenReturn(secret);
        lenient().when(stack.getDatabusCredentialSecret()).thenReturn(new Secret(secret, "path"));
        lenient().when(stackService.getByEnvironmentCrnAndAccountId(any(), any())).thenReturn(stack);
    }
}
