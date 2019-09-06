package com.sequenceiq.cloudbreak.component;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CreateCertificationTest.CreateCertificationTestConfig.class)
@TestPropertySource(properties = {
        "clusterdns.host=localhost",
        "clusterdns.port=8982",
        "cert.polling.attempt=50",
        "cert.base.domain.name=workload-dev.cloudera.com",
        "altus.ums.host=ums.thunderhead-dev.cloudera.com"
})
public class CreateCertificationTest {

    @Inject
    private CertificateCreationService certificateCreationService;

    @Value("${actor.crn}")
    private String actorCrn;

    @Value("${account.id}")
    private String accountId;

    @Test
    public void createCert() throws IOException {
        List<String> strings = certificateCreationService.create(actorCrn,
                accountId,
                "cluster-name",
                "env-name",
                false);
        Assert.fail("CERT: " + String.join(",", strings));
    }

    @Test
    public void onlyPolling() throws IOException {
        String requestId = "982c3446-2942-4f2c-893b-44c9dfe0e718";
        List<String> strings = certificateCreationService.polling(actorCrn, requestId);
        Assert.fail("CERT: " + String.join(",", strings));
    }

    @Configuration
    @ComponentScan(
            basePackages = {"com.sequenceiq.cloudbreak.certificate",
                    "com.sequenceiq.cloudbreak.client",
                    "com.sequenceiq.cloudbreak.auth.altus",
                    "com.sequenceiq.cloudbreak.auth"}
    )
    public static class CreateCertificationTestConfig {

    }
}
