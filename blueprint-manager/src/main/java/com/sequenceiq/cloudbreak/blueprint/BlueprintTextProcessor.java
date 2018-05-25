package com.sequenceiq.cloudbreak.blueprint;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.blueprint.configuration.HostgroupConfiguration;
import com.sequenceiq.cloudbreak.blueprint.configuration.HostgroupConfigurations;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteConfiguration;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteSettingsConfigurations;
import com.sequenceiq.cloudbreak.util.JsonUtil;

public class BlueprintTextProcessor {

    public static final String CONFIGURATIONS_NODE = "configurations";

    private static final String SETTINGS_NODE = "settings";

    private static final String SECURITY_NODE = "security";

    private static final String PROPERTIES_NODE = "properties";

    private static final String HOST_GROUPS_NODE = "host_groups";

    private static final String COMPONENTS_NODE = "components";

    private static final String BLUEPRINTS = "Blueprints";

    private static final String STACK_VERSION = "stack_version";

    private static final String STACK_NAME = "stack_name";

    private final ObjectNode blueprint;

    public BlueprintTextProcessor(@Nonnull String blueprintText) {
        try {
            blueprint = (ObjectNode) JsonUtil.readTree(blueprintText);
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to parse blueprint text.", e);
        }
    }

    public String asText() {
        try {
            return JsonUtil.writeValueAsString(blueprint);
        } catch (JsonProcessingException e) {
            throw new BlueprintProcessingException("Failed to render blueprint text.", e);
        }
    }

    public BlueprintTextProcessor addConfigEntries(List<BlueprintConfigurationEntry> configurationEntries, boolean override) {
        JsonNode configurationsNode = blueprint.path(CONFIGURATIONS_NODE);
        if (configurationsNode.isMissingNode()) {
            configurationsNode = blueprint.putArray(CONFIGURATIONS_NODE);
        }
        ArrayNode configurationsArrayNode = (ArrayNode) configurationsNode;
        for (BlueprintConfigurationEntry configurationEntry : configurationEntries) {
            JsonNode configFileNode = configurationsArrayNode.findPath(configurationEntry.getConfigFile());
            if (override || configFileNode.path(PROPERTIES_NODE).findPath(configurationEntry.getKey()).isMissingNode()) {
                if (configFileNode.isMissingNode()) {
                    ObjectNode arrayElementNode = configurationsArrayNode.addObject();
                    configFileNode = arrayElementNode.putObject(configurationEntry.getConfigFile());
                }
                JsonNode propertiesNode = configFileNode.path(PROPERTIES_NODE);
                if (!propertiesNode.isMissingNode()) {
                    ((ObjectNode) propertiesNode).put(configurationEntry.getKey(), configurationEntry.getValue());
                } else {
                    ((ObjectNode) configFileNode).put(configurationEntry.getKey(), configurationEntry.getValue());
                }
            }
        }
        return this;
    }

    public BlueprintTextProcessor addSettingsEntries(List<BlueprintConfigurationEntry> configurationEntries, boolean override) {
        JsonNode configurationsNode = blueprint.path(SETTINGS_NODE);
        if (configurationsNode.isMissingNode()) {
            configurationsNode = blueprint.putArray(SETTINGS_NODE);
        }
        ArrayNode configurationsArrayNode = (ArrayNode) configurationsNode;
        for (BlueprintConfigurationEntry configurationEntry : configurationEntries) {
            JsonNode configFileNode = configurationsArrayNode.findPath(configurationEntry.getConfigFile());
            if (override || configFileNode.findPath(configurationEntry.getKey()).isMissingNode()) {
                if (configFileNode.isMissingNode()) {
                    ObjectNode arrayElementNode = configurationsArrayNode.addObject();
                    ArrayNode recoveryDefinition = arrayElementNode.putArray(configurationEntry.getConfigFile());
                    ObjectNode recoveryEntry = recoveryDefinition.addObject();
                    recoveryEntry.put(configurationEntry.getKey(), configurationEntry.getValue());
                } else {
                    ObjectNode recoveryEntry = ((ArrayNode) configFileNode).addObject();
                    recoveryEntry.put(configurationEntry.getKey(), configurationEntry.getValue());
                }
            }
        }
        return this;
    }

    public boolean isAllConfigurationExistsInPathUnderConfigurationNode(List<String[]> pathList) {
        return pathList.stream().allMatch(path -> pathValue(path).isPresent());
    }

    public Optional<String> pathValue(String... path) {
        JsonNode currentNode = blueprint;
        for (int i = 0; i < path.length; i++) {
            if (currentNode.isArray()) {
                ArrayNode array = (ArrayNode) currentNode;
                Iterator<JsonNode> it = array.elements();
                boolean found = false;
                while (it.hasNext()) {
                    JsonNode candidate = it.next().path(path[i]);
                    if (!candidate.isMissingNode()) {
                        currentNode = candidate;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    currentNode = MissingNode.getInstance();
                }
            } else {
                currentNode = currentNode.path(path[i]);
            }
        }
        return currentNode.isValueNode() ? Optional.of(currentNode.textValue()) : Optional.empty();
    }

    public Set<String> getAllComponents() {
        return getComponentsByHostGroup().entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream())
                .distinct()
                .collect(Collectors.toSet());
    }

    public Set<String> getComponentsInHostGroup(String hostGroup) {
        Set<String> services = new HashSet<>();
        ArrayNode hostGroupsNode = getArrayFromObjectNodeByPath(blueprint, HOST_GROUPS_NODE);
        Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
        while (hostGroups.hasNext()) {
            JsonNode hostGroupNode = hostGroups.next();
            if (hostGroup.equals(hostGroupNode.path("name").textValue())) {
                Iterator<JsonNode> components = hostGroupNode.path(COMPONENTS_NODE).elements();
                while (components.hasNext()) {
                    services.add(components.next().path("name").textValue());
                }
                break;
            }
        }
        return services;
    }

    public BlueprintTextProcessor extendBlueprintHostGroupConfiguration(HostgroupConfigurations hostGroupConfig, boolean forced) {
        ArrayNode configurations = getArrayFromNodeByNodeName(blueprint, CONFIGURATIONS_NODE);
        ArrayNode hostgroups = getArrayFromObjectNodeByPath(blueprint, HOST_GROUPS_NODE);
        HostgroupConfigurations filteredConfiguraitons = hostGroupConfig.getFilteredConfigs(getExistingParametersFromGlobals(configurations), forced);

        for (HostgroupConfiguration filteredConfig : filteredConfiguraitons) {
            ObjectNode hostgroup = getHostgroup(hostgroups, filteredConfig.getName());
            JsonNode hostgroupConfig = hostgroup.path(CONFIGURATIONS_NODE);
            if (hostgroupConfig.isMissingNode()) {
                ArrayNode hostgroupConfigs = hostgroup.putArray(CONFIGURATIONS_NODE);
                for (SiteConfiguration e : filteredConfig.getSiteConfigs()) {
                    addSiteToHostgroupConfiguration(hostgroupConfigs, e.getName(), e.getProperties());
                }
            } else {
                for (SiteConfiguration e : filteredConfig.getSiteConfigs()) {
                    ArrayNode hostgroupConfigs = (ArrayNode) hostgroupConfig;
                    String siteName = e.getName();
                    ObjectNode site = (ObjectNode) hostgroupConfigs.findValue(siteName);

                    if (site == null) {
                        addSiteToHostgroupConfiguration(hostgroupConfigs, siteName, e.getProperties());
                    } else {
                        ObjectNode objectToModify = (ObjectNode) site.get(PROPERTIES_NODE);
                        if (objectToModify == null) {
                            objectToModify = site;
                        }
                        putAllObjects(objectToModify, e.getProperties(), forced);
                    }
                }
            }
        }
        return this;
    }

    private Set<String> getExistingParametersFromGlobals(ArrayNode configurations) {
        Set<String> result = new HashSet<>();
        if (configurations != null) {
            for (Iterator<JsonNode> i = configurations.elements(); i.hasNext();) {
                ObjectNode siteConfig = (ObjectNode) i.next();
                JsonNode properties = siteConfig.findValue(PROPERTIES_NODE);
                if (properties != null) {
                    siteConfig = (ObjectNode) properties;
                }
                for (Iterator<String> fields = siteConfig.fieldNames(); fields.hasNext();) {
                    String field = fields.next();
                    result.add(field);
                }
            }
        }
        return result;
    }

    private ObjectNode getHostgroup(ArrayNode hostgroups, String name) {
        try (Stream<JsonNode> jns = StreamSupport.stream(hostgroups.spliterator(), false)) {
            return (ObjectNode) jns.filter(jsonNode -> jsonNode.get("name").textValue().equals(name)).distinct().findFirst()
                    .orElseThrow(() -> new BlueprintProcessingException(String.format("There is no such host group as \"%s\"", name)));
        }
    }

    public BlueprintTextProcessor extendBlueprintGlobalConfiguration(SiteConfigurations globalConfig, boolean forced) {
        JsonNode configurations = blueprint.path(CONFIGURATIONS_NODE);
        if (configurations.isMissingNode() && globalConfig != null && !globalConfig.isEmpty()) {
            configurations = blueprint.putArray(CONFIGURATIONS_NODE);
            for (SiteConfiguration site : globalConfig) {
                addSiteToConfiguration((ArrayNode) configurations, site);
            }
        } else {
            if (globalConfig != null) {
                for (SiteConfiguration site : globalConfig) {
                    ObjectNode siteNode = (ObjectNode) configurations.findValue(site.getName());
                    if (siteNode == null) {
                        addSiteToConfiguration((ArrayNode) configurations, site);
                    } else {
                        ObjectNode objectToModify = (ObjectNode) siteNode.get(PROPERTIES_NODE);
                        if (objectToModify == null) {
                            objectToModify = siteNode;
                        }
                        putAllObjects(objectToModify, site.getProperties(), forced);
                    }
                }
            }
        }
        return this;
    }

    public BlueprintTextProcessor extendBlueprintGlobalSettings(SiteSettingsConfigurations globalConfig, boolean forced) {
        JsonNode configurations = blueprint.path(SETTINGS_NODE);
        if (configurations.isMissingNode() && globalConfig != null && !globalConfig.isEmpty()) {
            configurations = blueprint.putArray(SETTINGS_NODE);
            for (List<SiteConfiguration> site : globalConfig) {
                if (site.size() > 0) {
                    addSiteToSettings((ArrayNode) configurations, site.get(0).getName(), site);
                }
            }
        } else {
            if (globalConfig != null) {
                for (List<SiteConfiguration> site : globalConfig) {
                    if (site.size() > 0) {
                        ArrayNode siteNode = (ArrayNode) configurations.findValue(site.get(0).getName());
                        if (siteNode == null) {
                            addSiteToSettings((ArrayNode) configurations, site.get(0).getName(), site);
                        } else {
                            putAllSettingsObjects(siteNode, site, forced);
                        }
                    }
                }
            }
        }
        return this;
    }

    private void putAllSettingsObjects(ArrayNode siteNode, List<SiteConfiguration> siteList, boolean forced) {
        if (forced) {
            putAllSettings(siteNode, siteList);
        } else {
            putAllSettingsIfAbsent(siteNode, siteList);
        }
    }

    private void putAllSettingsIfAbsent(ArrayNode siteNode, List<SiteConfiguration> siteList) {
        for (SiteConfiguration conf :siteList) {
            ObjectNode replace = findBestMatch(siteNode, conf);
            if (replace != null) {
                continue;
            }
            replace = siteNode.addObject();
            putAll(replace, conf.getProperties());
        }
    }

    private void putAllSettings(ArrayNode siteNode, List<SiteConfiguration> siteList) {
        for (SiteConfiguration conf :siteList) {
            ObjectNode replace = findBestMatch(siteNode, conf);
            if (replace == null) {
                replace = siteNode.addObject();
            }
            replace.removeAll();
            putAll(replace, conf.getProperties());
        }
    }

    private ObjectNode findBestMatch(ArrayNode siteNode, SiteConfiguration conf) {
        ObjectNode result = null;
        int maxFound = 0;
        for (Iterator i = siteNode.iterator(); i.hasNext();) {
            int foundNum = 0;
            ObjectNode prop = (ObjectNode) i.next();
            for (String key : conf.getProperties().keySet()) {
                JsonNode found = prop.get(key);
                if (found != null) {
                    foundNum += 1;
                }
                if ("name".equals(key) && !conf.getProperties().get(key).equals(found.asText())) {
                    foundNum = 0;
                    continue;
                }
            }
            if (foundNum > maxFound) {
                maxFound = foundNum;
                result = prop;
            }
        }
        return result;
    }

    private void putAllObjects(ObjectNode objectToModify, Map<String, String> properties, boolean forced) {
        if (forced) {
            putAll(objectToModify, properties);
        } else {
            putAllIfAbsent(objectToModify, properties);
        }
    }

    private void putAll(ObjectNode objectToModify, Map<String, String> properties) {
        for (Map.Entry<String, String> prop : properties.entrySet()) {
            objectToModify.put(prop.getKey(), prop.getValue());
        }
    }

    private void putAllIfAbsent(ObjectNode objectToModify, Map<String, String> properties) {
        for (Map.Entry<String, String> prop : properties.entrySet()) {
            JsonNode exists = objectToModify.get(prop.getKey());
            if (exists == null) {
                objectToModify.put(prop.getKey(), prop.getValue());
            }
        }
    }

    private void addSiteToSettings(ArrayNode settings, String siteName, List<SiteConfiguration> siteList) {
        ObjectNode config = settings.addObject();
        ArrayNode siteNodeArray = config.putArray(siteName);
        for (SiteConfiguration site : siteList) {
            ObjectNode siteNode = siteNodeArray.addObject();
            putAll(siteNode, site.getProperties());
        }
    }

    private void addSiteToConfiguration(ArrayNode configurations, SiteConfiguration site) {
        ObjectNode config = configurations.addObject();
        ObjectNode siteNode = config.putObject(site.getName());
        ObjectNode props = siteNode.putObject(PROPERTIES_NODE);
        putAll(props, site.getProperties());
    }

    private void addSiteToHostgroupConfiguration(ArrayNode configurations, String siteName, Map<String, String> properties) {
        ObjectNode config = configurations.addObject();
        ObjectNode site = config.putObject(siteName);
        putAll(site, properties);
    }

    public Set<String> getHostGroupsWithComponent(String component) {
        Set<String> result = new HashSet<>();
        JsonNode hostGroups = blueprint.path(HOST_GROUPS_NODE);
        for (JsonNode hostGroup : hostGroups) {
            JsonNode components = hostGroup.path(COMPONENTS_NODE);
            for (JsonNode c : components) {
                String name = c.path("name").asText();
                if (name.equalsIgnoreCase(component)) {
                    result.add(hostGroup.path("name").asText());
                }
            }
        }
        return result;
    }

    public Map<String, Set<String>> getComponentsByHostGroup() {
        Map<String, Set<String>> result = new HashMap<>();
        JsonNode hostGroups = blueprint.path(HOST_GROUPS_NODE);
        for (JsonNode hostGroupNode : hostGroups) {
            JsonNode components = hostGroupNode.path(COMPONENTS_NODE);
            Set<String> componentNames = new HashSet<>();
            for (JsonNode componentNode : components) {
                componentNames.add(componentNode.path("name").asText());
            }
            result.put(hostGroupNode.path("name").asText(), componentNames);
        }
        return result;
    }

    public BlueprintTextProcessor addSettingsEntryStringToBlueprint(String config, boolean forced) {
        SiteSettingsConfigurations configs = convertTextToSettings(config);
        return extendBlueprintGlobalSettings(configs, forced);
    }

    public BlueprintTextProcessor addConfigEntryStringToBlueprint(String config, boolean forced) {
        SiteConfigurations configs = convertTextToConfiguration(config);
        return extendBlueprintGlobalConfiguration(configs, forced);
    }

    private SiteSettingsConfigurations convertTextToSettings(String config) {
        try {
            SiteSettingsConfigurations result = SiteSettingsConfigurations.getEmptyConfiguration();
            ObjectNode root = (ObjectNode) JsonUtil.readTree(config);

            for (Iterator<Map.Entry<String, JsonNode>> sites = root.fields(); sites.hasNext();) {
                Map.Entry<String, JsonNode> site = sites.next();
                ArrayNode siteNodeList = (ArrayNode) site.getValue();

                for (JsonNode siteNodeElement : siteNodeList) {
                    Map<String, String> siteprops = new HashMap<>();
                    for (Iterator<Map.Entry<String, JsonNode>> props = siteNodeElement.fields(); props.hasNext();) {
                        Map.Entry<String, JsonNode> prop = props.next();
                        siteprops.put(prop.getKey(), prop.getValue().textValue());
                    }
                    if (!siteprops.isEmpty()) {
                        result.addSiteSettings(site.getKey(), siteprops);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to parse configuration ('" + config + "').", e);
        }
    }

    private SiteConfigurations convertTextToConfiguration(String config) {
        try {
            SiteConfigurations result = SiteConfigurations.getEmptyConfiguration();
            ObjectNode root = (ObjectNode) JsonUtil.readTree(config);

            for (Iterator<Map.Entry<String, JsonNode>> sites = root.fields(); sites.hasNext();) {
                Map.Entry<String, JsonNode> site = sites.next();
                ObjectNode siteNode = (ObjectNode) site.getValue();
                JsonNode properties = siteNode.findValue(PROPERTIES_NODE);
                if (properties != null) {
                    siteNode = (ObjectNode) properties;
                }
                Map<String, String> siteprops = new HashMap<>();
                for (Iterator<Map.Entry<String, JsonNode>> props = siteNode.fields(); props.hasNext();) {
                    Map.Entry<String, JsonNode> prop = props.next();
                    siteprops.put(prop.getKey(), prop.getValue().textValue());
                }
                if (!siteprops.isEmpty()) {
                    result.addSiteConfiguration(site.getKey(), siteprops);
                }
            }
            return result;
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to parse configuration ('" + config + "').", e);
        }
    }

    public boolean componentExistsInBlueprint(String component) {
        boolean componentExists = false;
        ArrayNode hostGroupsNode = getArrayFromObjectNodeByPath(blueprint, HOST_GROUPS_NODE);
        Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
        while (hostGroups.hasNext() && !componentExists) {
            JsonNode hostGroupNode = hostGroups.next();
            componentExists = componentExistsInHostgroup(component, hostGroupNode);
        }
        return componentExists;
    }

    public boolean componentsExistsInBlueprint(Set<String> components) {
        for (String component : components) {
            if (componentExistsInBlueprint(component)) {
                return true;
            }
        }
        return false;
    }

    public BlueprintTextProcessor removeComponentFromBlueprint(String component) {
        ArrayNode hostGroupsNode = getArrayFromObjectNodeByPath(blueprint, HOST_GROUPS_NODE);
        Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
        while (hostGroups.hasNext()) {
            JsonNode hostGroupNode = hostGroups.next();
            Iterator<JsonNode> components = hostGroupNode.path(COMPONENTS_NODE).elements();
            while (components.hasNext()) {
                if (component.equals(components.next().path("name").textValue())) {
                    components.remove();
                }
            }
        }
        return this;
    }

    public String getStackName() {
        ObjectNode blueprintsNode = (ObjectNode) blueprint.path(BLUEPRINTS);
        return blueprintsNode.get(STACK_NAME).asText();
    }

    public String getStackVersion() {
        ObjectNode blueprintsNode = (ObjectNode) blueprint.path(BLUEPRINTS);
        return blueprintsNode.get(STACK_VERSION).asText();
    }

    public BlueprintTextProcessor modifyHdpVersion(String hdpVersion) {
        ObjectNode blueprintsNode = (ObjectNode) blueprint.path(BLUEPRINTS);
        blueprintsNode.remove(STACK_VERSION);
        String[] split = hdpVersion.split("\\.");
        blueprintsNode.put(STACK_VERSION, split[0] + '.' + split[1]);
        return this;
    }

    public BlueprintTextProcessor addComponentToHostgroups(String component, Collection<String> hostGroupNames) {
        ArrayNode hostGroupsNode = getArrayFromObjectNodeByPath(blueprint, HOST_GROUPS_NODE);
        Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
        while (hostGroups.hasNext()) {
            JsonNode hostGroupNode = hostGroups.next();
            String hostGroupName = hostGroupNode.path("name").textValue();
            if (hostGroupNames.contains(hostGroupName) && !componentExistsInHostgroup(component, hostGroupNode)) {
                ArrayNode components = getArrayFromJsonNodeByPath(hostGroupNode, COMPONENTS_NODE);
                components.addPOJO(new BlueprintTextProcessor.ComponentElement(component));
            }
        }
        return this;
    }

    public BlueprintTextProcessor addComponentToHostgroups(String component, Predicate<String> addToHostgroup) {
        ArrayNode hostGroupsNode = getArrayFromObjectNodeByPath(blueprint, HOST_GROUPS_NODE);
        Iterator<JsonNode> hostGroups = hostGroupsNode.elements();
        while (hostGroups.hasNext()) {
            JsonNode hostGroupNode = hostGroups.next();
            String hostGroupName = hostGroupNode.path("name").textValue();
            if (addToHostgroup.test(hostGroupName) && !componentExistsInHostgroup(component, hostGroupNode)) {
                ArrayNode components = getArrayFromJsonNodeByPath(hostGroupNode, COMPONENTS_NODE);
                components.addPOJO(new BlueprintTextProcessor.ComponentElement(component));
            }
        }
        return this;
    }

    public BlueprintTextProcessor setSecurityType(String type) {
        ObjectNode blueprintsNode = (ObjectNode) blueprint.path(BLUEPRINTS);
        ObjectNode security = (ObjectNode) blueprintsNode.get(SECURITY_NODE);
        if (security == null) {
            security = blueprintsNode.putObject(SECURITY_NODE);
        }
        security.put("type", type);
        return this;
    }

    private boolean componentExistsInHostgroup(String component, JsonNode hostGroupNode) {
        boolean componentExists = false;
        Iterator<JsonNode> components = hostGroupNode.path(COMPONENTS_NODE).elements();
        while (components.hasNext()) {
            if (component.equals(components.next().path("name").textValue())) {
                componentExists = true;
                break;
            }
        }
        return componentExists;
    }

    public BlueprintTextProcessor replaceConfiguration(String key, String configuration) {
        try {
            ArrayNode configurations = getArrayFromNodeByNodeName(blueprint, CONFIGURATIONS_NODE);
            Iterator<JsonNode> elements = configurations.elements();
            while (elements.hasNext()) {
                if (elements.next().get(key) != null) {
                    elements.remove();
                    break;
                }
            }
            configurations.add(JsonUtil.readTree(configuration));
            return this;
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to parse blueprint configuration text.", e);
        }
    }

    private ArrayNode getArrayFromJsonNodeByPath(JsonNode node, String path) {
        return getArrayFromObjectNodeByPath((ObjectNode) node, path);
    }

    private ArrayNode getArrayFromObjectNodeByPath(ObjectNode node, String path) {
        JsonNode jsonNode = node.path(path);
        if (jsonNode.isMissingNode()) {
            throw new BlueprintProcessingException(String.join("Unable to locate component: %s", path));
        } else {
            return (ArrayNode) jsonNode;
        }
    }

    private ArrayNode getArrayFromNodeByNodeName(ObjectNode node, String path) {
        JsonNode jsonNode = node.get(path);
        if (jsonNode.isMissingNode()) {
            throw new BlueprintProcessingException(String.join("Unable to locate component: %s", path));
        } else {
            return (ArrayNode) jsonNode;
        }
    }

    public ObjectNode getBlueprint() {
        return blueprint;
    }

    private static class ComponentElement {

        private String name;

        private ComponentElement(String component) {
            name = component;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
