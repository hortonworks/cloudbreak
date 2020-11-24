package com.sequenceiq.freeipa.service.freeipa.cert.root;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.RootCert;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.config.RootCertRegisterService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class FreeIpaRootCertificateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRootCertificateService.class);

    @Inject
    private StackService stackService;

    @Inject
    private RootCertService rootCertService;

    @Inject
    private RootCertRegisterService rootCertRegisterService;

    public String getRootCertificate(String environmentCrn, String accountId) throws FreeIpaClientException {
        Optional<RootCert> rootCert = rootCertService.findByEnvironmentCrn(environmentCrn);
        if (rootCert.isPresent()) {
            LOGGER.debug("FreeIPA CA cert found in DB for env: {}", environmentCrn);
            return rootCert.get().getCert();
        } else {
            LOGGER.debug("FreeIPA CA cert not found in DB for env: {}", environmentCrn);
            Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
            return rootCertRegisterService.register(stack).getCert();
        }
    }
}
