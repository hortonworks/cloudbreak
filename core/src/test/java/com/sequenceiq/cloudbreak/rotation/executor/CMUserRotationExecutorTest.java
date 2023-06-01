package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.rotation.context.CMUserRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class CMUserRotationExecutorTest {

    private static final String USER = "user";

    private static final String PASS = "pass";

    private static final String CLIENT_PREFIX = "client";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackService;

    @Mock
    private SecretService secretService;

    @InjectMocks
    private CMUserRotationExecutor underTest;

    @Test
    public void testRotation() throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = setup();
        doNothing().when(clusterSecurityService).createNewUser(anyString(), anyString(), anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        underTest.rotate(rotationContext);

        verify(clusterSecurityService, times(1)).createNewUser(
                eq("old" + USER),
                eq(USER),
                eq(PASS),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testRotationIfVaultCompromised() {
        when(secretService.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.rotate(rotationContext));
    }

    @Test
    public void testRollbackIfVaultCompromised() {
        when(secretService.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.rollback(rotationContext));
    }

    @Test
    public void testFinalizeIfVaultCompromised() {
        when(secretService.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.finalize(rotationContext));
    }

    @Test
    public void testFailedRotation() throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = setup();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService)
                .createNewUser(anyString(), anyString(), anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.rotate(rotationContext));

        verify(clusterSecurityService, times(1)).createNewUser(
                eq("old" + USER),
                eq(USER),
                eq(PASS),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testRollback() throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();

        underTest.rollback(rotationContext);

        verify(clusterSecurityService, times(1)).deleteUser(
                eq(USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testFinalize() throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();

        underTest.finalize(rotationContext);

        verify(clusterSecurityService, times(1)).deleteUser(
                eq("old" + USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    private static CMUserRotationContext getRotationContext() {
        return CMUserRotationContext.builder()
                .withUserSecret(USER)
                .withPasswordSecret(PASS)
                .withClientUserSecret(CLIENT_PREFIX + USER)
                .withClientPasswordSecret(CLIENT_PREFIX + PASS)
                .withResourceCrn("resource")
                .build();
    }

    private ClusterSecurityService setup() {
        lenient().when(stackService.getByCrn(anyString())).thenReturn(new StackDto());
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterSecurityService clusterSecurityService = mock(ClusterSecurityService.class);
        lenient().when(clusterApi.clusterSecurityService()).thenReturn(clusterSecurityService);
        lenient().when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(clusterApi);
        lenient().when(secretService.get(anyString())).thenAnswer(i -> i.getArguments()[0]);
        lenient().when(secretService.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), "old" + i.getArguments()[0]));
        return clusterSecurityService;
    }
}
