package com.sequenceiq.cloudbreak.rotation.cm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
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
        doNothing().when(clusterSecurityService).updateExistingUser(anyString(), anyString(), anyString(), anyString());

        RotationContext rotationContext = RotationContext.contextBuilder()
                .withUserPasswordSecrets(Map.of(USER, PASS))
                .withClientUserSecretSupplier(() -> CLIENT_PREFIX + USER)
                .withClientPasswordSecretSupplier(() -> CLIENT_PREFIX + PASS)
                .withResourceCrn("resource")
                .build();
        underTest.rotate(rotationContext);

        verify(secretService, times(4)).get(anyString());
        verify(clusterSecurityService, times(1)).updateExistingUser(
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS),
                eq(USER),
                eq(PASS));
    }

    @Test
    public void testFailedRotation() throws CloudbreakException {
        ClusterSecurityService clusterSecurityService = setup();
        doThrow(new CloudbreakException("something")).when(clusterSecurityService).updateExistingUser(anyString(), anyString(), anyString(), anyString());

        RotationContext rotationContext = RotationContext.contextBuilder()
                .withUserPasswordSecrets(Map.of(USER, PASS))
                .withClientUserSecretSupplier(() -> CLIENT_PREFIX + USER)
                .withClientPasswordSecretSupplier(() -> CLIENT_PREFIX + PASS)
                .withResourceCrn("resource")
                .build();
        Assert.assertThrows(SecretRotationException.class, () -> underTest.rotate(rotationContext));

        verify(secretService, times(4)).get(anyString());
        verify(clusterSecurityService, times(1)).updateExistingUser(
                eq(CLIENT_PREFIX + USER),
                eq(CLIENT_PREFIX + PASS),
                eq(USER),
                eq(PASS));
    }

    private ClusterSecurityService setup() {
        when(stackService.getByCrn(anyString())).thenReturn(new StackDto());
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterSecurityService clusterSecurityService = mock(ClusterSecurityService.class);
        when(clusterApi.clusterSecurityService()).thenReturn(clusterSecurityService);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(clusterApi);
        when(secretService.get(eq(USER))).thenReturn(USER);
        when(secretService.get(eq(PASS))).thenReturn(PASS);
        when(secretService.get(eq(CLIENT_PREFIX + USER))).thenReturn(CLIENT_PREFIX + USER);
        when(secretService.get(eq(CLIENT_PREFIX + PASS))).thenReturn(CLIENT_PREFIX + PASS);
        return clusterSecurityService;
    }
}
