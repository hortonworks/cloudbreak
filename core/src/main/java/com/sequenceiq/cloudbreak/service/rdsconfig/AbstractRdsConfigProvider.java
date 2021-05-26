package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

public abstract class AbstractRdsConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRdsConfigProvider.class);

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Inject
    private SecretService secretService;

    @Inject
    private DbUsernameConverterService dbUsernameConverterService;

    /**
     * Creates a map of database information for this provider's database, suitable for inclusion
     * in a salt pillar. The map usually contains only one key, that being the return value of
     * {@link #getPillarKey()}. The configuration information is ultimately used to create the
     * database.
     *
     * @param  stack   stack for cluster
     * @param  cluster cluster to associate database with
     * @return         salt pillar configuration information for this provider's database
     */
    public Map<String, Object> createServicePillarConfigMapIfNeeded(Stack stack, Cluster cluster) {
        if (isRdsConfigNeeded(cluster.getBlueprint(), cluster.hasGateway())) {
            Set<RDSConfig> rdsConfigs = createPostgresRdsConfigIfNeeded(stack, cluster);
            RDSConfig rdsConfig = rdsConfigs.stream().filter(rdsConfig1 -> rdsConfig1.getType().equalsIgnoreCase(getRdsType().name())).findFirst().get();
            if (rdsConfig.getStatus() == ResourceStatus.DEFAULT && rdsConfig.getDatabaseEngine() != DatabaseVendor.EMBEDDED) {
                Map<String, Object> postgres = new HashMap<>();
                if (dbServerConfigurer.isRemoteDatabaseNeeded(cluster)) {
                    DatabaseServerV4Response dbServerResponse = dbServerConfigurer.getDatabaseServer(cluster.getDatabaseServerCrn());
                    postgres.put("remote_db_url", dbServerResponse.getHost());
                    postgres.put("remote_db_port", dbServerResponse.getPort());
                    postgres.put("remote_admin", secretService.getByResponse(dbServerResponse.getConnectionUserName()));
                    postgres.put("remote_admin_pw", secretService.getByResponse(dbServerResponse.getConnectionPassword()));
                }
                String dbName = getDb();
                postgres.put("database", dbName);
                postgres.put("user", dbUsernameConverterService.toDatabaseUsername(rdsConfig.getConnectionUserName()));
                postgres.put("password", rdsConfig.getConnectionPassword());
                LOGGER.debug("RDS config added to pillar for name: {} databaseEngine: {}", dbName, rdsConfig.getDatabaseEngine());
                return Collections.singletonMap(getPillarKey(), postgres);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Creates a new RDSConfig object for this provider's database, if one is not already present in
     * the cluster and if one is called for by the cluster blueprint. Most of the database
     * information is based on the database type and this provider, but the database password is
     * chosen here.
     *
     * Note that this does not actually create the database. However, the RDSConfig object is saved
     * and associated with the cluster.
     *
     * @param  stack   stack for cluster
     * @param  cluster cluster to associate database with
     * @return         all RDSConfig objects for the cluster, potentially including a new one for
     *                 this provider's database
     */
    public Set<RDSConfig> createPostgresRdsConfigIfNeeded(Stack stack, Cluster cluster) {
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId());
        rdsConfigs = rdsConfigs.stream().map(c -> rdsConfigService.resolveVaultValues(c)).collect(Collectors.toSet());
        if (isRdsConfigNeeded(cluster.getBlueprint(), cluster.hasGateway())
                && rdsConfigs.stream().noneMatch(rdsConfig -> rdsConfig.getType().equalsIgnoreCase(getRdsType().name()))) {
            RDSConfig newRdsConfig;
            if (dbServerConfigurer.isRemoteDatabaseNeeded(cluster)) {
                newRdsConfig = dbServerConfigurer.createNewRdsConfig(stack, cluster, getDb(), getDbUser(), getRdsType());
            } else {
                LOGGER.debug("Creating postgres Database for {}", getRdsType().name());
                newRdsConfig = createNewRdsConfig(stack, cluster, getDb(), getDbUser(), getDbPort());
            }
            rdsConfigs = populateNewRdsConfig(rdsConfigs, stack, cluster, newRdsConfig);
        }
        return rdsConfigs;
    }

    private Set<RDSConfig> populateNewRdsConfig(Set<RDSConfig> rdsConfigs, Stack stack, Cluster cluster, RDSConfig rdsConfig) {
        rdsConfig = rdsConfigService.createIfNotExists(stack.getCreator(), rdsConfig, stack.getWorkspace().getId());
        rdsConfigs.add(rdsConfig);
        cluster.setRdsConfigs(rdsConfigs);
        clusterService.save(cluster);

        return rdsConfigs;
    }

    /**
     * Creates an RDSConfig object for a specific database.
     *
     * @param  stack   stack for naming purposes
     * @param  cluster cluster to associate database with
     * @param  dbName  database name
     * @param  dbUserName  database user
     * @param  dbPort  port for database connections (through gateway)
     * @return         RDSConfig object for database
     */
    private RDSConfig createNewRdsConfig(Stack stack, Cluster cluster, String dbName, String dbUserName, String dbPort) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(getRdsType().name() + '_' + stack.getName() + stack.getId());
        rdsConfig.setConnectionUserName(dbUserName);
        rdsConfig.setConnectionPassword(PasswordUtil.generatePassword());
        String databaseHost = stack.getPrimaryGatewayInstance().getDiscoveryFQDN();
        rdsConfig.setConnectionURL(String.format("jdbc:postgresql://%s:%s/%s", databaseHost, dbPort, dbName));
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        rdsConfig.setType(getRdsType().name());
        rdsConfig.setConnectionDriver(DatabaseVendor.POSTGRES.connectionDriver());
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setClusters(Collections.singleton(cluster));
        return rdsConfig;
    }

    protected List<String[]> createPathListFromConfigurations(String[] path, String[] configurations) {
        List<String[]> pathList = new ArrayList<>();
        Arrays.stream(configurations).forEach(configuration -> {
            List<String> pathWithConfig = Lists.newArrayList(path);
            pathWithConfig.add(configuration);
            pathList.add(pathWithConfig.toArray(new String[pathWithConfig.size()]));
        });
        return pathList;
    }

    protected abstract String getDbUser();

    protected abstract String getDb();

    protected abstract String getDbPort();

    protected abstract String getPillarKey();

    protected abstract DatabaseType getRdsType();

    protected abstract boolean isRdsConfigNeeded(Blueprint blueprint, boolean knoxGateway);

}
