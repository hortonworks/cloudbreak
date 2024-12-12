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

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
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
import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.CMUserRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class CMUserRotationExecutorTest {

    private static final String USER = "user";

    private static final String PASS = "pass";

    private static final String SECOND_USER_POSTFIX = "2";

    private static final String CLIENT_PREFIX = "client";

    private static final String BACKUP_SECRET_PREFIX = "old";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDtoService stackService;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private CMUserRotationExecutor underTest;

    @Test
    public void testRotation() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        doNothing().when(clusterSecurityService).createNewUser(anyString(), anyString(), anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        underTest.executeRotate(rotationContext, null);

        verify(clusterSecurityService, times(1)).createNewUser(
                eq(BACKUP_SECRET_PREFIX + USER),
                eq(USER),
                eq(PASS),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + USER),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + PASS));
        verify(clusterSecurityService, times(1)).createNewUser(
                eq(BACKUP_SECRET_PREFIX + USER + SECOND_USER_POSTFIX),
                eq(USER + SECOND_USER_POSTFIX),
                eq(PASS + SECOND_USER_POSTFIX),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + USER),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + PASS));
    }

    @Test
    public void testPreValidation() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        doNothing().when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        underTest.executePreValidation(rotationContext, null);

        verify(clusterSecurityService, times(1)).checkUser(
                eq(USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
        verify(clusterSecurityService, times(1)).checkUser(
                eq(USER + SECOND_USER_POSTFIX),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testPreValidationIfUserNotExists() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.executePreValidation(rotationContext, null));

        verify(clusterSecurityService, times(1)).checkUser(
                any(),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testPostValidationIfVaultCorrupted() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext, null));

        verifyNoInteractions(clusterSecurityService);
    }

    @Test
    public void testPostValidationIfUserNotExists() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext, null));

        verify(clusterSecurityService, times(1)).checkUser(
                any(),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testRotationIfVaultCompromised() {
        setup();
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(rotationContext,
                RotationMetadataTestUtil.metadataForRotation("resource", null)));
    }

    @Test
    public void testRollbackIfVaultCompromised() {
        setup();
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.executeRollback(rotationContext,
                RotationMetadataTestUtil.metadataForRollback("resource", null)));
    }

    @Test
    public void testFinalizeIfVaultCompromised() {
        setup();
        when(uncachedSecretServiceForRotation.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), null));
        CMUserRotationContext rotationContext = getRotationContext();

        assertThrows(SecretRotationException.class, () -> underTest.executeFinalize(rotationContext,
                RotationMetadataTestUtil.metadataForFinalize("resource", null)));
    }

    @Test
    public void testFailedRotation() throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = setup();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService)
                .createNewUser(anyString(), anyString(), anyString(), anyString(), anyString());

        CMUserRotationContext rotationContext = getRotationContext();
        assertThrows(SecretRotationException.class, () -> underTest.executeRotate(rotationContext,
                RotationMetadataTestUtil.metadataForRotation("resource", null)));

        verify(clusterSecurityService, times(1)).createNewUser(
                any(),
                any(),
                any(),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + USER),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + PASS));
    }

    @Test
    public void testRollback() throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();

        underTest.executeRollback(rotationContext, RotationMetadataTestUtil.metadataForRollback("resource", null));

        verify(clusterSecurityService, times(1)).deleteUser(
                eq(USER),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + USER),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + PASS));
        verify(clusterSecurityService, times(1)).deleteUser(
                eq(USER + SECOND_USER_POSTFIX),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + USER),
                eq(BACKUP_SECRET_PREFIX + CLIENT_PREFIX + PASS));
    }

    @Test
    public void testFinalize() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();

        underTest.executeFinalize(rotationContext, RotationMetadataTestUtil.metadataForFinalize("resource", null));

        verify(clusterSecurityService, times(1)).deleteUser(
                eq(BACKUP_SECRET_PREFIX + USER),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
        verify(clusterSecurityService, times(1)).deleteUser(
                eq(BACKUP_SECRET_PREFIX + USER + SECOND_USER_POSTFIX),
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS));
    }

    @Test
    public void testPostValidate() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();
        doNothing().when(clusterSecurityService).testUser(anyString(), anyString());

        underTest.executePostValidation(rotationContext, null);

        verify(clusterSecurityService).checkUser(eq(USER), anyString(), anyString());
        verify(clusterSecurityService).testUser(eq(USER), eq(PASS));
        verify(clusterSecurityService).checkUser(eq(USER + SECOND_USER_POSTFIX), anyString(), anyString());
        verify(clusterSecurityService).testUser(eq(USER + SECOND_USER_POSTFIX), eq(PASS + SECOND_USER_POSTFIX));
    }

    @Test
    public void testPostValidateIfTestFails() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();
        doNothing().when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());
        doThrow(new UnauthorizedException("something")).when(clusterSecurityService).testUser(anyString(), anyString());

        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext, null));

        verify(clusterSecurityService).checkUser(any(), anyString(), anyString());
        verify(clusterSecurityService).testUser(any(), any());
    }

    @Test
    public void testPostValidateIfCheckFails() throws Exception {
        ClusterSecurityService clusterSecurityService = setup();
        CMUserRotationContext rotationContext = getRotationContext();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService).checkUser(anyString(), anyString(), anyString());

        assertThrows(SecretRotationException.class, () -> underTest.executePostValidation(rotationContext, null));

        verify(clusterSecurityService).checkUser(any(), anyString(), anyString());
        verifyNoMoreInteractions(clusterSecurityService);
    }

    private static CMUserRotationContext getRotationContext() {
        return CMUserRotationContext.builder()
                .withRotatableSecrets(Set.of(Pair.of(USER, PASS),
                        Pair.of(USER + SECOND_USER_POSTFIX, PASS + SECOND_USER_POSTFIX)))
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
        lenient().when(uncachedSecretServiceForRotation.get(anyString())).thenAnswer(i -> i.getArguments()[0]);
        lenient().when(uncachedSecretServiceForRotation.getRotation(anyString())).thenAnswer(i ->
                new RotationSecret(String.valueOf(i.getArguments()[0]), BACKUP_SECRET_PREFIX + i.getArguments()[0]));
        return clusterSecurityService;
    }
}
