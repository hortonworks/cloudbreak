package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.services.ServiceAndHostService;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.cluster.filter.ConfigParam;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Service
public class AmbariConfigurationService {

    private static final Collection<String> CONFIG_LIST = new ArrayList<>(ConfigParam.values().length);

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariConfigurationService.class);

    private static final String AZURE_ADDRESS_SUFFIX = "cloudapp.net";

    @Value("${cb.ambari.database.databaseEngine}")
    private String databaseEngine;

    @Value("${cb.ambari.database.name}")
    private String name;

    @Value("${cb.ambari.database.host}")
    private String host;

    @Value("${cb.ambari.database.port}")
    private Integer port;

    @Value("${cb.ambari.database.username}")
    private String userName;

    @Inject
    private RdsConfigRepository rdsConfigRepository;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AmbariDatabaseMapper ambariDatabaseMapper;

    static {
        for (ConfigParam param : ConfigParam.values()) {
            CONFIG_LIST.add(param.key());
        }
    }

    public Map<String, String> getConfiguration(ServiceAndHostService ambariClient, String hostGroup) {
        Map<String, String> configuration = new HashMap<>();
        Set<Entry<String, Map<String, String>>> serviceConfigs = ambariClient.getServiceConfigMapByHostGroup(hostGroup).entrySet();
        for (Entry<String, Map<String, String>> serviceEntry : serviceConfigs) {
            for (Entry<String, String> configEntry : serviceEntry.getValue().entrySet()) {
                if (CONFIG_LIST.contains(configEntry.getKey())) {
                    configuration.put(configEntry.getKey(), replaceHostName(ambariClient, configEntry));
                }
            }
        }
        return configuration;
    }

    private String replaceHostName(ServiceAndHostService ambariClient, Entry<String, String> entry) {
        String result = entry.getValue();
        if (entry.getKey().startsWith("yarn.resourcemanager")) {
            int portStartIndex = result.indexOf(':');
            String internalAddress = result.substring(0, portStartIndex);
            String publicAddress = ambariClient.resolveInternalHostName(internalAddress);
            if (internalAddress.equals(publicAddress)) {
                if (internalAddress.contains(AZURE_ADDRESS_SUFFIX)) {
                    publicAddress = internalAddress.substring(0, internalAddress.indexOf('.') + 1) + AZURE_ADDRESS_SUFFIX;
                }
            }
            result = publicAddress + result.substring(portStartIndex);
        }
        return result;
    }

    public Optional<RDSConfig> createDefaultRdsConfigIfNeeded(Stack stack, Cluster cluster) {
        Set<RDSConfig> rdsConfigs = cluster.getRdsConfigs();
        if (rdsConfigs.stream().noneMatch(rdsConfig -> rdsConfig.getType().equalsIgnoreCase(RdsType.AMBARI.name()))) {
            LOGGER.info("Creating Ambari RDSConfig");
            return Optional.of(createAmbariDefaultRdsConf(stack, cluster, rdsConfigs));
        }
        return Optional.empty();
    }

    private RDSConfig createAmbariDefaultRdsConf(Stack stack, Cluster cluster, Set<RDSConfig> rdsConfigs) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(ambariDatabaseMapper.mapName(stack));
        rdsConfig.setConnectionUserName(userName);
        rdsConfig.setConnectionPassword(PasswordUtil.generatePassword());
        rdsConfig.setConnectionURL("jdbc:postgresql://" + host + ":" + port + "/" + name);
        rdsConfig.setDatabaseEngine(databaseEngine);
        rdsConfig.setType(RdsType.AMBARI.name());
        rdsConfig.setStatus(ResourceStatus.DEFAULT);
        rdsConfig.setCreationDate(new Date().getTime());
        rdsConfig.setOwner(stack.getOwner());
        rdsConfig.setAccount(stack.getAccount());
        rdsConfig.setClusters(Collections.singleton(cluster));
        rdsConfig.setConnectionDriver("org.postgresql.Driver");
        return rdsConfigService.create(rdsConfig);
    }

}
