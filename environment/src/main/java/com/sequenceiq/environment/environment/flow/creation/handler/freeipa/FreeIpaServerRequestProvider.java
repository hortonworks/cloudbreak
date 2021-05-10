package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.FreeIpaPasswordUtil;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;

@Component
class FreeIpaServerRequestProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationHandler.class);

    private static final String FREEIPA_DEFAULT_ACCOUNT_DOMAIN = "internal";

    private static final String FREEIPA_HOSTNAME = "ipaserver";

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private EnvironmentBasedDomainNameProvider environmentBasedDomainNameProvider;

    FreeIpaServerRequest create(EnvironmentDto environment) {
        String environmentName = environment.getName();
        String accountSubDomain = getAccountSubdomain();
        String domain = environmentBasedDomainNameProvider.getDomainName(environmentName, accountSubDomain);
        LOGGER.info("Generate domain: environmentName: {}, accountSubdomain {}, domain: {}", environment.getName(), accountSubDomain, domain);

        String adminGroupName = environment.getAdminGroupName();
        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setAdminPassword(FreeIpaPasswordUtil.generatePassword());
        freeIpaServerRequest.setDomain(domain);
        freeIpaServerRequest.setHostname(FREEIPA_HOSTNAME);
        freeIpaServerRequest.setAdminGroupName(adminGroupName);
        LOGGER.info("FreeIpaServerRequest created for environment: {}, request {}", environment.getName(), freeIpaServerRequest);
        return freeIpaServerRequest;
    }

    private String getAccountSubdomain() {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
        // I think this should be better/safer if we could use the account id of environment,
        UserManagementProto.Account account = grpcUmsClient.getAccountDetails(Crn.safeFromString(userCrn).getAccountId(), requestIdOptional);
        String accountSubdomain = account.getWorkloadSubdomain();
        if (accountSubdomain == null || accountSubdomain.isEmpty()) {
            accountSubdomain = FREEIPA_DEFAULT_ACCOUNT_DOMAIN;
            LOGGER.info("getWorkloadSubdomain was null, or empty, setting default subdomain: {}", accountSubdomain);
        }
        return accountSubdomain;
    }
}
