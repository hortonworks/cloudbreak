package com.sequenceiq.cloudbreak.cm.config.upgrade;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.getSafetyValveProperty;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiProxyHostsCollector.NIFI_WEB_PROXY_HOST;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.model.ApiConfig;
import com.cloudera.api.swagger.model.ApiRoleConfigGroup;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.LoadBalancerUserFacingFqdnProvider;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerConfigService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateService;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiProxyHostsCollector;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiRoles;
import com.sequenceiq.cloudbreak.cmtemplate.general.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.util.XmlUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

/**
 * Upgrading to CFM 4.12.0.10000+ makes NiFi validate the X-Forwarded-Host / X-ProxyHost headers sent by Knox against nifi.web.proxy.host,
 * which would make the NiFi UI unreachable through the load balancer or the public endpoint of the cluster (HTTP 421).
 * The property is only a NIFI_NODE role config from CFM 4.12.0.10000, so it is set through the nifi.properties safety valve, that is
 * supported by the CSD of the source CFM versions as well. Entries of the safety valve other than nifi.web.proxy.host are kept.
 * The safety valve overrides the nifi.web.proxy.host role config in the generated nifi.properties, so the hosts already present either in the
 * safety valve or in the role config are merged after the computed ones, so a custom proxy host keeps working after the upgrade.
 * A role config group is left untouched when its nifi.web.proxy.host role config already contains every computed host and the safety valve
 * does not set the property (e.g. clusters created with CFM 4.12.0.10000+).
 */
@Component
public class ClouderaManagerNifiProxyHostConfigurationService implements RuntimeUpgradeServiceConfigPatcher {

    static final String NIFI_PROPERTIES_SAFETY_VALVE = "staging/nifi.properties.xml_role_safety_valve";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerNifiProxyHostConfigurationService.class);

    private static final String NIFI_SERVICE_TYPE = "NIFI";

    private static final String CFM = "CFM";

    private static final String ROOT_ELEMENT = "configuration";

    @Inject
    private CmTemplateService cmTemplateService;

    @Inject
    private ClouderaManagerConfigService clouderaManagerConfigService;

    @Inject
    private LoadBalancerUserFacingFqdnProvider loadBalancerUserFacingFqdnProvider;

    @Override
    public boolean isPatchNeeded(StackDtoDelegate stack, Set<ClouderaManagerProduct> targetProducts) {
        return isTargetCfmVersionRequiringProxyHosts(targetProducts)
                && stack.hasGateway()
                && cmTemplateService.isServiceTypePresent(NIFI_SERVICE_TYPE, stack.getBlueprintJsonText());
    }

    @Override
    public void patchServiceConfig(ApiClient apiClient, StackDtoDelegate stack, Set<ClouderaManagerProduct> targetProducts) {
        Set<String> proxyHosts = collectProxyHosts(stack);
        if (proxyHosts.isEmpty()) {
            LOGGER.info("No proxy hosts found, {} is not configured for NiFi", NIFI_WEB_PROXY_HOST);
            return;
        }
        String serviceName = clouderaManagerConfigService.getServiceName(apiClient, stack.getName(), NIFI_SERVICE_TYPE)
                .orElseThrow(() -> new CloudbreakServiceException("NiFi service is not found in Cloudera Manager"));
        List<ApiRoleConfigGroup> nifiNodeRoleConfigGroups = clouderaManagerConfigService.readRoleConfigGroupConfigs(apiClient, stack.getName(), serviceName)
                .getItems().stream()
                .filter(roleConfigGroup -> NifiRoles.NIFI_NODE.equals(roleConfigGroup.getRoleType()))
                .toList();
        nifiNodeRoleConfigGroups.forEach(roleConfigGroup -> patchRoleConfigGroup(apiClient, stack, serviceName, roleConfigGroup, proxyHosts));
    }

    private void patchRoleConfigGroup(ApiClient apiClient, StackDtoDelegate stack, String serviceName, ApiRoleConfigGroup roleConfigGroup,
            Set<String> proxyHosts) {
        // the safety valve is customer content that may contain sensitive properties, so it must not be logged
        String currentSafetyValve = getConfigValue(roleConfigGroup, NIFI_PROPERTIES_SAFETY_VALVE);
        Optional<ParsedSafetyValve> parsedSafetyValve = parseSafetyValve(currentSafetyValve);
        if (parsedSafetyValve.isEmpty()) {
            LOGGER.warn("The {} of {} role config group cannot be parsed, {} is not configured",
                    NIFI_PROPERTIES_SAFETY_VALVE, roleConfigGroup.getName(), NIFI_WEB_PROXY_HOST);
            return;
        }
        String roleConfigProxyHosts = getConfigValue(roleConfigGroup, NIFI_WEB_PROXY_HOST);
        if (parsedSafetyValve.get().proxyHostValues().isEmpty() && containsAllProxyHosts(roleConfigProxyHosts, proxyHosts)) {
            // e.g. clusters created with CFM 4.12.0.10000+ get the role config at cluster creation, the safety valve would only duplicate it
            LOGGER.info("{} role config of {} role config group already contains every required host, the {} is not modified",
                    NIFI_WEB_PROXY_HOST, roleConfigGroup.getName(), NIFI_PROPERTIES_SAFETY_VALVE);
            return;
        }
        List<String> existingProxyHostValues = new ArrayList<>(parsedSafetyValve.get().proxyHostValues());
        Optional.ofNullable(roleConfigProxyHosts).ifPresent(existingProxyHostValues::add);
        String proxyHostsValue = String.join(",", mergeProxyHosts(proxyHosts, existingProxyHostValues));
        String patchedSafetyValve = parsedSafetyValve.get().otherEntries() + getSafetyValveProperty(NIFI_WEB_PROXY_HOST, proxyHostsValue);
        if (patchedSafetyValve.equals(currentSafetyValve)) {
            LOGGER.info("{} is already configured in the {} of {} role config group", NIFI_WEB_PROXY_HOST, NIFI_PROPERTIES_SAFETY_VALVE,
                    roleConfigGroup.getName());
        } else {
            LOGGER.info("Setting {} to {} in the {} of {} role config group", NIFI_WEB_PROXY_HOST, proxyHostsValue, NIFI_PROPERTIES_SAFETY_VALVE,
                    roleConfigGroup.getName());
            clouderaManagerConfigService.modifyRoleConfigGroup(apiClient, stack.getName(), serviceName, roleConfigGroup.getName(),
                    Map.of(NIFI_PROPERTIES_SAFETY_VALVE, patchedSafetyValve));
        }
    }

    /**
     * The computed hosts come first, then the hosts already configured. Placeholders like ${KNOX_HOST} are not kept, because the CSD of the
     * source CFM versions cannot substitute them and the Knox gateway hosts are part of the computed ones anyway.
     */
    static Set<String> mergeProxyHosts(Collection<String> computedProxyHosts, Collection<String> existingProxyHostValues) {
        Set<String> merged = new LinkedHashSet<>(computedProxyHosts);
        existingProxyHostValues.stream()
                .flatMap(value -> splitProxyHosts(value).stream())
                .filter(host -> !host.contains("${"))
                .forEach(merged::add);
        return merged;
    }

    private static boolean containsAllProxyHosts(String proxyHostsValue, Set<String> requiredProxyHosts) {
        return proxyHostsValue != null && splitProxyHosts(proxyHostsValue).containsAll(requiredProxyHosts);
    }

    private static List<String> splitProxyHosts(String proxyHostsValue) {
        return Arrays.stream(proxyHostsValue.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .toList();
    }

    private boolean isTargetCfmVersionRequiringProxyHosts(Set<ClouderaManagerProduct> targetProducts) {
        return targetProducts.stream()
                .filter(product -> CFM.equalsIgnoreCase(product.getName()))
                .map(ClouderaManagerProduct::getVersion)
                .anyMatch(NifiProxyHostsCollector::isProxyHostConfigRequired);
    }

    private Set<String> collectProxyHosts(StackDtoDelegate stack) {
        InstanceMetadataView primaryGateway = stack.getPrimaryGatewayInstance();
        Set<String> otherGatewayFqdns = GeneralClusterConfigsProvider.getOtherGatewayInstancesFqdns(primaryGateway, stack.getAllAvailableGatewayInstances());
        String externalFqdn = stack.getCluster() == null ? null : stack.getCluster().getFqdn();
        return NifiProxyHostsCollector.collect(Optional.ofNullable(primaryGateway).map(InstanceMetadataView::getDiscoveryFQDN),
                otherGatewayFqdns, externalFqdn, Optional.ofNullable(loadBalancerUserFacingFqdnProvider.getLoadBalancerUserFacingFQDN(stack.getId())));
    }

    private String getConfigValue(ApiRoleConfigGroup roleConfigGroup, String configName) {
        if (roleConfigGroup.getConfig() == null || roleConfigGroup.getConfig().getItems() == null) {
            return null;
        }
        return roleConfigGroup.getConfig().getItems().stream()
                .filter(config -> configName.equals(config.getName()))
                .map(ApiConfig::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * The safety valve contains a list of hadoop xml property elements without a root element. The nifi.web.proxy.host entries are separated
     * from every other content, which is kept as it is.
     *
     * @return the parsed safety valve or empty if the current content cannot be parsed
     */
    static Optional<ParsedSafetyValve> parseSafetyValve(String currentSafetyValve) {
        if (StringUtils.isBlank(currentSafetyValve)) {
            return Optional.of(new ParsedSafetyValve("", List.of()));
        }
        try {
            String xml = String.format("<%s>%s</%s>", ROOT_ELEMENT, currentSafetyValve, ROOT_ELEMENT);
            Document document = XmlUtil.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            StringBuilder otherEntries = new StringBuilder();
            List<String> proxyHostValues = new ArrayList<>();
            NodeList children = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (isProxyHostProperty(child)) {
                    NodeList values = ((Element) child).getElementsByTagName("value");
                    if (values.getLength() > 0) {
                        proxyHostValues.add(values.item(0).getTextContent());
                    }
                } else {
                    otherEntries.append(serialize(child));
                }
            }
            return Optional.of(new ParsedSafetyValve(StringUtils.stripEnd(otherEntries.toString(), null), proxyHostValues));
        } catch (Exception e) {
            // the SAX parser messages contain the reason and the position, not the content
            LOGGER.warn("Failed to parse the safety valve content", e);
            return Optional.empty();
        }
    }

    private static boolean isProxyHostProperty(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE || !"property".equals(node.getNodeName())) {
            return false;
        }
        NodeList names = ((Element) node).getElementsByTagName("name");
        return names.getLength() > 0 && NIFI_WEB_PROXY_HOST.equals(names.item(0).getTextContent().trim());
    }

    private static String serialize(Node node) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }

    record ParsedSafetyValve(String otherEntries, List<String> proxyHostValues) {
    }
}
