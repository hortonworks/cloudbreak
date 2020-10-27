package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.cache.cert.Cert;
import com.sequenceiq.freeipa.cache.cert.CertCacheConfiguration;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class FreeIpaRootCertificateService {

    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";

    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    @Cacheable(cacheNames = CertCacheConfiguration.NAME)
    public Cert getRootCertificate(String environmentCrn, String accountId) throws FreeIpaClientException {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);
        FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);

        return new Cert(convertToPemFormat(client.getRootCertificate()));
    }

    private String convertToPemFormat(String certificate) {
        return String.join("\n",
                BEGIN_CERTIFICATE,
                String.join("\n",
                        certificate.split("(?<=\\G.{64})")),
                END_CERTIFICATE);
    }
}
