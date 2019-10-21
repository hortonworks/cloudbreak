package com.sequenceiq.environment.credential.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.credential.model.response.CredentialViewResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialView;

public class CredentialViewConverterTest {

    private static final String CRED_NAME = "cred-name";

    private static final String CLOUD_PLATFORM = "alibaba-cloud";

    private static final String CREATOR = "creator-guy";

    private static final String CRN = "one-crn-a-day-keeps-the-doctor-away";

    private static final String DESCRIPTION = "describe that cred";

    private static final String VERIFICATION_STATUS_TEXT = "text";

    private final CredentialViewConverter underTest = new CredentialViewConverter();

    @Test
    public void testConvertCredentialView() {
        CredentialView credentialView = new CredentialView();
        credentialView.setName(CRED_NAME);
        credentialView.setCloudPlatform(CLOUD_PLATFORM);
        credentialView.setCreator(CREATOR);
        credentialView.setResourceCrn(CRN);
        credentialView.setDescription(DESCRIPTION);
        credentialView.setVerificationStatusText(VERIFICATION_STATUS_TEXT);

        CredentialViewResponse result = underTest.convert(credentialView);

        assertEquals(CRED_NAME, result.getName());
        assertEquals(CLOUD_PLATFORM, result.getCloudPlatform());
        assertEquals(CREATOR, result.getCreator());
        assertEquals(CRN, result.getCrn());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(VERIFICATION_STATUS_TEXT, result.getVerificationStatusText());
    }

    @Test
    public void testConvertCredential() {
        Credential credential = new Credential();
        credential.setName(CRED_NAME);
        credential.setCloudPlatform(CLOUD_PLATFORM);
        credential.setCreator(CREATOR);
        credential.setResourceCrn(CRN);
        credential.setDescription(DESCRIPTION);
        credential.setVerificationStatusText(VERIFICATION_STATUS_TEXT);

        CredentialView result = underTest.convert(credential);

        assertEquals(CRED_NAME, result.getName());
        assertEquals(CLOUD_PLATFORM, result.getCloudPlatform());
        assertEquals(CREATOR, result.getCreator());
        assertEquals(CRN, result.getResourceCrn());
        assertEquals(DESCRIPTION, result.getDescription());
        assertEquals(VERIFICATION_STATUS_TEXT, result.getVerificationStatusText());
    }
}