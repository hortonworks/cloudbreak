package com.sequenceiq.freeipa.service.config;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.client.RetryableFreeIpaClientException;
import com.sequenceiq.freeipa.entity.RootCert;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.cert.root.RootCertService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class RootCertRegisterService extends AbstractConfigRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootCertRegisterService.class);

    private static final String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";

    private static final String END_CERTIFICATE = "-----END CERTIFICATE-----";

    @Inject
    private FreeIpaClientFactory clientFactory;

    @Inject
    private StackService stackService;

    @Inject
    private RootCertService rootCertService;

    @Override
    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public void register(Long stackId) {
        try {
            Stack stack = stackService.getStackById(stackId);
            registerInternal(stack);
        } catch (FreeIpaClientException e) {
            LOGGER.error("Couldn't get FreeIPA CA certificate", e);
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    private RootCert registerInternal(Stack stack) throws FreeIpaClientException {
        MDCBuilder.buildMdcContext(stack);
        String rootCertificate = getRootCertFromFreeIpa(stack);
        RootCert rootCert = createRootCertEntity(stack, rootCertificate);
        return rootCertService.save(rootCert);
    }

    @Retryable(value = RetryableFreeIpaClientException.class,
            maxAttemptsExpression = RetryableFreeIpaClientException.MAX_RETRIES_EXPRESSION,
            backoff = @Backoff(delayExpression = RetryableFreeIpaClientException.DELAY_EXPRESSION,
                    multiplierExpression = RetryableFreeIpaClientException.MULTIPLIER_EXPRESSION))
    public RootCert register(Stack stack) throws FreeIpaClientException {
        return registerInternal(stack);
    }

    private String getRootCertFromFreeIpa(Stack stack) throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = clientFactory.getFreeIpaClientForStack(stack);
        return freeIpaClient.getRootCertificate();
    }

    private RootCert createRootCertEntity(Stack stack, String rootCertificate) {
        RootCert rootCert = new RootCert();
        rootCert.setStack(stack);
        rootCert.setEnvironmentCrn(stack.getEnvironmentCrn());
        rootCert.setCert(convertToPemFormat(rootCertificate));
        return rootCert;
    }

    @Override
    public void delete(Stack stack) {
        try {
            rootCertService.deleteByStack(stack);
        } catch (NotFoundException e) {
            LOGGER.info("Root cert not exists for environment {}", stack.getEnvironmentCrn());
        }
    }

    private String convertToPemFormat(String certificate) {
        return String.join("\n",
                BEGIN_CERTIFICATE,
                String.join("\n",
                        certificate.split("(?<=\\G.{64})")),
                END_CERTIFICATE);
    }
}
