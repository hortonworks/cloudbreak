package com.sequenceiq.cloudbreak.service.publicendpoint;

import static com.google.common.hash.Hashing.sipHash24;

import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.dns.LegacyEnvironmentNameBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.validation.HueWorkaroundValidatorService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class EnvironmentBasedDomainNameProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBasedDomainNameProvider.class);

    private static final String DOMAIN_PART_DELIMITER = ".";

    private static final String FREEIPA_DEFAULT_ACCOUNT_DOMAIN = "internal";

    @Inject
    private HueWorkaroundValidatorService hueWorkaroundValidatorService;

    @Inject
    private LegacyEnvironmentNameBasedDomainNameProvider legacyEnvironmentNameBasedDomainNameProvider;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public String getFullyQualifiedEndpointName(Set<String> hueHostGroups, String endpointName, DetailedEnvironmentResponse environment) {
        if (StringUtils.isEmpty(endpointName)) {
            throw new IllegalStateException("Endpoint name must be specified!");
        }
        String environmentDomainName = getDomain(environment);
        String fullyQualifiedEndpointName = endpointName + DOMAIN_PART_DELIMITER + environmentDomainName;
        LOGGER.info("The generated FQDN: {}, for environment domain: {}", fullyQualifiedEndpointName, environmentDomainName);
        try {
            hueWorkaroundValidatorService.validateForEnvironmentDomainName(hueHostGroups, fullyQualifiedEndpointName);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
        return fullyQualifiedEndpointName;
    }

    //It is responsible for creating a CN for the generated CSR, so the result could not exceed 64 chars.
    public String getCommonName(String endpointName, DetailedEnvironmentResponse environment) {
        String environmentDomainName = getDomain(environment);
        //Hashing the concatenation of endpoint and environment names with SipHash24 as they are unique within a base-domain and account
        String uniqueNameToBeHashed = endpointName + environmentDomainName;
        String clusterHash = sipHash24().hashUnencodedChars(uniqueNameToBeHashed).toString();
        String commonName = clusterHash + DOMAIN_PART_DELIMITER + environmentDomainName;
        LOGGER.info("The generated Common Name: {}, for environment: {}", commonName, environmentDomainName);
        return commonName;
    }

    private String getDomain(DetailedEnvironmentResponse environment) {
        String environmentDomain = environment.getEnvironmentDomain();
        if (StringUtils.isEmpty(environmentDomain)) {
            String accountWorkloadSubdomain = getAccountWorkloadSubdomain(environment.getCrn());
            environmentDomain = legacyEnvironmentNameBasedDomainNameProvider.getDomainName(environment.getName(), accountWorkloadSubdomain);
        }
        return environmentDomain;
    }

    private String getAccountWorkloadSubdomain(String environmentCrn) {
        //starting to use the CRN of the environment as the certificate renewal will be automatic and be triggered by PEM with internal user
        String accountId = Crn.safeFromString(environmentCrn).getAccountId();
        UserManagementProto.Account account = grpcUmsClient.getAccountDetails(accountId, regionAwareInternalCrnGeneratorFactory);
        String accountSubdomain = account.getWorkloadSubdomain();
        if (accountSubdomain == null || accountSubdomain.isEmpty()) {
            accountSubdomain = FREEIPA_DEFAULT_ACCOUNT_DOMAIN;
            LOGGER.info("getWorkloadSubdomain was null, or empty, setting default subdomain: {}", accountSubdomain);
        }
        return accountSubdomain;
    }
}
