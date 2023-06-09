package com.sequenceiq.freeipa.service.rotation;

import static com.sequenceiq.cloudbreak.rotation.secret.step.CommonSecretRotationStep.USER_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.rotation.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationProgressService;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataSecretModifier;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;
import com.sequenceiq.freeipa.service.rotation.executor.UserDataRotationExecutor;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UserDataRotationExecutorTest {

    private static final String TEST_A_PROPERTY = "TEST_A";

    private static final String TEST_B_PROPERTY = "TEST_B";

    private static final String TEST_C_PROPERTY = "TEST_C";

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:accountId:environment:resourceId";

    @Mock
    private StackService stackService;

    @Mock
    private SecretService secretService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @InjectMocks
    private UserDataRotationExecutor underTest;

    @Mock
    private Stack stack;

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
    public void rotateUpdatesRequestPropertiesOnly() {
        when(stack.getId()).thenReturn(1L);
        when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);


        when(secretService.getRotation("path/secretA")).thenReturn(new RotationSecret("A2", "A1"));
        when(secretService.getRotation("path/secretC")).thenReturn(new RotationSecret("C2", "C1"));

        underTest.rotate(new UserDataRotationContext(RESOURCE_CRN, List.of(
                Pair.of(new TestAModifier(), "path/secretA"), Pair.of(new TestCModifier(), "path/secretC"))));

        verify(userDataService).updateUserData(eq(1L), any());
    }

    @Test
    public void rotateThrowsExceptionIfSecretsAreNotRotated() {
        when(secretService.getRotation("path/secretA")).thenReturn(new RotationSecret("A1", null));

        SecretRotationException exception = assertThrows(SecretRotationException.class,
                () -> underTest.rotate(new UserDataRotationContext(RESOURCE_CRN, List.of(Pair.of(new TestAModifier(), "path/secretA")))));

        assertEquals(USER_DATA, exception.getFailedRotationStep());
        assertEquals("Secret is not in a rotated state. User data modification failed.", exception.getMessage());
        verifyNoMoreInteractions(userDataService);
    }

    @Test
    public void rollbackUpdatesRequestPropertiesOnly() {
        when(stack.getId()).thenReturn(1L);
        when(stackService.getByEnvironmentCrnAndAccountId(anyString(), anyString())).thenReturn(stack);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(anyString(), anyString())).thenReturn(stack);

        when(secretService.getRotation("path/secretA")).thenReturn(new RotationSecret("A2", "A1"));
        when(secretService.getRotation("path/secretC")).thenReturn(new RotationSecret("C2", "C1"));

        underTest.rotate(new UserDataRotationContext(RESOURCE_CRN, List.of(
                Pair.of(new TestAModifier(), "path/secretA"), Pair.of(new TestCModifier(), "path/secretC"))));

        verify(userDataService).updateUserData(eq(1L), any());
    }

    @Test
    public void rollbackThrowsExceptionIfSecretsAreNotRotated() {
        when(secretService.getRotation("path/secretA")).thenReturn(new RotationSecret("A1", null));

        SecretRotationException exception = assertThrows(SecretRotationException.class,
                () -> underTest.rotate(new UserDataRotationContext(RESOURCE_CRN, List.of(Pair.of(new TestAModifier(), "path/secretA")))));

        assertEquals(USER_DATA, exception.getFailedRotationStep());
        assertEquals("Secret is not in a rotated state. User data modification failed.", exception.getMessage());
        verifyNoMoreInteractions(userDataService);
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