package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ClusterExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.ExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Component
public class ServiceEndpointCollector {

    private static final int KNOX_PORT = 8443;

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEndpointCollector.class);

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    public Collection<ExposedServiceResponse> getKnoxServices(IdentityUser cbUser, String blueprintName) {
        Blueprint blueprint = blueprintService.getByName(blueprintName, cbUser);
        BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(blueprint.getBlueprintText());
        Set<String> blueprintComponents = blueprintTextProcessor.getAllComponents();
        String stackName = blueprintTextProcessor.getStackName();
        String stackVersion = blueprintTextProcessor.getStackVersion();
        VersionComparator versionComparator = new VersionComparator();
        if ("HDF".equals(stackName) && versionComparator.compare(() -> stackVersion, () -> "3.2") < 0) {
            return Collections.emptyList();
        } else {
            Collection<ExposedService> exposedServices = ExposedService.knoxServicesForComponents(blueprintComponents);
            return ExposedServiceResponse.fromExposedServices(exposedServices);
        }
    }

    public String getAmbariServerUrl(Cluster cluster, String ambariIp) {
        if (ambariIp != null) {
            String orchestrator = cluster.getStack().getOrchestrator().getType();
            if (YARN.equals(orchestrator)) {
                return String.format("http://%s:8080", ambariIp);
            } else {
                Gateway gateway = cluster.getGateway();
                if (gateway != null) {
                    Optional<GatewayTopology> gatewayTopologyWithAmbari = getGatewayTopologyWithAmbari(gateway);
                    Optional<String> ambariUrl = gatewayTopologyWithAmbari.map(gt -> getAmbariUrlFromGatewayTopology(ambariIp, gateway, gt));
                    if (ambariUrl.isPresent()) {
                        return ambariUrl.get();
                    }
                }
                return String.format("https://%s/ambari/", ambariIp);
            }
        }
        return null;
    }

    private Stream<String> getExposedServiceStream(GatewayTopology gatewayTopology) {
        if (gatewayTopology.getExposedServices() != null && gatewayTopology.getExposedServices().getValue() != null) {
            try {
                return gatewayTopology.getExposedServices().get(ExposedServices.class).getServices().stream();
            } catch (IOException e) {
                LOGGER.warn("Failed to get exposed services from Json.", e);
            }
        }
        return Stream.empty();
    }

    private Optional<String> getServiceUrlForService(ExposedService exposedService, String ambariIp, Gateway gateway, String topologyName) {
        if (hasKnoxUrl(exposedService) && ambariIp != null) {
            String url = GatewayType.CENTRAL == gateway.getGatewayType()
                    ? String.format("/%s/%s%s", gateway.getPath(), topologyName, exposedService.getKnoxUrl())
                    : String.format("https://%s:%s/%s/%s%s", ambariIp, KNOX_PORT, gateway.getPath(), topologyName, exposedService.getKnoxUrl());
            return Optional.of(url);
        }
        return Optional.empty();
    }

    private boolean hasKnoxUrl(ExposedService exposedService) {
        return StringUtils.isNotEmpty(exposedService.getKnoxUrl());
    }

    private boolean isExposed(ExposedService exposedService, Collection<String> exposedServices) {
        return exposedServices.contains(exposedService.getKnoxService());
    }

    private Optional<GatewayTopology> getGatewayTopologyWithAmbari(Gateway gateway) {
        return gateway.getTopologies().stream()
                .filter(gt -> getExposedServiceStream(gt)
                        .anyMatch(es -> ExposedService.AMBARI.getServiceName().equalsIgnoreCase(es)
                                || ExposedService.AMBARI.name().equalsIgnoreCase(es)
                                || ExposedService.AMBARI.getKnoxService().equalsIgnoreCase(es)))
                .findFirst();
    }

    private String getAmbariUrlFromGatewayTopology(String ambariIp, Gateway gateway, GatewayTopology gt) {
        return GatewayType.CENTRAL == gateway.getGatewayType()
                ? String.format("/%s/%s/ambari/", gateway.getPath(), gt.getTopologyName())
                : String.format("https://%s:%s/%s/%s/ambari/", ambariIp, KNOX_PORT, gateway.getPath(), gt.getTopologyName());
    }

    public Map<String, Collection<ClusterExposedServiceResponse>> prepareClusterExposedServices(Cluster cluster, String ambariIp) {
        BlueprintTextProcessor blueprintTextProcessor = new BlueprintProcessorFactory().get(cluster.getBlueprint().getBlueprintText());
        Set<String> componentsInBlueprint = blueprintTextProcessor.getAllComponents();
        Collection<ExposedService> knownExposedServices = ExposedService.knoxServicesForComponents(componentsInBlueprint);

        Gateway gateway = cluster.getGateway();
        Map<String, Collection<ClusterExposedServiceResponse>> clusterExposedServiceMap = new HashMap<>();
        if (gateway != null) {
            for (GatewayTopology gatewayTopology : gateway.getTopologies()) {
                List<ClusterExposedServiceResponse> clusterExposedServiceResponses = new ArrayList<>();
                Set<String> exposedServicesInTopology = gateway.getTopologies().stream()
                        .flatMap(this::getExposedServiceStream)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                for (ExposedService exposedService : knownExposedServices) {
                    ClusterExposedServiceResponse clusterExposedServiceResponse = new ClusterExposedServiceResponse();
                    clusterExposedServiceResponse.setDisplayName(exposedService.getPortName());
                    clusterExposedServiceResponse.setKnoxService(exposedService.getKnoxService());
                    clusterExposedServiceResponse.setServiceName(exposedService.getServiceName());
                    Optional<String> serviceUrlForService = getServiceUrlForService(exposedService, ambariIp,
                            gateway, gatewayTopology.getTopologyName());
                    serviceUrlForService.ifPresent(clusterExposedServiceResponse::setServiceUrl);
                    clusterExposedServiceResponse.setOpen(isExposed(exposedService, exposedServicesInTopology));
                    clusterExposedServiceResponses.add(clusterExposedServiceResponse);
                }
                clusterExposedServiceMap.put(gatewayTopology.getTopologyName(), clusterExposedServiceResponses);
            }
        }

        return clusterExposedServiceMap;
    }
}
