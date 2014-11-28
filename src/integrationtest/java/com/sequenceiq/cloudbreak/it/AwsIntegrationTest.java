package com.sequenceiq.cloudbreak.it;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class AwsIntegrationTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsIntegrationTest.class);

    @Value("${cb.it.aws.public.key}")
    private String awsSshPublicKey;

    @Value("${cb.it.aws.rolearn}")
    private String roleArn;

    @Override protected void decorateModel() {
        getTestContext().put("roleArn", roleArn);
        getTestContext().put("sshPublicKey", awsSshPublicKey);
    }

    @Override protected CloudProvider provider() {
        return CloudProvider.AWS;
    }

    @Test
    public void createAwsCluster() {
        super.integrationTest();
    }

}
