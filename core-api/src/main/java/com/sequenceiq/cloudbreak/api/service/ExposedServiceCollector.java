package com.sequenceiq.cloudbreak.api.service;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
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

    private final MultiValuedMap<String, ExposedService> exposedServices = new HashSetValuedHashMap<>();

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private ExposedServiceVersionSupport exposedServiceVersionSupportService;

    @PostConstruct
    public void init() {
        readExposedServices();
    }

    public ExposedService getAtlasService() {
        return getFirst("ATLAS");
    }

    public ExposedService getClouderaManagerService() {
        return getFirst("CLOUDERA_MANAGER");
    }

    public ExposedService getClouderaManagerUIService() {
        return getFirst("CLOUDERA_MANAGER_UI");
    }

    public ExposedService getHBaseRestService() {
        return getFirst("HBASE_REST");
    }

    public ExposedService getHBaseUIService() {
        return getFirst("HBASE_UI");
    }

    public ExposedService getHBaseJarsService() {
        return getFirst("HBASEJARS");
    }

    public ExposedService getHiveServerService() {
        return getFirst("HIVE_SERVER");
    }

    public ExposedService getHueService() {
        return getFirst("HUE");
    }

    public ExposedService getImpalaService() {
        return getFirst("IMPALA");
    }

    public ExposedService getImpalaDebugUIService() {
        return getFirst("IMPALA_DEBUG_UI");
    }

    public ExposedService getKuduService() {
        return getFirst("KUDU");
    }

    public ExposedService getQueueService() {
        return getFirst("QUEUEMANAGER_WEBAPP");
    }

    public ExposedService getNameNodeService() {
        return getFirst("NAMENODE");
    }

    public ExposedService getNiFiService() {
        return getFirst("NIFI");
    }

    public ExposedService getRangerService() {
        return getFirst("RANGER");
    }

    public ExposedService getResourceManagerWebService() {
        return getFirst("RESOURCEMANAGER_WEB");
    }

    public String getKnoxServiceName() {
        return "KNOX";
    }

    public ExposedService getByName(String name) {
        return getFirst(name);
    }

    public Set<String> getFullServiceListBasedOnList(Collection<String> services, Optional<String> bpVersion) {
        Set<String> result = new HashSet<>(services);
        if (services.contains("ALL")) {
            result = getAllKnoxExposed(bpVersion);
        }
        return result;
    }

    public boolean isKnoxExposed(String knoxService, Optional<String> bpVersion) {
        return getAllKnoxExposed(bpVersion).contains(knoxService);
    }

    public Collection<ExposedService> knoxServicesForComponents(Optional<String> bpVersion, Collection<String> components) {
        return filterSupportedKnoxServices(bpVersion)
                .stream()
                .filter(exposedService -> isVersionSupported(bpVersion, exposedService))
                .filter(exposedService ->
                        components.contains(exposedService.getServiceName())
                                || exposedService.getServiceName().equalsIgnoreCase(getKnoxServiceName())
                                || getClouderaManagerUIService().getServiceName().equals(exposedService.getServiceName())
                                || getClouderaManagerService().getServiceName().equals(exposedService.getServiceName())
                                // IMPALA_DEBUG_UI needs to be exposed under the same service name, but with different purpose
                                || (getImpalaDebugUIService().getServiceName().equals(exposedService.getServiceName())
                                && components.contains(getImpalaService().getServiceName())))
                .collect(Collectors.toList());
    }

    public Set<String> getAllKnoxExposed(Optional<String> bpVersion) {
        return filterSupportedKnoxServices(bpVersion).stream()
                .map(ExposedService::getKnoxService)
                .collect(Collectors.toSet());
    }

    public Set<String> getAllServiceNames() {
        return exposedServices.values()
                .stream()
                .map(ExposedService::getServiceName)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
    }

    public Map<String, Integer> getAllServicePorts(Optional<String> bpVersion, boolean tls) {
        return exposedServices.values()
                .stream()
                .filter(x -> hasServicePort(tls, x))
                .filter(x -> isVersionSupported(bpVersion, x))
                .collect(Collectors.toMap(ExposedService::getKnoxService, v -> tls ? v.getTlsPort() : v.getPort()));
    }

    private void readExposedServices() {
        LOGGER.debug("Loading exposed-services.json");
        String exposedServiceDefinition = cloudbreakResourceReaderService.resourceDefinition("exposed-services");
        try {
            JsonUtil.readValue(exposedServiceDefinition, ExposedServices.class).getServices()
                    .forEach(exposedService -> exposedServices.put(exposedService.getName(), exposedService));
            String exposedServiceNames = String.join(",", exposedServices.keySet());
            LOGGER.info("The following exposed service(s) has loaded (in total: {}): {}", exposedServices.size(), exposedServiceNames);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot initialize Exposed services.", e);
        }
    }

    private Collection<ExposedService> filterSupportedKnoxServices(Optional<String> bpVersion) {
        return exposedServices.values().stream()
                .filter(x -> StringUtils.isNotEmpty(x.getKnoxService()))
                .filter(x -> isVersionSupported(bpVersion, x))
                .collect(Collectors.toList());
    }

    private boolean isVersionSupported(Optional<String> bpVersion, ExposedService exposedService) {
        return exposedServiceVersionSupportService.maxVersionSupported(bpVersion, exposedService.getMaxVersion())
                && exposedServiceVersionSupportService.minVersionSupported(bpVersion, exposedService.getMinVersion());
    }

    private boolean hasServicePort(boolean tls, ExposedService exposedService) {
        return StringUtils.isNotEmpty(exposedService.getServiceName())
                && StringUtils.isNotEmpty(exposedService.getKnoxService())
                && Objects.nonNull(tls ? exposedService.getTlsPort() : exposedService.getPort());
    }

    private ExposedService getFirst(String serviceName) {
        return exposedServices.get(serviceName)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Given service \"" + serviceName + "\" not found"));
    }
}
