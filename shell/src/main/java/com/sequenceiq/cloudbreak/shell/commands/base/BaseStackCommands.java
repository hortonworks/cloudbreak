package com.sequenceiq.cloudbreak.shell.commands.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;
import com.sequenceiq.cloudbreak.shell.exception.ValidationException;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.util.CloudbreakShellUtil;

public class BaseStackCommands implements BaseCommands, StackCommands {

    private ShellContext shellContext;
    private CloudbreakShellUtil cloudbreakShellUtil;

    public BaseStackCommands(ShellContext shellContext, CloudbreakShellUtil cloudbreakShellUtil) {
        this.shellContext = shellContext;
        this.cloudbreakShellUtil = cloudbreakShellUtil;
    }

    @CliAvailabilityIndicator(value = "stack list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "stack list", help = "Shows all of your stack")
    public String list() {
        try {
            Set<StackResponse> publics = shellContext.cloudbreakClient().stackEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator({ "stack show --id", "stack show --name" })
    @Override
    public boolean showAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "stack show --id", help = "Show the stack by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "stack show --name", help = "Show the stack by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @Override
    public String show(Long id, String name) {
        try {
            StackResponse stackResponse = getStackResponse(name, id);
            if (stackResponse != null) {
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(stackResponse), "FIELD", "VALUE");
            }
            return "No stack specified (select a stack by --id or --name).";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "stack delete --id", "stack delete --name" })
    @Override
    public boolean deleteAvailable() {
        return !shellContext.isMarathonMode();
    }

    @Override
    public String deleteById(Long id) throws Exception {
        return delete(id, null);
    }

    @Override
    public String deleteByName(String name) throws Exception {
        return delete(null, name);
    }

    @CliCommand(value = "stack delete --id", help = "Delete the stack by its id")
    public String deleteByName(
            @CliOption(key = "", mandatory = true) Long id,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack termination", specifiedDefaultValue = "false") Boolean wait) throws Exception {
        return delete(id, null, wait);
    }

    @CliCommand(value = "stack delete --name", help = "Delete the stack by its name")
    public String deleteById(
            @CliOption(key = "", mandatory = true) String name,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack termination", specifiedDefaultValue = "false") Boolean wait) throws Exception {
        return delete(null, name, wait);
    }

    @Override
    public String delete(Long id, String name) throws Exception {
        return delete(id, name, false);
    }

    public String delete(Long id, String name, Boolean wait) {
        try {
            wait = wait == null ? false : wait;
            if (id != null) {
                shellContext.cloudbreakClient().stackEndpoint().delete(Long.valueOf(id), false);
                shellContext.setHint(Hints.CREATE_CLUSTER);
                shellContext.removeStack(id.toString());
                if (wait) {
                    CloudbreakShellUtil.WaitResult waitResult = cloudbreakShellUtil.waitAndCheckStackStatus(Long.valueOf(id), Status.DELETE_COMPLETED.name());
                    if (CloudbreakShellUtil.WaitResult.FAILED.equals(waitResult)) {
                        throw shellContext.exceptionTransformer().transformToRuntimeException("Stack terminated failed on stack with id: " + id);
                    } else {
                        return "Stack terminated with id: " + id;
                    }
                } else {
                    return "Stack termination started with id: " + id;
                }
            } else if (name != null) {
                StackResponse response = shellContext.cloudbreakClient().stackEndpoint().getPublic(name);
                shellContext.cloudbreakClient().stackEndpoint().deletePublic(name, false);
                shellContext.setHint(Hints.CREATE_CLUSTER);

                if (wait) {
                    CloudbreakShellUtil.WaitResult waitResult = cloudbreakShellUtil.waitAndCheckStackStatus(response.getId(), Status.DELETE_COMPLETED.name());
                    if (CloudbreakShellUtil.WaitResult.FAILED.equals(waitResult)) {
                        throw shellContext.exceptionTransformer()
                                .transformToRuntimeException("Stack terminated failed on stack with id: " + response.getId());
                    } else {
                        return "Stack terminated with name: " + name;
                    }
                } else {
                    return "Stack termination started with name: " + name;
                }
            }
            return "Stack not specified. (select by using --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "stack select --id", "stack select --name" })
    @Override
    public boolean selectAvailable() {
        return shellContext.isStackAccessible() && !shellContext.isMarathonMode();
    }

    @Override
    public String select(Long id, String name) {
        try {
            if (id != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().get(id);
                if (stack != null) {
                    shellContext.addStack(id.toString(), stack.getName());
                    if (shellContext.isCredentialAvailable()) {
                        shellContext.setHint(Hints.CREATE_CLUSTER);
                    } else {
                        shellContext.setHint(Hints.CONFIGURE_HOSTGROUP);
                    }
                    prepareCluster(id.toString());
                    shellContext.prepareInstanceGroups(stack);
                    return "Stack selected, id: " + id;
                }

            } else if (name != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name);
                if (stack != null) {
                    Long stackId = stack.getId();
                    shellContext.addStack(stackId.toString(), name);
                    if (shellContext.isCredentialAvailable()) {
                        shellContext.setHint(Hints.CREATE_CLUSTER);
                    } else {
                        shellContext.setHint(Hints.CONFIGURE_HOSTGROUP);
                    }
                    prepareCluster(stackId.toString());
                    shellContext.prepareInstanceGroups(stack);
                    return "Stack selected, name: " + name;
                }
            }
            return "No stack specified. (select by using --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "stack select --id", help = "Delete the stack by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "stack select --name", help = "Delete the stack by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true)String name) throws Exception {
        return select(null, name);
    }

    @Override
    public boolean createStackAvailable(String platform) {
        return shellContext.isCredentialAvailable()
                && shellContext.getActiveCloudPlatform().equals(platform)
                && shellContext.getActiveNetworkId() != null
                && shellContext.getActiveSecurityGroupId() != null
                && (shellContext.getActiveHostGroups().size() == shellContext.getInstanceGroups().size() - 1
                && shellContext.getActiveHostGroups().size() != 0) && !shellContext.isMarathonMode();
    }

    @Override
    public String create(String name, StackRegion region, StackAvailabilityZone availabilityZone, Boolean publicInAccount, OnFailureAction onFailureAction,
            AdjustmentType adjustmentType, Long threshold, Boolean relocateDocker, Boolean wait, PlatformVariant platformVariant, String platform,
            Map<String, String> params) {
        try {
            validateNetwork();
            validateSecurityGroup();
            validateRegion(region);
            validateAvailabilityZone(region, availabilityZone);
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            wait = wait == null ? false : wait;
            IdJson id;
            StackRequest stackRequest = new StackRequest();
            stackRequest.setName(name);
            stackRequest.setRegion(region.getName());
            stackRequest.setRelocateDocker(relocateDocker);
            if (availabilityZone != null) {
                stackRequest.setAvailabilityZone(availabilityZone.getName());
            }
            stackRequest.setOnFailureAction(onFailureAction == null ? OnFailureAction.ROLLBACK : OnFailureAction.valueOf(onFailureAction.name()));
            stackRequest.setSecurityGroupId(Long.valueOf(shellContext.getActiveSecurityGroupId()));
            stackRequest.setNetworkId(Long.valueOf(shellContext.getActiveNetworkId()));
            FailurePolicyJson failurePolicyJson = new FailurePolicyJson();
            stackRequest.setCredentialId(Long.valueOf(shellContext.getCredentialId()));
            failurePolicyJson.setAdjustmentType(adjustmentType == null ? AdjustmentType.BEST_EFFORT : AdjustmentType.valueOf(adjustmentType.name()));
            failurePolicyJson.setThreshold(threshold == null ? 1L : threshold);
            stackRequest.setFailurePolicy(failurePolicyJson);
            stackRequest.setPlatformVariant(platformVariant == null ? "" : platformVariant.getName());
            stackRequest.setCloudPlatform(platform);
            stackRequest.setParameters(params);
            List<InstanceGroupJson> instanceGroupJsonList = new ArrayList<>();
            for (Map.Entry<String, InstanceGroupEntry> stringObjectEntry : shellContext.getInstanceGroups().entrySet()) {
                InstanceGroupEntry instanceGroupEntry = stringObjectEntry.getValue();
                InstanceGroupJson instanceGroupJson = new InstanceGroupJson();
                instanceGroupJson.setType(InstanceGroupType.valueOf(instanceGroupEntry.getType()));
                instanceGroupJson.setTemplateId(instanceGroupEntry.getTemplateId());
                instanceGroupJson.setNodeCount(instanceGroupEntry.getNodeCount());
                instanceGroupJson.setGroup(stringObjectEntry.getKey());
                instanceGroupJsonList.add(instanceGroupJson);
            }
            stackRequest.setInstanceGroups(instanceGroupJsonList);

            if (publicInAccount) {
                id = shellContext.cloudbreakClient().stackEndpoint().postPublic(stackRequest);
            } else {
                id = shellContext.cloudbreakClient().stackEndpoint().postPrivate(stackRequest);
            }
            StackResponse stackResponse = new StackResponse();
            stackResponse.setName(stackRequest.getName());
            stackResponse.setId(id.getId());
            shellContext.addStack(id.getId().toString(), name);
            shellContext.setHint(Hints.CREATE_CLUSTER);

            if (wait) {
                CloudbreakShellUtil.WaitResult waitResult = cloudbreakShellUtil.waitAndCheckStackStatus(id.getId(), Status.AVAILABLE.name());
                if (CloudbreakShellUtil.WaitResult.FAILED.equals(waitResult)) {
                    throw shellContext.exceptionTransformer().transformToRuntimeException("Stack creation failed with name:" + name);
                } else {
                    return "Stack creation finished with name: " + name;
                }
            }
            return String.format("Stack creation started with id: '%s' and name: '%s'", id.getId(), name);
        } catch (ValidationException ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }

    }

    @CliAvailabilityIndicator({ "stack node", "stack stop --id", "stack stop --name", "stack start --id", "stack start --name" })
    public boolean nodeAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode();
    }

    private String stop(StackResponse stackResponse) throws Exception {
        shellContext.addStack(stackResponse.getId().toString(), stackResponse.getName());
        prepareCluster(stackResponse.getId().toString());
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.STOPPED);
        cloudbreakShellUtil.checkResponse("stopStack",
                shellContext.cloudbreakClient().stackEndpoint().put(Long.valueOf(shellContext.getStackId()), updateStackJson));
        return "Stack is stopping";
    }

    public String stop(Long id, String name) {
        try {
            if (id != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().get(id);
                if (stack != null) {
                    return stop(stack);
                }
            } else if (name != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name);
                if (stack != null) {
                    return stop(stack);
                }
            }
            return "Stack was not specified";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "stack stop --id", help = "Stop the stack by its id")
    public String stopById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return stop(id, null);
    }

    @CliCommand(value = "stack stop --name", help = "Stop the stack by its name")
    public String stopByName(@CliOption(key = "", mandatory = true)String name) throws Exception {
        return stop(null, name);
    }

    private String start(StackResponse stackResponse) throws Exception {
        shellContext.addStack(stackResponse.getId().toString(), stackResponse.getName());
        prepareCluster(stackResponse.getId().toString());
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.STARTED);
        cloudbreakShellUtil.checkResponse("startStack",
                shellContext.cloudbreakClient().stackEndpoint().put(Long.valueOf(shellContext.getStackId()), updateStackJson));
        return "Stack is starting";
    }

    public String start(Long id, String name) {
        try {
            if (id != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().get(id);
                if (stack != null) {
                    return start(stack);
                }
            } else if (name != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name);
                if (stack != null) {
                    return start(stack);
                }
            }
            return "Stack was not specified";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "stack start --id", help = "Start the stack by its id")
    public String startById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return start(id, null);
    }

    @CliCommand(value = "stack start --name", help = "Start the stack by its name")
    public String startByName(@CliOption(key = "", mandatory = true)String name) throws Exception {
        return start(null, name);
    }

    @CliCommand(value = "stack node --ADD", help = "Add new nodes to the cluster")
    public String addNode(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") InstanceGroup instanceGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be added to the stack") Integer adjustment,
            @CliOption(key = "withClusterUpScale", mandatory = false, help = "Do the upscale with the cluster together") Boolean withClusterUpScale) {
        try {
            if (adjustment < 1) {
                return "The adjustment value in case of node addition should be at least 1.";
            }
            UpdateStackJson updateStackJson = new UpdateStackJson();
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setScalingAdjustment(adjustment);
            instanceGroupAdjustmentJson.setWithClusterEvent(withClusterUpScale == null ? false : withClusterUpScale);
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup.getName());
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            cloudbreakShellUtil.checkResponse("upscaleStack",
                    shellContext.cloudbreakClient().stackEndpoint().put(Long.valueOf(shellContext.getStackId()), updateStackJson));
            return shellContext.getStackId();
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "stack node --REMOVE", help = "Remove nodes from the cluster")
    public String removeNode(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") InstanceGroup instanceGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be removed to the stack") Integer adjustment) {
        try {
            if (adjustment > -1) {
                return "The adjustment value in case of node removal should be negative.";
            }
            UpdateStackJson updateStackJson = new UpdateStackJson();
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setScalingAdjustment(adjustment);
            instanceGroupAdjustmentJson.setWithClusterEvent(false);
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup.getName());
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            cloudbreakShellUtil.checkResponse("downscaleStack",
                    shellContext.cloudbreakClient().stackEndpoint().put(Long.valueOf(shellContext.getStackId()), updateStackJson));
            return shellContext.getStackId();
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = "stack metadata")
    public boolean metadataAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "stack metadata", help = "Shows the stack metadata")
    public String metadata(
            @CliOption(key = "id", mandatory = false, help = "Id of the stack") Long id,
            @CliOption(key = "name", mandatory = false, help = "Name of the stack") String name,
            @CliOption(key = "instancegroup", mandatory = false, help = "Instancegroup of the stack") String group,
            @CliOption(key = "outputType", mandatory = false, help = "OutputType of the response") OutPutType outPutType) {
        try {
            outPutType = outPutType == null ? OutPutType.RAW : outPutType;
            StackResponse stackResponse = getStackResponse(name, id);
            if (stackResponse != null && stackResponse.getInstanceGroups() != null) {
                Map<String, List<String>> stringListMap = collectMetadata(
                        stackResponse.getInstanceGroups() == null ? new ArrayList<InstanceGroupJson>() : stackResponse.getInstanceGroups(), group);
                return shellContext.outputTransformer().render(outPutType, stringListMap, "FIELD", "VALUE");
            }
            return "No stack specified.";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = "stack sync")
    public boolean syncAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode();
    }

    @CliCommand(value = "stack sync", help = "Sync the stack")
    public String sync() {
        try {
            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setStatus(StatusRequest.SYNC);
            shellContext.cloudbreakClient().stackEndpoint().put(Long.valueOf(shellContext.getStackId()), updateStackJson);
            return "Stack is syncing";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    private StackResponse getStackResponse(String name, Long id) {
        if (name != null) {
            return shellContext.cloudbreakClient().stackEndpoint().getPublic(name);
        } else if (id != null) {
            return shellContext.cloudbreakClient().stackEndpoint().get(id);
        }
        return null;
    }

    private Map<String, List<String>> collectMetadata(List<InstanceGroupJson> instanceGroups, final String group) {
        final Map<String, List<String>> returnValues = new HashMap<>();
        for (InstanceGroupJson instanceGroup : instanceGroups) {
            List<String> list = new ArrayList<>();
            for (InstanceMetaDataJson instanceMetaDataJson : instanceGroup.getMetadata()) {
                if (instanceMetaDataJson.getPublicIp() != null) {
                    list.add(instanceMetaDataJson.getPublicIp());
                }
            }
            returnValues.put(instanceGroup.getGroup(), list);
        }
        if (everyGroupDataNeeded(group)) {
            return returnValues;
        }
        return new HashMap<String, List<String>>() {
            {
                put(group, returnValues.get(group));
            }
        };
    }

    private boolean everyGroupDataNeeded(String group) {
        return group == null || "".equals(group);
    }

    private void validateAvailabilityZone(StackRegion region, StackAvailabilityZone availabilityZone) {
        Collection<String> zonesByRegion = shellContext.getAvailabilityZonesByRegion(shellContext.getActiveCloudPlatform(), region.getName());
        if (availabilityZone != null && zonesByRegion != null && !zonesByRegion.contains(availabilityZone.getName())) {
            throw new ValidationException("Availability zone is not in the selected region. The available zones in the regions are: " + zonesByRegion);
        }
    }

    private void validateNetwork() {
        Long networkId = shellContext.getActiveNetworkId();
        if (networkId == null || (networkId != null && shellContext.getNetworksByProvider().get(networkId) == shellContext.getActiveCloudPlatform())) {
            throw new ValidationException("A network must be selected with the same cloud platform as the credential!");
        }
    }

    private void validateSecurityGroup() {
        Long securityGroupId = shellContext.getActiveSecurityGroupId();
        if (securityGroupId == null) {
            throw new ValidationException("A security group must be selected");
        }
    }

    private void validateRegion(StackRegion region) {
        Collection<String> regionsByPlatform = shellContext.getRegionsByPlatform(shellContext.getActiveCloudPlatform());
        if (regionsByPlatform != null && !regionsByPlatform.isEmpty() && !regionsByPlatform.contains(region.getName())) {
            throw new ValidationException("Region is not available for the selected platform.");
        }
    }

    private void prepareCluster(String stackId) {
        try {
            ClusterResponse cluster = shellContext.cloudbreakClient().clusterEndpoint().get(Long.valueOf(stackId));
            if (cluster != null) {
                String blueprintId = cluster.getBlueprintId().toString();
                shellContext.addBlueprint(blueprintId);
            }
        } catch (Exception e) {
            return;
        }
    }

}
