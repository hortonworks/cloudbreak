package com.sequenceiq.cloudbreak.it;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class GccIntegrationTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GccIntegrationTest.class);

    @Value("${cb.it.gcc.project.id}")
    private String projectid;

    @Value("${cb.it.gcc.serviceaccount.id}")
    private String serviceAccountId;

    @Value("${cb.it.gcc.serviceaccount.private.key}")
    private String serviceAccountPrivateKey;

    @Value("${cb.it.gcc.public.key}")
    private String publicKey;

    @Override protected void decorateModel() {
        getTestContext().put("projectId", projectid);
        getTestContext().put("serviceAccountId", serviceAccountId);
        getTestContext().put("serviceAccountPrivateKey", serviceAccountPrivateKey);
        getTestContext().put("publicKey", publicKey);
    }

    @Override protected CloudProvider provider() {
        return CloudProvider.GCC;
    }

    @Test
    public void createGccCluster() {
        super.integrationTestFlow();
    }

}
