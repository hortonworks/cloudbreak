package com.sequenceiq.cloudbreak.converter.v2;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.template.views.LdapView;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
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

    @Override
    public BlueprintPreparationObject convert(Stack source) {
        try {
            Optional<SmartSenseSubscription> aDefault = smartSenseSubscriptionService.getDefault();
            LdapConfig ldapConfig = source.getCluster().getLdapConfig();

            return BlueprintPreparationObject.Builder.builder()
                    .withStack(source)
                    .withCluster(source.getCluster())
                    .withAmbariClient(clientFactory.getAmbariClient(source, source.getCluster()))
                    .withRdsConfigs(hiveConfigProvider.createPostgresRdsConfigIfNeeded(source, source.getCluster()))
                    .withHostgroups(hostGroupService.getByCluster(source.getCluster().getId()))
                    .withStackRepoDetails(clusterComponentConfigProvider.getHDPRepo(source.getCluster().getId()))
                    .withIdentityUser(userDetailsService.getDetails(source.getCluster().getOwner(), UserFilterField.USERID))
                    .withAmbariDatabase(clusterComponentConfigProvider.getAmbariDatabase(source.getCluster().getId()))
                    .withOrchestratorType(orchestratorTypeResolver.resolveType(source.getOrchestrator()))
                    .withFqdns(ambariFqdnCollector.collectFqdns(source))
                    .withSmartSenseSubscriptionId(aDefault.isPresent() ? Optional.of(aDefault.get().getSubscriptionId()) : Optional.empty())
                    .withLdapView(ldapConfig == null ? Optional.empty() : Optional.of(new LdapView(ldapConfig)))
                    .build();
        } catch (CloudbreakException e) {
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }
}
