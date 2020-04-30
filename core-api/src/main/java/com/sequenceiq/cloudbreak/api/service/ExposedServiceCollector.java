package com.sequenceiq.cloudbreak.api.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;

@Service
public class ExposedServiceCollector {

    public static final String ALL = "ALL";

    private static final Logger LOGGER = LoggerFactory.getLogger(ExposedServiceCollector.class);

    private Map<String, ExposedService> exposedServices = new HashMap<>();

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @PostConstruct
    public void init() {
        readExposedServices();
    }

    public ExposedService getAtlasService() {
        return exposedServices.get("ATLAS");
    }

    public ExposedService getClouderaManagerService() {
        return exposedServices.get("CLOUDERA_MANAGER");
    }

    public ExposedService getClouderaManagerUIService() {
        return exposedServices.get("CLOUDERA_MANAGER_UI");
    }

    public ExposedService getHBaseRestService() {
        return exposedServices.get("HBASE_REST");
    }

    public ExposedService getHBaseUIService() {
        return exposedServices.get("HBASE_UI");
    }

    public ExposedService getHBaseJarsService() {
        return exposedServices.get("HBASEJARS");
    }

    public ExposedService getHiveServerService() {
        return exposedServices.get("HIVE_SERVER");
    }

    public ExposedService getHueService() {
        return exposedServices.get("HUE");
    }

    public ExposedService getImpalaService() {
        return exposedServices.get("IMPALA");
    }

    public ExposedService getImpalaDebugUIService() {
        return exposedServices.get("IMPALA_DEBUG_UI");
    }

    public ExposedService getKuduService() {
        return exposedServices.get("KUDU");
    }

    public ExposedService getQueueService() {
        return exposedServices.get("QUEUEMANAGER_WEBAPP");
    }

    public ExposedService getNameNodeService() {
        return exposedServices.get("NAMENODE");
    }

    public ExposedService getNiFiService() {
        return exposedServices.get("NIFI");
    }

    public ExposedService getRangerService() {
        return exposedServices.get("RANGER");
    }

    public ExposedService getResourceManagerWebService() {
        return exposedServices.get("RESOURCEMANAGER_WEB");
    }

    public ExposedService getByName(String name) {
        return exposedServices.get(name);
    }

    public Set<String> getFullServiceListBasedOnList(Collection<String> services) {
        Set<String> result = new HashSet<>(services);
        if (services.contains("ALL")) {
            result = getAllKnoxExposed();
        }
        return result;
    }

    public boolean isKnoxExposed(String knoxService) {
        return getAllKnoxExposed().contains(knoxService);
    }

    public Collection<ExposedService> knoxServicesForComponents(Collection<String> components) {
        return filterSupportedKnoxServices().stream()
                .filter(exposedService ->
                        components.contains(exposedService.getServiceName())
                                || getClouderaManagerUIService().getServiceName().equals(exposedService.getServiceName())
                                || getClouderaManagerService().getServiceName().equals(exposedService.getServiceName())
                                // IMPALA_DEBUG_UI needs to be exposed under the same service name, but with different purpose
                                || (getImpalaDebugUIService().getServiceName().equals(exposedService.getServiceName())
                                && components.contains(getImpalaService().getServiceName())))
                .collect(Collectors.toList());
    }

    public Set<String> getAllKnoxExposed() {
        return filterSupportedKnoxServices().stream().map(ExposedService::getKnoxService).collect(Collectors.toSet());
    }

    public List<String> getAllServiceNames() {
        List<String> allServiceName = exposedServices.values().stream()
                .filter(x -> StringUtils.isNotEmpty(x.getServiceName()))
                .map(ExposedService::getServiceName).collect(Collectors.toList());
        return List.copyOf(allServiceName);
    }

    public Map<String, Integer> getAllServicePorts(boolean tls) {
        return exposedServices.values().stream().filter(x -> StringUtils.isNotEmpty(x.getServiceName())
                && StringUtils.isNotEmpty(x.getKnoxService())
                && Objects.nonNull(tls ? x.getTlsPort() : x.getPort()))
                .collect(Collectors.toMap(ExposedService::getKnoxService, v -> tls ? v.getTlsPort() : v.getPort()));
    }

    private void readExposedServices() {
        LOGGER.debug("Loading exposed-services.json");
        String exposedServiceDefinition = cloudbreakResourceReaderService.resourceDefinition("exposed-services");
        try {
            exposedServices = JsonUtil.readValue(exposedServiceDefinition, ExposedServices.class).getServices()
                    .stream()
                    .collect(Collectors.toMap(ExposedService::getName, Function.identity()));
            String exposedServiceNames = String.join(",", exposedServices.keySet());
            LOGGER.info("The following exposed service(s) has loaded (in total: {}): {}", exposedServices.size(), exposedServiceNames);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot initialize Exposed services.", e);
        }
    }

    private Collection<ExposedService> filterSupportedKnoxServices() {
        return exposedServices.values().stream().filter(x -> StringUtils.isNotEmpty(x.getKnoxService())).collect(Collectors.toList());
    }

}
