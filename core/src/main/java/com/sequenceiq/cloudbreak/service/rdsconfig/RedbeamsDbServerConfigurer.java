package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
public class RedbeamsDbServerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDbServerConfigurer.class);

    private static final String JDBC_PATTERN = "\\/\\/(.*?):(\\d*)";

    private static final Pattern AZURE_DATABASE_SERVER_HOST_PATTERN =
        Pattern.compile(".*\\.(database\\.azure\\.com|database\\.windows\\.net|database\\.usgovcloudapi\\.net|"
            + "database\\.microsoftazure\\.de|database\\.chinacloudapi\\.cn)");

    @Inject
    private RedbeamsClientService redbeamsClientService;

    @Inject
    private DatabaseCommon dbCommon;

    @Inject
    private SecretService secretService;

    /**
     * Creates an RDSConfig object for a specific database.
     *
     * @param  stack   stack for naming purposes
     * @param  cluster cluster to associate database with
     * @param  dbName  database name
     * @param  dbUser  database user
     * @param  type    database type
     * @return         RDSConfig object for database
     */
    public RDSConfig createNewRdsConfig(Stack stack, Cluster cluster, String dbName, String dbUser, DatabaseType type) {
        DatabaseServerV4Response resp = getDatabaseServer(cluster.getDatabaseServerCrn());
        LOGGER.info("Using redbeams for remote database configuration");

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionURL(dbCommon.getJdbcConnectionUrl(resp.getDatabaseVendor(), resp.getHost(), resp.getPort(), Optional.of(dbName)));
        rdsConfig.setConnectionUserName(formConnectionUserName(resp, dbUser));
        rdsConfig.setConnectionPassword(PasswordUtil.generatePassword());
        rdsConfig.setDatabaseEngine(DatabaseVendor.fromValue(resp.getDatabaseVendor()));
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setName(type.name() + '_' + stack.getName() + stack.getId());
        rdsConfig.setType(type.name());
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setClusters(Collections.singleton(cluster));
        LOGGER.info("Created RDS config {} for database type {} with connection URL {}, connection username {}",
            rdsConfig.getName(), type, rdsConfig.getConnectionURL(), rdsConfig.getConnectionUserName());
        return rdsConfig;
    }

    private String formConnectionUserName(DatabaseServerV4Response resp, String dbUser) {
        String host = resp.getHost();
        if (AZURE_DATABASE_SERVER_HOST_PATTERN.matcher(host.toLowerCase(Locale.US)).matches()) {
            LOGGER.debug("Detected Azure database server {}, appending short hostname to connection username", host);
            return dbUser + "@" + host.substring(0, host.indexOf('.'));
        }

        return dbUser;
    }

    public boolean isRemoteDatabaseNeeded(Cluster cluster) {
        return StringUtils.isNotEmpty(cluster.getDatabaseServerCrn());
    }

    public DatabaseServerV4Response getDatabaseServer(String serverCrn) {
        DatabaseServerV4Response resp = redbeamsClientService.getByCrn(serverCrn);
        if (resp == null) {
            throw new NotFoundException("Database server not found with crn: " + serverCrn);
        }
        return resp;
    }
}
