package com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.compute.EncryptionSetIdentity;
import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@ExtendWith(MockitoExtension.class)
class DiskEncryptionSetCreationCheckerTaskTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String DISK_ENCRYPTION_SET_NAME = "diskEncryptionSetName";

    private static final String ID = "id";

    private static final String PRINCIPAL_OBJECT_ID = "principalObjectId";

    @Mock
    private AzureClient azureClient;

    private DiskEncryptionSetCreationCheckerTask underTest;

    private AuthenticatedContext authenticatedContext;

    @BeforeEach
    void setUp() {
        authenticatedContext = createAuthenticatedContext(azureClient);
        underTest = new DiskEncryptionSetCreationCheckerTask(authenticatedContext, createCheckerContext());
    }

    private static DiskEncryptionSetCreationCheckerContext createCheckerContext() {
        return new DiskEncryptionSetCreationCheckerContext(RESOURCE_GROUP_NAME, DISK_ENCRYPTION_SET_NAME);
    }

    private static AuthenticatedContext createAuthenticatedContext(AzureClient azureClient) {
        AuthenticatedContext context = mock(AuthenticatedContext.class);
        when(context.getParameter(AzureClient.class)).thenReturn(azureClient);
        return context;
    }

    static Object[][] constructorTestWhenNpeDataProvider() {
        return new Object[][]{
                // testCaseName authenticatedContext, DiskEncryptionSetCreationCheckerContext checkerContext
                {"null, null", null, null},
                {"null, checkerContext", null, createCheckerContext()},
                {"authenticatedContext(null), checkerContext", createAuthenticatedContext(null), createCheckerContext()},
                {"authenticatedContext(azureClient), null", createAuthenticatedContext(mock(AzureClient.class)), null},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("constructorTestWhenNpeDataProvider")
    void constructorTestWhenNpe(String testCaseName, AuthenticatedContext authenticatedContext, DiskEncryptionSetCreationCheckerContext checkerContext) {
        assertThrows(NullPointerException.class, () -> new DiskEncryptionSetCreationCheckerTask(authenticatedContext, checkerContext));
    }

    @Test
    void constructorTestWhenSuccess() {
        assertThat(underTest.getAuthenticatedContext()).isSameAs(authenticatedContext);
    }

    private static DiskEncryptionSetInner createDes(String id, boolean withIdentity, String principalObjectId) {
        DiskEncryptionSetInner des = mock(DiskEncryptionSetInner.class);
        when(des.id()).thenReturn(id);
        if (withIdentity) {
            EncryptionSetIdentity identity = mock(EncryptionSetIdentity.class);
            when(des.identity()).thenReturn(identity);
            when(identity.principalId()).thenReturn(principalObjectId);
        }
        return des;
    }

    static Object[][] completedDataProvider() {
        return new Object[][]{
                // testCaseName des completedExpected
                {"null", null, false},
                {"DES(null, false, null)", createDes(null, false, null), false},
                {"DES(ID, false, null)", createDes(ID, false, null), false},
                {"DES(null, true, null)", createDes(null, true, null), false},
                {"DES(ID, true, null)", createDes(ID, true, null), false},
                {"DES(null, true, PRINCIPAL_OBJECT_ID)", createDes(null, true, PRINCIPAL_OBJECT_ID), false},
                {"DES(ID, true, PRINCIPAL_OBJECT_ID)", createDes(ID, true, PRINCIPAL_OBJECT_ID), true},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("completedDataProvider")
    void completedTest(String testCaseName, DiskEncryptionSetInner des, boolean completedExpected) {
        assertThat(underTest.completed(des)).isEqualTo(completedExpected);
    }

    @Test
    void doCallTestWhenException() {
        RuntimeException e = new RuntimeException();
        when(azureClient.getDiskEncryptionSetByName(RESOURCE_GROUP_NAME, DISK_ENCRYPTION_SET_NAME)).thenThrow(e);

        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> underTest.doCall());

        assertThat(runtimeException).isSameAs(e);
    }

    @Test
    void doCallTestWhenSuccess() {
        DiskEncryptionSetInner des = mock(DiskEncryptionSetInner.class);
        when(azureClient.getDiskEncryptionSetByName(RESOURCE_GROUP_NAME, DISK_ENCRYPTION_SET_NAME)).thenReturn(des);

        DiskEncryptionSetInner result = underTest.doCall();

        assertThat(result).isSameAs(des);
    }

}