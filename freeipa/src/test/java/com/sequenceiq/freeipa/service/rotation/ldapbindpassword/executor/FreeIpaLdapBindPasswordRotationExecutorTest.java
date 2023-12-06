package com.sequenceiq.freeipa.service.rotation.ldapbindpassword.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.v1.LdapConfigV1Service;
import com.sequenceiq.freeipa.service.binduser.LdapBindUserNameProvider;
import com.sequenceiq.freeipa.service.binduser.UserSyncBindUserService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.rotation.ldapbindpassword.context.FreeIpaLdapBindPasswordRotationContext;
import com.sequenceiq.freeipa.service.rotation.ldapbindpassword.context.FreeIpaLdapBindPasswordRotationContext.FreeipaLdapBindPasswordRotationContextBuilder;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaLdapBindPasswordRotationExecutorTest {

    private static final String BIND_PASSWORD_SECRET = "bindPasswordSecret";

    private static final String NEW_PASSWORD = "new-password";

    private static final String OLD_PASSWORD = "old-password";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:cloudera:environment:123456-postfix";

    private static final String CLUSTER_NAME = "clusterName";

    @Mock
    private StackService stackService;

    @Mock
    private SecretService secretService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private LdapConfigV1Service ldapConfigService;

    @Mock
    private LdapBindUserNameProvider userNameProvider;

    @Mock
    private UserSyncBindUserService userSyncBindUserService;

    @InjectMocks
    private FreeIpaLdapBindPasswordRotationExecutor rotationExecutor;

    @Mock
    private Stack stack;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Test
    void rotateShouldThrowExceptionWhenNotInRotationState() {
        RotationSecret rotationSecret = new RotationSecret(NEW_PASSWORD, null);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        when(secretService.getRotation(eq(BIND_PASSWORD_SECRET))).thenReturn(rotationSecret);

        SecretRotationException e = assertThrows(SecretRotationException.class,
                () -> rotationExecutor.rotate(getContext(true, false)));
        assertEquals("Freeipa LDAP bind password is not in rotation state in Vault, thus rotation is not possible.",
                e.getMessage());
    }

    @Test
    void rotateShouldBeSuccessful() throws Exception {
        RotationSecret rotationSecret = new RotationSecret(NEW_PASSWORD, OLD_PASSWORD);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        when(secretService.getRotation(eq(BIND_PASSWORD_SECRET))).thenReturn(rotationSecret);
        when(freeIpaClientFactory.getFreeIpaClientForStack(eq(stack))).thenReturn(freeIpaClient);
        when(userNameProvider.createBindUserName(any())).thenReturn(CLUSTER_NAME);

        rotationExecutor.rotate(getContext(true, false));
        ArgumentCaptor<String> userNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(freeIpaClient, times(1)).userShow(userNameCaptor.capture());
        assertEquals(CLUSTER_NAME, userNameCaptor.getValue());
        verify(ldapConfigService, times(1)).setBindUserPassword(eq(freeIpaClient), any(), eq(NEW_PASSWORD));
    }

    @Test
    void rollbackShouldBeSuccessful() throws Exception {
        RotationSecret rotationSecret = new RotationSecret(NEW_PASSWORD, OLD_PASSWORD);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        when(secretService.getRotation(eq(BIND_PASSWORD_SECRET))).thenReturn(rotationSecret);
        when(freeIpaClientFactory.getFreeIpaClientForStack(eq(stack))).thenReturn(freeIpaClient);
        when(userNameProvider.createBindUserName(any())).thenReturn(CLUSTER_NAME);

        rotationExecutor.rollback(getContext(true, false));
        ArgumentCaptor<String> userNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(freeIpaClient, times(1)).userShow(userNameCaptor.capture());
        assertEquals(CLUSTER_NAME, userNameCaptor.getValue());
        verify(ldapConfigService, times(1)).setBindUserPassword(eq(freeIpaClient), any(), eq(OLD_PASSWORD));
    }

    @Test
    void rotateShouldUpdateUserSyncPasswordUserSyncFlagIsSet() throws Exception {
        RotationSecret rotationSecret = new RotationSecret(NEW_PASSWORD, OLD_PASSWORD);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        when(secretService.getRotation(eq(BIND_PASSWORD_SECRET))).thenReturn(rotationSecret);
        when(freeIpaClientFactory.getFreeIpaClientForStack(eq(stack))).thenReturn(freeIpaClient);
        when(userSyncBindUserService.getUserSyncBindUserName(eq(ENVIRONMENT_CRN))).thenReturn("usersync-postfix");

        rotationExecutor.rotate(getContext(false, true));
        ArgumentCaptor<String> userNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(freeIpaClient, times(1)).userShow(userNameCaptor.capture());
        assertEquals("usersync-postfix", userNameCaptor.getValue());
        verify(ldapConfigService, times(1)).setBindUserPassword(eq(freeIpaClient), any(), eq(NEW_PASSWORD));
    }

    @Test
    void rotateShouldFailWhenUserSyncFlagAndClusterNameIsSet() throws Exception {
        RotationSecret rotationSecret = new RotationSecret(NEW_PASSWORD, OLD_PASSWORD);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        when(secretService.getRotation(eq(BIND_PASSWORD_SECRET))).thenReturn(rotationSecret);

        SecretRotationException sre = assertThrows(SecretRotationException.class,
                () -> rotationExecutor.rotate(getContext(true, true)));
        assertEquals("Freeipa bind password rotation failed", sre.getMessage());
        verify(ldapConfigService, never()).setBindUserPassword(eq(freeIpaClient), any(), eq(NEW_PASSWORD));
    }

    private FreeIpaLdapBindPasswordRotationContext getContext(boolean withClusterName, boolean withRotateUserSync) {
        FreeipaLdapBindPasswordRotationContextBuilder builder = FreeIpaLdapBindPasswordRotationContext.builder()
                .withResourceCrn(ENVIRONMENT_CRN)
                .withBindPasswordSecret(BIND_PASSWORD_SECRET)
                .withRotateUserSyncUser(withRotateUserSync);
        if (withClusterName) {
            builder.withClusterName(CLUSTER_NAME);
        }
        return builder.build();
    }
}