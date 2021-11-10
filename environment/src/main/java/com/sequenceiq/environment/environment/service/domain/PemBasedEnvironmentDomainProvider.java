package com.sequenceiq.environment.environment.service.domain;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.exception.EnvironmentServiceException;

@Component
public class PemBasedEnvironmentDomainProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PemBasedEnvironmentDomainProvider.class);

    @Inject
    private DnsManagementService dnsManagementService;

    public String generate(Environment environment) {
        String managedDomain = null;
        String environmentName = environment.getName();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("Generate managed domain name via PEM for environment ame: '{}' and account: '{}'", environmentName, accountId);
        try {
            managedDomain = dnsManagementService.generateManagedDomain(accountId, environmentName);
            LOGGER.info("Generated managed domain name via PEM: '{}' for environment name: '{}' and account: '{}'", managedDomain, environmentName, accountId);
        } catch (Exception ex) {
            LOGGER.warn("Failed to generate managed domain name for the environment '{}' via PEM", environmentName, ex);
            throw new EnvironmentServiceException("Failed to generate managed domain name for the environment", ex);
        }
        if (StringUtils.isEmpty(managedDomain)) {
            throw new EnvironmentServiceException("Failed to generate managed domain name for the environment, initialization failed.");
        }
        return managedDomain;
    }
}
