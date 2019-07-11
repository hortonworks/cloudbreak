package com.sequenceiq.cloudbreak.service.rdsconfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.util.PasswordUtil;

public abstract class AbstractRdsConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRdsConfigProvider.class);

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    @Inject
    private RedbeamsClientService redbeamsClientService;

    public Map<String, Object> createServicePillarConfigMapIfNeeded(Stack stack, Cluster cluster) {
        if (isRdsConfigNeeded(cluster.getBlueprint())) {
            Set<RDSConfig> rdsConfigs = createPostgresRdsConfigIfNeeded(stack, cluster);
            RDSConfig rdsConfig = rdsConfigs.stream().filter(rdsConfig1 -> rdsConfig1.getType().equalsIgnoreCase(getRdsType().name())).findFirst().get();
            if (rdsConfig.getStatus() == ResourceStatus.DEFAULT && rdsConfig.getDatabaseEngine() != DatabaseVendor.EMBEDDED) {
                Map<String, Object> postgres = new HashMap<>();
                String db = getDb();
                if (dbServerConfigurer.isRemoteDatabaseNeeded(cluster)) {
                    postgres.put("remote_db_url", dbServerConfigurer.getHostFromJdbcUrl(rdsConfig.getConnectionURL()));
                    postgres.put("remote_db_port", dbServerConfigurer.getPortFromJdbcUrl(rdsConfig.getConnectionURL()));
                    postgres.put("remote_admin", rdsConfig.getConnectionUserName());
                    postgres.put("remote_admin_pw", rdsConfig.getConnectionPassword());
                }
                postgres.put("database", db);
                postgres.put("user", getDbUser());
                postgres.put("password", rdsConfig.getConnectionPassword());
                LOGGER.debug("Rds config added: {}, databaseEngine: {}", db, rdsConfig.getDatabaseEngine());
                return Collections.singletonMap(getPillarKey(), postgres);
            }
        }
        return Collections.emptyMap();
    }

    public Set<RDSConfig> createPostgresRdsConfigIfNeeded(Stack stack, Cluster cluster) {
        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId());
        rdsConfigs = rdsConfigs.stream().map(c -> rdsConfigService.resolveVaultValues(c)).collect(Collectors.toSet());
        if (isRdsConfigNeeded(cluster.getBlueprint())
                && rdsConfigs.stream().noneMatch(rdsConfig -> rdsConfig.getType().equalsIgnoreCase(getRdsType().name()))) {
            if (dbServerConfigurer.isRemoteDatabaseNeeded(cluster)) {
                RDSConfig rbRdsConfig = dbServerConfigurer.getRdsConfig(stack, cluster, getDb(), getRdsType());
                rdsConfigs = populateRdsConfig(rdsConfigs, stack, cluster, rbRdsConfig);
            } else {
                LOGGER.debug("Creating postgres Database for {}", getRdsType().name());
                rdsConfigs = createPostgresRdsConf(stack, cluster, rdsConfigs, getDbUser(), getDbPort(), getDb());
            }
        }
        return rdsConfigs;
    }

    private Set<RDSConfig> populateRdsConfig(Set<RDSConfig> rdsConfigs, Stack stack, Cluster cluster, RDSConfig rdsConfig) {
        rdsConfig = rdsConfigService.createIfNotExists(stack.getCreator(), rdsConfig, stack.getWorkspace().getId());
        if (rdsConfigs == null) {
            rdsConfigs = new HashSet<>();
        }
        rdsConfigs.add(rdsConfig);
        cluster.setRdsConfigs(rdsConfigs);
        clusterService.save(cluster);

        return rdsConfigs;
    }

    private Set<RDSConfig> createPostgresRdsConf(Stack stack, Cluster cluster,
            Set<RDSConfig> rdsConfigs, String dbUserName, String dbPort, String dbName) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(getRdsType().name() + '_' + stack.getName() + stack.getId());
        rdsConfig.setConnectionUserName(dbUserName);
        rdsConfig.setConnectionPassword(PasswordUtil.generatePassword());
        String primaryGatewayIp = stack.getPrimaryGatewayInstance().getPrivateIp();
        rdsConfig.setConnectionURL(String.format("jdbc:postgresql://%s:%s/%s", primaryGatewayIp, dbPort, dbName));
        rdsConfig.setDatabaseEngine(DatabaseVendor.POSTGRES);
        rdsConfig.setType(getRdsType().name());
        rdsConfig.setConnectionDriver(DatabaseVendor.POSTGRES.connectionDriver());
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setClusters(Collections.singleton(cluster));
        return populateRdsConfig(rdsConfigs, stack, cluster, rdsConfig);
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

    protected abstract boolean isRdsConfigNeeded(Blueprint blueprint);

}
