package com.sequenceiq.cloudbreak.component;

import java.io.IOException;
import java.security.KeyPair;
import java.util.List;

import javax.inject.Inject;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;

@Ignore
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CreateCertificationTest.CreateCertificationTestConfig.class)
@TestPropertySource(properties = {
        "clusterdns.host=localhost",
        "clusterdns.port=9983",
        "gateway.cert.polling.attempt=50",
        "gateway.cert.base.domain.name=workload-dev.cloudera.com",
        "altus.ums.host=ums.thunderhead-dev.cloudera.com",
        "actor.crn=<>",
        "account.id=<>"
})
public class CreateCertificationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateCertificationTest.class);

    @Inject
    private CertificateCreationService certificateCreationService;

    @Value("${actor.crn}")
    private String actorCrn;

    @Value("${account.id}")
    private String accountId;

    @Test
    public void createCert() throws IOException {
        KeyPair keyPair = PkiUtil.generateKeypair();
        LOGGER.info("Key: \n{}", PkiUtil.convert(keyPair.getPrivate()));
        LOGGER.info("Pub: \n{}", PkiUtil.convert(keyPair.getPublic()));
        String domain = ".tb-local.xcu2-8y8x.workload-dev.cloudera.com";
        String commonName = "a7c2a45fc8f917fe";
        String endpointName = "really-really-long-named-cluster-tbihari2";
        List<String> subjectAlternativeNames = List.of(
                commonName + domain,
                endpointName + domain
        );
        PKCS10CertificationRequest csr = PkiUtil.csr(keyPair, commonName, subjectAlternativeNames);
        List<String> strings = certificateCreationService.create(actorCrn,
                accountId,
                commonName,
                "env-tb",
                csr);
        LOGGER.info("CERT: \n" + String.join("\n", strings));
    }

    @Test
    public void onlyPolling() throws IOException {
        String requestId = "982c3446-2942-4f2c-893b-44c9dfe0e718";
        List<String> strings = certificateCreationService.polling(actorCrn, requestId);
        LOGGER.info("CERT: " + String.join(",", strings));
    }

    @Configuration
    @ComponentScan(
            basePackages = {"com.sequenceiq.cloudbreak.certificate",
                    "com.sequenceiq.cloudbreak.dns",
                    "com.sequenceiq.cloudbreak.client",
                    "com.sequenceiq.cloudbreak.auth.altus",
                    "com.sequenceiq.cloudbreak.auth"}
    )
    public static class CreateCertificationTestConfig {

    }
}
