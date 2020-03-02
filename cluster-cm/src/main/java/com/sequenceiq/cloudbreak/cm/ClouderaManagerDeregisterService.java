package com.sequenceiq.cloudbreak.cm;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Service
public class ClouderaManagerDeregisterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerDeregisterService.class);

    private static final String COMPONENT_NIFI_REGISTRY_SERVER = "NIFI_REGISTRY_SERVER";

    private static final String COMPONENT_NIFI_NODE = "NIFI_NODE";

    private static final String SERVICE_NIFIREGISTRY = "nifiregistry";

    private static final String SERVICE_NIFI = "nifi";

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public void deregisterServices(HttpClientConfig clientConfig, Stack stack) {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        try {
            ApiClient client = clouderaManagerApiClientProvider.getClient(stack.getGatewayPort(), user, password, clientConfig);
            CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(stack.getCluster().getBlueprint().getBlueprintText());
            if (cmTemplateProcessor.isCMComponentExistsInBlueprint(COMPONENT_NIFI_REGISTRY_SERVER)) {
                clouderaManagerApiFactory.getServicesResourceApi(client)
                        .serviceCommandByName(stack.getName(), "RemoveRangerRepo", SERVICE_NIFIREGISTRY);
            }
            if (cmTemplateProcessor.isCMComponentExistsInBlueprint(COMPONENT_NIFI_NODE)) {
                clouderaManagerApiFactory.getServicesResourceApi(client)
                        .serviceCommandByName(stack.getName(), "RemoveRangerRepo", SERVICE_NIFI);
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't remove services. This has to be done manually."
                    + " It's possible that CM or the instance is not running. Notification is sent to the UI.", e);
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(),
                    ResourceEvent.CLUSTER_CM_SECURITY_GROUP_TOO_STRICT, List.of(e.getMessage()));
        }
    }

}
