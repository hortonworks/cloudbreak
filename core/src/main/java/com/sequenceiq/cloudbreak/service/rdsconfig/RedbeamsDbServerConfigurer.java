package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.RdsSslMode;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.SslConfigV4Response;

@Service
public class RedbeamsDbServerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDbServerConfigurer.class);

    @Inject
    private RedbeamsClientService redbeamsClientService;

    @Inject
    private DatabaseCommon dbCommon;

    @Inject
    private DbUsernameConverterService dbUsernameConverterService;

    /**
     * Creates an RDSConfig object for a specific database.
     *
     * @param stack   stack for naming purposes
     * @param cluster cluster to associate database with
     * @param dbName  database name
     * @param dbUser  database user
     * @param type    database type
     * @return RDSConfig object for database
     */
    public RDSConfig createNewRdsConfig(String stackName, Long stackId, String dbServerCrn, Long clusterId, String dbName, String dbUser, DatabaseType type) {
        DatabaseServerV4Response resp = getDatabaseServer(dbServerCrn);
        LOGGER.info("Using redbeams for remote database configuration: {}", resp.toString());
        if (Objects.nonNull(resp.getStatus()) && !resp.getStatus().isAvailable()) {
            String message = String.format("Redbeams database server is not available (%s) with message: %s", resp.getStatus(), resp.getStatusReason());
            LOGGER.warn(message);
            throw new CloudbreakServiceException(message);
        }

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionURL(dbCommon.getJdbcConnectionUrl(resp.getDatabaseVendor(), resp.getHost(), resp.getPort(), Optional.of(dbName)));
        rdsConfig.setSslMode(getSslMode(resp));
        rdsConfig.setConnectionUserName(dbUsernameConverterService.toConnectionUsername(resp.getHost(), dbUser));
        rdsConfig.setConnectionPassword(PasswordUtil.generatePassword());
        rdsConfig.setConnectionDriver(resp.getConnectionDriver());
        rdsConfig.setDatabaseEngine(DatabaseVendor.fromValue(resp.getDatabaseVendor()));
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setName(type.name() + '_' + stackName + stackId);
        rdsConfig.setType(type.name());
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setCreationDate(new Date().getTime());
        Cluster attachedCluster = new Cluster();
        attachedCluster.setId(clusterId);
        rdsConfig.setClusters(Collections.singleton(attachedCluster));
        LOGGER.info("Created RDS config {} for database type {} with connection URL {}, connection username {}",
                rdsConfig.getName(), type, rdsConfig.getConnectionURL(), rdsConfig.getConnectionUserName());
        return rdsConfig;
    }

    private RdsSslMode getSslMode(DatabaseServerV4Response response) {
        SslMode sslMode = Optional.ofNullable(response.getSslConfig()).map(SslConfigV4Response::getSslMode).orElse(null);
        return SslMode.isEnabled(sslMode) ? RdsSslMode.ENABLED : RdsSslMode.DISABLED;
    }

    public boolean isRemoteDatabaseNeeded(String dbServerCrn) {
        return StringUtils.isNotEmpty(dbServerCrn);
    }

    public DatabaseServerV4Response getDatabaseServer(String serverCrn) {
        DatabaseServerV4Response resp = redbeamsClientService.getByCrn(serverCrn);
        if (resp == null) {
            throw new NotFoundException("Database server not found with crn: " + serverCrn);
        }
        return resp;
    }
}
