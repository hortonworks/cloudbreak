package com.sequenceiq.cloudbreak.shell.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.SmartSenseSubscriptionJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.commands.provider.AzureCommands;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.OutputTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class ShellContext {

    private static final String ACCESSIBLE = "accessible";

    private Map<String, Collection<String>> platformToVariants;

    private Map<String, Collection<String>> regions;

    private Map<String, Map<String, Collection<String>>> availabilityZones;

    private Map<String, Map<String, Collection<VmTypeJson>>> vmTypesPerZones = new HashMap<>();

    private Map<String, Map<String, String>> defaultVmTypePerZones = new HashMap<>();

    private Map<String, Collection<String>> volumeTypes;

    private Map<String, List<Map<String, String>>> instanceTypes;

    private Map<String, Collection<String>> orchestrators;

    private Focus focus;

    private Hints hint;

    private String byosOrchestrator;

    private String byosEndpoint;

    private final Map<PropertyKey, String> properties = new HashMap<>();

    private Map<String, InstanceGroupEntry> instanceGroups;

    private Map<String, HostgroupEntry> hostGroups;

    private Set<String> activeHostGroups;

    private Set<String> activeInstanceGroups;

    private final Set<String> activeTemplates = new HashSet<>();

    private final Set<String> activeTemplateNames = new HashSet<>();

    private final Map<Long, TemplateResponse> templateMap = new HashMap<>();

    private String activeCloudPlatform;

    private final Map<Long, String> networksByProvider = new HashMap<>();

    private Map<Long, String> securityGroups = new HashMap<>();

    private final Map<Long, String> rdsConfigs = new HashMap<>();

    private final Map<String, AvailabilitySetEntry> azureAvailabilitySets = new HashMap<>();

    private Long activeNetworkId;

    private FileSystemType fileSystemType;

    private Map<String, Object> fileSystemParameters = new HashMap<>();

    private final Map<Long, String> ldapConfigs = new HashMap<>();

    private Boolean defaultFileSystem;

    private Long selectedMarathonStackId;

    private String selectedMarathonStackName;

    private Set<String> constraintTemplates;

    private Set<String> enabledPlatforms = new HashSet<>();

    private Map<String, MarathonHostgroupEntry> marathonHostgroups;

    private Long selectedYarnStackId;

    private String selectedYarnStackName;

    private Map<String, YarnHostgroupEntry> yarnHostgroups;

    private AmbariDatabaseDetailsJson ambariDatabaseDetailsJson;

    private SmartSenseSubscriptionJson smartSenseSubscription;

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Inject
    private ResponseTransformer responseTransformer;

    @Inject
    private ExceptionTransformer exceptionTransformer;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private OutputTransformer outputTransformer;

    public ShellContext() {
        focus = getRootFocus();
        hint = Hints.NONE;
        instanceGroups = new HashMap<>();
        hostGroups = new HashMap<>();
        activeHostGroups = new HashSet<>();
        activeInstanceGroups = new HashSet<>();
        constraintTemplates = new HashSet<>();
        marathonHostgroups = new HashMap<>();
        yarnHostgroups = new HashMap<>();
    }

    public ResponseTransformer responseTransformer() {
        return responseTransformer;
    }

    public ExceptionTransformer exceptionTransformer() {
        return exceptionTransformer;
    }

    public CloudbreakClient cloudbreakClient() {
        return cloudbreakClient;
    }

    public OutputTransformer outputTransformer() {
        return outputTransformer;
    }

    public ObjectMapper objectMapper() {
        return objectMapper;
    }

    public boolean isStackAvailable() {
        return isPropertyAvailable(PropertyKey.STACK_ID);
    }

    public void addStack(String id, String name) {
        addProperty(PropertyKey.STACK_ID, id);
        addProperty(PropertyKey.STACK_NAME, name);
        setStackAccessible();
    }

    public void resetFileSystemConfiguration() {
        fileSystemParameters = new HashMap<>();
        defaultFileSystem = null;
        defaultFileSystem = null;
    }

    public String getActiveCloudPlatform() {
        return activeCloudPlatform == null ? "" : activeCloudPlatform;
    }

    public boolean isAzureActiveCredential() {
        return AzureCommands.PLATFORM.equals(getActiveCloudPlatform());
    }

    public void removeStack() {
        removeProperty(PropertyKey.STACK_ID);
    }

    public Map<String, InstanceGroupEntry> getInstanceGroups() {
        return instanceGroups;
    }

    public Map<String, HostgroupEntry> getHostGroups() {
        return hostGroups;
    }

    public Map<String, InstanceGroupEntry> putInstanceGroup(String name, InstanceGroupEntry value) {
        instanceGroups.put(name, value);
        return instanceGroups;
    }

    public Map<String, HostgroupEntry> putHostGroup(String name, HostgroupEntry hostgroupEntry) {
        hostGroups.put(name, hostgroupEntry);
        return hostGroups;
    }

    public Set<String> getActiveTemplates() {
        return activeTemplates;
    }

    public Set<String> getActiveTemplateNames() {
        return activeTemplateNames;
    }

    public Map<Long, TemplateResponse> getTemplateMap() {
        return templateMap;
    }

    public boolean isBlueprintAvailable() {
        return isPropertyAvailable(PropertyKey.BLUEPRINT_ID);
    }

    public void addBlueprint(String id) throws Exception {
        if (getStackId() == null) {
            instanceGroups = new HashMap<>();
            activeInstanceGroups = new HashSet<>();
        }
        hostGroups = new HashMap<>();
        activeHostGroups = new HashSet<>();
        String blueprintText = getBlueprintText(id);
        JsonNode hostGroups = objectMapper.readTree(blueprintText.getBytes()).get("host_groups");
        for (JsonNode hostGroup : hostGroups) {
            activeHostGroups.add(hostGroup.get("name").asText());
            if (getStackId() == null) {
                activeInstanceGroups.add(hostGroup.get("name").asText());
            }
        }
        addProperty(PropertyKey.BLUEPRINT_ID, id);
        setBlueprintAccessible();
    }

    public String getBlueprintText() {
        return getBlueprintText(getBlueprintId());
    }

    public void prepareInstanceGroups(StackResponse stack) {
        instanceGroups = new HashMap<>();
        activeInstanceGroups = new HashSet<>();
        for (InstanceGroupResponse instanceGroup : stack.getInstanceGroups()) {
            activeInstanceGroups.add(instanceGroup.getGroup());
            instanceGroups.put(
                    instanceGroup.getGroup(),
                    new InstanceGroupEntry(
                            instanceGroup.getTemplateId(),
                            instanceGroup.getSecurityGroupId(),
                            instanceGroup.getNodeCount(),
                            instanceGroup.getType().name(),
                            instanceGroup.getParameters()
                    )
            );
        }
    }

    public Long getSelectedMarathonStackId() {
        return selectedMarathonStackId;
    }

    public String getSelectedMarathonStackName() {
        return selectedMarathonStackName;
    }

    public void setSelectedMarathonStackName(String selectedMarathonStackName) {
        this.selectedMarathonStackName = selectedMarathonStackName;
    }

    public boolean isSelectedMarathonStackAvailable() {
        return selectedMarathonStackId != null;
    }

    public void resetSelectedMarathonStackId() {
        selectedMarathonStackId = null;
    }

    public void setSelectedMarathonStackId(Long selectedMarathonStackId) {
        this.selectedMarathonStackId = selectedMarathonStackId;
    }

    public void resetMarathonHostGroups() {
        marathonHostgroups = new HashMap<>();
    }

    public Long getSelectedYarnStackId() {
        return selectedYarnStackId;
    }

    public String getSelectedYarnStackName() {
        return selectedYarnStackName;
    }

    public void setSelectedYarnStackName(String selectedYarnStackName) {
        this.selectedYarnStackName = selectedYarnStackName;
    }

    public boolean isSelectedYarnStackAvailable() {
        return selectedYarnStackId != null;
    }

    public void resetSelectedYarnStackId() {
        selectedYarnStackId = null;
    }

    public void setSelectedYarnStackId(Long selectedYarnStackId) {
        this.selectedYarnStackId = selectedYarnStackId;
    }

    public void resetYarnHostGroups() {
        yarnHostgroups = new HashMap<>();
    }

    public Set<String> getConstraints() {
        return constraintTemplates;
    }

    public void setConstraints(Set<ConstraintTemplateResponse> constraintTemplateResponses) {
        constraintTemplates = new HashSet<>();
        for (ConstraintTemplateResponse constraintTemplateResponse : constraintTemplateResponses) {
            constraintTemplates.add(constraintTemplateResponse.getName());
        }
    }

    public Boolean isPlatformSelectionDisabled() {
        return cloudbreakClient.accountPreferencesEndpoint().isPlatformSelectionDisabled().get("disabled");
    }

    public Set<String> getEnabledPlatforms() {
        return enabledPlatforms;
    }

    public boolean isPlatformAvailable(String platform) {
        return enabledPlatforms == null || enabledPlatforms.contains(platform);
    }

    public void setEnabledPlatforms(Set<String> enabledPlatforms) {
        this.enabledPlatforms = enabledPlatforms;
    }

    public Map<String, MarathonHostgroupEntry> putMarathonHostGroup(String name, MarathonHostgroupEntry hostgroupEntry) {
        marathonHostgroups.put(name, hostgroupEntry);
        return marathonHostgroups;
    }

    public Map<String, MarathonHostgroupEntry> getMarathonHostGroups() {
        return marathonHostgroups;
    }

    public Map<String, AvailabilitySetEntry> putAzureAvailabilitySet(String name, AvailabilitySetEntry azureAvailabilitySetEntry) {
        azureAvailabilitySets.put(name, azureAvailabilitySetEntry);
        return azureAvailabilitySets;
    }

    public Map<String, AvailabilitySetEntry> getAzureAvailabilitySets() {
        return azureAvailabilitySets;
    }

    public Map<String, YarnHostgroupEntry> putYarnHostGroup(String name, YarnHostgroupEntry hostgroupEntry) {
        yarnHostgroups.put(name, hostgroupEntry);
        return yarnHostgroups;
    }

    public Map<String, YarnHostgroupEntry> getYarnHostGroups() {
        return yarnHostgroups;
    }

    public boolean isCredentialAvailable() {
        return isPropertyAvailable(PropertyKey.CREDENTIAL_ID);
    }

    public void setCredential(String id) {
        CredentialResponse credential = cloudbreakClient.credentialEndpoint().get(Long.valueOf(id));
        activeCloudPlatform = credential.getCloudPlatform();
        List<TemplateResponse> templateResponses = new ArrayList<>();
        for (TemplateResponse templateResponse : cloudbreakClient.templateEndpoint().getPublics()) {
            if (activeCloudPlatform.equals(templateResponse.getCloudPlatform())) {
                templateResponses.add(templateResponse);
            }
        }
        fillTemplates(templateResponses);
        addProperty(PropertyKey.CREDENTIAL_ID, id);
        if ("BYOS".equals(credential.getCloudPlatform())) {
            byosOrchestrator = credential.getParameters().get("type").toString();
            byosEndpoint = credential.getParameters().get("apiEndpoint").toString();
        } else {
            byosOrchestrator = null;
            byosEndpoint = null;
        }
        setCredentialAccessible();
    }

    public String getApiEndpoint() {
        return byosEndpoint;
    }

    public CredentialResponse getCredentialById(String id) {
        return cloudbreakClient.credentialEndpoint().get(Long.valueOf(id));
    }

    public void putTemplate(TemplateResponse t) {
        if (activeCloudPlatform != null && activeCloudPlatform.equals(t.getCloudPlatform())) {
            templateMap.put(t.getId(), t);
            activeTemplateNames.add(t.getName());
            activeTemplates.add(t.getId().toString());
        }
    }

    private void fillTemplates(List<TemplateResponse> templateList) {
        for (TemplateResponse t : templateList) {
            templateMap.put(t.getId(), t);
            activeTemplateNames.add(t.getName());
            activeTemplates.add(t.getId().toString());
        }
    }

    public FileSystemType getFileSystemType() {
        return fileSystemType;
    }

    public void setFileSystemType(FileSystemType fileSystemType) {
        this.fileSystemType = fileSystemType;
    }

    public Map<String, Object> getFileSystemParameters() {
        return fileSystemParameters;
    }

    public void setFileSystemParameters(Map<String, Object> fileSystemParameters) {
        this.fileSystemParameters = fileSystemParameters;
    }

    public Boolean getDefaultFileSystem() {
        return defaultFileSystem;
    }

    public void setDefaultFileSystem(Boolean defaultFileSystem) {
        this.defaultFileSystem = defaultFileSystem;
    }

    public void setPlatformToVariantsMap(Map<String, Collection<String>> platformToVariants) {
        this.platformToVariants = platformToVariants;
    }

    public Collection<String> getVariantsByPlatform(String platform) {
        return platformToVariants.get(platform);
    }

    public void setRegions(Map<String, Collection<String>> regions) {
        this.regions = regions;
    }

    public Collection<String> getRegionsByPlatform(String platform) {
        return regions.get(platform);
    }

    public void setAvailabilityZones(Map<String, Map<String, Collection<String>>> availabilityZones) {
        this.availabilityZones = availabilityZones;
    }

    public Collection<String> getAvailabilityZonesByPlatform(String platform) {
        Collection<String> result = Lists.newArrayList();
        Map<String, Collection<String>> regionZones = availabilityZones.get(platform);
        for (Collection<String> zones : regionZones.values()) {
            result.addAll(zones);
        }
        return result;
    }

    public Collection<String> getAvailabilityZonesByRegion(String platform, String region) {
        return availabilityZones.get(platform).get(region);
    }

    public Map<String, Map<String, Collection<VmTypeJson>>> getVmTypesPerZones() {
        return vmTypesPerZones;
    }

    public void setVmTypesPerZones(Map<String, Map<String, Collection<VmTypeJson>>> vmTypesPerZones) {
        this.vmTypesPerZones = vmTypesPerZones;
    }

    public Map<String, Map<String, String>> getDefaultVmTypePerZones() {
        return defaultVmTypePerZones;
    }

    public void setDefaultVmTypePerZones(Map<String, Map<String, String>> defaultVmTypePerZones) {
        this.defaultVmTypePerZones = defaultVmTypePerZones;
    }

    public Collection<String> getInstanceTypeNamesByPlatform(String platform) {
        Collection<String> result = Lists.newArrayList();
        Collection<Map<String, String>> platformInstances = instanceTypes.get(platform);
        for (Map<String, String> instance : platformInstances) {
            result.add(instance.get("value"));
        }
        return result;
    }

    public Collection<String> getOrchestratorNamesByPlatform(String platform) {
        return orchestrators.get(platform);
    }

    public Set<String> getActiveHostGroups() {
        return activeHostGroups;
    }

    public Set<String> getActiveInstanceGroups() {
        return activeInstanceGroups;
    }

    public void setBlueprintAccessible() {
        addProperty(PropertyKey.BLUEPRINT_ACCESSIBLE, ACCESSIBLE);
    }

    public boolean isBlueprintAccessible() {
        return isPropertyAvailable(PropertyKey.BLUEPRINT_ACCESSIBLE);
    }

    public boolean isRdsConfigAccessible() {
        return isPropertyAvailable(PropertyKey.RDSCONFIG_ACCESSIBLE);
    }

    public void setCredentialAccessible() {
        addProperty(PropertyKey.CREDENTIAL_ACCESSIBLE, ACCESSIBLE);
    }

    public boolean isCredentialAccessible() {
        return isPropertyAvailable(PropertyKey.CREDENTIAL_ACCESSIBLE);
    }

    public void setStackAccessible() {
        addProperty(PropertyKey.STACK_ACCESSIBLE, ACCESSIBLE);
    }

    public boolean isStackAccessible() {
        return isPropertyAvailable(PropertyKey.STACK_ACCESSIBLE);
    }

    public void setRecipeAccessible() {
        addProperty(PropertyKey.RECIPE_ACCESSIBLE, ACCESSIBLE);
    }

    public boolean isRecipeAccessible() {
        return isPropertyAvailable(PropertyKey.RECIPE_ACCESSIBLE);
    }

    public String getStackId() {
        return getLastPropertyValue(PropertyKey.STACK_ID);
    }

    public String getStackName() {
        return getLastPropertyValue(PropertyKey.STACK_NAME);
    }

    public String getBlueprintId() {
        return getLastPropertyValue(PropertyKey.BLUEPRINT_ID);
    }

    public void removeBlueprintId() {
        removeProperty(PropertyKey.BLUEPRINT_ID);
    }

    public String getRecipeId() {
        return getLastPropertyValue(PropertyKey.RECIPE_ID);
    }

    public String getCredentialId() {
        return getLastPropertyValue(PropertyKey.CREDENTIAL_ID);
    }

    public Map<Long, String> getNetworksByProvider() {
        return networksByProvider;
    }

    public Map<Long, String> getRdsConfigs() {
        return rdsConfigs;
    }

    public void addRdsConfig(String rdsConfigId) {
        addProperty(PropertyKey.RDSCONFIG_ID, rdsConfigId);
    }

    public void setRdsConfigAccessible() {
        addProperty(PropertyKey.RDSCONFIG_ACCESSIBLE, ACCESSIBLE);
    }

    public String getRdsConfigId() {
        return getLastPropertyValue(PropertyKey.RDSCONFIG_ID);
    }

    public boolean isLdapConfigAccessible() {
        return isPropertyAvailable(PropertyKey.LDAPCONFIG_ACCESSIBLE);
    }

    public void setLdapConfigAccessible() {
        addProperty(PropertyKey.LDAPCONFIG_ACCESSIBLE, ACCESSIBLE);
    }

    public void addLdapConfig(String id) {
        addProperty(PropertyKey.LDAPCONFIG_ID, id);
    }

    public String getLdapConfigId() {
        return getLastPropertyValue(PropertyKey.LDAPCONFIG_ID);
    }

    public Map<Long, String> getLdapConfigs() {
        return ldapConfigs;
    }

    public void putNetwork(Long id, String provider) {
        networksByProvider.put(id, provider);
    }

    public void putNetworks(Map<Long, String> networksByProvider) {
        this.networksByProvider.putAll(networksByProvider);
    }

    public void putRdsConfig(Long id, String name) {
        rdsConfigs.put(id, name);
    }

    public void putLdapConfig(Long id, String name) {
        ldapConfigs.put(id, name);
    }

    public void putSecurityGroup(Long id, String name) {
        securityGroups.put(id, name);
    }

    public Map<Long, String> getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(Map<Long, String> securityGroups) {
        this.securityGroups = securityGroups;
    }

    public Long getActiveNetworkId() {
        return activeNetworkId;
    }

    public void setActiveNetworkId(Long activeNetworkId) {
        this.activeNetworkId = activeNetworkId;
    }

    /**
     * Sets the focus to the root.
     */
    public void resetFocus() {
        focus = getRootFocus();
    }

    /**
     * Sets the focus.
     *
     * @param id   target of the focus
     * @param type type of the focus
     */
    public void setFocus(String id, FocusType type) {
        focus = new Focus(id, type);
    }

    public FocusType getFocusType() {
        return focus.getType();
    }

    /**
     * Sets what should be the next hint message.
     *
     * @param hint the new message
     */
    public void setHint(Hints hint) {
        this.hint = hint;
    }

    /**
     * Returns the context sensitive prompt.
     *
     * @return text of the prompt
     */
    public String getPrompt() {
        return focus.isType(FocusType.ROOT) ? "cloudbreak-shell>" : formatPrompt(focus.getPrefix(), focus.getValue());
    }

    /**
     * Returns some context sensitive hint.
     *
     * @return hint
     */
    public String getHint() {
        return "Hint: " + hint.message();
    }

    private Focus getRootFocus() {
        return new Focus("root", FocusType.ROOT);
    }

    private String formatPrompt(String prefix, String postfix) {
        return prefix + (postfix == null ? "" : ':' + postfix) + '>';
    }

    private boolean isPropertyAvailable(PropertyKey key) {
        return properties.get(key) != null && !properties.get(key).isEmpty();
    }

    public boolean isMarathonMode() {
        return "MESOS".equals(byosOrchestrator);
    }

    public boolean isYarnMode() {
        return "YARN".equals(byosOrchestrator);
    }

    private void addProperty(PropertyKey key, String value) {
        properties.remove(key);
        properties.put(key, value);
    }

    private void removeProperty(PropertyKey key) {
        properties.remove(key);
    }

    private String getLastPropertyValue(PropertyKey key) {
        try {
            return properties.get(key);
        } catch (Exception ex) {
            return "";
        }
    }

    private String getBlueprintText(String id) {
        BlueprintResponse bp = cloudbreakClient.blueprintEndpoint().get(Long.valueOf(id));
        return bp.getAmbariBlueprint();
    }

    public void setVolumeTypes(Map<String, Collection<String>> volumeTypes) {
        this.volumeTypes = volumeTypes;
    }

    public Collection<String> getVolumeTypesByPlatform(String platform) {
        return volumeTypes.get(platform);
    }

    public void setInstanceTypes(Map<String, List<Map<String, String>>> instanceTypes) {
        this.instanceTypes = instanceTypes;
    }

    public void setOrchestrators(Map<String, Collection<String>> orchestrators) {
        this.orchestrators = orchestrators;
    }

    public void setAmbariDatabaseDetailsJson(AmbariDatabaseDetailsJson details) {
        ambariDatabaseDetailsJson = details;
    }

    public AmbariDatabaseDetailsJson getAmbariDatabaseDetailsJson() {
        return ambariDatabaseDetailsJson;
    }

    public void resetAmbariDatabaseDetailsJson() {
        ambariDatabaseDetailsJson = null;
    }

    public SmartSenseSubscriptionJson getSmartSenseSubscription() {
        if (smartSenseSubscription == null) {
            try {
                smartSenseSubscription = cloudbreakClient().smartSenseSubscriptionEndpoint().get();
            } catch (Exception ex) {
                smartSenseSubscription = new SmartSenseSubscriptionJson();
                smartSenseSubscription.setId(Long.MIN_VALUE);
            }
        }
        return smartSenseSubscription.getId().equals(Long.MIN_VALUE) ? null : smartSenseSubscription;
    }

    public void resetSmartSenseSubscription() {
        smartSenseSubscription = null;
    }

    private enum PropertyKey {
        CREDENTIAL_ID,
        BLUEPRINT_ID,
        RECIPE_ID,
        STACK_ID,
        STACK_NAME,
        CREDENTIAL_ACCESSIBLE,
        BLUEPRINT_ACCESSIBLE,
        STACK_ACCESSIBLE,
        RECIPE_ACCESSIBLE,
        RDSCONFIG_ACCESSIBLE,
        RDSCONFIG_ID,
        RDSCONFIG_NAME,
        LDAPCONFIG_ID,
        LDAPCONFIG_ACCESSIBLE
    }
}