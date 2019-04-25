package com.sequenceiq.cloudbreak.service;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Response;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintTextProcessorUtil;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.ClusterManagerType;

@Service
public class ServiceEndpointCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEndpointCollector.class);

    @Value("${cb.knox.port}")
    private String knoxPort;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private AmbariHaComponentFilter ambariHaComponentFilter;

    @Inject
    private ComponentLocatorService componentLocatorService;

    public Collection<ExposedServiceV4Response> getKnoxServices(Long workspaceId, String blueprintName) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(blueprintName, workspaceId);
        return getKnoxServices(blueprint);
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
                    // when knox gateway is enabled, but ambari is not exposed, there is no available ambari URL
                    return ambariUrl.orElse("");
                }
                return String.format("https://%s/", ambariIp);
            }
        }
        return null;
    }

    public Map<String, Collection<ClusterExposedServiceV4Response>> prepareClusterExposedServices(Cluster cluster, String ambariIp) {
        if (cluster.getBlueprint() != null) {
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            if (StringUtils.isNotEmpty(blueprintText)) {
                AmbariBlueprintTextProcessor blueprintTextProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
                Collection<ExposedService> knownExposedServices = getExposedServices(blueprintTextProcessor, Collections.emptySet());
                Gateway gateway = cluster.getGateway();
                Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServiceMap = new HashMap<>();
                Map<String, List<String>> privateIps = componentLocatorService.getComponentPrivateIp(cluster.getId(), blueprintTextProcessor,
                        knownExposedServices.stream().map(ExposedService::getServiceName).collect(Collectors.toSet()));
                if (gateway != null) {
                    for (GatewayTopology gatewayTopology : gateway.getTopologies()) {
                        List<ClusterExposedServiceV4Response> clusterExposedServiceResponses = new ArrayList<>();
                        Set<String> exposedServicesInTopology = gateway.getTopologies().stream()
                                .flatMap(this::getExposedServiceStream)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        for (ExposedService exposedService : knownExposedServices) {
                            ClusterExposedServiceV4Response clusterExposedServiceResponse = new ClusterExposedServiceV4Response();
                            clusterExposedServiceResponse.setMode(exposedService.isSSOSupported() ? gateway.getSsoType() : SSOType.NONE);
                            clusterExposedServiceResponse.setDisplayName(exposedService.getPortName());
                            clusterExposedServiceResponse.setKnoxService(exposedService.getKnoxService());
                            clusterExposedServiceResponse.setServiceName(exposedService.getServiceName());
                            Optional<String> serviceUrlForService = getServiceUrlForService(exposedService, ambariIp,
                                    gateway, gatewayTopology.getTopologyName(), blueprintTextProcessor.getStackVersion(), privateIps);
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
        return Collections.emptyMap();
    }

    private Collection<ExposedService> getExposedServices(AmbariBlueprintTextProcessor ambariBlueprintTextProcessor, Set<String> removableComponents) {
        Set<String> blueprintComponents = ambariBlueprintTextProcessor.getAllComponents();
        blueprintComponents.removeAll(removableComponents);
        String stackName = ambariBlueprintTextProcessor.getStackName();
        String stackVersion = ambariBlueprintTextProcessor.getStackVersion();
        VersionComparator versionComparator = new VersionComparator();
        return "HDF".equals(stackName) && versionComparator.compare(() -> stackVersion, () -> "3.2") < 0
                ? Collections.emptyList()
                : ExposedService.knoxServicesForComponents(blueprintComponents).stream()
                    .filter(exposedService -> !("HDP".equals(stackName)
                            && versionComparator.compare(() -> stackVersion, () -> "2.6") <= 0
                            && excludedServicesForHdp26().contains(exposedService)))
                    .collect(Collectors.toSet());
    }

    private List<ExposedService> excludedServicesForHdp26() {
        return Lists.newArrayList(ExposedService.LIVY_SERVER, ExposedService.RESOURCEMANAGER_WEB_V2,
                ExposedService.LOGSEARCH);
    }

    private Collection<ExposedServiceV4Response> getKnoxServices(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        if (BlueprintTextProcessorUtil.getClusterManagerType(blueprintText) == ClusterManagerType.AMBARI) {
            AmbariBlueprintTextProcessor blueprintTextProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
            Set<String> haComponents = ambariHaComponentFilter.getHaComponents(blueprintTextProcessor);
            haComponents.remove(ExposedService.RANGER.getServiceName());
            return ExposedServiceV4Response.fromExposedServices(getExposedServices(blueprintTextProcessor, haComponents));
        } else {
            return List.of();
        }
    }

    private Stream<String> getExposedServiceStream(GatewayTopology gatewayTopology) {
        if (gatewayTopology.getExposedServices() != null && gatewayTopology.getExposedServices().getValue() != null) {
            try {
                return gatewayTopology.getExposedServices().get(ExposedServices.class).getServices().stream();
            } catch (IOException e) {
                LOGGER.debug("Failed to get exposed services from Json.", e);
            }
        }
        return Stream.empty();
    }

    private Optional<String> getServiceUrlForService(ExposedService exposedService, String ambariIp, Gateway gateway,
            String topologyName, String stackVersion, Map<String, List<String>> privateIps) {
        if (hasKnoxUrl(exposedService) && ambariIp != null) {
            VersionComparator versionComparator = new VersionComparator();
            if (ExposedService.HIVE_SERVER.equals(exposedService) || ExposedService.HIVE_SERVER_INTERACTIVE.equals(exposedService)) {
                return getHiveJdbcUrl(gateway, ambariIp);
            } else if (ExposedService.NAMENODE.equals(exposedService) && versionComparator.compare(() -> stackVersion, () -> "2.6") > 0) {
                return getHdfsUIUrl(gateway, ambariIp, privateIps.get(ExposedService.NAMENODE.getServiceName()).iterator().next());
            } else {
                String url = GatewayType.CENTRAL == gateway.getGatewayType()
                        ? String.format("/%s/%s%s", gateway.getPath(), topologyName, exposedService.getKnoxUrl())
                        : String.format("https://%s:%s/%s/%s%s", ambariIp, knoxPort, gateway.getPath(), topologyName, exposedService.getKnoxUrl());
                return Optional.of(url);
            }
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
        return getGatewayTopology(ExposedService.AMBARI, gateway);
    }

    private Optional<GatewayTopology> getGatewayTopologyWithHive(Gateway gateway) {
        return getGatewayTopology(ExposedService.HIVE_SERVER, gateway);
    }

    private Optional<GatewayTopology> getGatewayTopologyWithNameNode(Gateway gateway) {
        return getGatewayTopology(ExposedService.NAMENODE, gateway);
    }

    private Optional<GatewayTopology> getGatewayTopology(ExposedService exposedService, Gateway gateway) {
        return gateway.getTopologies().stream()
                .filter(gt -> getExposedServiceStream(gt)
                        .anyMatch(es -> exposedService.getServiceName().equalsIgnoreCase(es)
                                || exposedService.name().equalsIgnoreCase(es)
                                || exposedService.getKnoxService().equalsIgnoreCase(es)))
                .findFirst();
    }

    private String getAmbariUrlFromGatewayTopology(String ambariIp, Gateway gateway, GatewayTopology gt) {
        return GatewayType.CENTRAL == gateway.getGatewayType()
                ? String.format("/%s/%s/ambari/", gateway.getPath(), gt.getTopologyName())
                : String.format("https://%s:%s/%s/%s/ambari/", ambariIp, knoxPort, gateway.getPath(), gt.getTopologyName());
    }

    private Optional<String> getHdfsUIUrl(Gateway gateway, String ambariIp, String nameNodePrivateIp) {
        return getGatewayTopologyWithNameNode(gateway)
                .map(gt -> getHdfsUIUrlWithHostParameterFromGatewayTopology(ambariIp, gt, nameNodePrivateIp));
    }

    private Optional<String> getHiveJdbcUrl(Gateway gateway, String ambariIp) {
        return getGatewayTopologyWithHive(gateway)
                .map(gt -> getHiveJdbcUrlFromGatewayTopology(ambariIp, gt));
    }

    private String getHiveJdbcUrlFromGatewayTopology(String ambariIp, GatewayTopology gt) {
        Gateway gateway = gt.getGateway();
        return String.format("jdbc:hive2://%s:%s/;ssl=true;sslTrustStore=/cert/gateway.jks;trustStorePassword=${GATEWAY_JKS_PASSWORD};"
                + "transportMode=http;httpPath=%s/%s/hive", ambariIp, knoxPort, gateway.getPath(), gt.getTopologyName());
    }

    private String getHdfsUIUrlWithHostParameterFromGatewayTopology(String ambariIp, GatewayTopology gt, String nameNodePrivateIp) {
        Gateway gateway = gt.getGateway();
        String url = String.format("https://%s:%s/%s/%s%s?host=http://%s:%s", ambariIp, knoxPort, gateway.getPath(), gt.getTopologyName(),
                ExposedService.NAMENODE.getKnoxUrl(), nameNodePrivateIp, ExposedService.NAMENODE.getDefaultPort());
        return url;
    }
}
