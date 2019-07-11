package com.sequenceiq.cloudbreak.service.rdsconfig;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.POSTGRES;

import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

@Service
public class RedbeamsDbServerConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsDbServerConfigurer.class);

    private static final String JDBC_PATTERN = "\\/\\/(.*?):(\\d*)";

    @Inject
    private RedbeamsClientService redbeamsClientService;

    @Inject
    private SecretService secretService;

    public RDSConfig getRdsConfig(Stack stack, Cluster cluster, String db, DatabaseType type) {
        DatabaseServerV4Response resp = redbeamsClientService.getByCrn(cluster.getDatabaseServerCrn());
        if (resp == null) {
            throw new NotFoundException("RDS not found with crn: " + cluster.getDatabaseServerCrn());
        }
        LOGGER.info("Using redbeams for remote database configuration");
        return convertToRds(resp, stack, cluster, db, type);
    }

    public String getHostFromJdbcUrl(String jdbcUrl) {
        Pattern compile = Pattern.compile(JDBC_PATTERN);
        Matcher matcher = compile.matcher(jdbcUrl);
        matcher.find();
        return matcher.group(1);
    }

    public String getPortFromJdbcUrl(String jdbcUrl) {
        Pattern compile = Pattern.compile(JDBC_PATTERN);
        Matcher matcher = compile.matcher(jdbcUrl);
        matcher.find();
        return matcher.group(2);
    }

    private RDSConfig convertToRds(DatabaseServerV4Response source,
            Stack stack,
            Cluster cluster,
            String db,
            DatabaseType type) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setConnectionURL(String.format("jdbc:postgresql://%s:%s/%s", source.getHost(), source.getPort(), db));
        rdsConfig.setConnectionUserName(secretService.getByResponse(source.getConnectionUserName()));
        rdsConfig.setConnectionPassword(secretService.getByResponse(source.getConnectionPassword()));
        rdsConfig.setDatabaseEngine(POSTGRES);
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setName(type.name() + '_' + stack.getName() + stack.getId());
        rdsConfig.setType(type.name());
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setClusters(Collections.singleton(cluster));
        return rdsConfig;
    }

    public boolean isRemoteDatabaseNeeded(Cluster cluster) {
        return StringUtils.isNotEmpty(cluster.getDatabaseServerCrn());
    }
}
