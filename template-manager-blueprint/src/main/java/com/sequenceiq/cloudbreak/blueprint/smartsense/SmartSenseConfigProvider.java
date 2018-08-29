package com.sequenceiq.cloudbreak.blueprint.smartsense;

import static com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType.GATEWAY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.template.processor.configuration.BlueprintConfigurationEntry;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.SmartsenseConfigurationLocator;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;

@Component
public class SmartSenseConfigProvider implements BlueprintComponentConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmartSenseConfigProvider.class);

    private static final String SMART_SENSE_SERVER_CONFIG_FILE = "hst-server-conf";

    private static final String HST_SERVER_COMPONENT = "HST_SERVER";

    private static final String HST_AGENT_COMPONENT = "HST_AGENT";

    private static final String RESOURCEMANAGER_COMPONENT = "RESOURCEMANAGER";

    private static final int SMART_SENSE_CLUSTER_NAME_MAX_LENGTH = 64;

    private static final String SMART_SENSE_PRODUCT_INFO_FILE = "product-info";

    @Value("${cb.product.id:cloudbreak}")
    private String productId;

    @Value("${cb.component.cluster.id:cloudbreak-hdp}")
    private String clustersComponentId;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private SmartsenseConfigurationLocator smartsenseConfigurationLocator;

    @Override
    public BlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, BlueprintTextProcessor blueprintProcessor) {
        String smartSenseId = source.getSmartSenseSubscription().get().getSubscriptionId();
        Set<String> hostGroupNames = source.getHostgroupViews().stream().map(getHostGroupNameMapper()).collect(Collectors.toSet());
        addSmartSenseServerToBp(blueprintProcessor, source.getHostgroupViews(), hostGroupNames);
        blueprintProcessor.addComponentToHostgroups(HST_AGENT_COMPONENT, hostGroupNames::contains);
        List<BlueprintConfigurationEntry> configs = new ArrayList<>(getSmartSenseServerConfigs(source, smartSenseId));
        return blueprintProcessor.addConfigEntries(configs, true);
    }

    @Override
    public boolean specialCondition(TemplatePreparationObject source, String blueprintText) {
        return smartsenseConfigurationLocator.smartsenseConfigurableBySubscriptionId(source.getSmartSenseSubscription().isPresent()
                ? Optional.ofNullable(source.getSmartSenseSubscription().get().getSubscriptionId())
                : Optional.empty());
    }

    private Function<HostgroupView, String> getHostGroupNameMapper() {
        return HostgroupView::getName;
    }

    private String addSmartSenseServerToBp(BlueprintTextProcessor blueprintProcessor, Iterable<HostgroupView> hostgroupViews,
            Collection<String> hostGroupNames) {
        if (!blueprintProcessor.componentExistsInBlueprint(HST_SERVER_COMPONENT)) {
            String aHostGroupName = hostGroupNames.stream().sorted(String::compareTo).findFirst().get();
            boolean singleNodeGatewayFound = false;
            for (HostgroupView hostGroup : hostgroupViews) {
                if (hostGroup.isInstanceGroupConfigured() && GATEWAY.equals(hostGroup.getInstanceGroupType()) && hostGroup.getNodeCount().equals(1)) {
                    aHostGroupName = hostGroup.getName();
                    singleNodeGatewayFound = true;
                    break;
                }
            }

            if (!singleNodeGatewayFound && blueprintProcessor.componentExistsInBlueprint(RESOURCEMANAGER_COMPONENT)) {
                Optional<String> hostGroupNameOfNameNode = hostGroupNames
                        .stream()
                        .filter(hGName -> blueprintProcessor.getComponentsInHostGroup(hGName).contains(RESOURCEMANAGER_COMPONENT))
                        .findFirst();
                if (hostGroupNameOfNameNode.isPresent()) {
                    aHostGroupName = hostGroupNameOfNameNode.get();
                }

            }
            LOGGER.info("Adding '{}' component to '{}' hosgroup in the Blueprint.", HST_SERVER_COMPONENT, aHostGroupName);
            final String finalAHostGroupName = aHostGroupName;
            blueprintProcessor.addComponentToHostgroups(HST_SERVER_COMPONENT, finalAHostGroupName::equals);
        }
        return blueprintProcessor.asText();
    }

    private Collection<? extends BlueprintConfigurationEntry> getSmartSenseServerConfigs(TemplatePreparationObject source, String smartSenseId) {
        Collection<BlueprintConfigurationEntry> configs = new ArrayList<>();
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.account.name", "Hortonworks_Cloud_HDP"));
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.notification.email", "aws-marketplace@hortonworks.com"));
        String clusterName = getClusterName(source);
        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "cluster.name", clusterName));

        configs.add(new BlueprintConfigurationEntry(SMART_SENSE_SERVER_CONFIG_FILE, "customer.smartsense.id", smartSenseId));

        HSTMetadataInstanceInfoJson instanceInfoJson = new HSTMetadataInstanceInfoJson(
                source.getFlexSubscription().isPresent() ? source.getFlexSubscription().get().getSubscriptionId() : "",
                clusterName,
                source.getGeneralClusterConfigs().getUuid(),
                cloudbreakNodeConfig.getInstanceUUID());
        HSTMetadataJson productInfo = new HSTMetadataJson(clustersComponentId, instanceInfoJson, productId, cbVersion);
        try {
            Json productInfoJson = new Json(productInfo);
            configs.add(new BlueprintConfigurationEntry(SMART_SENSE_PRODUCT_INFO_FILE, "product-info-content", productInfoJson.getValue()));
        } catch (JsonProcessingException ignored) {
            LOGGER.error("The 'product-info-content' SmartSense config could not be added to the Blueprint.");
        }
        return configs;
    }

    private String getClusterName(TemplatePreparationObject source) {
        String ssClusterNamePattern = "cbc--%s--%s";
        String clusterName = source.getGeneralClusterConfigs().getClusterName();
        String ssClusterName = String.format(ssClusterNamePattern, clusterName, source.getGeneralClusterConfigs().getUuid());
        if (ssClusterName.length() > SMART_SENSE_CLUSTER_NAME_MAX_LENGTH) {
            int charsOverTheLimit = ssClusterName.length() - SMART_SENSE_CLUSTER_NAME_MAX_LENGTH;
            ssClusterName = charsOverTheLimit < clusterName.length()
                    ? String.format(ssClusterNamePattern, clusterName.substring(0, clusterName.length() - charsOverTheLimit),
                    source.getGeneralClusterConfigs().getUuid()) : ssClusterName.substring(0, SMART_SENSE_CLUSTER_NAME_MAX_LENGTH);
        }
        return ssClusterName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder({"flexSubscriptionId", "guid", "name", "parentGuid"})
    static class HSTMetadataInstanceInfoJson {
        private final String flexSubscriptionId;

        private final String guid;

        private final String name;

        private final String parentGuid;

        HSTMetadataInstanceInfoJson(String flexSubscriptionId, String guid, String name, String parentGuid) {
            this.flexSubscriptionId = flexSubscriptionId;
            this.guid = guid;
            this.name = name;
            this.parentGuid = parentGuid;
        }

        public String getFlexSubscriptionId() {
            return flexSubscriptionId;
        }

        public String getGuid() {
            return guid;
        }

        public String getName() {
            return name;
        }

        public String getParentGuid() {
            return parentGuid;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonPropertyOrder({"componentId", "instanceInfo", "productId", "productVersion", "schemaVersion", "type"})
    static class HSTMetadataJson {

        private static final String SCHEMA_VERSION = "1.0.0";

        private static final String TYPE = "cluster";

        private final String componentId;

        private final HSTMetadataInstanceInfoJson instanceInfo;

        private final String productId;

        private final String productVersion;

        HSTMetadataJson(String componentId, HSTMetadataInstanceInfoJson instanceInfo, String productId, String productVersion) {
            this.componentId = componentId;
            this.instanceInfo = instanceInfo;
            this.productId = productId;
            this.productVersion = productVersion;
        }

        public String getComponentId() {
            return componentId;
        }

        public HSTMetadataInstanceInfoJson getInstanceInfo() {
            return instanceInfo;
        }

        public String getProductId() {
            return productId;
        }

        public String getProductVersion() {
            return productVersion;
        }

        public String getSchemaVersion() {
            return SCHEMA_VERSION;
        }

        public String getType() {
            return TYPE;
        }
    }

}
