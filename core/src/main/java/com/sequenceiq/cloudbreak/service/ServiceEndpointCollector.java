package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.ClusterExposedServiceView;

@Service
public class ServiceEndpointCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceEndpointCollector.class);

    private static final String API_TOPOLOGY_POSTFIX = "-api";

    @Value("${cb.https.port}")
    private String httpsPort;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

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
                    ExposedService exposedService = ExposedService.CLOUDERA_MANAGER_UI;
                    Optional<GatewayTopology> gatewayTopology = getGatewayTopologyForService(gateway, exposedService);
                    Optional<String> managerUrl = gatewayTopology
                            .map(t -> getExposedServiceUrl(managerIp, gateway, t.getTopologyName(), exposedService, false));
                    // when knox gateway is enabled, but cm is not exposed, use the default url
                    return managerUrl.orElse(String.format("https://%s/", managerIp));
                }
                return String.format("https://%s/clouderamanager/", managerIp);
            }
        }
        return null;
    }

    public Map<String, Collection<ClusterExposedServiceV4Response>> prepareClusterExposedServices(Cluster cluster, String managerIp) {
        if (cluster.getBlueprint() != null) {
            String blueprintText = cluster.getBlueprint().getBlueprintText();
            if (isNotEmpty(blueprintText)) {
                BlueprintTextProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
                Collection<ExposedService> knownExposedServices = getExposedServices(cluster.getBlueprint());
                Gateway gateway = cluster.getGateway();
                Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServiceMap = new HashMap<>();
                Map<String, List<String>> privateIps = componentLocatorService.getComponentLocation(cluster.getId(), processor,
                        knownExposedServices.stream().map(ExposedService::getServiceName).collect(Collectors.toSet()));
                if (privateIps.containsKey(ExposedService.IMPALA.getServiceName())) {
                    setImpalaDebugUIToCoordinator(cluster, privateIps);
                }
                if (gateway != null) {
                    for (GatewayTopology gatewayTopology : gateway.getTopologies()) {
                        Set<String> exposedServicesInTopology = gateway.getTopologies().stream()
                                .flatMap(this::getExposedServiceStream)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        List<ClusterExposedServiceV4Response> uiServices = new ArrayList<>();
                        List<ClusterExposedServiceV4Response> apiServices = new ArrayList<>();
                        boolean autoTlsEnabled = cluster.getAutoTlsEnabled();
                        SecurityConfig securityConfig = cluster.getStack().getSecurityConfig();
                        for (ExposedService exposedService : knownExposedServices) {
                            if (exposedService.isUISupported()) {
                                List<ClusterExposedServiceV4Response> uiServiceOnPrivateIps = createServiceEntries(exposedService, gateway, gatewayTopology,
                                        managerIp, privateIps, exposedServicesInTopology, false, autoTlsEnabled, securityConfig);
                                uiServices.addAll(uiServiceOnPrivateIps);
                            }
                            if (exposedService.isAPISupported()) {
                                List<ClusterExposedServiceV4Response> apiServiceOnPrivateIps = createServiceEntries(exposedService, gateway, gatewayTopology,
                                        managerIp, privateIps, exposedServicesInTopology, true, autoTlsEnabled, securityConfig);
                                apiServices.addAll(apiServiceOnPrivateIps);
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

    public Map<String, Collection<ClusterExposedServiceView>> prepareClusterExposedServicesViews(Cluster cluster, String managerIp) {
        Map<String, Collection<ClusterExposedServiceView>> result = new HashMap<>();

        for (Map.Entry<String, Collection<ClusterExposedServiceV4Response>> entry : prepareClusterExposedServices(cluster, managerIp).entrySet()) {
            Set<ClusterExposedServiceView> views = new HashSet<>();
            for (ClusterExposedServiceV4Response response : entry.getValue()) {
                views.add(new ClusterExposedServiceView(
                        response.getServiceName(),
                        response.getDisplayName(),
                        response.getKnoxService(),
                        response.getServiceUrl()));
            }
            result.put(entry.getKey(), views);
        }
        return result;
    }

    private void setImpalaDebugUIToCoordinator(Cluster cluster, Map<String, List<String>> privateIps) {
        List<String> impalaCoordinators = componentLocatorService.getImpalaCoordinatorLocations(cluster)
                .values().stream().flatMap(List::stream).collect(Collectors.toList());
        privateIps.put(ExposedService.IMPALA_DEBUG_UI.getServiceName(), impalaCoordinators);
    }

    private List<ClusterExposedServiceV4Response> createServiceEntries(ExposedService exposedService, Gateway gateway, GatewayTopology gatewayTopology,
            String managerIp, Map<String, List<String>> privateIps,
            Set<String> exposedServicesInTopology, boolean api, boolean autoTlsEnabled, SecurityConfig securityConfig) {
        List<ClusterExposedServiceV4Response> services = new ArrayList<>();
        List<String> serviceUrlsForService = getServiceUrlsForService(exposedService, managerIp,
                gateway, gatewayTopology.getTopologyName(), privateIps, api, autoTlsEnabled);
        serviceUrlsForService.addAll(getJdbcUrlsForService(exposedService, managerIp, gateway, securityConfig));
        if (serviceUrlsForService.isEmpty()) {
            services.add(createExposedServiceResponse(exposedService, gateway, exposedServicesInTopology, api, null));
        } else {
            serviceUrlsForService.forEach(url -> {
                services.add(createExposedServiceResponse(exposedService, gateway, exposedServicesInTopology, api, url));
            });
        }
        return services;
    }

    private ClusterExposedServiceV4Response createExposedServiceResponse(ExposedService exposedService,
            Gateway gateway, Set<String> exposedServicesInTopology, boolean api, String url) {
        ClusterExposedServiceV4Response service = new ClusterExposedServiceV4Response();
        service.setMode(api ? SSOType.PAM : getSSOType(exposedService, gateway));
        service.setDisplayName(exposedService.getDisplayName());
        service.setKnoxService(exposedService.getKnoxService());
        service.setServiceName(exposedService.getServiceName());
        service.setOpen(isExposed(exposedService, exposedServicesInTopology));
        if (isNotEmpty(url)) {
            service.setServiceUrl(url);
        }
        return service;
    }

    private SSOType getSSOType(ExposedService exposedService, Gateway gateway) {
        return exposedService.isSSOSupported() ? gateway.getSsoType() : SSOType.NONE;
    }

    /**
     * Get all exposed services for blueprint, there are filtered by visibility
     *
     * @param blueprint given blueprint
     * @return all visible exposed services for the given blueprint
     */
    private Collection<ExposedService> getExposedServices(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintText();
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        List<String> components = processor.getAllComponents().stream().map(ServiceComponent::getComponent).collect(Collectors.toList());
        return ExposedService.knoxServicesForComponents(components)
                .stream()
                .filter(ExposedService::isVisible)
                .collect(Collectors.toList());
    }

    private Collection<ExposedServiceV4Response> getKnoxServices(Blueprint blueprint) {
        return ExposedServiceV4Response.fromExposedServices(getExposedServices(blueprint));
    }

    private Stream<String> getExposedServiceStream(GatewayTopology gatewayTopology) {
        if (gatewayTopology.getExposedServices() != null && gatewayTopology.getExposedServices().getValue() != null) {
            try {
                return gatewayTopology.getExposedServices().get(ExposedServices.class).getFullServiceList().stream();
            } catch (IOException e) {
                LOGGER.debug("Failed to get exposed services from Json.", e);
            }
        }
        return Stream.empty();
    }

    //CHECKSTYLE:OFF
    private List<String> getServiceUrlsForService(ExposedService exposedService, String managerIp, Gateway gateway,
            String topologyName, Map<String, List<String>> privateIps, boolean api, boolean autoTlsEnabled) {
        List<String> urls = new ArrayList<>();
        if (hasKnoxUrl(exposedService) && managerIp != null) {
            switch (exposedService) {
                case NAMENODE:
                    addNameNodeUrl(managerIp, gateway, privateIps, autoTlsEnabled, urls);
                    break;
                case HBASE_UI:
                    addHbaseUrl(managerIp, gateway, privateIps, urls);
                    break;
                case RESOURCEMANAGER_WEB:
                    addResourceManagerUrl(exposedService, managerIp, gateway, topologyName, api, urls);
                    break;
                case NAMENODE_HDFS:
                    urls.add(buildKnoxUrlWithProtocol("hdfs", managerIp, exposedService, autoTlsEnabled));
                    break;
                case JOBTRACKER:
                    urls.add(buildKnoxUrlWithProtocol("rpc", managerIp, exposedService, autoTlsEnabled));
                    break;
                case IMPALA_DEBUG_UI:
                    addImplaDebugUrl(managerIp, gateway, privateIps, autoTlsEnabled, urls);
                    break;
                default:
                    urls.add(getExposedServiceUrl(managerIp, gateway, topologyName, exposedService, api));
                    break;
            }
        }
        return urls;
    }
    //CHECKSTYLE:ON

    private List<String> getJdbcUrlsForService(ExposedService exposedService, String managerIp, Gateway gateway, SecurityConfig securityConfig) {
        List<String> urls = new ArrayList<>();
        if (hasKnoxUrl(exposedService) && managerIp != null) {
            switch (exposedService) {
                case HIVE_SERVER:
                case HIVE_SERVER_INTERACTIVE:
                    getHiveJdbcUrl(gateway, managerIp, securityConfig).ifPresent(urls::add);
                    break;
                case IMPALA:
                    getImpalaJdbcUrl(gateway, managerIp).ifPresent(urls::add);
                    break;
                default:
                    break;
            }
        }
        return urls;
    }

    private boolean hasKnoxUrl(ExposedService exposedService) {
        return isNotEmpty(exposedService.getKnoxUrl());
    }

    private void addNameNodeUrl(String managerIp, Gateway gateway, Map<String, List<String>> privateIps, boolean autoTlsEnabled, List<String> urls) {
        List<String> hdfsUrls = privateIps.get(ExposedService.NAMENODE.getServiceName())
                .stream()
                .map(namenodeIp -> getHdfsUIUrl(gateway, managerIp, namenodeIp, autoTlsEnabled))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        urls.addAll(hdfsUrls);
    }

    private void addHbaseUrl(String managerIp, Gateway gateway, Map<String, List<String>> privateIps, List<String> urls) {
        List<String> hbaseUrls = privateIps.get(ExposedService.HBASE_UI.getServiceName())
                .stream()
                .map(hbaseIp -> getHBaseServiceUrl(gateway, managerIp, hbaseIp))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        urls.addAll(hbaseUrls);
    }

    private void addResourceManagerUrl(ExposedService exposedService, String managerIp, Gateway gateway, String topologyName, boolean api, List<String> urls) {
        String knoxUrl = api ? "/resourcemanager/" : exposedService.getKnoxUrl();
        String topology = api ? topologyName + API_TOPOLOGY_POSTFIX : topologyName;
        urls.add(buildKnoxUrl(managerIp, gateway, knoxUrl, topology));
    }

    private void addImplaDebugUrl(String managerIp, Gateway gateway, Map<String, List<String>> privateIps, boolean autoTlsEnabled, List<String> urls) {
        Optional<String> coordinatorUrl = privateIps.get(ExposedService.IMPALA_DEBUG_UI.getServiceName())
                .stream()
                .map(coordinator -> getImpalaCoordinatorUrl(gateway, managerIp, coordinator, autoTlsEnabled))
                .flatMap(Optional::stream)
                .findFirst();
        if (coordinatorUrl.isPresent()) {
            urls.add(coordinatorUrl.get());
        }
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
                        .anyMatch(es -> exposedService.getServiceName().equalsIgnoreCase(es)
                                || exposedService.name().equalsIgnoreCase(es)
                                || exposedService.getKnoxService().equalsIgnoreCase(es)))
                .findFirst();
    }

    private String getExposedServiceUrl(String managerIp, Gateway gateway, String topologyName,
            ExposedService exposedService, boolean api) {
        String topology = api ? topologyName + API_TOPOLOGY_POSTFIX : topologyName;
        return buildKnoxUrl(managerIp, gateway, exposedService.getKnoxUrl(), topology);
    }

    private String buildKnoxUrlWithProtocol(String protocol, String managerIp, ExposedService exposedService, boolean autoTlsEnabled) {
        Integer port;
        if (autoTlsEnabled) {
            port = exposedService.getTlsPort();
        } else {
            port = exposedService.getPort();
        }
        return String.format("%s://%s:%s", protocol, managerIp, port);
    }

    private String buildKnoxUrl(String managerIp, Gateway gateway, String knoxUrl, String topology) {
        String result;
        if (GatewayType.CENTRAL == gateway.getGatewayType()) {
            result = String.format("/%s/%s%s", gateway.getPath(), topology, knoxUrl);
        } else {
            if (gatewayListeningOnHttpsPort(gateway)) {
                result = String.format("https://%s/%s/%s%s", managerIp, gateway.getPath(), topology, knoxUrl);
            } else {
                result = String.format("https://%s:%s/%s/%s%s", managerIp, gateway.getGatewayPort(), gateway.getPath(), topology, knoxUrl);
            }
        }
        return result;
    }

    private Optional<String> getHdfsUIUrl(Gateway gateway, String managerIp, String nameNodePrivateIp, boolean autoTlsEnabled) {
        return getGatewayTopologyWithExposedService(gateway, ExposedService.NAMENODE)
                .map(gt -> getHdfsUIUrlWithHostParameterFromGatewayTopology(managerIp, gt, nameNodePrivateIp, autoTlsEnabled));
    }

    private Optional<String> getImpalaCoordinatorUrl(Gateway gateway, String managerIp, String coordinatorPrivateIps, boolean autoTlsEnabled) {
        return getGatewayTopologyWithExposedService(gateway, ExposedService.IMPALA_DEBUG_UI)
                .map(gt -> getImpalaCoordinatorUrlWithHostFromGatewayTopology(managerIp, gt, coordinatorPrivateIps, autoTlsEnabled));
    }

    private Optional<String> getHBaseServiceUrl(Gateway gateway, String managerIp, String hbaseMasterPrivateIp) {
        return getGatewayTopologyWithExposedService(gateway, ExposedService.HBASE_UI)
                .map(gt -> getHBaseUIUrlWithHostParameterFromGatewayTopology(managerIp, gt, hbaseMasterPrivateIp));
    }

    private Optional<String> getHiveJdbcUrl(Gateway gateway, String ambariIp, SecurityConfig securityConfig) {
        return getGatewayTopologyWithHive(gateway)
                .map(gt -> getHiveJdbcUrlFromGatewayTopology(ambariIp, gt, securityConfig));
    }

    private Optional<String> getImpalaJdbcUrl(Gateway gateway, String ambariIp) {
        return getGatewayTopology(ExposedService.IMPALA, gateway)
                .map(gt -> getImpalaJdbcUrlFromGatewayTopology(ambariIp, gt));
    }

    private String getHiveJdbcUrlFromGatewayTopology(String managerIp, GatewayTopology gt, SecurityConfig securityConfig) {
        Gateway gateway = gt.getGateway();
        String jdbcAddressPart = "jdbc:hive2://%s/;";
        if (!gatewayListeningOnHttpsPort(gateway)) {
            jdbcAddressPart = "jdbc:hive2://%s:" + gateway.getGatewayPort() + "/;";
        }
        if (securityConfig != null && securityConfig.getUserFacingCert() != null) {
            return String.format(jdbcAddressPart + "ssl=true;transportMode=http;httpPath=%s/%s%s/hive",
                    managerIp, gateway.getPath(), gt.getTopologyName(), API_TOPOLOGY_POSTFIX);
        }
        return String.format(jdbcAddressPart + "ssl=true;sslTrustStore=/cert/gateway.jks;trustStorePassword=${GATEWAY_JKS_PASSWORD};"
                + "transportMode=http;httpPath=%s/%s%s/hive", managerIp, gateway.getPath(), gt.getTopologyName(), API_TOPOLOGY_POSTFIX);
    }

    private String getImpalaJdbcUrlFromGatewayTopology(String managerIp, GatewayTopology gt) {
        Gateway gateway = gt.getGateway();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("jdbc:impala://%s/;ssl=1;transportMode=http;httpPath=%s/%s%s/impala;AuthMech=3;",
                    managerIp, gateway.getPath(), gt.getTopologyName(), API_TOPOLOGY_POSTFIX);
        } else {
            return String.format("jdbc:impala://%s:%s/;ssl=1;transportMode=http;httpPath=%s/%s%s/impala;AuthMech=3;",
                    managerIp, gateway.getGatewayPort(), gateway.getPath(), gt.getTopologyName(), API_TOPOLOGY_POSTFIX);
        }
    }

    private String getHdfsUIUrlWithHostParameterFromGatewayTopology(String managerIp, GatewayTopology gt, String nameNodePrivateIp, boolean autoTlsEnabled) {
        Gateway gateway = gt.getGateway();
        Integer port = autoTlsEnabled ? ExposedService.NAMENODE.getTlsPort() : ExposedService.NAMENODE.getPort();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("https://%s/%s/%s%s?host=%s://%s:%s", managerIp, gateway.getPath(), gt.getTopologyName(),
                    ExposedService.NAMENODE.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), nameNodePrivateIp, port);
        } else {
            return String.format("https://%s:%s/%s/%s%s?host=%s://%s:%s", managerIp, gateway.getGatewayPort(), gateway.getPath(), gt.getTopologyName(),
                    ExposedService.NAMENODE.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), nameNodePrivateIp, port);
        }
    }

    private String getHBaseUIUrlWithHostParameterFromGatewayTopology(String managerIp, GatewayTopology gt, String nameNodePrivateIp) {
        Gateway gateway = gt.getGateway();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("https://%s/%s/%s%s?host=%s&port=%s", managerIp, gateway.getPath(), gt.getTopologyName(),
                    ExposedService.HBASE_UI.getKnoxUrl(), nameNodePrivateIp, ExposedService.HBASE_UI.getPort());
        } else {
            return String.format("https://%s:%s/%s/%s%s?host=%s&port=%s", managerIp, gateway.getGatewayPort(), gateway.getPath(), gt.getTopologyName(),
                    ExposedService.HBASE_UI.getKnoxUrl(), nameNodePrivateIp, ExposedService.HBASE_UI.getPort());
        }
    }

    private String getImpalaCoordinatorUrlWithHostFromGatewayTopology(String managerIp, GatewayTopology gt, String impalaPrivateIp, boolean autoTlsEnabled) {
        Gateway gateway = gt.getGateway();
        Integer port = autoTlsEnabled ? ExposedService.IMPALA_DEBUG_UI.getTlsPort() : ExposedService.IMPALA_DEBUG_UI.getPort();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("https://%s/%s/%s%s?scheme=%s&host=%s&port=%s", managerIp, gateway.getPath(), gt.getTopologyName(),
                    ExposedService.IMPALA_DEBUG_UI.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), impalaPrivateIp, port);
        } else {
            return String.format("https://%s:%s/%s/%s%s?scheme=%s&host=%s&port=%s", managerIp, gateway.getGatewayPort(), gateway.getPath(),
                    gt.getTopologyName(), ExposedService.IMPALA_DEBUG_UI.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), impalaPrivateIp, port);
        }
    }

    private String getHttpProtocol(boolean autoTlsEnabled) {
        return autoTlsEnabled ? "https" : "http";
    }

    private boolean gatewayListeningOnHttpsPort(Gateway gateway) {
        return gateway.getGatewayPort().equals(Integer.valueOf(httpsPort));
    }
}
