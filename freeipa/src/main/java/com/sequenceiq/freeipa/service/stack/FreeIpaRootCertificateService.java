package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.FreeIpaClientFactory;

@Service
public class FreeIpaRootCertificateService {

    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";

    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private StackService stackService;

    public String getRootCertificate(String environmentCrn) throws Exception {
        Stack stack = stackService.getByEnvironmentCrn(environmentCrn);
        FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);

        return convertToPemFormat(client.getRootCertificate());
    }

    private String convertToPemFormat(String certificate) {
        return String.join("\n",
                BEGIN_CERTIFICATE,
                String.join("\n",
                        certificate.split("(?<=\\G.{64})")),
                END_CERTIFICATE);
    }
}
