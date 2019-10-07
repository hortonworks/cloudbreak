package com.sequenceiq.environment.credential.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.cdp.environments.model.CreateAWSCredentialRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;

class CredentialRequestToCreateAWSCredentialRequestConverterTest {

    private CredentialRequestToCreateAWSCredentialRequestConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CredentialRequestToCreateAWSCredentialRequestConverter();
    }

    @Test
    void convert() {
        CredentialRequest request = new CredentialRequest();
        request.setName("name");
        request.setDescription("desc");
        AwsCredentialParameters aws = new AwsCredentialParameters();
        RoleBasedParameters roleBased = new RoleBasedParameters();
        roleBased.setRoleArn("arn");
        aws.setRoleBased(roleBased);
        request.setAws(aws);
        CreateAWSCredentialRequest result = underTest.convert(request);
        assertEquals(request.getName(), result.getCredentialName());
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(request.getAws().getRoleBased().getRoleArn(), result.getRoleArn());
    }
}
