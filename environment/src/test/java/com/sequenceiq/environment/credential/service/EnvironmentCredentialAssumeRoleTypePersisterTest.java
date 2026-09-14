package com.sequenceiq.environment.credential.service;

import static com.sequenceiq.common.model.CredentialType.ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.RoleBasedCredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;

@ExtendWith(MockitoExtension.class)
class EnvironmentCredentialAssumeRoleTypePersisterTest {

    private static final String CREDENTIAL_CRN = "crn:cdp:environments:us-west-1:acc:credential:cred";

    private static final String ACCOUNT_ID = "accountId";

    private static final String SA_ROLE = "SA_ROLE";

    private static final String DELEGATOR = "DELEGATOR";

    @Mock
    private CredentialRetrievalService credentialRetrievalService;

    @Mock
    private CredentialUpdateService credentialUpdateService;

    @InjectMocks
    private EnvironmentCredentialAssumeRoleTypePersister underTest;

    @Test
    void persistSkipsWithoutErrorWhenCredentialNotPersistedYet() {
        when(credentialRetrievalService.getOptionalByCrnForAccountId(CREDENTIAL_CRN, ACCOUNT_ID, ENVIRONMENT)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> underTest.persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, SA_ROLE));

        verify(credentialUpdateService, never()).updateAttributes(any(), any());
    }

    @Test
    void persistSkipsWhenNoAwsRoleBasedAttributes() {
        Credential credential = credentialWithAttributes(new CredentialAttributes());
        when(credentialRetrievalService.getOptionalByCrnForAccountId(CREDENTIAL_CRN, ACCOUNT_ID, ENVIRONMENT)).thenReturn(Optional.of(credential));

        underTest.persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, SA_ROLE);

        verify(credentialUpdateService, never()).updateAttributes(any(), any());
    }

    @Test
    void persistUpdatesWhenRoleAssumeTypeChanged() {
        Credential credential = credentialWithRoleAssumeType(SA_ROLE);
        when(credentialRetrievalService.getOptionalByCrnForAccountId(CREDENTIAL_CRN, ACCOUNT_ID, ENVIRONMENT)).thenReturn(Optional.of(credential));

        underTest.persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DELEGATOR);

        ArgumentCaptor<Credential> captor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialUpdateService, times(1)).updateAttributes(captor.capture(), any());
        assertEquals(DELEGATOR, readRoleAssumeType(captor.getValue()));
    }

    @Test
    void persistSkipsUpdateWhenRoleAssumeTypeUnchanged() {
        Credential credential = credentialWithRoleAssumeType(SA_ROLE);
        when(credentialRetrievalService.getOptionalByCrnForAccountId(CREDENTIAL_CRN, ACCOUNT_ID, ENVIRONMENT)).thenReturn(Optional.of(credential));

        underTest.persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, SA_ROLE);

        verify(credentialUpdateService, never()).updateAttributes(any(), any());
    }

    @Test
    void persistSwallowsExceptionWhenUpdateFails() {
        Credential credential = credentialWithRoleAssumeType(SA_ROLE);
        when(credentialRetrievalService.getOptionalByCrnForAccountId(CREDENTIAL_CRN, ACCOUNT_ID, ENVIRONMENT)).thenReturn(Optional.of(credential));
        when(credentialUpdateService.updateAttributes(any(), any())).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> underTest.persistAssumeRoleType(CREDENTIAL_CRN, ACCOUNT_ID, DELEGATOR));
    }

    private Credential credentialWithRoleAssumeType(String roleAssumeType) {
        RoleBasedCredentialAttributes roleBased = new RoleBasedCredentialAttributes();
        roleBased.setRoleAssumeType(roleAssumeType);
        AwsCredentialAttributes aws = new AwsCredentialAttributes();
        aws.setRoleBased(roleBased);
        CredentialAttributes attributes = new CredentialAttributes();
        attributes.setAws(aws);
        return credentialWithAttributes(attributes);
    }

    private Credential credentialWithAttributes(CredentialAttributes attributes) {
        Credential credential = new Credential();
        credential.setName("cred");
        credential.setType(ENVIRONMENT);
        credential.setResourceCrn(CREDENTIAL_CRN);
        credential.setAccountId(ACCOUNT_ID);
        credential.setAttributes(new Json(attributes).getValue());
        return credential;
    }

    private String readRoleAssumeType(Credential credential) {
        try {
            return new Json(credential.getAttributes()).get(CredentialAttributes.class).getAws().getRoleBased().getRoleAssumeType();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
