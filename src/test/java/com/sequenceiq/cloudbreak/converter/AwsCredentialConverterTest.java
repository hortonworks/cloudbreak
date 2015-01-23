package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.doNothing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.controller.json.CredentialJson;
import com.sequenceiq.cloudbreak.controller.json.SnsTopicJson;
import com.sequenceiq.cloudbreak.controller.validation.AWSCredentialParam;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.SnsTopic;
import com.sequenceiq.cloudbreak.domain.TemporaryAwsCredentials;
import com.sequenceiq.cloudbreak.service.credential.RsaPublicKeyValidator;

public class AwsCredentialConverterTest {

    public static final String DUMMY_DESCRIPTION = "dummyDescription";
    public static final String DUMMY_ROLE_ARN = "dummyRoleArn";
    public static final String DUMMY_NAME = "dummyName";
    @InjectMocks
    private AwsCredentialConverter underTest;

    @Mock
    private SnsTopicConverter snsTopicConverter;

    @Mock
    private RsaPublicKeyValidator rsaPublicKeyValidator;

    private AwsCredential awsCredential;

    private CredentialJson credentialJson;

    @Before
    public void setUp() {
        underTest = new AwsCredentialConverter();
        MockitoAnnotations.initMocks(this);
        awsCredential = createAwsCredential();
        credentialJson = createCredentialJson();
    }

    @Test
    public void testConvertAwsCredentialEntityToJson() {
        // GIVEN
        given(snsTopicConverter.convertAllEntityToJson(anySetOf(SnsTopic.class)))
                .willReturn(new HashSet<SnsTopicJson>());
        // WHEN
        CredentialJson result = underTest.convert(awsCredential);
        // THEN
        assertEquals(result.getCloudPlatform(), CloudPlatform.AWS);
        assertEquals(result.getDescription(), awsCredential.getDescription());
        assertEquals(result.getName(), awsCredential.getName());
    }

    @Test
    public void testConvertAwsCredentialEntityToJsonWhenDescriptionIsNull() {
        // GIVEN
        awsCredential.setDescription(null);
        given(snsTopicConverter.convertAllEntityToJson(anySetOf(SnsTopic.class)))
                .willReturn(new HashSet<SnsTopicJson>());
        // WHEN
        CredentialJson result = underTest.convert(awsCredential);
        // THEN
        assertEquals(result.getDescription(), "");
    }

    @Test
    public void testConvertAwsCredentialJsonToEntity() {
        // GIVEN
        doNothing().when(rsaPublicKeyValidator).validate(any(AwsCredential.class));
        // WHEN
        AwsCredential result = underTest.convert(credentialJson);
        // THEN
        assertEquals(result.getRoleArn(),
                credentialJson.getParameters().get(AWSCredentialParam.ROLE_ARN.getName()));
        assertEquals(result.getName(), credentialJson.getName());
    }

    private AwsCredential createAwsCredential() {
        AwsCredential awsCredential = new AwsCredential();
        awsCredential.setId(1L);
        awsCredential.setName(DUMMY_NAME);
        awsCredential.setRoleArn(DUMMY_ROLE_ARN);
        awsCredential.setSnsTopics(new HashSet<SnsTopic>());
        awsCredential.setDescription(DUMMY_DESCRIPTION);
        awsCredential.setTemporaryAwsCredentials(new TemporaryAwsCredentials());
        awsCredential.setPublicInAccount(true);
        return awsCredential;
    }

    private CredentialJson createCredentialJson() {
        CredentialJson credentialJson = new CredentialJson();
        credentialJson.setCloudPlatform(CloudPlatform.AWS);
        credentialJson.setDescription(DUMMY_DESCRIPTION);
        credentialJson.setId(1L);
        credentialJson.setName(DUMMY_NAME);
        Map<String, Object> params = new HashMap<>();
        params.put(AWSCredentialParam.ROLE_ARN.getName(), DUMMY_ROLE_ARN);
        params.put(AWSCredentialParam.SNS_TOPICS.getName(), new HashSet<SnsTopic>());
        credentialJson.setParameters(params);
        return credentialJson;
    }

}
