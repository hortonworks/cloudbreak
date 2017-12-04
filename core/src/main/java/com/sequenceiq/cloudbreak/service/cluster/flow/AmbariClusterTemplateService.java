package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.cluster.AmbariAuthenticationProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariServiceException;
import com.sequenceiq.cloudbreak.service.cluster.flow.kerberos.KerberosPrincipalResolver;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class AmbariClusterTemplateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterTemplateService.class);

    private static final String KEY_TYPE = "PERSISTED";

    @Inject
    private KerberosPrincipalResolver kerberosPrincipalResolver;

    @Inject
    private AmbariAuthenticationProvider ambariAuthenticationProvider;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    public void addClusterTemplate(Cluster cluster, Map<String, List<Map<String, String>>> hostGroupMappings, AmbariClient ambariClient) {
        String clusterName = cluster.getName();
        String blueprintName = cluster.getBlueprint().getAmbariName();
        String configStrategy = cluster.getConfigStrategy().name();
        String clusterTemplate;

        if (ambariClient.getClusterName() == null) {
            try {
                String repositoryVersion = ambariRepositoryVersionService.getRepositoryVersion(cluster.getId(), cluster.getStack().getOrchestrator());
                if (cluster.isSecure()) {
                    KerberosConfig kerberosConfig = cluster.getKerberosConfig();
                    String principal = kerberosPrincipalResolver.resolvePrincipalForKerberos(kerberosConfig);
                    clusterTemplate = ambariClient.createSecureCluster(clusterName, blueprintName, hostGroupMappings, configStrategy,
                            cluster.getPassword(), principal, kerberosConfig.getKerberosPassword(), KEY_TYPE, false, repositoryVersion);
                } else {
                    clusterTemplate = ambariClient.createCluster(clusterName, blueprintName, hostGroupMappings, configStrategy,
                            ambariAuthenticationProvider.getAmbariPassword(cluster), false, repositoryVersion);
                }
                LOGGER.info("Submitted cluster creation template: {}", JsonUtil.minify(clusterTemplate));
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
