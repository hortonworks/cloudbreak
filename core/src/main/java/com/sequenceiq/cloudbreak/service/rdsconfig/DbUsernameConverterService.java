package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ConnectionNameFormat;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
public class DbUsernameConverterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbUsernameConverterService.class);

    private static final Pattern AZURE_DATABASE_SERVER_HOST_PATTERN =
            Pattern.compile(".*\\.(database\\.azure\\.com|database\\.windows\\.net|database\\.usgovcloudapi\\.net|"
                    + "database\\.microsoftazure\\.de|database\\.chinacloudapi\\.cn)");

    public String toConnectionUsername(DatabaseServerV4Response dbServer, String dbUser) {
        ConnectionNameFormat connectionNameFormat = getConnectionNameFormat(dbServer);
        if (connectionNameFormat == ConnectionNameFormat.USERNAME_WITH_HOSTNAME) {
            LOGGER.debug("Detected USERNAME_WITH_HOSTNAME name format on db server {}, appending short hostname to connection username", dbServer.getHost());
            return dbUser + "@" + StringUtils.substringBefore(dbServer.getHost(), ".");
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

    private ConnectionNameFormat getConnectionNameFormat(DatabaseServerV4Response databaseServerV4Response) {
        if (databaseServerV4Response.getDatabasePropertiesV4Response() != null
                && databaseServerV4Response.getDatabasePropertiesV4Response().getConnectionNameFormat() != null) {
            return databaseServerV4Response.getDatabasePropertiesV4Response().getConnectionNameFormat();
        } else if (isAzureSingleServer(databaseServerV4Response.getHost())) {
            return ConnectionNameFormat.USERNAME_WITH_HOSTNAME;
        } else {
            return ConnectionNameFormat.USERNAME_ONLY;
        }
    }

    private static boolean isAzureSingleServer(String hostname) {
        return AZURE_DATABASE_SERVER_HOST_PATTERN.matcher(hostname.toLowerCase(Locale.US)).matches();
    }
}
