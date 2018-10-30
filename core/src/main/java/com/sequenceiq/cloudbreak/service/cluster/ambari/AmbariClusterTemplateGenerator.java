package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.services.ClusterService;
import com.sequenceiq.cloudbreak.blueprint.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.VaultService;

@Service
public class AmbariClusterTemplateGenerator {

    private static final String KEY_TYPE = "PERSISTED";

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    @Inject
    private VaultService vaultService;

    String generateClusterTemplate(Cluster cluster, Map<String, List<Map<String, String>>> hostGroupMappings,
            ClusterService ambariClient) throws CloudbreakException {
        String blueprintName = cluster.getBlueprint().getAmbariName();
        String configStrategy = cluster.getConfigStrategy().name();
        String clusterTemplate;

        String repositoryVersion = ambariRepositoryVersionService.getRepositoryVersion(cluster.getId(), cluster.getStack().getOrchestrator());
        if (cluster.isSecure()) {
            KerberosConfig kerberosConfig = cluster.getKerberosConfig();
            String principal = kerberosDetailService.resolvePrincipalForKerberos(kerberosConfig);
            clusterTemplate = ambariClient.createClusterJson(blueprintName, hostGroupMappings,
                    ambariSecurityConfigProvider.getAmbariUserProvidedPassword(cluster), configStrategy,
                    principal, vaultService.resolveSingleValue(kerberosConfig.getPassword()), KEY_TYPE, false, repositoryVersion);
        } else {
            clusterTemplate = ambariClient.createClusterJson(blueprintName, hostGroupMappings,
                    ambariSecurityConfigProvider.getAmbariUserProvidedPassword(cluster), configStrategy,
                    null, null, null, false, repositoryVersion);
        }
        return clusterTemplate;
    }

}
