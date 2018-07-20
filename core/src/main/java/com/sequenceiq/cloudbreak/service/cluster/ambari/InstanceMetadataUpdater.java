package com.sequenceiq.cloudbreak.service.cluster.ambari;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetadataType;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component
public class InstanceMetadataUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMetadataUpdater.class);

    private Pattern saltBootstrapVersionPattern = Pattern.compile("Version: (.*)");

    @Value("#{'${cb.instance.packages}'.split(',')}")
    private String[] packages;

    @Value("${cb.instance.saltboot.versionCommand}")
    private String saltBootstrapVersionCmd;

    @Value("${cb.instance.saltboot.packageName}")
    private String saltBootstrapPackageName;

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
        GatewayConfig gatewayConfig = null;
        for (InstanceMetaData gateway : stack.getGatewayInstanceMetadata()) {
            if (InstanceMetadataType.GATEWAY_PRIMARY.equals(gateway.getInstanceMetadataType())) {
                gatewayConfig = gatewayConfigService.getGatewayConfig(stack, gateway, enableKnox);
            }
        }
        HostOrchestrator hostOrchestrator = hostOrchestratorResolver.get(stack.getOrchestrator().getType());
        Map<String, Map<String, String>> packageVersions = hostOrchestrator.getPackageVersionsFromAllHosts(gatewayConfig, packages);
        Map<String, String> saltbootVersions = hostOrchestrator.runCommandOnAllHosts(gatewayConfig, saltBootstrapVersionCmd);
        for (Map.Entry<String, String> saltbootVersion : saltbootVersions.entrySet()) {
            packageVersions.get(saltbootVersion.getKey()).put(saltBootstrapPackageName, parseSaltBootstrapVersion(saltbootVersion.getValue()));
        }
        Set<InstanceMetaData> instanceMetaDataSet = stack.getNotDeletedInstanceMetaDataSet();
        for (InstanceMetaData im : instanceMetaDataSet) {
            Map<String, String> packageVersionsOnHost = packageVersions.get(im.getDiscoveryFQDN());
            if (!CollectionUtils.isEmpty(packageVersionsOnHost)) {
                Image image = im.getImage().get(Image.class);
                image = updatePackageVersions(image, packageVersionsOnHost);
                im.setImage(new Json(image));
                instanceMetaDataRepository.save(im);
            }
        }
        List<String> packagesWithMultipleVersions = collectPackagesWithMultipleVersions(instanceMetaDataSet);
        if (packagesWithMultipleVersions.size() > 0) {
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(),
                    cloudbreakMessagesService.getMessage(Msg.PACKAGES_ON_INSTANCES_ARE_DIFFERENT.code(),
                            Collections.singletonList(packagesWithMultipleVersions.stream().collect(Collectors.joining(",")))));

        }
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

    private String parseSaltBootstrapVersion(String versionCommandOutput) {
        Matcher matcher = saltBootstrapVersionPattern.matcher(versionCommandOutput);
        String result = "UNKNOWN";
        if (matcher.matches()) {
            result = matcher.group(1);
        }
        return result;
    }

    private Image updatePackageVersions(Image image, Map<String, String> packageVersionsOnHost) {
        return new Image(image.getImageName(), image.getUserdata(), image.getOs(), image.getOsType(), image.getImageCatalogUrl(),
                image.getImageCatalogName(), image.getImageId(), packageVersionsOnHost);
    }

    private enum Msg {
        PACKAGES_ON_INSTANCES_ARE_DIFFERENT("ambari.cluster.sync.instance.different.packages");

        private final String code;

        Msg(String msgCode) {
            code = msgCode;
        }

        public String code() {
            return code;
        }
    }
}
