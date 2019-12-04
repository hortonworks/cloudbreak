package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DbUsernameConverterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbUsernameConverterService.class);

    private static final Pattern AZURE_DATABASE_SERVER_HOST_PATTERN =
            Pattern.compile(".*\\.(database\\.azure\\.com|database\\.windows\\.net|database\\.usgovcloudapi\\.net|"
                    + "database\\.microsoftazure\\.de|database\\.chinacloudapi\\.cn)");

    public String toConnectionUsername(String host, String dbUser) {
        if (isAzure(host)) {
            LOGGER.debug("Detected Azure database server {}, appending short hostname to connection username", host);
            return dbUser + "@" + StringUtils.substringBefore(host, ".");
        }

        return dbUser;
    }

    /**
     * Converts a connection username to a db username.
     * On azure, connection username follows the 'username@short-hostname' pattern.
     * On postgres, however, users have to be in 'username' format, without the '@' sign => code has to take username.
     *
     * @param username a connection username
     * @return returns the username part before the '@' sign
     */
    public String toDatabaseUsername(String username) {
        return StringUtils.substringBefore(username, "@");
    }

    private static boolean isAzure(String hostname) {
        return AZURE_DATABASE_SERVER_HOST_PATTERN.matcher(hostname.toLowerCase(Locale.US)).matches();
    }
}