package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
        for (InstanceMetaData im : stack.getNotDeletedInstanceMetaDataSet()) {
            Map<String, String> packageVersionsOnHost = packageVersions.get(im.getDiscoveryFQDN());
            if (!CollectionUtils.isEmpty(packageVersionsOnHost)) {
                Image image = im.getImage().get(Image.class);
                image = updatePackageVersions(image, packageVersionsOnHost);
                im.setImage(new Json(image));
                instanceMetaDataRepository.save(im);
            }
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
}
