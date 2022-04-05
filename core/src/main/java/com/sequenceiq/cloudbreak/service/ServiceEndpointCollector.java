package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
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

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
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

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ServiceEndpointCollectorEntitlementComparator serviceEndpointCollectorEntitlementComparator;

    public Collection<ExposedServiceV4Response> getKnoxServices(Long workspaceId, String blueprintName) {
        Blueprint blueprint = blueprintService.getByNameForWorkspaceId(blueprintName, workspaceId);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint could not be find by name: '%s'.", blueprintName));
        }
        return getKnoxServices(blueprint, entitlementService.getEntitlements(blueprint.getWorkspace().getTenant().getName()));
    }

    public String getManagerServerUrl(Cluster cluster, String managerIp) {
        if (managerIp != null) {
            String orchestrator = cluster.getStack().getOrchestrator().getType();
            if (YARN.equals(orchestrator)) {
                return String.format("http://%s:8080", managerIp);
            } else {
                Gateway gateway = cluster.getGateway();
                if (gateway != null) {
                    Optional<String> version = Optional.ofNullable(cluster.getBlueprint()).map(Blueprint::getStackVersion);
                    ExposedService exposedService = exposedServiceCollector.getClouderaManagerUIService();
                    Optional<GatewayTopology> gatewayTopology = getGatewayTopologyForService(gateway, exposedService, version);
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
        String blueprintText = getBlueprintString(cluster);
        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServiceMap = new HashMap<>();
        if (!Strings.isNullOrEmpty(blueprintText)) {
            BlueprintTextProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
            Collection<ExposedService> knownExposedServices = getExposedServices(
                    blueprintText,
                    entitlementService.getEntitlements(cluster.getWorkspace().getTenant().getName()));
            Gateway gateway = cluster.getGateway();
            Optional<String> version = Optional.ofNullable(cluster.getBlueprint()).map(Blueprint::getStackVersion);
            Map<String, List<String>> privateIps = componentLocatorService.getComponentLocation(cluster.getId(), processor,
                    knownExposedServices.stream().map(ExposedService::getServiceName).collect(Collectors.toSet()));
            LOGGER.debug("The private IPs in the cluster {}", privateIps);
            if (privateIps.containsKey(exposedServiceCollector.getImpalaService().getServiceName())) {
                setImpalaDebugUIToCoordinator(cluster, privateIps);
            }
            if (gateway != null) {
                for (GatewayTopology gatewayTopology : gateway.getTopologies()) {
                    generateGatewayTopology(cluster, managerIp, clusterExposedServiceMap, knownExposedServices, gateway, privateIps, gatewayTopology, version);
                }
            }
        }
        return clusterExposedServiceMap;
    }

    private void generateGatewayTopology(
            Cluster cluster,
            String managerIp,
            Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServiceMap,
            Collection<ExposedService> knownExposedServices,
            Gateway gateway,
            Map<String, List<String>> privateIps,
            GatewayTopology gatewayTopology,
            Optional<String> version) {
        LOGGER.debug("Generating the topology for '{}' topologies", gatewayTopology.getTopologyName());
        Set<String> exposedServicesInTopology = gateway.getTopologies().stream()
                .flatMap(e -> getExposedServiceStream(e, version))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<ClusterExposedServiceV4Response> uiServices = new ArrayList<>();
        List<ClusterExposedServiceV4Response> apiServices = new ArrayList<>();
        boolean autoTlsEnabled = cluster.getAutoTlsEnabled();
        LOGGER.debug("AutoTls enabled '{}' for the cluster", autoTlsEnabled);
        SecurityConfig securityConfig = cluster.getStack().getSecurityConfig();
        String managerServerUrl = getManagerServerUrl(cluster, managerIp);
        for (ExposedService exposedService : knownExposedServices) {
            if (exposedService.isCmProxied()) {
                List<ClusterExposedServiceV4Response> uiServiceOnPrivateIps = createCmProxiedServiceEntries(exposedService, gateway,
                        gatewayTopology, managerServerUrl, cluster.getName());
                uiServices.addAll(uiServiceOnPrivateIps);
            } else {
                if (!exposedService.isApiOnly()) {
                    List<ClusterExposedServiceV4Response> uiServiceOnPrivateIps = createServiceEntries(exposedService, gateway, gatewayTopology,
                            managerIp, privateIps, exposedServicesInTopology, false, autoTlsEnabled, securityConfig, version);
                    uiServices.addAll(uiServiceOnPrivateIps);
                }
                if (exposedService.isApiIncluded()) {
                    List<ClusterExposedServiceV4Response> apiServiceOnPrivateIps = createServiceEntries(exposedService, gateway, gatewayTopology,
                            managerIp, privateIps, exposedServicesInTopology, true, autoTlsEnabled, securityConfig, version);
                    apiServices.addAll(apiServiceOnPrivateIps);
                }
            }
        }
        clusterExposedServiceMap.put(gatewayTopology.getTopologyName(), uiServices);
        clusterExposedServiceMap.put(gatewayTopology.getTopologyName() + API_TOPOLOGY_POSTFIX, apiServices);
    }

    private String getBlueprintString(Cluster cluster) {
        String blueprintText = null;
        if (!Strings.isNullOrEmpty(cluster.getExtendedBlueprintText())) {
            blueprintText = cluster.getExtendedBlueprintText();
        } else if (cluster.getBlueprint() != null) {
            blueprintText = cluster.getBlueprint().getBlueprintText();
        }
        return blueprintText;
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

    public List<GatewayTopologyV4Response> filterByStackType(StackType stackType, List<GatewayTopologyV4Response> topologies) {
        for (GatewayTopologyV4Response topology : topologies) {
            topology.setExposedServices(topology.getExposedServices()
                    .stream()
                    .filter(e -> (exposedServiceCollector.getByName(e).isVisibleForDatahub() && stackType.equals(StackType.WORKLOAD))
                            || (exposedServiceCollector.getByName(e).isVisibleForDatalake() && stackType.equals(StackType.DATALAKE)))
                    .collect(Collectors.toList()));
        }
        return topologies;
    }

    private void setImpalaDebugUIToCoordinator(Cluster cluster, Map<String, List<String>> privateIps) {
        List<String> impalaCoordinators = componentLocatorService.getImpalaCoordinatorLocations(cluster)
                .values().stream().flatMap(List::stream).collect(Collectors.toList());
        privateIps.put(exposedServiceCollector.getImpalaDebugUIService().getServiceName(), impalaCoordinators);
    }

    private List<ClusterExposedServiceV4Response> createServiceEntries(
            ExposedService exposedService,
            Gateway gateway,
            GatewayTopology gatewayTopology,
            String managerIp,
            Map<String, List<String>> privateIps,
            Set<String> exposedServicesInTopology,
            boolean api,
            boolean autoTlsEnabled,
            SecurityConfig securityConfig,
            Optional<String> version) {
        List<ClusterExposedServiceV4Response> services = new ArrayList<>();
        List<String> serviceUrlsForService = getServiceUrlsForService(exposedService, managerIp,
                gateway, gatewayTopology.getTopologyName(), privateIps, api, autoTlsEnabled, version);
        serviceUrlsForService.addAll(getJdbcUrlsForService(exposedService, managerIp, gateway, securityConfig, version));
        if (serviceUrlsForService.isEmpty()) {
            services.add(createExposedServiceResponse(exposedService, gateway, exposedServicesInTopology, api, null));
        } else {
            serviceUrlsForService.forEach(url -> {
                services.add(createExposedServiceResponse(exposedService, gateway, exposedServicesInTopology, api, url));
            });
        }
        return services;
    }

    private List<ClusterExposedServiceV4Response> createCmProxiedServiceEntries(
            ExposedService exposedService,
            Gateway gateway,
            GatewayTopology gatewayTopology,
            String managerIp,
            String clusterName) {
        List<ClusterExposedServiceV4Response> services = new ArrayList<>();
        String serviceUrlsForService = getCmProxiedServiceUrlsForService(exposedService, gateway, gatewayTopology.getTopologyName(), managerIp, clusterName);
        if (!Strings.isNullOrEmpty(serviceUrlsForService)) {
            services.add(createCmProxiedExposedServiceResponse(exposedService, serviceUrlsForService));
        }
        return services;
    }

    private ClusterExposedServiceV4Response createCmProxiedExposedServiceResponse(ExposedService exposedService, String url) {
        ClusterExposedServiceV4Response service = new ClusterExposedServiceV4Response();
        service.setMode(SSOType.SSO_PROVIDER);
        service.setDisplayName(exposedService.getDisplayName());
        service.setKnoxService(exposedService.getKnoxService());
        service.setServiceName(exposedService.getServiceName());
        service.setOpen(true);
        if (isNotEmpty(url)) {
            service.setServiceUrl(url);
        }
        return service;
    }

    private ClusterExposedServiceV4Response createExposedServiceResponse(
            ExposedService exposedService,
            Gateway gateway,
            Set<String> exposedServicesInTopology,
            boolean api,
            String url) {
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
        return exposedService.isSsoSupported() ? gateway.getSsoType() : SSOType.NONE;
    }

    /**
     * Get all exposed services for blueprint, there are filtered by visibility
     *
     * @param blueprintText given blueprintText
     * @return all visible exposed services for the given blueprint
     */
    private Collection<ExposedService> getExposedServices(String blueprintText, List<String> entitlements) {
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(blueprintText);
        List<String> components = processor.getAllComponents().stream().map(ServiceComponent::getComponent).collect(Collectors.toList());
        return exposedServiceCollector.knoxServicesForComponents(processor.getVersion(), components)
                .stream()
                .filter(ExposedService::isVisible)
                .filter(e -> serviceEndpointCollectorEntitlementComparator.entitlementSupported(entitlements, e.getEntitlement()))
                .collect(Collectors.toList());
    }

    private Collection<ExposedServiceV4Response> getKnoxServices(Blueprint blueprint, List<String> entitlements) {
        return ExposedServiceV4Response.fromExposedServices(
                getExposedServices(blueprint.getBlueprintText(), entitlements));
    }

    private Stream<String> getExposedServiceStream(GatewayTopology gatewayTopology, Optional<String> version) {
        if (gatewayTopology.getExposedServices() != null && gatewayTopology.getExposedServices().getValue() != null) {
            try {
                ExposedServices exposedServices = gatewayTopology.getExposedServices().get(ExposedServices.class);
                return exposedServiceCollector.getFullServiceListBasedOnList(exposedServices.getServices(), version).stream();
            } catch (IOException e) {
                LOGGER.debug("Failed to get exposed services from Json.", e);
            }
        }
        return Stream.empty();
    }

    //CHECKSTYLE:OFF
    private List<String> getServiceUrlsForService(
            ExposedService exposedService,
            String managerIp,
            Gateway gateway,
            String topologyName,
            Map<String, List<String>> privateIps,
            boolean api,
            boolean autoTlsEnabled,
            Optional<String> version) {
        List<String> urls = new ArrayList<>();
        if (hasKnoxUrl(exposedService) && managerIp != null) {
            switch (exposedService.getName()) {
                case "NAMENODE":
                    addNameNodeUrl(managerIp, gateway, privateIps, autoTlsEnabled, urls, version);
                    break;
                case "HBASE_UI":
                    addHbaseUrl(managerIp, gateway, privateIps, urls, version);
                    break;
                case "HBASEJARS":
                    addHbaseJarsUrl(managerIp, gateway, privateIps, urls, version);
                    break;
                case "RESOURCEMANAGER_WEB":
                    addResourceManagerUrl(exposedService, managerIp, gateway, topologyName, api, urls);
                    break;
                case "NAMENODE_HDFS":
                    urls.add(buildKnoxUrlWithProtocol("hdfs", managerIp, exposedService, autoTlsEnabled));
                    break;
                case "JOBTRACKER":
                    urls.add(buildKnoxUrlWithProtocol("rpc", managerIp, exposedService, autoTlsEnabled));
                    break;
                case "RESOURCEMANAGERAPI":
                    urls.add(buildKnoxUrlWithProtocol("rpc", managerIp, exposedService, autoTlsEnabled));
                    break;
                case "IMPALA_DEBUG_UI":
                    addImplaDebugUrl(managerIp, gateway, privateIps, autoTlsEnabled, urls, version);
                    break;
                case "KUDU":
                    addKuduUrl(managerIp, gateway, privateIps, autoTlsEnabled, urls, version);
                    break;
                case "HIVE_SERVER":
                    // there is no HTTP endpoint for Hive server
                    break;
                default:
                    urls.add(getExposedServiceUrl(managerIp, gateway, topologyName, exposedService, api));
                    break;
            }
        }
        return urls;
    }
    //CHECKSTYLE:ON

    private String getCmProxiedServiceUrlsForService(
            ExposedService exposedService,
            Gateway gateway,
            String topologyName,
            String managerIp,
            String clusterName) {
        String url = "";
        if (hasKnoxUrl(exposedService) && managerIp != null) {
            url = getCmProxiedExposedServiceUrl(managerIp, exposedService, clusterName);
        }
        return url;
    }

    private List<String> getJdbcUrlsForService(
            ExposedService exposedService,
            String managerIp,
            Gateway gateway,
            SecurityConfig securityConfig,
            Optional<String> version) {
        List<String> urls = new ArrayList<>();
        if (hasKnoxUrl(exposedService) && managerIp != null) {
            switch (exposedService.getName()) {
                case "HIVE_SERVER":
                    getHiveJdbcUrl(gateway, managerIp, securityConfig, version).ifPresent(urls::add);
                    break;
                case "IMPALA":
                    getImpalaJdbcUrl(gateway, managerIp, version).ifPresent(urls::add);
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

    private void addNameNodeUrl(
            String managerIp,
            Gateway gateway,
            Map<String, List<String>> privateIps,
            boolean autoTlsEnabled,
            List<String> urls,
            Optional<String> version) {
        String serviceName = exposedServiceCollector.getNameNodeService().getServiceName();
        if (!privateIps.containsKey(serviceName)) {
            LOGGER.info("Cannot find private ip for the {} exposed service", serviceName);
        } else {
            LOGGER.debug("Select {} service from {}", serviceName, privateIps);
            List<String> hdfsUrls = privateIps.get(serviceName)
                    .stream()
                    .map(namenodeIp -> getHdfsUIUrl(gateway, managerIp, namenodeIp, autoTlsEnabled, version))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
            urls.addAll(hdfsUrls);
        }
    }

    private void addHbaseUrl(
            String managerIp,
            Gateway gateway,
            Map<String, List<String>> privateIps,
            List<String> urls,
            Optional<String> version) {
        String serviceName = exposedServiceCollector.getHBaseUIService().getServiceName();
        if (!privateIps.containsKey(serviceName)) {
            LOGGER.info("Cannot find private ip for the {} exposed service", serviceName);
        } else {
            List<String> hbaseUrls = privateIps.get(serviceName)
                    .stream()
                    .map(hbaseIp -> getHBaseServiceUrl(gateway, managerIp, hbaseIp, version))
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
            urls.addAll(hbaseUrls);
        }
    }

    private void addHbaseJarsUrl(
            String managerIp,
            Gateway gateway,
            Map<String, List<String>> privateIps,
            List<String> urls,
            Optional<String> version) {
        String serviceName = exposedServiceCollector.getHBaseJarsService().getServiceName();
        if (!privateIps.containsKey(serviceName)) {
            LOGGER.info("Cannot find private ip for the {} exposed service", serviceName);
        } else {
            // Grab the first HBase master. Any will do, so we'll take the first
            privateIps.get(serviceName)
                    .stream()
                    .map(hbaseIp -> getHBaseJarsServiceUrl(gateway, managerIp, hbaseIp, version))
                    .flatMap(Optional::stream)
                    .findFirst()
                    .ifPresent(hbaseUrl -> urls.add(hbaseUrl));
        }
    }

    private void addResourceManagerUrl(
            ExposedService exposedService,
            String managerIp,
            Gateway gateway,
            String topologyName,
            boolean api,
            List<String> urls) {
        String knoxUrl = api ? "/resourcemanager/" : exposedService.getKnoxUrl();
        String topology = api ? topologyName + API_TOPOLOGY_POSTFIX : topologyName;
        urls.add(buildKnoxUrl(managerIp, gateway, knoxUrl, topology, !exposedService.isWithoutProxyPath()));
    }

    private void addImplaDebugUrl(
            String managerIp,
            Gateway gateway,
            Map<String, List<String>> privateIps,
            boolean autoTlsEnabled,
            List<String> urls,
            Optional<String> version) {
        String serviceName = exposedServiceCollector.getImpalaDebugUIService().getServiceName();
        if (!privateIps.containsKey(serviceName)) {
            LOGGER.info("Cannot find private ip for the {} exposed service", serviceName);
        } else {
            Optional<String> coordinatorUrl = privateIps.get(serviceName)
                    .stream()
                    .map(coordinator -> getImpalaCoordinatorUrl(gateway, managerIp, coordinator, autoTlsEnabled, version))
                    .flatMap(Optional::stream)
                    .findFirst();
            if (coordinatorUrl.isPresent()) {
                urls.add(coordinatorUrl.get());
            }
        }
    }

    private void addKuduUrl(
            String managerIp,
            Gateway gateway,
            Map<String, List<String>> privateIps,
            boolean autoTlsEnabled,
            List<String> urls,
            Optional<String> version) {
        String serviceName = exposedServiceCollector.getKuduService().getServiceName();
        if (!privateIps.containsKey(serviceName)) {
            LOGGER.info("Cannot find private ip for the {} exposed service", serviceName);
        } else {
            Optional<String> coordinatorUrl = privateIps.get(serviceName)
                    .stream()
                    .map(coordinator -> getKuduUrl(gateway, managerIp, coordinator, autoTlsEnabled, version))
                    .flatMap(Optional::stream)
                    .findFirst();
            if (coordinatorUrl.isPresent()) {
                urls.add(coordinatorUrl.get());
            }
        }
    }

    private boolean isExposed(ExposedService exposedService, Collection<String> exposedServices) {
        return exposedServices.contains(exposedService.getKnoxService());
    }

    private Optional<GatewayTopology> getGatewayTopologyForService(
            Gateway gateway,
            ExposedService exposedService,
            Optional<String> version) {
        return getGatewayTopology(exposedService, gateway, version);
    }

    private Optional<GatewayTopology> getGatewayTopologyWithHive(Gateway gateway, Optional<String> version) {
        return getGatewayTopology(exposedServiceCollector.getHiveServerService(), gateway, version);
    }

    private Optional<GatewayTopology> getGatewayTopologyWithExposedService(
            Gateway gateway,
            ExposedService exposedService,
            Optional<String> version) {
        return getGatewayTopology(exposedService, gateway, version);
    }

    private Optional<GatewayTopology> getGatewayTopology(
            ExposedService exposedService,
            Gateway gateway,
            Optional<String> version) {
        return gateway.getTopologies()
                .stream()
                .filter(gt -> getExposedServiceStream(gt, version)
                        .anyMatch(es -> exposedService.getServiceName().equalsIgnoreCase(es)
                                || exposedService.getName().equalsIgnoreCase(es)
                                || exposedService.getKnoxService().equalsIgnoreCase(es)))
                .findFirst();
    }

    private String getExposedServiceUrl(
            String managerIp,
            Gateway gateway,
            String topologyName,
            ExposedService exposedService,
            boolean api) {
        String topology = api ? topologyName + API_TOPOLOGY_POSTFIX : topologyName;
        return buildKnoxUrl(managerIp, gateway, exposedService.getKnoxUrl(), topology, !exposedService.isWithoutProxyPath());
    }

    private String getCmProxiedExposedServiceUrl(
            String managerIp,
            ExposedService exposedService,
            String clusterName) {
        return buildCMProxyUrl(managerIp, exposedService.getKnoxUrl(), clusterName);
    }

    private String buildKnoxUrlWithProtocol(
            String protocol,
            String managerIp,
            ExposedService exposedService,
            boolean autoTlsEnabled) {
        Integer port;
        if (autoTlsEnabled) {
            port = exposedService.getTlsPort();
        } else {
            port = exposedService.getPort();
        }
        return String.format("%s://%s:%s", protocol, managerIp, port);
    }

    private String buildCMProxyUrl(String managerIp, String knoxUrl, String clusterName) {
        return new StringBuilder()
                .append(managerIp.replaceAll("/home/", ""))
                .append(knoxUrl.replaceAll("cluster-name", clusterName))
                .toString();
    }

    private String buildKnoxUrl(
            String managerIp,
            Gateway gateway,
            String knoxUrl,
            String topology,
            boolean useTopology) {
        String result;
        if (GatewayType.CENTRAL == gateway.getGatewayType()) {
            result = String.format("/%s/%s%s", gateway.getPath(), topology, knoxUrl);
        } else {
            if (gatewayListeningOnHttpsPort(gateway)) {
                if (useTopology) {
                    result = String.format("https://%s/%s/%s%s", managerIp, gateway.getPath(), topology, knoxUrl);
                } else {
                    result = String.format("https://%s/%s%s", managerIp, gateway.getPath(), knoxUrl);
                }
            } else {
                result = String.format("https://%s:%s/%s/%s%s", managerIp, gateway.getGatewayPort(), gateway.getPath(), topology, knoxUrl);
            }
        }
        return result;
    }

    private Optional<String> getHdfsUIUrl(
            Gateway gateway,
            String managerIp,
            String nameNodePrivateIp,
            boolean autoTlsEnabled,
            Optional<String> version) {
        return getGatewayTopologyWithExposedService(gateway, exposedServiceCollector.getNameNodeService(), version)
                .map(gt -> getHdfsUIUrlWithHostParameterFromGatewayTopology(managerIp, gt, nameNodePrivateIp, autoTlsEnabled));
    }

    private Optional<String> getImpalaCoordinatorUrl(
            Gateway gateway,
            String managerIp,
            String coordinatorPrivateIps,
            boolean autoTlsEnabled,
            Optional<String> version) {
        return getGatewayTopologyWithExposedService(gateway, exposedServiceCollector.getImpalaDebugUIService(), version)
                .map(gt -> getImpalaCoordinatorUrlWithHostFromGatewayTopology(managerIp, gt, coordinatorPrivateIps, autoTlsEnabled));
    }

    private Optional<String> getKuduUrl(
            Gateway gateway,
            String managerIp,
            String coordinatorPrivateIps,
            boolean autoTlsEnabled,
            Optional<String> version) {
        return getGatewayTopologyWithExposedService(gateway, exposedServiceCollector.getKuduService(), version)
                .map(gt -> getKuduUrlWithHostFromGatewayTopology(managerIp, gt, coordinatorPrivateIps, autoTlsEnabled));
    }

    private Optional<String> getHBaseServiceUrl(
            Gateway gateway,
            String managerIp,
            String hbaseMasterPrivateIp,
            Optional<String> version) {
        return getGatewayTopologyWithExposedService(gateway, exposedServiceCollector.getHBaseUIService(), version)
                .map(gt -> getHBaseUIUrlWithHostParameterFromGatewayTopology(managerIp, gt, hbaseMasterPrivateIp));
    }

    private Optional<String> getHBaseJarsServiceUrl(
            Gateway gateway,
            String managerIp,
            String hbaseMasterPrivateIp,
            Optional<String> version) {
        return getGatewayTopologyWithExposedService(gateway, exposedServiceCollector.getHBaseJarsService(), version)
                .map(gt -> getHBaseJarsUrlFromGatewayTopology(managerIp, gt, hbaseMasterPrivateIp));
    }

    private Optional<String> getHiveJdbcUrl(
            Gateway gateway,
            String ambariIp,
            SecurityConfig securityConfig,
            Optional<String> version) {
        return getGatewayTopologyWithHive(gateway, version)
                .map(gt -> getHiveJdbcUrlFromGatewayTopology(ambariIp, gt, securityConfig));
    }

    private Optional<String> getImpalaJdbcUrl(Gateway gateway, String ambariIp, Optional<String> version) {
        return getGatewayTopology(exposedServiceCollector.getImpalaService(), gateway, version)
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
            return String.format("jdbc:impala://%s:443/;ssl=1;transportMode=http;httpPath=%s/%s%s/impala;AuthMech=3;",
                    managerIp, gateway.getPath(), gt.getTopologyName(), API_TOPOLOGY_POSTFIX);
        } else {
            return String.format("jdbc:impala://%s:%s/;ssl=1;transportMode=http;httpPath=%s/%s%s/impala;AuthMech=3;",
                    managerIp, gateway.getGatewayPort(), gateway.getPath(), gt.getTopologyName(), API_TOPOLOGY_POSTFIX);
        }
    }

    private String getHdfsUIUrlWithHostParameterFromGatewayTopology(
            String managerIp,
            GatewayTopology gt,
            String nameNodePrivateIp,
            boolean autoTlsEnabled) {
        Gateway gateway = gt.getGateway();
        ExposedService namenode = exposedServiceCollector.getNameNodeService();
        Integer port = autoTlsEnabled ? namenode.getTlsPort() : namenode.getPort();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("https://%s/%s/%s%s?host=%s://%s:%s", managerIp, gateway.getPath(), gt.getTopologyName(),
                    namenode.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), nameNodePrivateIp, port);
        } else {
            return String.format("https://%s:%s/%s/%s%s?host=%s://%s:%s", managerIp, gateway.getGatewayPort(), gateway.getPath(), gt.getTopologyName(),
                    namenode.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), nameNodePrivateIp, port);
        }
    }

    private String getHBaseUIUrlWithHostParameterFromGatewayTopology(
            String managerIp,
            GatewayTopology gt,
            String hbaseMasterPrivateIp) {
        Gateway gateway = gt.getGateway();
        ExposedService hbaseUi = exposedServiceCollector.getHBaseUIService();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("https://%s/%s/%s%s?host=%s&port=%s", managerIp, gateway.getPath(), gt.getTopologyName(),
                    hbaseUi.getKnoxUrl(), hbaseMasterPrivateIp, hbaseUi.getPort());
        } else {
            return String.format("https://%s:%s/%s/%s%s?host=%s&port=%s", managerIp, gateway.getGatewayPort(), gateway.getPath(), gt.getTopologyName(),
                    hbaseUi.getKnoxUrl(), hbaseMasterPrivateIp, hbaseUi.getPort());
        }
    }

    private String getHBaseJarsUrlFromGatewayTopology(
            String managerIp,
            GatewayTopology gt,
            String hbaseMasterPrivateIp) {
        Gateway gateway = gt.getGateway();
        ExposedService hbaseJars = exposedServiceCollector.getHBaseJarsService();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("https://%s/%s/%s%s%s", managerIp, gateway.getPath(), gt.getTopologyName(),
                    API_TOPOLOGY_POSTFIX, hbaseJars.getKnoxUrl());
        } else {
            return String.format("https://%s:%s/%s/%s%s%s", managerIp, gateway.getGatewayPort(), gateway.getPath(), gt.getTopologyName(),
                    API_TOPOLOGY_POSTFIX, hbaseJars.getKnoxUrl());
        }
    }

    private String getImpalaCoordinatorUrlWithHostFromGatewayTopology(
            String managerIp,
            GatewayTopology gt,
            String impalaPrivateIp,
            boolean autoTlsEnabled) {
        Gateway gateway = gt.getGateway();
        ExposedService impalaDebugUi = exposedServiceCollector.getImpalaDebugUIService();
        Integer port = autoTlsEnabled ? impalaDebugUi.getTlsPort() : impalaDebugUi.getPort();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("https://%s/%s/%s%s?scheme=%s&host=%s&port=%s", managerIp, gateway.getPath(), gt.getTopologyName(),
                    impalaDebugUi.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), impalaPrivateIp, port);
        } else {
            return String.format("https://%s:%s/%s/%s%s?scheme=%s&host=%s&port=%s", managerIp, gateway.getGatewayPort(), gateway.getPath(),
                    gt.getTopologyName(), impalaDebugUi.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), impalaPrivateIp, port);
        }
    }

    private String getKuduUrlWithHostFromGatewayTopology(
            String managerIp,
            GatewayTopology gt,
            String kuduPrivateIp,
            boolean autoTlsEnabled) {
        Gateway gateway = gt.getGateway();
        ExposedService kudu = exposedServiceCollector.getKuduService();
        Integer port = autoTlsEnabled ? kudu.getTlsPort() : kudu.getPort();
        if (gatewayListeningOnHttpsPort(gateway)) {
            return String.format("https://%s/%s/%s%s?scheme=%s&host=%s&port=%s", managerIp, gateway.getPath(), gt.getTopologyName(),
                    kudu.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), kuduPrivateIp, port);
        } else {
            return String.format("https://%s:%s/%s/%s%s?scheme=%s&host=%s&port=%s", managerIp, gateway.getGatewayPort(), gateway.getPath(),
                    gt.getTopologyName(), kudu.getKnoxUrl(), getHttpProtocol(autoTlsEnabled), kuduPrivateIp, port);
        }
    }

    private String getHttpProtocol(boolean autoTlsEnabled) {
        return autoTlsEnabled ? "https" : "http";
    }

    private boolean gatewayListeningOnHttpsPort(Gateway gateway) {
        return gateway.getGatewayPort().equals(Integer.valueOf(httpsPort));
    }
}
