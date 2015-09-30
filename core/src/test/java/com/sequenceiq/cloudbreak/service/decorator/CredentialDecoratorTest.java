package com.sequenceiq.cloudbreak.service.decorator;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.Credential;

public class CredentialDecoratorTest {

    private Decorator<Credential> decorator = new CredentialDecorator();

    @Test
    public void testDecorateWithAwsCredential() throws Exception {
        AwsCredential aws = new AwsCredential();

        decorator.decorate(aws);
        assertTrue(aws.getKeyPairName() != null);
        assertTrue(aws.getKeyPairName().matches("cloudbreak-key-\\d+"));
    }

}