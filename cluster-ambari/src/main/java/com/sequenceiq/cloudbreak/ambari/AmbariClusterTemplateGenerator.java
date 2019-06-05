package com.sequenceiq.cloudbreak.ambari;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.services.ClusterService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;

@Service
public class AmbariClusterTemplateGenerator {

    private static final String KEY_TYPE = "PERSISTED";

    private static final String PRINCIPAL = "/admin";

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    String generateClusterTemplate(Cluster cluster, Map<String, List<Map<String, String>>> hostGroupMappings,
            ClusterService ambariClient, KerberosConfig kerberosConfig) {
        String blueprintName = cluster.getBlueprint().getStackName();
        String configStrategy = ofNullable(cluster.getConfigStrategy()).orElse(ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES).name();
        String clusterTemplate;

        String repositoryVersion = ambariRepositoryVersionService.getRepositoryVersion(cluster.getId());
        if (kerberosConfig != null) {
            clusterTemplate = ambariClient.createClusterJson(blueprintName, hostGroupMappings,
                    ambariSecurityConfigProvider.getClusterUserProvidedPassword(cluster), configStrategy,
                    kerberosConfig.getPrincipal(), kerberosConfig.getPassword(), KEY_TYPE, false, repositoryVersion);
        } else {
            clusterTemplate = ambariClient.createClusterJson(blueprintName, hostGroupMappings,
                    ambariSecurityConfigProvider.getClusterUserProvidedPassword(cluster), configStrategy,
                    null, null, null, false, repositoryVersion);
        }
        return clusterTemplate;
    }
}
