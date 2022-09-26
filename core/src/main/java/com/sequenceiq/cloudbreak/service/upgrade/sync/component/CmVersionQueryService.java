package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.model.PackageInfo;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.Package;
import com.sequenceiq.cloudbreak.service.cluster.PackageName;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
@ConfigurationProperties(prefix = "cb.instance")
public class CmVersionQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmVersionQueryService.class);

    private List<Package> packages;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    public List<Package> getPackages() {
        return packages;
    }

    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }

    /**
     * Will query all CM related package versions (CM server and CM agent as well) from the nodes.
     * <p>
     *
     * @param stack The stack, with metadata to be able to build the client to query package versions
     * @return List of package info found in each host (map key is host fqdn)
     */
    public Map<String, List<PackageInfo>> queryCmPackageInfo(StackDtoDelegate stack) throws CloudbreakOrchestratorFailedException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Map<String, Optional<String>> packageMap = getPackageStream()
                .collect(Collectors.toMap(
                        PackageName::getName,
                        packageName -> Optional.ofNullable(packageName.getPattern())));
        Map<String, List<PackageInfo>> fullPackageVersionsFromAllHosts = hostOrchestrator.getFullPackageVersionsFromAllHosts(gatewayConfig, packageMap);
        LOGGER.debug("Reading CM package info, found packages: " + fullPackageVersionsFromAllHosts);
        return fullPackageVersionsFromAllHosts;
    }

    public PackageInfo checkCmPackageInfoConsistency(Map<String, List<PackageInfo>> cmPackageVersionsFromAllHosts) {
        Multimap<String, PackageInfo> pkgVersionsMMap = HashMultimap.create();
        cmPackageVersionsFromAllHosts.values()
                .forEach(packageInfoList -> packageInfoList.forEach(
                        packageInfo -> pkgVersionsMMap.put(packageInfo.getName(), packageInfo)));
        validatePackageHasMultipleVersions(pkgVersionsMMap);
        Set<PackageInfo> distinctPackageInfos = new HashSet<>(pkgVersionsMMap.values());
        validatePackageInfoExists(distinctPackageInfos);
        return getPackageInfoWithLatestVersion(distinctPackageInfos);
    }

    private PackageInfo getPackageInfoWithLatestVersion(Set<PackageInfo> distinctPackageInfos) {
        return distinctPackageInfos.stream()
                .max((p1, p2) -> new VersionComparator().compare(p1::getFullVersion, p2::getFullVersion))
                .get();
    }

    private void validatePackageInfoExists(Set<PackageInfo> distinctPackageInfos) {
        if (distinctPackageInfos.size() == 0) {
            String error = "Error during sync! CM server and agent versions cannot be determined!";
            logAndThrowError(error);
        }
    }

    private void validatePackageHasMultipleVersions(Multimap<String, PackageInfo> pkgVersionsMMap) {

        Set<String> packagesToValidate = getPackagesToValidateForMultipleVersions();

        HashMultimap<String, PackageInfo> filteredPkgVersionsMMap = pkgVersionsMMap.entries()
                .stream()
                .filter(stringPackageInfoEntry -> packagesToValidate.contains(stringPackageInfoEntry.getKey()))
                .collect(Multimaps.toMultimap(Map.Entry::getKey, Map.Entry::getValue, HashMultimap::create));
        if (filteredPkgVersionsMMap.keySet().size() < filteredPkgVersionsMMap.size()) {
            String packageErrorStr = filteredPkgVersionsMMap.asMap()
                    .values()
                    .stream()
                    .filter(packageInfos -> packageInfos.size() > 1)
                    .map(Object::toString)
                    .collect(Collectors.joining("Package: "));
            logAndThrowError("Error during sync! The following package(s) has multiple versions present on the machines. Package: " + packageErrorStr);
        }
    }

    private Stream<PackageName> getPackageStream() {
        return packages.stream()
                .filter(aPackage -> aPackage.getName().equals(ImagePackageVersion.CM.getKey()))
                .map(Package::getPkg)
                .flatMap(List::stream);
    }

    private Set<String> getPackagesToValidateForMultipleVersions() {
        return getPackageStream()
                .filter(PackageName::getValidateForMultipleVersions)
                .map(PackageName::getName)
                .collect(Collectors.toSet());
    }

    private void logAndThrowError(String error) {
        LOGGER.warn(error);
        throw new CloudbreakServiceException(error);
    }

}
