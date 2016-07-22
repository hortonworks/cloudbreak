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
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
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
    private Map<String, Collection<String>> volumeTypes;
    private Map<String, List<Map<String, String>>> instanceTypes;
    private Map<String, Collection<String>> orchestrators;
    private Focus focus;
    private Hints hint;
    private Map<PropertyKey, String> properties = new HashMap<>();
    private Map<String, InstanceGroupEntry> instanceGroups = new HashMap<>();
    private Map<String, HostgroupEntry> hostGroups = new HashMap<>();
    private Set<String> activeHostGroups = new HashSet<>();
    private Set<String> activeInstanceGroups = new HashSet<>();
    private Set<String> activeTemplates = new HashSet<>();
    private Set<String> activeTemplateNames = new HashSet<>();
    private String activeCloudPlatform;
    private Map<Long, String> networksByProvider = new HashMap<>();
    private Map<Long, String> securityGroups = new HashMap<>();
    private Map<Long, String> rdsConfigs = new HashMap<>();
    private Long activeNetworkId;
    private Long activeSecurityGroupId;
    private FileSystemType fileSystemType;
    private Map<String, Object> fileSystemParameters = new HashMap<>();
    private Boolean defaultFileSystem;
    private Long selectedMarathonStackId;
    private String selectedMarathonStackName;
    private Set<String> constraintTemplates = new HashSet<>();
    private Map<String, MarathonHostgroupEntry> marathonHostgroups = new HashMap<>();

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
        this.focus = getRootFocus();
        this.hint = Hints.NONE;
        this.instanceGroups = new HashMap<>();
        this.hostGroups = new HashMap<>();
        this.activeHostGroups = new HashSet<>();
        this.activeInstanceGroups = new HashSet<>();
        constraintTemplates = new HashSet<>();
        marathonHostgroups = new HashMap<>();
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
        this.fileSystemParameters = new HashMap<>();
        this.defaultFileSystem = null;
        this.defaultFileSystem = null;
    }

    public String getActiveCloudPlatform() {
        return this.activeCloudPlatform == null ? "" : this.activeCloudPlatform;
    }

    public boolean isAzureActiveCredential() {
        return AzureCommands.PLATFORM.equals(getActiveCloudPlatform());
    }

    public void removeStack(String id) {
        removeProperty(PropertyKey.STACK_ID, id);
    }

    public Map<String, InstanceGroupEntry> getInstanceGroups() {
        return this.instanceGroups;
    }

    public Map<String, HostgroupEntry> getHostGroups() {
        return hostGroups;
    }

    public Map<String, InstanceGroupEntry> putInstanceGroup(String name, InstanceGroupEntry value) {
        this.instanceGroups.put(name, value);
        return this.instanceGroups;
    }

    public Map<String, HostgroupEntry> putHostGroup(String name, HostgroupEntry hostgroupEntry) {
        this.hostGroups.put(name, hostgroupEntry);
        return this.hostGroups;
    }

    public Set<String> getActiveTemplates() {
        return activeTemplates;
    }

    public Set<String> getActiveTemplateNames() {
        return activeTemplateNames;
    }

    public boolean isBlueprintAvailable() {
        return isPropertyAvailable(PropertyKey.BLUEPRINT_ID);
    }

    public void addBlueprint(String id) throws Exception {
        this.instanceGroups = new HashMap<>();
        this.hostGroups = new HashMap<>();
        this.activeInstanceGroups = new HashSet<>();
        this.activeHostGroups = new HashSet<>();
        String blueprintText = getBlueprintText(id);
        JsonNode hostGroups = objectMapper.readTree(blueprintText.getBytes()).get("host_groups");
        for (JsonNode hostGroup : hostGroups) {
            activeHostGroups.add(hostGroup.get("name").asText());
            activeInstanceGroups.add(hostGroup.get("name").asText());
        }
        addProperty(PropertyKey.BLUEPRINT_ID, id);
        setBlueprintAccessible();
    }

    public String getBlueprintText() {
        return getBlueprintText(getBlueprintId());
    }

    public void prepareInstanceGroups(StackResponse stack) {
        this.instanceGroups = new HashMap<>();
        this.activeInstanceGroups = new HashSet<>();
        for (InstanceGroupJson instanceGroup : stack.getInstanceGroups()) {
            this.activeInstanceGroups.add(instanceGroup.getGroup());
            instanceGroups.put(
                    instanceGroup.getGroup(),
                    new InstanceGroupEntry(
                            Long.valueOf(instanceGroup.getTemplateId()),
                            Integer.valueOf(instanceGroup.getNodeCount()),
                            instanceGroup.getType().name()
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
        this.marathonHostgroups = new HashMap<>();
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

    public Map<String, MarathonHostgroupEntry> putMarathonHostGroup(String name, MarathonHostgroupEntry hostgroupEntry) {
        this.marathonHostgroups.put(name, hostgroupEntry);
        return this.marathonHostgroups;
    }

    public Map<String, MarathonHostgroupEntry> getMarathonHostGroups() {
        return marathonHostgroups;
    }

    public boolean isCredentialAvailable() {
        return isPropertyAvailable(PropertyKey.CREDENTIAL_ID);
    }

    public void setCredential(String id) throws Exception {
        CredentialResponse credential = cloudbreakClient.credentialEndpoint().get(Long.valueOf(id));
        this.activeCloudPlatform = credential.getCloudPlatform();
        List<TemplateResponse> templateResponses = new ArrayList<>();
        for (TemplateResponse templateResponse : cloudbreakClient.templateEndpoint().getPublics()) {
            if (this.activeCloudPlatform.equals(templateResponse.getCloudPlatform())) {
                templateResponses.add(templateResponse);
            }
        }
        fillTemplates(templateResponses);
        addProperty(PropertyKey.CREDENTIAL_ID, id);
        setCredentialAccessible();
    }

    private void fillTemplates(List<TemplateResponse> templateList) {
        for (TemplateResponse t : templateList) {
            this.activeTemplateNames.add(t.getName());
            this.activeTemplates.add(t.getId().toString());
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

    public Collection<String> getInstanceTypeNamesByPlatform(String platform) {
        Collection<String> result = Lists.newArrayList();
        Collection<Map<String, String>> platformInstances = instanceTypes.get(platform);
        for (Map<String, String> instance : platformInstances) {
            result.add(instance.get("value"));
        }
        return result;
    }

    public Collection<String> getOrchestratorNamesByPlatform(String platform) {
        Collection<String> result = Lists.newArrayList();
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

    public boolean isSssdConfigAccessible() {
        return isPropertyAvailable(PropertyKey.SSSDCONFIG_ACCESSIBLE);
    }

    public void setSssdConfigAccessible() {
        addProperty(PropertyKey.SSSDCONFIG_ACCESSIBLE, ACCESSIBLE);
    }

    public void addSssdConfig(String id) throws Exception {
        addProperty(PropertyKey.SSSDCONFIG_ID, id);
        setSssdConfigAccessible();
    }

    public void addRdsConfig(String rdsConfigId) {
        addProperty(PropertyKey.RDSCONFIG_ID, rdsConfigId);
        setSssdConfigAccessible();
    }

    public void setRdsConfigAccessible() {
        addProperty(PropertyKey.RDSCONFIG_ACCESSIBLE, ACCESSIBLE);
    }

    public String getSssdConfigId() {
        return getLastPropertyValue(PropertyKey.SSSDCONFIG_ID);
    }

    public String getRdsConfigId() {
        return getLastPropertyValue(PropertyKey.RDSCONFIG_ID);
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

    public void putSecurityGroup(Long id, String name) {
        this.securityGroups.put(id, name);
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

    public Long getActiveSecurityGroupId() {
        return activeSecurityGroupId;
    }

    public void setActiveSecurityGroupId(Long id) {
        this.activeSecurityGroupId = id;
    }

    /**
     * Sets the focus to the root.
     */
    public void resetFocus() {
        this.focus = getRootFocus();
    }

    /**
     * Sets the focus.
     *
     * @param id   target of the focus
     * @param type type of the focus
     */
    public void setFocus(String id, FocusType type) {
        this.focus = new Focus(id, type);
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
        return prefix + (postfix == null ? "" : ":" + postfix) + ">";
    }

    private boolean isPropertyAvailable(PropertyKey key) {
        return properties.get(key) != null && !properties.get(key).isEmpty();
    }

    public boolean isMarathonMode() {
        return getFocusType().equals(FocusType.MARATHON);
    }

    private void addProperty(PropertyKey key, String value) {
        properties.remove(key);
        properties.put(key, value);
    }

    private void removeProperty(PropertyKey key, String value) {
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
        SSSDCONFIG_ACCESSIBLE,
        SSSDCONFIG_ID,
        RDSCONFIG_ACCESSIBLE,
        RDSCONFIG_ID,
        RDSCONFIG_NAME
    }
}