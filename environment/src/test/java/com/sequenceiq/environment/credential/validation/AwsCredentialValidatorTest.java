package com.sequenceiq.environment.credential.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.aws.AwsCredentialValidator;

public class AwsCredentialValidatorTest {

    private final AwsCredentialValidator awsCredentialValidator = new AwsCredentialValidator();

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

        ValidationResult validationResult = awsCredentialValidator.validateUpdate(original, newCred, resultBuilder);

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

        ValidationResult result = awsCredentialValidator.validateUpdate(original, newCred, resultBuilder);

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

        ValidationResult result = awsCredentialValidator.validateUpdate(original, newCred, resultBuilder);

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

        ValidationResult result = awsCredentialValidator.validateUpdate(original, newCred, resultBuilder);

        assertEquals(1, result.getErrors().size());
        assertThat(result.getErrors().get(0),
                CoreMatchers.containsString("Missing attributes from the JSON!"));
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
