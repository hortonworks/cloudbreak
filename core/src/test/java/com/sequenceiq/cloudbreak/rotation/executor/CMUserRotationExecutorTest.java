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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.exception.UnauthorizedException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.CMUserRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
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

    @Mock
    private SecretRotationStepProgressService secretRotationProgressService;

    @InjectMocks
    private CMUserRotationExecutor underTest;

    @BeforeEach
    public void mockProgressService() {
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    public void testRotation() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        doNothing().when(clusterSecurityService).createNewUser(anyString(), anyString(), anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        underTest.executeRotate(rotationContext, null);

        verify(clusterSecurityService, times(1)).createNewUser(
                eq("old" + USER),
                eq(USER),
                eq(PASS),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testPreValidation() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        doNothing().when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        underTest.executePreValidation(rotationContext);

        verify(clusterSecurityService, times(1)).checkUser(
                eq(USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testPreValidationIfUserNotExists() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.executePreValidation(rotationContext));

        verify(clusterSecurityService, times(1)).checkUser(
                eq(USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testPostValidationIfVaultCorrupted() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        when(secretService.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext));

        verifyNoInteractions(clusterSecurityService);
    }

    @Test
    public void testPostValidationIfUserNotExists() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext));

        verify(clusterSecurityService, times(1)).checkUser(
                eq(USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testRotationIfVaultCompromised() {
        when(secretService.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(rotationContext, null));
    }

    @Test
    public void testRollbackIfVaultCompromised() {
        when(secretService.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.executeRollback(rotationContext, null));
    }

    @Test
    public void testFinalizeIfVaultCompromised() {
        when(secretService.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.executeFinalize(rotationContext, null));
    }

    @Test
    public void testFailedRotation() throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = setup();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService)
                .createNewUser(anyString(), anyString(), anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(rotationContext, null));

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

        underTest.executeRollback(rotationContext, null);

        verify(clusterSecurityService, times(1)).deleteUser(
                eq(USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testFinalize() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();

        underTest.executeFinalize(rotationContext, null);

        verify(clusterSecurityService, times(1)).deleteUser(
                eq("old" + USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testPostValidate() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();
        doNothing().when(clusterSecurityService).testUser(anyString(), anyString());

        underTest.executePostValidation(rotationContext);

        verify(clusterSecurityService).checkUser(eq(USER), anyString(), anyString());
        verify(clusterSecurityService).testUser(eq(USER), eq(PASS));
    }

    @Test
    public void testPostValidateIfTestFails() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();
        doNothing().when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());
        doThrow(new UnauthorizedException("something")).when(clusterSecurityService).testUser(anyString(), anyString());

        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext));

        verify(clusterSecurityService).checkUser(eq(USER), anyString(), anyString());
        verify(clusterSecurityService).testUser(eq(USER), eq(PASS));
    }

    @Test
    public void testPostValidateIfCheckFails() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());

        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext));

        verify(clusterSecurityService).checkUser(eq(USER), anyString(), anyString());
        verifyNoMoreInteractions(clusterSecurityService);
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
