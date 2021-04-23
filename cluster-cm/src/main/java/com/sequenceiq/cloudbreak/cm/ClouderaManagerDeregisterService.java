package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.datalake.DatalakeDto;
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

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public void deregisterServices(HttpClientConfig clientConfig, Stack stack, Optional<DatalakeDto> datalakeDto) {
        Cluster cluster = stack.getCluster();
        String user = cluster.getCloudbreakAmbariUser();
        String password = cluster.getCloudbreakAmbariPassword();
        ClouderaManagerRepo clouderaManagerRepoDetails = clusterComponentConfigProvider.getClouderaManagerRepoDetails(cluster.getId());
        try {
            CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(stack.getCluster().getBlueprint().getBlueprintText());
            if (CMRepositoryVersionUtil.isRangerTearDownSupported(clouderaManagerRepoDetails)) {
                if (datalakeDto.isPresent()) {
                    LOGGER.info("The current cluster {} is a Data Hub cluster so teardown REQUIRED.", stack.getName());
                    DatalakeDto datalake = datalakeDto.get();
                    ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(datalakeClient(datalake));
                    clustersResourceApi.tearDownWorkloadCluster(datalake.getName(), stack.getName());
                } else {
                    LOGGER.info("The current cluster {} is a Data Lake cluster so teardown NOT_REQUIRED.", stack.getName());
                }
            } else {
                ApiClient client = clouderaManagerApiClientProvider.getV31Client(stack.getGatewayPort(), user, password, clientConfig);
                if (cmTemplateProcessor.isCMComponentExistsInBlueprint(COMPONENT_NIFI_REGISTRY_SERVER)) {
                    LOGGER.info("The current cluster {} contains NIFI_REGISTRY_SERVER and ranger teardown not supported. " +
                            "CDP will call RemoveRangerRepo command.", stack.getName());
                    clouderaManagerApiFactory.getServicesResourceApi(client)
                            .serviceCommandByName(stack.getName(), "RemoveRangerRepo", SERVICE_NIFIREGISTRY);
                }
                if (cmTemplateProcessor.isCMComponentExistsInBlueprint(COMPONENT_NIFI_NODE)) {
                    LOGGER.info("The current cluster {} contains NIFI_NODE and ranger teardown not supported. " +
                            "CDP will call RemoveRangerRepo command.", stack.getName());
                    clouderaManagerApiFactory.getServicesResourceApi(client)
                            .serviceCommandByName(stack.getName(), "RemoveRangerRepo", SERVICE_NIFI);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Couldn't remove services. This has to be done manually."
                    + " It's possible that CM or the instance is not running. Notification is sent to the UI.", e);
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(),
                    ResourceEvent.CLUSTER_CM_SECURITY_GROUP_TOO_STRICT, List.of(e.getMessage()));
        }
    }

    private ApiClient datalakeClient(DatalakeDto datalakeDto) throws ClouderaManagerClientInitException {
        return clouderaManagerApiClientProvider.getV43Client(
                datalakeDto.getGatewayPort(),
                datalakeDto.getUser(),
                datalakeDto.getPassword(),
                datalakeDto.getHttpClientConfig());
    }

}
