package com.sequenceiq.cloudbreak.dns;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentBasedDomainNameProvider {
    static final String NAMES_SHOULD_BE_SPECIFIED_MSG = "Environment and account names should be specified!";

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentBasedDomainNameProvider.class);

    private static final String DOMAIN_PART_DELIMITER = ".";

    private static final String DOMAIN_PATTERN = "^((?=[a-z0-9-]{1,40}\\.)(xn--)?[a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,40}$";

    private static final int MAX_SIZE_OF_DOMAIN = 62;

    @Value("${cb.dns.root.domain:cloudera.site}")
    private String rootDomain;

    public String getDomain(String environmentName, String accountName) {
        if (StringUtils.isEmpty(environmentName) || StringUtils.isEmpty(accountName)) {
            LOGGER.warn("The required parameters hasn't been specified, environment name: {}, account name: {}", environmentName, accountName);
            throw new IllegalStateException(NAMES_SHOULD_BE_SPECIFIED_MSG);
        }

        LOGGER.info("Generating domain with environment name: '{}', account name: '{}' and root domain: '{}'", environmentName, accountName, rootDomain);
        StringBuilder sb = new StringBuilder()
                .append(environmentName)
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

    public String getDomainName(String clusterName, String environmentName, String accountName) {
        if (StringUtils.isEmpty(clusterName)) {
            throw new IllegalStateException("Cluster name should be specified!");
        }

        String domain = getDomain(environmentName, accountName);
        return clusterName + DOMAIN_PART_DELIMITER + domain;
    }

    private void validateDomainPattern(String result) {
        boolean matches = Pattern.matches(DOMAIN_PATTERN, result);
        if (!matches) {
            String msg = String.format("The generated domain('%s') doesn't match with domain allowed pattern '%s'", result, DOMAIN_PATTERN);
            LOGGER.warn(msg);
            throw new IllegalStateException(msg);
        } else if (result.length() > MAX_SIZE_OF_DOMAIN) {
            String msg = String.format("The length of the generated domain('%s') is longer than the allowed 62 characters", result);
            LOGGER.warn(msg);
            throw new IllegalStateException(msg);
        }
    }
}
