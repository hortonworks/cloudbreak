package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_REQUESTED;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component
@ConfigurationProperties(prefix = "cb.instance")
public class InstanceMetadataUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataUpdater.class);

    private final Pattern saltBootstrapVersionPattern = Pattern.compile("Version: (.*)");

    private List<Package> packages;

    @Inject
    private HostOrchestratorResolver hostOrchestratorResolver;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public void updatePackageVersionsOnAllInstances(Stack stack) throws Exception {
        Boolean enableKnox = stack.getCluster().getGateway() != null;
        GatewayConfig gatewayConfig = getGatewayConfig(stack, enableKnox);
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());

        Map<String, Map<String, String>> packageVersionsByNameByHost = getPackageVersionByNameByHost(gatewayConfig, hostOrchestrator);

        Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        Map<String, Multimap<String, String>> changedVersionsByHost =
                updateInstanceMetaDataWithPackageVersions(packageVersionsByNameByHost, instanceMetaDataSet);
        notifyIfPackagesHaveChangedVersions(stack, changedVersionsByHost);

        List<String> packagesWithMultipleVersions = collectPackagesWithMultipleVersions(instanceMetaDataSet);
        notifyIfPackagesHaveDifferentVersions(stack, packagesWithMultipleVersions);

        Map<String, List<String>> instancesWithMissingPackageVersions = collectInstancesWithMissingPackageVersions(instanceMetaDataSet);
        notifyIfInstancesMissingPackageVersion(stack, instancesWithMissingPackageVersions);
    }

    private GatewayConfig getGatewayConfig(Stack stack, Boolean enableKnox) {
        GatewayConfig gatewayConfig = null;
        for (InstanceMetaData gateway : stack.getGatewayInstanceMetadata()) {
            if (InstanceMetadataType.GATEWAY_PRIMARY.equals(gateway.getInstanceMetadataType())) {
                gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gateway, enableKnox);
            }
        }
        return gatewayConfig;
    }

    private Map<String, Multimap<String, String>> updateInstanceMetaDataWithPackageVersions(Map<String, Map<String, String>> packageVersionsByNameByHost,
            Set<InstanceMetaData> instanceMetaDataSet) throws IOException {

        Map<String, Multimap<String, String>> changedVersionsByHost = new HashMap<>();
        for (InstanceMetaData im : instanceMetaDataSet) {
            Map<String, String> packageVersionsOnHost = packageVersionsByNameByHost.get(im.getDiscoveryFQDN());
            if (!CollectionUtils.isEmpty(packageVersionsOnHost)) {
                Image image = im.getImage().get(Image.class);
                if (!image.getPackageVersions().equals(packageVersionsOnHost)) {
                    Multimap<String, String> pkgVersionsMMap = LinkedHashMultimap.create();
                    pkgVersionsMMap.putAll(Multimaps.forMap(packageVersionsOnHost));
                    pkgVersionsMMap.putAll(Multimaps.forMap(image.getPackageVersions()));
                    changedVersionsByHost.put(im.getDiscoveryFQDN(), pkgVersionsMMap);
                }
                image = updatePackageVersions(image, packageVersionsOnHost);
                im.setImage(new Json(image));
                instanceMetaDataRepository.save(im);
            }
        }
        return changedVersionsByHost;
    }

    private void notifyIfPackagesHaveChangedVersions(Stack stack, Map<String, Multimap<String, String>> changedVersionsByHost) {
        if (!changedVersionsByHost.isEmpty()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_REQUESTED.name(),
                    cloudbreakMessagesService.getMessage(Msg.PACKAGE_VERSIONS_ARE_CHANGED.code(),
                            Collections.singletonList(changedVersionsByHost.entrySet().stream()
                                    .map(entry -> String.format("On Instance ID: [%s], package versions have been changed: [%s]",
                                            entry.getKey(), entry.getValue().toString()))
                                    .collect(Collectors.joining("\r\n")))));
        }
    }

    private void notifyIfInstancesMissingPackageVersion(Stack stack, Map<String, List<String>> instancesWithMissingPackageVersions) {
        if (!instancesWithMissingPackageVersions.isEmpty()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.PACKAGE_VERSIONS_ON_INSTANCES_ARE_MISSING.code(),
                            Collections.singletonList(instancesWithMissingPackageVersions.entrySet().stream()
                                    .map(entry -> String.format("Instance ID: [%s] Packages without version: [%s]",
                                            entry.getKey(), StringUtils.join(entry.getValue(), ",")))
                                    .collect(Collectors.joining(" * ")))));
        }
    }

    private void notifyIfPackagesHaveDifferentVersions(Stack stack, List<String> packagesWithMultipleVersions) {
        if (!packagesWithMultipleVersions.isEmpty()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.PACKAGES_ON_INSTANCES_ARE_DIFFERENT.code(),
                            Collections.singletonList(String.join(",", packagesWithMultipleVersions))));
        }
    }

    private Map<String, Map<String, String>> getPackageVersionByNameByHost(GatewayConfig gatewayConfig, HostOrchestrator hostOrchestrator)
            throws CloudbreakOrchestratorFailedException {
        Map<String, Map<String, String>> packageVersionsByNameByHost = getPackageVersionByPackageName(gatewayConfig, hostOrchestrator);
        addPackageVersionsFromCommand(gatewayConfig, hostOrchestrator, packageVersionsByNameByHost);
        addPackageVersionsFromGrain(gatewayConfig, hostOrchestrator, packageVersionsByNameByHost);
        return packageVersionsByNameByHost;
    }

    private void addPackageVersionsFromCommand(GatewayConfig gatewayConfig, HostOrchestrator hostOrchestrator,
            Map<String, Map<String, String>> packageVersionsByNameByHost) throws CloudbreakOrchestratorFailedException {
        List<Package> packagesWithCommand = packages.stream().filter(pkg -> StringUtils.isNotBlank(pkg.getCommand())).collect(Collectors.toList());
        for (Package packageWithCommand : packagesWithCommand) {
            Map<String, String> versionsByHost = hostOrchestrator.runCommandOnAllHosts(gatewayConfig, packageWithCommand.getCommand());
            for (Entry<String, String> entry : versionsByHost.entrySet()) {
                packageVersionsByNameByHost.computeIfAbsent(entry.getKey(), s -> new HashMap<>())
                        .put(packageWithCommand.getName(), parseSaltBootstrapVersion(entry.getValue()));
            }
        }
    }

    private void addPackageVersionsFromGrain(GatewayConfig gatewayConfig, HostOrchestrator hostOrchestrator,
            Map<String, Map<String, String>> packageVersionsByNameByHost) throws CloudbreakOrchestratorFailedException {
        List<Package> packagesWithGrain = packages.stream().filter(pkg -> StringUtils.isNotBlank(pkg.getGrain())).collect(Collectors.toList());
        for (Package packageWithGrain : packagesWithGrain) {
            Map<String, String> versionsByHost = hostOrchestrator.getGrainOnAllHosts(gatewayConfig, packageWithGrain.getGrain());
            for (Entry<String, String> entry : versionsByHost.entrySet()) {
                packageVersionsByNameByHost.computeIfAbsent(entry.getKey(), s -> new HashMap<>()).put(packageWithGrain.getName(), entry.getValue());
            }
        }
    }

    private Map<String, Map<String, String>> getPackageVersionByPackageName(GatewayConfig gatewayConfig, HostOrchestrator hostOrchestrator)
            throws CloudbreakOrchestratorFailedException {
        Map<String, String> pkgNames = mapPackageToPkgNameAndNameMap();

        // Map<host, Map<pkgName, version>
        Map<String, Map<String, String>> packageVersionsByPkgNameByHost =
                hostOrchestrator.getPackageVersionsFromAllHosts(gatewayConfig, pkgNames.keySet().toArray(new String[pkgNames.size()]));

        return mapHostPkgNameVersionToHostNameVersionMap(pkgNames, packageVersionsByPkgNameByHost);
    }

    private Map<String, Map<String, String>> mapHostPkgNameVersionToHostNameVersionMap(Map<String, String> pkgNames,
            Map<String, Map<String, String>> packageVersionsByPkgNameByHost) {
        // Map<host, Map<name, version>
        Map<String, Map<String, String>> packageVersionsByNameByHost = new HashMap<>();
        for (Entry<String, Map<String, String>> entry : packageVersionsByPkgNameByHost.entrySet()) {
            Map<String, String> versionByName =
                    entry.getValue().entrySet().stream()
                            .filter(e -> StringUtils.isNotBlank(e.getValue()))
                            .collect(Collectors.toMap(e -> pkgNames.get(e.getKey()), Map.Entry::getValue));
            packageVersionsByNameByHost.put(entry.getKey(), versionByName);
        }
        return packageVersionsByNameByHost;
    }

    private Map<String, String> mapPackageToPkgNameAndNameMap() {
        /*
         * From Package { List<String> pkgName; String name; }
         * To Map<pkgName, name>
         */
        return packages.stream().filter(pkg -> pkg.getPkgName() != null && !pkg.getPkgName().isEmpty())
                .flatMap(pkg -> pkg.getPkgName().stream()
                        .map(pkgName -> new SimpleEntry<>(pkgName, pkg.getName())))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public List<String> collectPackagesWithMultipleVersions(Collection<InstanceMetaData> instanceMetadataList) {
        try {
            Multimap<String, String> pkgVersionsMMap = HashMultimap.create();
            for (InstanceMetaData im : instanceMetadataList) {
                Image image = im.getImage().get(Image.class);
                for (Map.Entry<String, String> packageEntry : image.getPackageVersions().entrySet()) {
                    pkgVersionsMMap.put(packageEntry.getKey(), packageEntry.getValue());
                }
            }
            List<String> packagesWithMultipleVersions = new ArrayList<>();
            for (String pkg : pkgVersionsMMap.keySet()) {
                if (pkgVersionsMMap.get(pkg).size() > 1) {
                    packagesWithMultipleVersions.add(pkg);
                }
            }
            return packagesWithMultipleVersions;
        } catch (IOException ex) {
            LOGGER.warn("Cannot collect package versions from hosts", ex);
            return Collections.emptyList();
        }
    }

    public Map<String, List<String>> collectInstancesWithMissingPackageVersions(Collection<InstanceMetaData> instanceMetaDatas) {
        Map<String, List<String>> instancesWithMissingPackagVersions = new HashMap<>();

        for (InstanceMetaData instanceMetaData : instanceMetaDatas) {
            try {
                Image image = instanceMetaData.getImage().get(Image.class);
                Set<String> packages = image.getPackageVersions().keySet();
                List<String> missingPackageVersions = this.packages.stream().map(Package::getName).collect(Collectors.toList());
                missingPackageVersions.removeAll(packages);
                if (!missingPackageVersions.isEmpty()) {
                    instancesWithMissingPackagVersions.put(instanceMetaData.getInstanceId(), missingPackageVersions);
                }
            } catch (IOException e) {
                LOGGER.warn("Missing image information for instance: " + instanceMetaData.getInstanceId(), e);
            }
        }

        return instancesWithMissingPackagVersions;
    }

    public List<Package> getPackages() {
        return packages;
    }

    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }

    private String parseSaltBootstrapVersion(String versionCommandOutput) {
        Matcher matcher = saltBootstrapVersionPattern.matcher(versionCommandOutput);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return versionCommandOutput;
    }

    private Image updatePackageVersions(Image image, Map<String, String> packageVersionsOnHost) {
        return new Image(image.getImageName(), image.getUserdata(), image.getOs(), image.getOsType(), image.getImageCatalogUrl(),
                image.getImageCatalogName(), image.getImageId(), packageVersionsOnHost);
    }

    public enum Msg {
        PACKAGES_ON_INSTANCES_ARE_DIFFERENT("ambari.cluster.sync.instance.different.packages"),
        PACKAGE_VERSIONS_ON_INSTANCES_ARE_MISSING("ambari.cluster.sync.instance.missing.package.versions"),
        PACKAGE_VERSIONS_ARE_CHANGED("ambari.cluster.sync.instance.changed.packages");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }

    public static class Package {
        private String name;

        private List<String> pkgName;

        private String command;

        private boolean prewarmed;

        private String grain;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getPkgName() {
            return pkgName;
        }

        public void setPkgName(List<String> pkgName) {
            this.pkgName = pkgName;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public boolean isPrewarmed() {
            return prewarmed;
        }

        public void setPrewarmed(boolean prewarmed) {
            this.prewarmed = prewarmed;
        }

        public String getGrain() {
            return grain;
        }

        public void setGrain(String grain) {
            this.grain = grain;
        }
    }
}
