package com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres;

import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigProviderFactory;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsDbServerConfigurer;

@Service
public class PostgresConfigService {

    @Inject
    private RdsConfigProviderFactory rdsConfigProviderFactory;

    @Inject
    private RedbeamsDbServerConfigurer dbServerConfigurer;

    public void decorateServicePillarWithPostgresIfNeeded(Map<String, SaltPillarProperties> servicePillar, Stack stack, Cluster cluster) {
        Map<String, Object> postgresConfig = initPostgresConfig(stack, cluster);

        if (!postgresConfig.isEmpty()) {
            servicePillar.put("postgresql-server", new SaltPillarProperties("/postgresql/postgre.sls", singletonMap("postgres", postgresConfig)));
        }
    }

    public Set<RDSConfig> createRdsConfigIfNeeded(Stack stack, Cluster cluster) {
        return rdsConfigProviderFactory.getAllSupportedRdsConfigProviders().stream().map(provider ->
                provider.createPostgresRdsConfigIfNeeded(stack, cluster)).reduce((first, second) -> second).orElse(Collections.emptySet());
    }

    private Map<String, Object> initPostgresConfig(Stack stack, Cluster cluster) {
        Map<String, Object> postgresConfig = new HashMap<>();
        if (dbServerConfigurer.isRemoteDatabaseNeeded(cluster)) {
            postgresConfig.put("configure_remote_db", "true");
        }
        rdsConfigProviderFactory.getAllSupportedRdsConfigProviders().forEach(provider ->
                postgresConfig.putAll(provider.createServicePillarConfigMapIfNeeded(stack, cluster)));
        return postgresConfig;
    }

}
