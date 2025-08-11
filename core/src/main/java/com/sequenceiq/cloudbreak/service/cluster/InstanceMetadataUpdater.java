package com.sequenceiq.cloudbreak.service.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_PACKAGES_ON_INSTANCES_ARE_DIFFERENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_PACKAGE_VERSIONS_ON_INSTANCES_ARE_MISSING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_PACKAGE_VERSION_CANNOT_BE_QUERIED;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
@ConfigurationProperties(prefix = "cb.instance")
public class InstanceMetadataUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataUpdater.class);

    private List<Package> packages;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private StackService stackService;

    public void updatePackageVersionsOnAllInstances(Long stackId) throws Exception {
        Stack stack = getStackForFreshInstanceStatuses(stackId);
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);

        Map<String, Map<String, String>> packageVersionsByNameByHost = getPackageVersionByNameByHost(gatewayConfig, hostOrchestrator);

        List<String> failedVersionQueriesByHost =
                updateInstanceMetaDataIfVersionQueryFailed(packageVersionsByNameByHost, stack);
        notifyIfVersionsCannotBeQueried(stack, failedVersionQueriesByHost);

        Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedAndNotZombieInstanceMetaDataSet();

        updateInstanceMetaDataWithPackageVersions(stackId, packageVersionsByNameByHost, instanceMetaDataSet);

        List<String> packagesWithMultipleVersions = collectPackagesWithMultipleVersions(instanceMetaDataSet);
        notifyIfPackagesHaveDifferentVersions(stack, packagesWithMultipleVersions);

        Map<String, List<String>> instancesWithMissingPackageVersions = collectInstancesWithMissingPackageVersions(instanceMetaDataSet);
        notifyIfInstancesMissingPackageVersion(stack, instancesWithMissingPackageVersions);
    }

    private Stack getStackForFreshInstanceStatuses(Long stackId) {
        return stackService.getByIdWithListsInTransaction(stackId);
    }

    private List<String> updateInstanceMetaDataIfVersionQueryFailed(Map<String, Map<String, String>> packageVersionsByNameByHost,
            Stack stack) throws IOException {
        Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedAndNotZombieInstanceMetaDataSet();

        List<String> failedVersionQueriesByHost = Lists.newArrayList();
        for (InstanceMetaData im : instanceMetaDataSet) {
            Map<String, String> packageVersionsOnHost = packageVersionsByNameByHost.get(im.getDiscoveryFQDN());
            if (CollectionUtils.isEmpty(packageVersionsOnHost)) {
                failedVersionQueriesByHost.add(im.getDiscoveryFQDN());
                Image image = im.getImage().get(Image.class);
                image.getPackageVersions().clear();
                im.setImage(new Json(image));
                im.setInstanceStatus(InstanceStatus.SERVICES_UNHEALTHY);
                im.setStatusReason("Version query is failed on host");
                instanceMetaDataService.save(im);
            }
        }
        return failedVersionQueriesByHost;
    }

    private Map<String, Multimap<String, String>> updateInstanceMetaDataWithPackageVersions(Long stackId, Map<String,
            Map<String, String>> packageVersionsByNameByHost, Set<InstanceMetaData> instanceMetaDataSet) throws IOException {
        Map<String, Multimap<String, String>> changedVersionsByHost = new HashMap<>();
        for (InstanceMetaData im : instanceMetaDataSet) {
            Map<String, String> packageVersionsOnHost = packageVersionsByNameByHost.get(im.getDiscoveryFQDN());
            if (!CollectionUtils.isEmpty(packageVersionsOnHost)) {
                Image image = im.getImage().get(Image.class);
                if (!Maps.transformValues(image.getPackageVersions(), this::removeBuildVersion)
                        .equals(Maps.transformValues(packageVersionsOnHost, this::removeBuildVersion))) {
                    Multimap<String, String> pkgVersionsMMap = LinkedHashMultimap.create();
                    pkgVersionsMMap.putAll(Multimaps.forMap(Maps.transformValues(packageVersionsOnHost, this::removeBuildVersion)));
                    pkgVersionsMMap.putAll(Multimaps.forMap(Maps.transformValues(image.getPackageVersions(), this::removeBuildVersion)));
                    changedVersionsByHost.put(im.getDiscoveryFQDN(), pkgVersionsMMap);
                }
                image = getImageWithPackageVersions(stackId, image, packageVersionsOnHost);
                im.setImage(new Json(image));
                instanceMetaDataService.save(im);
            }
        }
        return changedVersionsByHost;
    }

    private void notifyIfVersionsCannotBeQueried(Stack stack, List<String> failedVersionQueryByHost) {
        if (!failedVersionQueryByHost.isEmpty()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_FAILED.name(),
                    CLUSTER_PACKAGE_VERSION_CANNOT_BE_QUERIED,
                    Collections.singletonList(String.join("\r\n", failedVersionQueryByHost)));
        }
    }

    private void notifyIfInstancesMissingPackageVersion(Stack stack, Map<String, List<String>> instancesWithMissingPackageVersions) {
        if (!instancesWithMissingPackageVersions.isEmpty()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    CLUSTER_PACKAGE_VERSIONS_ON_INSTANCES_ARE_MISSING,
                    Collections.singletonList(instancesWithMissingPackageVersions.entrySet().stream()
                            .map(entry -> String.format("Instance ID: [%s] Packages without version: [%s]",
                                    entry.getKey(), StringUtils.join(entry.getValue(), ",")))
                            .collect(Collectors.joining(" * "))));
        }
    }

    private void notifyIfPackagesHaveDifferentVersions(Stack stack, List<String> packagesWithMultipleVersions) {
        if (!packagesWithMultipleVersions.isEmpty()) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    CLUSTER_PACKAGES_ON_INSTANCES_ARE_DIFFERENT,
                    Collections.singletonList(String.join(",", packagesWithMultipleVersions)));
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
                String version = parseVersion(entry.getValue(), packageWithCommand.getCommandVersionPattern());
                if (!StringUtils.equalsAny(version, "false", "null", null)) {
                    packageVersionsByNameByHost.computeIfAbsent(entry.getKey(), s -> new HashMap<>())
                            .put(packageWithCommand.getName(), version);
                }
            }
        }
    }

    private void addPackageVersionsFromGrain(GatewayConfig gatewayConfig, HostOrchestrator hostOrchestrator,
            Map<String, Map<String, String>> packageVersionsByNameByHost) throws CloudbreakOrchestratorFailedException {
        List<Package> packagesWithGrain = packages.stream().filter(pkg -> StringUtils.isNotBlank(pkg.getGrain())).collect(Collectors.toList());
        for (Package packageWithGrain : packagesWithGrain) {
            Map<String, JsonNode> versionsByHost = hostOrchestrator.getGrainOnAllHosts(gatewayConfig, packageWithGrain.getGrain());
            for (Entry<String, JsonNode> entry : versionsByHost.entrySet()) {
                String entryValue = entry.getValue().textValue();
                if (!StringUtils.equalsAny(entryValue, "false", "null", null)) {
                    packageVersionsByNameByHost.computeIfAbsent(entry.getKey(), s -> new HashMap<>())
                            .put(packageWithGrain.getName(), entryValue);
                }
            }
        }
    }

    private Map<String, Map<String, String>> getPackageVersionByPackageName(GatewayConfig gatewayConfig, HostOrchestrator hostOrchestrator)
            throws CloudbreakOrchestratorFailedException {
        Map<PackageName, String> pkgNames = mapPackageToPkgNameAndNameMap();
        Map<String, Optional<String>> pkgNameStringMap = pkgNames.keySet().stream()
                .collect(Collectors.toMap(PackageName::getName, pkgName -> Optional.ofNullable(pkgName.getPattern())));

        // Map<host, Map<pkgName, version>
        Map<String, Map<String, String>> packageVersionsByPkgNameByHost =
                hostOrchestrator.getPackageVersionsFromAllHosts(gatewayConfig, pkgNameStringMap);

        return mapHostPkgNameVersionToHostNameVersionMap(pkgNames, packageVersionsByPkgNameByHost);
    }

    private Map<String, Map<String, String>> mapHostPkgNameVersionToHostNameVersionMap(Map<PackageName, String> pkgNames,
            Map<String, Map<String, String>> packageVersionsByPkgNameByHost) {
        // Map<host, Map<name, version>
        Map<String, Map<String, String>> packageVersionsByNameByHost = new HashMap<>();
        for (Entry<String, Map<String, String>> entry : packageVersionsByPkgNameByHost.entrySet()) {
            Map<String, String> versionByName =
                    entry.getValue().entrySet().stream()
                            .filter(e -> StringUtils.isNotBlank(e.getValue()) && !StringUtils.equalsAny(e.getValue(), "false", "null", null))
                            .collect(Collectors.toMap(e -> getPkgNameStringMap(pkgNames).get(e.getKey()), Entry::getValue, (a1, a2) -> a1));
            packageVersionsByNameByHost.put(entry.getKey(), versionByName);
        }
        return packageVersionsByNameByHost;
    }

    private Map<String, String> getPkgNameStringMap(Map<PackageName, String> pkgNames) {
        return pkgNames.entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().getName(), Entry::getValue));
    }

    private Map<PackageName, String> mapPackageToPkgNameAndNameMap() {
        /*
         * From Package { List<String> pkgName; String name; }
         * To Map<pkgName, name>
         */
        return packages.stream().filter(pkg -> pkg.getPkg() != null && !pkg.getPkg().isEmpty())
                .flatMap(pkg -> pkg.getPkg().stream()
                        .map(pkgName -> new AbstractMap.SimpleEntry<>(pkgName, pkg.getName())))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public List<String> collectPackagesWithMultipleVersions(Collection<? extends InstanceMetadataView> instanceMetadataList) {
        try {
            Multimap<String, String> pkgVersionsMMap = HashMultimap.create();
            for (InstanceMetadataView im : instanceMetadataList) {
                Image image = im.getImage().get(Image.class);
                for (Entry<String, String> packageEntry : image.getPackageVersions().entrySet()) {
                    pkgVersionsMMap.put(packageEntry.getKey(), packageEntry.getValue());
                }
            }
            List<String> packagesWithMultipleVersions = new ArrayList<>();
            for (String pkg : pkgVersionsMMap.keySet()) {
                if (pkgVersionsMMap.get(pkg).size() > 1) {
                    packagesWithMultipleVersions.add(pkg);
                }
            }
            if (!packagesWithMultipleVersions.isEmpty()) {
                LOGGER.debug("Packages unfortunately do have multiple versions: {}", pkgVersionsMMap);
            }
            return packagesWithMultipleVersions;
        } catch (IOException ex) {
            LOGGER.warn("Cannot collect package versions from hosts", ex);
            return Collections.emptyList();
        }
    }

    public Map<String, List<String>> collectInstancesWithMissingPackageVersions(Collection<? extends InstanceMetadataView> instanceMetaDatas) {
        Map<String, List<String>> instancesWithMissingPackageVersions = new HashMap<>();

        for (InstanceMetadataView instanceMetaData : instanceMetaDatas) {
            try {
                Image image = instanceMetaData.getImage().get(Image.class);
                Set<String> packages = image.getPackageVersions().keySet();
                List<String> missingPackageVersions = this.packages.stream().map(Package::getName).collect(Collectors.toList());
                missingPackageVersions.removeAll(packages);
                if (!missingPackageVersions.isEmpty()) {
                    instancesWithMissingPackageVersions.put(instanceMetaData.getInstanceId(), missingPackageVersions);
                }
            } catch (IOException e) {
                LOGGER.warn("Missing image information for instance: " + instanceMetaData.getInstanceId(), e);
            }
        }

        return instancesWithMissingPackageVersions;
    }

    public List<Package> getPackages() {
        return packages;
    }

    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }

    private String parseVersion(String versionCommandOutput, String pattern) {
        if (pattern != null) {
            Matcher matcher = Pattern.compile(pattern).matcher(versionCommandOutput);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return versionCommandOutput;
    }

    public boolean isPackagesVersionEqual(String expectedVersion, String actualVersion) {
        return removeBuildVersion(expectedVersion).equalsIgnoreCase(removeBuildVersion(actualVersion));
    }

    private String removeBuildVersion(String version) {
        return version.split("-")[0];
    }

    private Image getImageWithPackageVersions(Long stackId, Image image, Map<String, String> packageVersionsOnHost) {
        return new Image(image.getImageName(), new HashMap<>(), image.getOs(), image.getOsType(), image.getArchitecture(), image.getImageCatalogUrl(),
                image.getImageCatalogName(), image.getImageId(), packageVersionsOnHost, image.getDate(), image.getCreated(), image.getTags());
    }

}
