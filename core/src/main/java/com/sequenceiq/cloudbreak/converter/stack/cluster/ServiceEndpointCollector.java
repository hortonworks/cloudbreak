package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;

import java.io.IOException;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.Port;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.network.NetworkUtils;

@Component
public class ServiceEndpointCollector {

    private static final int KNOX_PORT = 8443;

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEndpointCollector.class);

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    public Map<String, String> collectServiceUrlsForPorts(Cluster cluster, String ambariIp) {
        Set<HostGroup> hostGroups = cluster.getHostGroups();
        Blueprint blueprint = cluster.getBlueprint();

        Map<String, String> result = new HashMap<>();
        List<Port> ports = NetworkUtils.getAllPorts();
        try {
            JsonNode hostGroupsNode = blueprintValidator.getHostGroupNode(blueprint);
            Map<String, HostGroup> hostGroupMap = blueprintValidator.createHostGroupMap(hostGroups);
            for (JsonNode hostGroupNode : hostGroupsNode) {
                String hostGroupName = blueprintValidator.getHostGroupName(hostGroupNode);
                HostGroup actualHostgroup = hostGroupMap.get(hostGroupName);
                String serviceAddress;
                if (actualHostgroup.getConstraint().getInstanceGroup() != null) {
                    InstanceMetaData instanceMetaData = actualHostgroup.getConstraint().getInstanceGroup()
                            .getNotDeletedInstanceMetaDataSet().iterator().next();
                    serviceAddress = instanceMetaData.getPublicIpWrapper();
                } else {
                    serviceAddress = actualHostgroup.getHostMetadata().iterator().next().getHostName();
                }
                JsonNode componentsNode = blueprintValidator.getComponentsNode(hostGroupNode);
                for (JsonNode componentNode : componentsNode) {
                    String componentName = componentNode.get("name").asText();
                    StackServiceComponentDescriptor componentDescriptor = stackServiceComponentDescs.get(componentName);
                    Map<String, String> collectedPorts = collectServicePorts(ports, ambariIp, serviceAddress, componentDescriptor, cluster.getGateway());
                    result.putAll(collectedPorts);
                }
            }
        } catch (Exception ignored) {
            return result;
        }
        return result;
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

    private Map<String, String> collectServicePorts(Iterable<Port> ports, String ambariIp, String serviceAddress,
            StackServiceComponentDescriptor compDescriptor, Gateway gateway) {
        if (compDescriptor != null && compDescriptor.isMaster()) {
            if (gateway != null) {
                Map<String, String> collectedPorts = new HashMap<>();
                Set<String> exposedServices = gateway.getTopologies().stream()
                        .flatMap(this::getExposedServiceStream)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                for (Port port : ports) {
                    Map<String, String> result = doCollectUrlsForPorts(port, serviceAddress, ambariIp, compDescriptor, exposedServices, gateway);
                    collectedPorts.putAll(result);
                }
                return collectedPorts;
            }
        }
        return Collections.emptyMap();
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

    private Map<String, String> doCollectUrlsForPorts(Port port, String serviceAddress, String ambariIp,
            StackServiceComponentDescriptor componentDescriptor, Collection<String> exposedServices, Gateway gateway) {
        Map<String, String> collectedPorts = new HashMap<>();
        if (port.getExposedService().getServiceName().equalsIgnoreCase(componentDescriptor.getName())) {
            if (ambariIp != null) {
                gateway.getTopologies()
                        .stream()
                        .map(gt -> getServiceUrlForPort(port, ambariIp, exposedServices, gateway, gt.getTopologyName()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .forEach(url -> collectedPorts.put(port.getExposedService().getPortName(), url));
            } else if (serviceAddress != null) {
                String url = String.format("http://%s:%s%s", serviceAddress, port.getPort(), port.getExposedService().getPostFix());
                collectedPorts.put(port.getExposedService().getPortName(), url);
            }
        }
        return collectedPorts;
    }

    private Optional<String> getServiceUrlForPort(Port port, String ambariIp, Collection<String> exposedServices, Gateway gateway, String topologyName) {
        if (hasKnoxUrl(port) && isExposed(port, exposedServices)) {
            String url = GatewayType.CENTRAL == gateway.getGatewayType()
                    ? String.format("/%s/%s%s", gateway.getPath(), topologyName, port.getExposedService().getKnoxUrl())
                    : String.format("https://%s:%s/%s/%s%s", ambariIp, KNOX_PORT, gateway.getPath(), topologyName, port.getExposedService().getKnoxUrl());
            return Optional.of(url);
        }
        return Optional.empty();
    }

    private boolean hasKnoxUrl(Port port) {
        return StringUtils.isNotEmpty(port.getExposedService().getKnoxUrl());
    }

    private boolean isExposed(Port port, Collection<String> exposedServices) {
        return exposedServices.contains(port.getExposedService().getKnoxService());
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
}
