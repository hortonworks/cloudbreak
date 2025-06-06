package com.sequenceiq.environment.credential.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.aws.AwsCredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.aws.AwsCredentialValidator;

@ExtendWith(MockitoExtension.class)
public class AwsCredentialValidatorTest {

    @Mock
    private CloudParameterService cloudParameterService;

    @InjectMocks
    private AwsCredentialValidator underTest;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testValidUpdate() {
        Credential original = new Credential();
        ObjectNode rootOriginal = getAwsAttributes();
        putKeyBased(rootOriginal);
        original.setAttributes(rootOriginal.toString());

        Credential newCred = new Credential();
        ObjectNode rootNew = getAwsAttributes();
        putKeyBased(rootNew);
        newCred.setAttributes(rootNew.toString());
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        ValidationResult validationResult = underTest.validateUpdate(original, newCred, resultBuilder);

        assertFalse(validationResult.hasError());
    }

    @Test
    public void testKeyBasedToRoleBased() {
        Credential original = new Credential();
        ObjectNode rootOriginal = getAwsAttributes();
        putKeyBased(rootOriginal);
        original.setAttributes(rootOriginal.toString());

        Credential newCred = new Credential();
        ObjectNode rootNew = getAwsAttributes();
        putRoleBased(rootNew);
        newCred.setAttributes(rootNew.toString());
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        ValidationResult result = underTest.validateUpdate(original, newCred, resultBuilder);

        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0),
                CoreMatchers.containsString("Cannot change AWS credential from key based to role based."));
    }

    @Test
    public void testRoleBasedToKeyBased() {
        Credential original = new Credential();
        ObjectNode rootOriginal = getAwsAttributes();
        putRoleBased(rootOriginal);
        original.setAttributes(rootOriginal.toString());

        Credential newCred = new Credential();
        ObjectNode rootNew = getAwsAttributes();
        putKeyBased(rootNew);
        newCred.setAttributes(rootNew.toString());
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        ValidationResult result = underTest.validateUpdate(original, newCred, resultBuilder);

        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0),
                CoreMatchers.containsString("Cannot change AWS credential from role based to key based."));
    }

    @Test
    public void testWithMissingAwsAttributes() {
        Credential original = new Credential();
        ObjectNode rootOriginal = getAwsAttributes();
        putRoleBased(rootOriginal);
        original.setAttributes(rootOriginal.toString());

        Credential newCred = new Credential();
        newCred.setAttributes(objectMapper.createObjectNode().toString());
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        ValidationResult result = underTest.validateUpdate(original, newCred, resultBuilder);

        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0),
                CoreMatchers.containsString("Missing attributes from the JSON!"));
    }

    @Test
    public void testDefaultRegionChangeWhenTheProvidedRegionIsNotInTheCloudRegions() {
        Credential original = new Credential();
        ObjectNode rootOriginal = getAwsAttributes();
        putRoleBased(rootOriginal);
        original.setAttributes(rootOriginal.toString());

        Credential newCred = getCredentialWithAwsAttributesAndDefaultRegionAs("dummy");
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        CloudRegions cloudRegions = mock(CloudRegions.class);
        when(cloudRegions.getRegionNames()).thenReturn(Set.of());
        when(cloudParameterService.getCdpRegions(anyString(), anyString())).thenReturn(cloudRegions);

        ValidationResult result = underTest.validateUpdate(original, newCred, resultBuilder);

        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0),
                CoreMatchers.containsString("The specified default region 'dummy' is not supported on AWS by CDP."));
    }

    @Test
    public void testDefaultRegionChangeWhenTheProvidedRegionIsInTheCloudRegions() {
        Credential original = new Credential();
        ObjectNode rootOriginal = getAwsAttributes();
        putRoleBased(rootOriginal);
        original.setAttributes(rootOriginal.toString());

        Credential newCred = getCredentialWithAwsAttributesAndDefaultRegionAs("desired-region");
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        CloudRegions cloudRegions = mock(CloudRegions.class);
        when(cloudRegions.getRegionNames()).thenReturn(Set.of("desired-region"));
        when(cloudParameterService.getCdpRegions(anyString(), anyString())).thenReturn(cloudRegions);

        ValidationResult result = underTest.validateUpdate(original, newCred, resultBuilder);

        assertEquals(0, result.getErrors().size());
    }

    private Credential getCredentialWithAwsAttributesAndDefaultRegionAs(String defaultRegion) {
        AwsCredentialAttributes awsCredentialAttributes = new AwsCredentialAttributes();
        awsCredentialAttributes.setDefaultRegion(defaultRegion);
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        credentialAttributes.setAws(awsCredentialAttributes);
        Credential newCred = new Credential();
        newCred.setAttributes(JsonUtil.writeValueAsStringUnchecked(credentialAttributes));
        return newCred;
    }

    private ObjectNode getAwsAttributes() {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode aws = objectMapper.createObjectNode();
        root.put("aws", aws);
        return root;
    }

    private void putRoleBased(ObjectNode root) {
        ((ObjectNode) root.get("aws")).put("roleBased", objectMapper.createObjectNode());
    }

    private void putKeyBased(ObjectNode root) {
        ((ObjectNode) root.get("aws")).put("keyBased", objectMapper.createObjectNode());
    }
}
