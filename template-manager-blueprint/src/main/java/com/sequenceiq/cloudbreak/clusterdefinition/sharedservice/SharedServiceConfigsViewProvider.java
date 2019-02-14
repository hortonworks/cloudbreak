package com.sequenceiq.cloudbreak.clusterdefinition.sharedservice;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@Component
public class SharedServiceConfigsViewProvider {

    private static final String DEFAULT_RANGER_PORT = "6080";

    @Inject
    private AmbariBlueprintUtils ambariBlueprintUtils;

    @Inject
    private ServiceDescriptorDataProvider serviceDescriptorDataProvider;

    public SharedServiceConfigsView createSharedServiceConfigs(ClusterDefinition clusterDefinition, String ambariPassword,
            Optional<DatalakeResources> datalakeResources) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        if (datalakeResources.isPresent()) {
            DatalakeResources datalakeResource = datalakeResources.get();
            String rangerPort = serviceDescriptorDataProvider.getRangerPort(datalakeResource.getServiceDescriptorMap(), DEFAULT_RANGER_PORT);
            sharedServiceConfigsView.setRangerAdminPassword(serviceDescriptorDataProvider.getRangerAdminPassword(datalakeResource));
            sharedServiceConfigsView.setAttachedCluster(true);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setDatalakeAmbariIp(datalakeResource.getDatalakeAmbariIp());
            sharedServiceConfigsView.setDatalakeAmbariFqdn(datalakeResource.getDatalakeAmbariFqdn());
            sharedServiceConfigsView.setDatalakeComponents(datalakeResource.getDatalakeComponentSet());
            sharedServiceConfigsView.setRangerAdminPort(rangerPort);
            sharedServiceConfigsView.setRangerAdminHost(serviceDescriptorDataProvider.getRangerAdminHost(datalakeResource));
        } else if (ambariBlueprintUtils.isSharedServiceReadyBlueprint(clusterDefinition)) {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(true);
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
        } else {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
        }

        return sharedServiceConfigsView;
    }

    public SharedServiceConfigsView createSharedServiceConfigs(Stack source, Optional<DatalakeResources> datalakeResources) {
        Cluster cluster = source.getCluster();
        return createSharedServiceConfigs(cluster.getClusterDefinition(), cluster.getPassword(), datalakeResources);
    }
}
