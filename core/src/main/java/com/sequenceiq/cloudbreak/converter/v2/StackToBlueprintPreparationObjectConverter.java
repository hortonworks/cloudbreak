package com.sequenceiq.cloudbreak.converter.v2;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.template.views.LdapView;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientFactory;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariFqdnCollector;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.HiveConfigProvider;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

@Component
public class StackToBlueprintPreparationObjectConverter extends AbstractConversionServiceAwareConverter<Stack, BlueprintPreparationObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToBlueprintPreparationObjectConverter.class);

    @Inject
    private AmbariClientFactory clientFactory;

    @Inject
    private HiveConfigProvider hiveConfigProvider;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private AmbariFqdnCollector ambariFqdnCollector;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Inject
    private StackInfoService stackInfoService;

    @Inject
    private HdfConfigProvider hdfConfigProvider;

    @Override
    public BlueprintPreparationObject convert(Stack source) {
        try {
            Optional<SmartSenseSubscription> aDefault = smartSenseSubscriptionService.getDefault();
            Cluster cluster = source.getCluster();
            LdapConfig ldapConfig = cluster.getLdapConfig();
            StackRepoDetails hdpRepo = clusterComponentConfigProvider.getHDPRepo(cluster.getId());
            Map<String, List<String>> fqdns = ambariFqdnCollector.collectFqdns(source);

            return BlueprintPreparationObject.Builder.builder()
                    .withStack(source)
                    .withCluster(cluster)
                    .withAmbariClient(clientFactory.getAmbariClient(source, cluster))
                    .withRdsConfigs(hiveConfigProvider.createPostgresRdsConfigIfNeeded(source, cluster))
                    .withHostgroups(hostGroupService.getByCluster(cluster.getId()))
                    .withStackRepoDetails(hdpRepo)
                    .withIdentityUser(userDetailsService.getDetails(cluster.getOwner(), UserFilterField.USERID))
                    .withAmbariDatabase(clusterComponentConfigProvider.getAmbariDatabase(cluster.getId()))
                    .withOrchestratorType(orchestratorTypeResolver.resolveType(source.getOrchestrator()))
                    .withFqdns(fqdns)
                    .withBlueprintStackInfo(stackInfoService.blueprintStackInfo(cluster.getBlueprint().getBlueprintText()))
                    .withSmartSenseSubscriptionId(aDefault.isPresent() ? Optional.of(aDefault.get().getSubscriptionId()) : Optional.empty())
                    .withLdapView(ldapConfig == null ? Optional.empty() : Optional.of(new LdapView(ldapConfig)))
                    .withHdfConfigs(hdfConfigProvider.nodeIdentities(cluster.getHostGroups(), fqdns, cluster.getBlueprint().getBlueprintText()))
                    .build();
        } catch (CloudbreakException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        } catch (IOException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }
}
