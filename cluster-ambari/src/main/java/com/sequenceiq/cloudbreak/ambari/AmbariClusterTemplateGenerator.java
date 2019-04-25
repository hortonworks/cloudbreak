package com.sequenceiq.cloudbreak.ambari;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.ambari.client.services.ClusterService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Service
public class AmbariClusterTemplateGenerator {

    private static final String KEY_TYPE = "PERSISTED";

    private static final String PRINCIPAL = "/admin";

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    String generateClusterTemplate(Cluster cluster, Map<String, List<Map<String, String>>> hostGroupMappings,
            ClusterService ambariClient) {
        String blueprintName = cluster.getBlueprint().getStackName();
        String configStrategy = ofNullable(cluster.getConfigStrategy()).orElse(ConfigStrategy.ALWAYS_APPLY_DONT_OVERRIDE_CUSTOM_VALUES).name();
        String clusterTemplate;

        String repositoryVersion = ambariRepositoryVersionService.getRepositoryVersion(cluster.getId());
        if (cluster.getKerberosConfig() != null) {
            KerberosConfig kerberosConfig = cluster.getKerberosConfig();
            String principal = resolvePrincipalForKerberos(kerberosConfig);
            clusterTemplate = ambariClient.createClusterJson(blueprintName, hostGroupMappings,
                    ambariSecurityConfigProvider.getClusterUserProvidedPassword(cluster), configStrategy,
                    principal, kerberosConfig.getPassword(), KEY_TYPE, false, repositoryVersion);
        } else {
            clusterTemplate = ambariClient.createClusterJson(blueprintName, hostGroupMappings,
                    ambariSecurityConfigProvider.getClusterUserProvidedPassword(cluster), configStrategy,
                    null, null, null, false, repositoryVersion);
        }
        return clusterTemplate;
    }

    private String resolvePrincipalForKerberos(@Nonnull KerberosConfig kerberosConfig) {
        return Strings.isNullOrEmpty(kerberosConfig.getPrincipal()) ? kerberosConfig.getAdmin() + PRINCIPAL
                : kerberosConfig.getPrincipal();
    }

}
