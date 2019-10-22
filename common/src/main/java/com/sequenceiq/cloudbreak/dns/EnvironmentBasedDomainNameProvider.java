package com.sequenceiq.cloudbreak.dns;

import static com.google.common.hash.Hashing.sipHash24;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentBasedDomainNameProvider {
    static final String ENV_NAME_SHOULD_BE_SPECIFIED_MSG = "Domain name cannot be generated, since environment name must be specified!";

    static final String ACCOUNT_NAME_IS_EMTPY_FORMAT = "Domain name cannot be generated for environment: %s, " +
            " WorkloadSubdomain in your UMS Account details is null, or empty!";

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBasedDomainNameProvider.class);

    private static final String DOMAIN_PART_DELIMITER = ".";

    private static final String DOMAIN_PATTERN = "^((?=[a-z0-9-]{1,40}\\.)(xn--)?[a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,40}$";

    private static final int MAX_SIZE_OF_DOMAIN = 62;

    private static final Integer MAX_LENGTH_OF_ENVIRONMENT = 8;

    @Value("${gateway.cert.base.domain.name:wl.cloudera.site}")
    private String rootDomain;

    public String getDomainName(String environmentName, String accountName) {
        if (StringUtils.isEmpty(environmentName)) {
            LOGGER.error("The required parameters hasn't been specified, environment name: {}, account name: {}", environmentName, accountName);
            throw new IllegalStateException(ENV_NAME_SHOULD_BE_SPECIFIED_MSG);
        }

        if (StringUtils.isEmpty(accountName)) {
            LOGGER.error("The required parameters hasn't been specified, environment name: {}, account name: {}", environmentName, accountName);
            throw new IllegalStateException(String.format(ACCOUNT_NAME_IS_EMTPY_FORMAT, environmentName));
        }

        LOGGER.info("Generating domain with environment name: '{}', account name: '{}' and root domain: '{}'", environmentName, accountName, rootDomain);
        StringBuilder sb = new StringBuilder()
                .append(truncateEnvironment(environmentName))
                .append(DOMAIN_PART_DELIMITER)
                .append(accountName);

        if (!rootDomain.startsWith(DOMAIN_PART_DELIMITER)) {
            sb = sb.append(DOMAIN_PART_DELIMITER);
        }
        String result = sb.append(rootDomain).toString();
        validateDomainPattern(result);
        LOGGER.info("Generated domain: '{}'", result);
        return result;
    }

    public String getFullyQualifiedEndpointName(String endpointName, String environmentName, String accountName) {
        if (StringUtils.isEmpty(endpointName)) {
            throw new IllegalStateException("Endpoint name must be specified!");
        }
        String domain = getDomainName(environmentName, accountName);
        return endpointName + DOMAIN_PART_DELIMITER + domain;
    }

    //It is responsible for creating a CN for the generated CSR, so the result could not exceed 64 chars.
    public String getCommonName(String endpointName, String environmentName, String accountName) {
        String domain = getDomainName(environmentName, accountName);
        //Hashing the concatenation of endpoint and environment names with SipHash24 as they are unique within a base-domain and account
        String uniqueNameToBeHashed = endpointName + environmentName;
        String clusterHash = sipHash24().hashUnencodedChars(uniqueNameToBeHashed).toString();
        return clusterHash + DOMAIN_PART_DELIMITER + domain;
    }

    private void validateDomainPattern(String result) {
        boolean matches = Pattern.matches(DOMAIN_PATTERN, result);
        if (!matches) {
            String msg = String.format("The generated domain('%s') doesn't match with domain allowed pattern '%s'", result, DOMAIN_PATTERN);
            LOGGER.warn(msg);
            throw new IllegalStateException(msg);
        } else if (result.length() > MAX_SIZE_OF_DOMAIN) {
            String msg = String.format("The length of the generated domain('%s') is longer than the allowed %s characters", result, MAX_SIZE_OF_DOMAIN);
            LOGGER.warn(msg);
            throw new IllegalStateException(msg);
        }
    }

    private String truncateEnvironment(String environment) {
        int truncatePoint = environment.length();
        if (truncatePoint > MAX_LENGTH_OF_ENVIRONMENT) {
            truncatePoint = MAX_LENGTH_OF_ENVIRONMENT;
        }
        if (environment.charAt(truncatePoint - 1) == '-') {
            truncatePoint--;
        }
        return environment.substring(0, truncatePoint);
    }
}
