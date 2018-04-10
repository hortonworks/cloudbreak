package com.sequenceiq.cloudbreak.service.cluster.ambari;

import com.sequenceiq.ambari.client.services.ClusterService;
import com.sequenceiq.cloudbreak.blueprint.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AmbariClusterTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterTemplateService.class);

    private static final String KEY_TYPE = "PERSISTED";

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Inject
    private AmbariSecurityConfigProvider ambariSecurityConfigProvider;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    public void addClusterTemplate(Cluster cluster, Map<String, List<Map<String, String>>> hostGroupMappings, ClusterService ambariClient) {
        String clusterName = cluster.getName();
        String blueprintName = cluster.getBlueprint().getAmbariName();
        String configStrategy = cluster.getConfigStrategy().name();
        String clusterTemplate;

        if (ambariClient.getClusterName() == null) {
            try {
                String repositoryVersion = ambariRepositoryVersionService.getRepositoryVersion(cluster.getId(), cluster.getStack().getOrchestrator());
                if (cluster.isSecure()) {
                    KerberosConfig kerberosConfig = cluster.getKerberosConfig();
                    String principal = kerberosDetailService.resolvePrincipalForKerberos(kerberosConfig);
                    // TODO: do this in Java
                    clusterTemplate = ambariClient.createSecureCluster(clusterName, blueprintName, hostGroupMappings, configStrategy,
                            ambariSecurityConfigProvider.getAmbariPassword(cluster), principal, kerberosConfig.getPassword(), KEY_TYPE, false,
                            repositoryVersion);
                } else {
                    clusterTemplate = ambariClient.createCluster(clusterName, blueprintName, hostGroupMappings, configStrategy,
                            ambariSecurityConfigProvider.getAmbariPassword(cluster), false, repositoryVersion);
                }
                LOGGER.info("Submitted cluster creation template: {}", JsonUtil.minify(clusterTemplate, Collections.singleton("credentials")));
            } catch (Exception exception) {
                String msg = "Ambari client failed to apply cluster creation template.";
                LOGGER.error(msg, exception);
                throw new AmbariServiceException(msg, exception);
            }
        } else {
            LOGGER.info("Ambari cluster already exists: {}", clusterName);
        }
    }
}
