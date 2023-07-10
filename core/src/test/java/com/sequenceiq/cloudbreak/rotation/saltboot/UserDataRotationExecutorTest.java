package com.sequenceiq.cloudbreak.rotation.saltboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.UserDataRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationProgressService;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataSecretModifier;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class UserDataRotationExecutorTest {

    private static final Map<InstanceGroupType, String> USER_DATA = Map.of(InstanceGroupType.GATEWAY, "userdata");

    private static final String TEST_A_PROPERTY = "TEST_A";

    private static final String TEST_B_PROPERTY = "TEST_B";

    private static final String TEST_C_PROPERTY = "TEST_C";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:accountId:cluster:resourceId";

    @Mock
    private StackDtoService stackService;

    @Mock
    private SecretService secretService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Spy
    private Optional<List<UserDataSecretModifier>> userDataModifiers = Optional.of(List.of(new TestAModifier(), new TestBModifier(), new TestCModifier()));

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Resource resource;

    @Mock
    private CloudResource cloudResource;

    @InjectMocks
    private UserDataRotationExecutor underTest;

    @Mock
    private StackDto stack;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private SecretRotationProgressService secretRotationProgressService;

    @BeforeEach
    public void setUp() throws IllegalAccessException {
        lenient().when(cloudStackConverter.convert(any())).thenReturn(cloudStack);
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.of(secretRotationProgressService), true);
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    public void rotateUpdatesRequestPropertiesOnly() throws Exception {
        when(stack.getId()).thenReturn(1L);
        when(stackService.getByCrn(anyString())).thenReturn(stack);
        setupCloudMocks("AWS::EC2::LaunchTemplate");

        when(userDataService.getUserData(anyLong()))
                .thenReturn(Map.of(
                        InstanceGroupType.CORE, """
                                export TEST_A=A1
                                export TEST_B=B1
                                export TEST_C=C1
                                """,
                        InstanceGroupType.GATEWAY, """
                                export TEST_A=A1
                                export TEST_B=B1
                                export TEST_C=C1
                                """));
        when(secretService.getRotation("path/secretA")).thenReturn(new RotationSecret("A2", "A1"));
        when(secretService.getRotation("path/secretC")).thenReturn(new RotationSecret("C2", "C1"));

        underTest.rotate(new UserDataRotationContext(RESOURCE_CRN,
                List.of(Pair.of(new TestAModifier(), "path/secretA"), Pair.of(new TestCModifier(), "path/secretC"))));

        verify(userDataService).createOrUpdateUserData(1L, Map.of(
                InstanceGroupType.CORE, """
                        export TEST_A=A2
                        export TEST_B=B1
                        export TEST_C=C2
                        """,
                InstanceGroupType.GATEWAY, """
                        export TEST_A=A2
                        export TEST_B=B1
                        export TEST_C=C2
                        """));
        verify(resourceConnector).updateUserData(eq(authenticatedContext), eq(cloudStack), eq(List.of(cloudResource)), anyMap());
    }

    @Test
    public void rotateThrowsExceptionIfSecretsAreNotRotated() {
        when(secretService.getRotation("path/secretA")).thenReturn(new RotationSecret("A1", null));

        SecretRotationException exception = assertThrows(SecretRotationException.class,
                () -> underTest.rotate(new UserDataRotationContext(RESOURCE_CRN, List.of(Pair.of(new TestAModifier(), "path/secretA")))));

        assertEquals(CommonSecretRotationStep.USER_DATA, exception.getFailedRotationStep());
        assertEquals("Secret is not in a rotated state. User data modification failed.", exception.getMessage());
        verifyNoMoreInteractions(userDataService);
    }

    @Test
    public void rollbackUpdatesRequestPropertiesOnly() {
        when(stack.getId()).thenReturn(1L);
        when(stackService.getByCrn(anyString())).thenReturn(stack);

        when(userDataService.getUserData(anyLong()))
                .thenReturn(Map.of(
                        InstanceGroupType.CORE, """
                                export TEST_A=A2
                                export TEST_B=B1
                                export TEST_C=C2
                                """,
                        InstanceGroupType.GATEWAY, """
                                export TEST_A=A2
                                export TEST_B=B1
                                export TEST_C=C2
                                """));
        when(secretService.getRotation("path/secretA")).thenReturn(new RotationSecret("A2", "A1"));
        when(secretService.getRotation("path/secretC")).thenReturn(new RotationSecret("C2", "C1"));

        underTest.rollback(new UserDataRotationContext(RESOURCE_CRN,
                List.of(Pair.of(new TestAModifier(), "path/secretA"), Pair.of(new TestCModifier(), "path/secretC"))));

        verify(userDataService).createOrUpdateUserData(1L, Map.of(
                InstanceGroupType.CORE, """
                        export TEST_A=A1
                        export TEST_B=B1
                        export TEST_C=C1
                        """,
                InstanceGroupType.GATEWAY, """
                        export TEST_A=A1
                        export TEST_B=B1
                        export TEST_C=C1
                        """));
    }

    @Test
    public void rollbackThrowsExceptionIfSecretsAreNotRotated() {
        when(secretService.getRotation("path/secretA")).thenReturn(new RotationSecret("A1", null));

        SecretRotationException exception = assertThrows(SecretRotationException.class,
                () -> underTest.rotate(new UserDataRotationContext(RESOURCE_CRN, List.of(Pair.of(new TestAModifier(), "path/secretA")))));

        assertEquals(CommonSecretRotationStep.USER_DATA, exception.getFailedRotationStep());
        assertEquals("Secret is not in a rotated state. User data modification failed.", exception.getMessage());
        verifyNoMoreInteractions(userDataService);
    }

    private void setupCloudMocks(String template) {
        when(stackService.getByCrn(anyString())).thenReturn(stack);
        when(stack.getCreator()).thenReturn(new User());
        when(stack.getWorkspace()).thenReturn(new Workspace());
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(stackUtil.getCloudCredential(any())).thenReturn(cloudCredential);
        when(cloudStackConverter.convert(any())).thenReturn(cloudStack);
        when(cloudStack.getTemplate()).thenReturn(template);

        lenient().when(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(any(), any(), anyLong())).thenReturn(List.of(resource));
        lenient().when(resourceToCloudResourceConverter.convert(any())).thenReturn(cloudResource);

        lenient().when(userDataService.getUserData(anyLong())).thenReturn(USER_DATA);

        lenient().when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        lenient().when(cloudConnector.authentication()).thenReturn(authenticator);
        lenient().when(authenticator.authenticate(any(), any())).thenReturn(authenticatedContext);
        lenient().when(cloudConnector.resources()).thenReturn(resourceConnector);
    }

    static class TestAModifier implements UserDataSecretModifier {

        @Override
        public void modify(UserDataReplacer userData, String secretValue) {
            userData.replace(TEST_A_PROPERTY, secretValue);
        }
    }

    static class TestBModifier implements UserDataSecretModifier {

        @Override
        public void modify(UserDataReplacer userData, String secretValue) {
            userData.replace(TEST_B_PROPERTY, secretValue);
        }
    }

    static class TestCModifier implements UserDataSecretModifier {

        @Override
        public void modify(UserDataReplacer userData, String secretValue) {
            userData.replace(TEST_C_PROPERTY, secretValue);
        }
    }
}