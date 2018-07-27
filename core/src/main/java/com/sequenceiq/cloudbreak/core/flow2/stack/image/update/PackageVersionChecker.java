package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.testng.collections.Lists;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.cluster.ambari.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ambari.InstanceMetadataUpdater.Package;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component
public class PackageVersionChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PackageVersionChecker.class);

    @Inject
    private InstanceMetadataUpdater instanceMetadataUpdater;

    @Inject
    private CloudbreakMessagesService messagesService;

    public CheckResult compareImageAndInstancesMandatoryPackageVersion(StatedImage newImage, Set<InstanceMetaData> instanceMetaDataSet) {
        Map<String, String> packageVersions = newImage.getImage().getPackageVersions();
        List<String> packagesToCompare;
        if (newImage.getImage().isPrewarmed()) {
            packagesToCompare = instanceMetadataUpdater.getPackages().stream().map(Package::getName).collect(Collectors.toList());
        } else {
            packagesToCompare = instanceMetadataUpdater.getPackages().stream().filter(pkg -> !pkg.isPrewarmed())
                    .map(Package::getName).collect(Collectors.toList());
        }

        List<String> missingPackageVersion = Lists.newArrayList();
        List<String> differentPackageVersion = Lists.newArrayList();
        Map<String, String> instancePackageVersions = null;
        try {
            instancePackageVersions = instanceMetaDataSet.stream().findFirst().orElseThrow().getImage().get(Image.class).getPackageVersions();
        } catch (IOException e) {
            LOGGER.warn("Could not get image", e);
            return CheckResult.failed("Could not get image");
        }

        for (String packageToCompare : packagesToCompare) {
            String packageVersionInImage = packageVersions.get(packageToCompare);
            if (StringUtils.isBlank(packageVersionInImage)) {
                LOGGER.warn("Missing package in image: " + packageToCompare);
                missingPackageVersion.add(packageToCompare);
            } else {
                if (!packageVersionInImage.equalsIgnoreCase(instancePackageVersions.get(packageToCompare))) {
                    LOGGER.warn(String.format("Different package [%s] version on image [%s] and on instance [%s]",
                            packageToCompare, packageVersionInImage, instancePackageVersions.get(packageToCompare)));
                    differentPackageVersion.add(packageToCompare);
                }
            }
        }

        if (!missingPackageVersion.isEmpty() || !differentPackageVersion.isEmpty()) {
            String message = messagesService.getMessage(Msg.PACKAGES_ARE_DIFFERENT_IN_IMAGE.code(),
                    Lists.newArrayList(StringUtils.join(missingPackageVersion, ","), StringUtils.join(differentPackageVersion, ",")));
            return CheckResult.failed(message);
        } else {
            return CheckResult.ok();
        }
    }

    public CheckResult checkInstancesHaveAllMandatoryPackageVersion(Set<InstanceMetaData> instanceMetaDataSet) {
        Map<String, List<String>> instancesWithMissingPackageVersions = instanceMetadataUpdater.collectInstancesWithMissingPackageVersions(instanceMetaDataSet);
        if (!instancesWithMissingPackageVersions.isEmpty()) {
            String message = messagesService.getMessage(InstanceMetadataUpdater.Msg.PACKAGE_VERSIONS_ON_INSTANCES_ARE_MISSING.code(),
                    Collections.singletonList(instancesWithMissingPackageVersions.entrySet().stream()
                            .map(entry -> String.format("Instance ID: [%s] Packages without version: [%s]",
                                    entry.getKey(), StringUtils.join(entry.getValue(), ",")))
                            .collect(Collectors.joining(" * "))));
            return CheckResult.failed(message);
        } else {
            return CheckResult.ok();
        }
    }

    public CheckResult checkInstancesHaveMultiplePackageVersions(Set<InstanceMetaData> instanceMetaDataSet) {
        List<String> packagesWithMultipleVersions = instanceMetadataUpdater.collectPackagesWithMultipleVersions(instanceMetaDataSet);
        if (!packagesWithMultipleVersions.isEmpty()) {
            String message = messagesService.getMessage(InstanceMetadataUpdater.Msg.PACKAGES_ON_INSTANCES_ARE_DIFFERENT.code(),
                    Collections.singletonList(packagesWithMultipleVersions.stream().collect(Collectors.joining(","))));
            return CheckResult.failed(message);
        } else {
            return CheckResult.ok();
        }
    }

    private enum Msg {
        PACKAGES_ARE_DIFFERENT_IN_IMAGE("stack.image.update.packages.different");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }
}
