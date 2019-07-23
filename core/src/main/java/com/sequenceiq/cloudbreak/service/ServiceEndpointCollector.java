package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService.getServiceNameBasedOnClusterVariant;
import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintTextProcessorUtil;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;

@Service
public class ServiceEndpointCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEndpointCollector.class);

    private static final String API_TOPOLOGY_POSTFIX = "-api";

    @Value("${cb.knox.port}")
    private String knoxPort;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private AmbariHaComponentFilter ambariHaComponentFilter;

    @Inject
    private ComponentLocatorService componentLocatorService;

    public Collection<ExposedServiceV4Response> getKnoxServices(Long workspaceId, String blueprintName) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(blueprintName, workspaceId);
        return getKnoxServices(blueprint);
    }

    public String getManagerServerUrl(Cluster cluster, String managerIp) {
        if (managerIp != null) {
            String orchestrator = cluster.getStack().getOrchestrator().getType();
            if (YARN.equals(orchestrator)) {
                return String.format("http://%s:8080", managerIp);
            } else {
                Gateway gateway = cluster.getGateway();
                if (gateway != null) {
                    String variant = cluster.getVariant();
                    ExposedService exposedService = isNotEmpty(variant) && variant.equals(ClusterApi.CLOUDERA_MANAGER)
                            ? ExposedService.CLOUDERA_MANAGER_UI : ExposedService.AMBARI;
                    Optional<GatewayTopology> gatewayTopology = getGatewayTopologyForService(gateway, exposedService);
                    Optional<String> managerUrl = gatewayTopology.map(t -> getExposedServiceUrl(managerIp, gateway, t.getTopologyName(), exposedService, false));
                    // when knox gateway is enabled, but ambari/cm is not exposed, use the default url
                    return managerUrl.orElse(String.format("https://%s/", managerIp));
                }
                return String.format("https://%s/", managerIp);
            }
        }
        return null;
    }

    public Map<String, Collection<ClusterExposedServiceV4Response>> prepareClusterExposedServices(Cluster cluster, String managerIp) {
        if (cluster.getBlueprint() != null) {
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            if (isNotEmpty(blueprintText)) {
                boolean ambariBlueprint = blueprintService.isAmbariBlueprint(cluster.getBlueprint());
                BlueprintTextProcessor processor = ambariBlueprint
                        ? ambariBlueprintProcessorFactory.get(blueprintText)
                        : cmTemplateProcessorFactory.get(blueprintText);
                Collection<ExposedService> knownExposedServices = ambariBlueprint
                        ? getExposedServices(ambariBlueprintProcessorFactory.get(blueprintText), Collections.emptySet())
                        : getExposedServices(cluster.getBlueprint());
                Gateway gateway = cluster.getGateway();
                Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServiceMap = new HashMap<>();
                Map<String, List<String>> privateIps = componentLocatorService.getComponentLocation(cluster.getId(), processor,
                        knownExposedServices.stream().map(ExposedService::getCmServiceName).collect(Collectors.toSet()));
                if (gateway != null) {
                    for (GatewayTopology gatewayTopology : gateway.getTopologies()) {
                        Set<String> exposedServicesInTopology = gateway.getTopologies().stream()
                                .flatMap(this::getExposedServiceStream)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        List<ClusterExposedServiceV4Response> uiServices = new ArrayList<>();
                        List<ClusterExposedServiceV4Response> apiServices = new ArrayList<>();
                        for (ExposedService exposedService : knownExposedServices) {
                            if (exposedService.isUISupported()) {
                                ClusterExposedServiceV4Response uiService = createServiceEntry(exposedService, gateway, gatewayTopology,
                                        managerIp, privateIps, exposedServicesInTopology, false);
                                uiServices.add(uiService);
                            }
                            if (exposedService.isAPISupported()) {
                                ClusterExposedServiceV4Response apiService = createServiceEntry(exposedService, gateway, gatewayTopology,
                                        managerIp, privateIps, exposedServicesInTopology, true);
                                apiServices.add(apiService);
                            }
                        }
                        clusterExposedServiceMap.put(gatewayTopology.getTopologyName(), uiServices);
                        clusterExposedServiceMap.put(gatewayTopology.getTopologyName() + API_TOPOLOGY_POSTFIX, apiServices);
                    }
                }
                return clusterExposedServiceMap;
            }
        }
        return Collections.emptyMap();
    }

    private ClusterExposedServiceV4Response createServiceEntry(ExposedService exposedService, Gateway gateway, GatewayTopology gatewayTopology,
            String managerIp, Map<String, List<String>> privateIps, Set<String> exposedServicesInTopology, boolean api) {
        ClusterExposedServiceV4Response service = new ClusterExposedServiceV4Response();
        service.setMode(api ? SSOType.PAM : getSSOType(exposedService, gateway));
        service.setDisplayName(exposedService.getDisplayName());
        service.setKnoxService(exposedService.getKnoxService());
        service.setServiceName(exposedService.getCmServiceName());
        Optional<String> serviceUrlForService = getServiceUrlForService(exposedService, managerIp,
                gateway, gatewayTopology.getTopologyName(), privateIps, api);
        serviceUrlForService.ifPresent(service::setServiceUrl);
        service.setOpen(isExposed(exposedService, exposedServicesInTopology));
        return service;
    }

    private SSOType getSSOType(ExposedService exposedService, Gateway gateway) {
        return exposedService.isSSOSupported() ? gateway.getSsoType() : SSOType.NONE;
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
        return Lists.newArrayList(ExposedService.LIVY2_SERVER, ExposedService.LOGSEARCH);
    }

    private Collection<ExposedService> getExposedServices(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        return ExposedService.knoxServicesForCmComponents(
                processor.getAllComponents().stream().map(
                        ServiceComponent::getComponent).collect(Collectors.toList()));
    }

    private Collection<ExposedServiceV4Response> getKnoxServices(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        if (BlueprintTextProcessorUtil.getClusterManagerType(blueprintText) == ClusterManagerType.AMBARI) {
            AmbariBlueprintTextProcessor blueprintTextProcessor = ambariBlueprintProcessorFactory.get(blueprintText);
            Set<String> haComponents = ambariHaComponentFilter.getHaComponents(blueprintTextProcessor);
            haComponents.remove(ExposedService.RANGER.getAmbariServiceName());
            return ExposedServiceV4Response.fromExposedServices(getExposedServices(blueprintTextProcessor, haComponents));
        } else {
            return ExposedServiceV4Response.fromExposedServices(getExposedServices(blueprint));

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

    private Optional<String> getServiceUrlForService(ExposedService exposedService, String managerIp, Gateway gateway,
            String topologyName, Map<String, List<String>> privateIps, boolean api) {
        if (hasKnoxUrl(exposedService) && managerIp != null) {
            if (ExposedService.HIVE_SERVER.equals(exposedService) || ExposedService.HIVE_SERVER_INTERACTIVE.equals(exposedService)) {
                return getHiveJdbcUrl(gateway, managerIp);
            } else if (ExposedService.NAMENODE.equals(exposedService)) {
                return getHdfsUIUrl(gateway, managerIp, privateIps.get(ExposedService.NAMENODE.getCmServiceName()).iterator().next());
            } else if (ExposedService.HBASE_UI.equals(exposedService)) {
                return getHBaseServiceUrl(gateway, managerIp, privateIps.get(ExposedService.HBASE_UI.getCmServiceName()).iterator().next());
            } else {
                return Optional.of(getExposedServiceUrl(managerIp, gateway, topologyName, exposedService, api));
            }
        }
        return Optional.empty();
    }

    private boolean hasKnoxUrl(ExposedService exposedService) {
        return isNotEmpty(exposedService.getKnoxUrl());
    }

    private boolean isExposed(ExposedService exposedService, Collection<String> exposedServices) {
        return exposedServices.contains(exposedService.getKnoxService());
    }

    private Optional<GatewayTopology> getGatewayTopologyForService(Gateway gateway, ExposedService exposedService) {
        return getGatewayTopology(exposedService, gateway);
    }

    private Optional<GatewayTopology> getGatewayTopologyWithHive(Gateway gateway) {
        return getGatewayTopology(ExposedService.HIVE_SERVER, gateway);
    }

    private Optional<GatewayTopology> getGatewayTopologyWithExposedService(Gateway gateway, ExposedService exposedService) {
        return getGatewayTopology(exposedService, gateway);
    }

    private Optional<GatewayTopology> getGatewayTopology(ExposedService exposedService, Gateway gateway) {
        return gateway.getTopologies().stream()
                .filter(gt -> getExposedServiceStream(gt)
                        .anyMatch(es -> getServiceNameBasedOnClusterVariant(exposedService).equalsIgnoreCase(es)
                                || exposedService.name().equalsIgnoreCase(es)
                                || exposedService.getKnoxService().equalsIgnoreCase(es)))
                .findFirst();
    }

    private String getExposedServiceUrl(String managerIp, Gateway gateway, String topologyName, ExposedService exposedService, boolean api) {
        String topology = api ? topologyName + API_TOPOLOGY_POSTFIX : topologyName;
        return GatewayType.CENTRAL == gateway.getGatewayType()
                ? String.format("/%s/%s%s", gateway.getPath(), topology, exposedService.getKnoxUrl())
                : String.format("https://%s:%s/%s/%s%s", managerIp, knoxPort, gateway.getPath(), topology, exposedService.getKnoxUrl());
    }

    private Optional<String> getHdfsUIUrl(Gateway gateway, String managerIp, String nameNodePrivateIp) {
        return getGatewayTopologyWithExposedService(gateway, ExposedService.NAMENODE)
                .map(gt -> getHdfsUIUrlWithHostParameterFromGatewayTopology(managerIp, gt, nameNodePrivateIp));
    }

    private Optional<String> getHBaseServiceUrl(Gateway gateway, String managerIp, String hbaseMasterPrivateIp) {
        return getGatewayTopologyWithExposedService(gateway, ExposedService.HBASE_UI)
                .map(gt -> getHBaseUIUrlWithHostParameterFromGatewayTopology(managerIp, gt, hbaseMasterPrivateIp));
    }

    private Optional<String> getHiveJdbcUrl(Gateway gateway, String ambariIp) {
        return getGatewayTopologyWithHive(gateway)
                .map(gt -> getHiveJdbcUrlFromGatewayTopology(ambariIp, gt));
    }

    private String getHiveJdbcUrlFromGatewayTopology(String managerIp, GatewayTopology gt) {
        Gateway gateway = gt.getGateway();
        return String.format("jdbc:hive2://%s:%s/;ssl=true;sslTrustStore=/cert/gateway.jks;trustStorePassword=${GATEWAY_JKS_PASSWORD};"
                + "transportMode=http;httpPath=%s/%s%s/hive", managerIp, knoxPort, gateway.getPath(), gt.getTopologyName(), API_TOPOLOGY_POSTFIX);
    }

    private String getHdfsUIUrlWithHostParameterFromGatewayTopology(String managerIp, GatewayTopology gt, String nameNodePrivateIp) {
        Gateway gateway = gt.getGateway();
        String url = String.format("https://%s:%s/%s/%s%s?host=http://%s:%s", managerIp, knoxPort, gateway.getPath(), gt.getTopologyName(),
                ExposedService.NAMENODE.getKnoxUrl(), nameNodePrivateIp, ExposedService.NAMENODE.getCmPort());
        return url;
    }

    private String getHBaseUIUrlWithHostParameterFromGatewayTopology(String managerIp, GatewayTopology gt, String nameNodePrivateIp) {
        Gateway gateway = gt.getGateway();
        String url = String.format("https://%s:%s/%s/%s%s?host=%s&port=%s", managerIp, knoxPort, gateway.getPath(), gt.getTopologyName(),
                ExposedService.HBASE_UI.getKnoxUrl(), nameNodePrivateIp, ExposedService.HBASE_UI.getCmPort());
        return url;
    }
}
