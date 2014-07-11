package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.validation.RequiredAzureCredentialParam;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AzureCredentialConverterTest {

    private static final String DUMMY_JKS = "dummyJks";
    private static final String DUMMY_NAME = "dummyName";
    private static final String DUMMY_SUBSCRIPTION_ID = "dummySubscriptionId";
    private static final String DUMMY_DESCRIPTION = "dummyDescription";

    private AzureCredentialConverter underTest;

    private AzureCredential azureCredential;

    private CredentialJson credentialJson;

    @Before
    public void setUp() {
        underTest = new AzureCredentialConverter();
        azureCredential = createAzureCredential();
        credentialJson = createCredentialJson();
    }

    @Test
    public void testConvertAzureCredentialEntityToJson() {
        // GIVEN
        // WHEN
        CredentialJson result = underTest.convert(azureCredential);
        // THEN
        assertEquals(result.getCloudPlatform(), azureCredential.getCloudPlatform());
        assertEquals(result.getName(), azureCredential.getName());
        assertEquals(result.getDescription(), azureCredential.getDescription());
        assertEquals(result.getParameters().get(RequiredAzureCredentialParam.JKS_PASSWORD.getName()),
                azureCredential.getJks());
    }

    @Test
    public void testConvertAzureCredentialJsonToEntity() {
        // GIVEN
        // WHEN
        AzureCredential result = underTest.convert(credentialJson);
        // THEN
        assertEquals(result.getCloudPlatform(), credentialJson.getCloudPlatform());
        assertEquals(result.getJks(),
                credentialJson.getParameters().get(RequiredAzureCredentialParam.JKS_PASSWORD.getName()));
        assertEquals(result.getName(), credentialJson.getName());
    }

    private CredentialJson createCredentialJson() {
        CredentialJson credentialJson = new CredentialJson();
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredAzureCredentialParam.JKS_PASSWORD.getName(), DUMMY_JKS);
        params.put(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName(), DUMMY_SUBSCRIPTION_ID);
        credentialJson.setParameters(params);
        credentialJson.setCloudPlatform(CloudPlatform.AZURE);
        credentialJson.setId(1L);
        credentialJson.setName(DUMMY_NAME);
        credentialJson.setDescription(DUMMY_DESCRIPTION);
        return credentialJson;
    }

    private AzureCredential createAzureCredential() {
        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setJks(DUMMY_JKS);
        azureCredential.setName(DUMMY_NAME);
        azureCredential.setSubscriptionId(DUMMY_SUBSCRIPTION_ID);
        azureCredential.setCloudPlatform(CloudPlatform.AZURE);
        azureCredential.setDescription(DUMMY_DESCRIPTION);
        azureCredential.setId(1L);
        return azureCredential;
    }
}
