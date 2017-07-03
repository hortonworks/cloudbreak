package com.sequenceiq.cloudbreak.shell.commands.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.api.model.VmTypeJson;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.commands.provider.OpenStackCommands;
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

    public static final long TIMEOUT = 5000L;

    private ShellContext shellContext;

    private CloudbreakShellUtil cloudbreakShellUtil;

    public BaseStackCommands(ShellContext shellContext, CloudbreakShellUtil cloudbreakShellUtil) {
        this.shellContext = shellContext;
        this.cloudbreakShellUtil = cloudbreakShellUtil;
    }

    @CliAvailabilityIndicator(value = "stack list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "stack list", help = "Shows all of your stacks")
    public String list() {
        try {
            Set<StackResponse> publics = shellContext.cloudbreakClient().stackEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator({"stack show --id", "stack show --name"})
    @Override
    public boolean showAvailable() {
        return true;
    }

    @CliCommand(value = "stack show --id", help = "Show the stack by its id")
    @Override
    public String showById(
            @CliOption(key = "", mandatory = true) Long id,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(id, null, outPutType);
    }

    @CliCommand(value = "stack show --name", help = "Show the stack by its name")
    @Override
    public String showByName(
            @CliOption(key = "", mandatory = true) String name,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(null, name, outPutType);
    }

    @Override
    public String show(Long id, String name, OutPutType outPutType) {
        try {
            outPutType = outPutType == null ? OutPutType.RAW : outPutType;
            StackResponse stackResponse = getStackResponse(name, id);
            if (stackResponse != null) {
                return shellContext.outputTransformer().render(outPutType, shellContext.responseTransformer()
                        .transformObjectToStringMap(stackResponse, "stackTemplate"), "FIELD", "VALUE");
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No stack specified (select a stack by --id or --name)");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = {"stack delete --id", "stack delete --name"})
    @Override
    public boolean deleteAvailable() {
        return true;
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
            @CliOption(key = "wait", help = "Wait for stack termination", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true", mandatory = false) Long timeout) {
        return delete(id, null, wait, timeout);
    }

    @CliCommand(value = "stack delete --name", help = "Delete the stack by its name")
    public String deleteById(
            @CliOption(key = "", mandatory = true) String name,
            @CliOption(key = "wait", help = "Wait for stack termination", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true", mandatory = false) Long timeout) {
        return delete(null, name, wait, timeout);
    }

    @Override
    public String delete(Long id, String name) throws Exception {
        return delete(id, name, false, TIMEOUT);
    }

    public String delete(Long id, String name, boolean wait, Long timeout) {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().stackEndpoint().delete(id, false, false);
                shellContext.setHint(Hints.CREATE_CLUSTER);
                shellContext.removeStack();
                if (wait) {
                    CloudbreakShellUtil.WaitResult waitResult = cloudbreakShellUtil.waitAndCheckStackStatus(id, Status.DELETE_COMPLETED.name(), timeout);
                    if (CloudbreakShellUtil.WaitResultStatus.FAILED.equals(waitResult.getWaitResultStatus())) {
                        throw shellContext.exceptionTransformer().transformToRuntimeException("Stack termination failed: " + waitResult.getReason());
                    } else {
                        return "Stack terminated with id: " + id;
                    }
                } else {
                    return "Stack termination started with id: " + id;
                }
            } else if (name != null) {
                StackResponse response = shellContext.cloudbreakClient().stackEndpoint().getPublic(name, new HashSet<>());
                shellContext.cloudbreakClient().stackEndpoint().deletePublic(name, false, false);
                shellContext.setHint(Hints.CREATE_CLUSTER);

                if (wait) {
                    CloudbreakShellUtil.WaitResult waitResult =
                            cloudbreakShellUtil.waitAndCheckStackStatus(response.getId(), Status.DELETE_COMPLETED.name(), timeout);
                    if (CloudbreakShellUtil.WaitResultStatus.FAILED.equals(waitResult.getWaitResultStatus())) {
                        throw shellContext.exceptionTransformer()
                                .transformToRuntimeException("Stack termination failed: " + waitResult.getReason());
                    } else {
                        return "Stack terminated with name: " + name;
                    }
                } else {
                    return "Stack termination started with name: " + name;
                }
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("Stack not specified. (select by using --id or --name)");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = {"stack select --id", "stack select --name"})
    @Override
    public boolean selectAvailable() {
        return shellContext.isStackAccessible() && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String select(Long id, String name) {
        try {
            if (id != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().get(id, new HashSet<>());
                if (stack != null) {
                    shellContext.addStack(id.toString(), stack.getName());
                    shellContext.setCredential(stack.getCredentialId().toString());
                    shellContext.setActiveNetworkId(stack.getNetworkId());
                    prepareCluster(id.toString());
                    shellContext.prepareInstanceGroups(stack);
                    if (shellContext.getBlueprintId() == null) {
                        shellContext.setHint(Hints.SELECT_BLUEPRINT);
                    } else {
                        shellContext.setHint(Hints.SELECT_CREDENTIAL);
                    }
                    return "Stack selected, id: " + id;
                }

            } else if (name != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name, new HashSet<>());
                if (stack != null) {
                    Long stackId = stack.getId();
                    shellContext.addStack(stackId.toString(), name);
                    shellContext.setCredential(stack.getCredentialId().toString());
                    shellContext.setActiveNetworkId(stack.getNetworkId());
                    prepareCluster(stackId.toString());
                    shellContext.prepareInstanceGroups(stack);
                    if (shellContext.getBlueprintId() == null) {
                        shellContext.setHint(Hints.SELECT_BLUEPRINT);
                    } else {
                        shellContext.setHint(Hints.SELECT_CREDENTIAL);
                    }
                    return "Stack selected, name: " + name;
                }
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No stack specified. (select by using --id or --name)");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "stack select --id", help = "Select the stack by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "stack select --name", help = "Select the stack by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return select(null, name);
    }

    @Override
    public boolean createStackAvailable(String platform) {
        return shellContext.isCredentialAvailable()
                && shellContext.getActiveCloudPlatform().equals(platform)
                && shellContext.getActiveNetworkId() != null
                && (shellContext.getActiveHostGroups().size() == shellContext.getInstanceGroups().size()
                && shellContext.getActiveHostGroups().size() != 0) && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String create(String name, StackRegion region, StackAvailabilityZone availabilityZone, boolean publicInAccount, OnFailureAction onFailureAction,
            AdjustmentType adjustmentType, Long threshold, Boolean relocateDocker, boolean wait, PlatformVariant platformVariant, String orchestrator,
            String platform, String ambariVersion, String hdpVersion, String imageCatalog, Map<String, String> params, Map<String, String> userDefinedTags,
            String customImage, Long timeout) {
        try {
            validateNetwork();
            validateRegion(region);
            validateInstanceGroups(platform, region.getName(), availabilityZone == null ? null : availabilityZone.getName());
            validateAvailabilityZone(region, availabilityZone);
            Long id;
            StackRequest stackRequest = new StackRequest();
            stackRequest.setName(name);
            stackRequest.setRegion(region.getName());
            stackRequest.setRelocateDocker(relocateDocker);
            if (availabilityZone != null) {
                stackRequest.setAvailabilityZone(availabilityZone.getName());
            }
            stackRequest.setOnFailureAction(onFailureAction == null ? OnFailureAction.DO_NOTHING : OnFailureAction.valueOf(onFailureAction.name()));
            stackRequest.setNetworkId(shellContext.getActiveNetworkId());
            FailurePolicyRequest failurePolicyRequest = new FailurePolicyRequest();
            stackRequest.setCredentialId(Long.valueOf(shellContext.getCredentialId()));
            failurePolicyRequest.setAdjustmentType(adjustmentType == null ? AdjustmentType.BEST_EFFORT : AdjustmentType.valueOf(adjustmentType.name()));
            failurePolicyRequest.setThreshold(threshold == null ? 1L : threshold);
            stackRequest.setFailurePolicy(failurePolicyRequest);
            stackRequest.setPlatformVariant(platformVariant == null ? "" : platformVariant.getName());
            stackRequest.setCloudPlatform(platform);
            stackRequest.setParameters(params);
            stackRequest.setTags(Collections.singletonMap("userDefined", userDefinedTags));
            stackRequest.setAmbariVersion(ambariVersion);
            stackRequest.setHdpVersion(hdpVersion);
            stackRequest.setImageCatalog(imageCatalog);
            stackRequest.setCustomImage(customImage);
            stackRequest.setMultiGateway(shellContext.isMultipleGatewayEnabled());
            OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
            orchestratorRequest.setType(orchestrator);
            stackRequest.setOrchestrator(orchestratorRequest);
            List<InstanceGroupRequest> instanceGroupRequestList = new ArrayList<>();
            for (Map.Entry<String, InstanceGroupEntry> stringObjectEntry : shellContext.getInstanceGroups().entrySet()) {
                InstanceGroupEntry instanceGroupEntry = stringObjectEntry.getValue();
                InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
                instanceGroupRequest.setType(InstanceGroupType.valueOf(instanceGroupEntry.getType()));
                instanceGroupRequest.setTemplateId(instanceGroupEntry.getTemplateId());
                instanceGroupRequest.setNodeCount(instanceGroupEntry.getNodeCount());
                instanceGroupRequest.setGroup(stringObjectEntry.getKey());
                instanceGroupRequest.setSecurityGroupId(instanceGroupEntry.getSecurityGroupId());
                instanceGroupRequest.setParameters(instanceGroupEntry.getAttributes());
                instanceGroupRequestList.add(instanceGroupRequest);
            }
            stackRequest.setInstanceGroups(instanceGroupRequestList);

            if (publicInAccount) {
                id = shellContext.cloudbreakClient().stackEndpoint().postPublic(stackRequest).getId();
            } else {
                id = shellContext.cloudbreakClient().stackEndpoint().postPrivate(stackRequest).getId();
            }
            StackResponse stackResponse = new StackResponse();
            stackResponse.setName(stackRequest.getName());
            stackResponse.setId(id);
            shellContext.addStack(id.toString(), name);
            shellContext.setHint(Hints.CREATE_CLUSTER);

            if (wait) {
                waitUntilStackAvailable(id, "Stack creation failed:", timeout);
                return "Stack creation finished with name: " + name;
            }
            return String.format("Stack creation started with id: '%s' and name: '%s'", id, name);
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }

    }

    @Override
    public StackResponse create(StackRequest stackRequest, Boolean publicInAccount, Boolean wait, Long timeout) {
        try {
            Long id;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().stackEndpoint().postPublic(stackRequest).getId();
            } else {
                id = shellContext.cloudbreakClient().stackEndpoint().postPrivate(stackRequest).getId();
            }
            StackResponse stackResponse = new StackResponse();
            stackResponse.setName(stackRequest.getName());
            stackResponse.setId(id);
            shellContext.addStack(id.toString(), stackRequest.getName());
            shellContext.setHint(Hints.CREATE_CLUSTER);

            if (wait) {
                waitUntilStackAvailable(id, "Stack creation failed:", timeout);
                return stackResponse;
            }
            return stackResponse;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator({"stack node --ADD", "stack node --REMOVE", "stack stop --id", "stack stop --name", "stack start --id", "stack start --name"})
    public boolean nodeAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    private String stop(StackResponse stackResponse) {
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
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().get(id, new HashSet<>());
                if (stack != null) {
                    return stop(stack);
                }
            } else if (name != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name, new HashSet<>());
                if (stack != null) {
                    return stop(stack);
                }
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("Stack was not specified");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "stack stop --id", help = "Stop the stack by its id")
    public String stopById(@CliOption(key = "", mandatory = true) Long id) {
        return stop(id, null);
    }

    @CliCommand(value = "stack stop --name", help = "Stop the stack by its name")
    public String stopByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return stop(null, name);
    }

    private String start(StackResponse stackResponse) {
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
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().get(id, new HashSet<>());
                if (stack != null) {
                    return start(stack);
                }
            } else if (name != null) {
                StackResponse stack = shellContext.cloudbreakClient().stackEndpoint().getPublic(name, new HashSet<>());
                if (stack != null) {
                    return start(stack);
                }
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("Stack was not specified");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "stack start --id", help = "Start the stack by its id")
    public String startById(@CliOption(key = "", mandatory = true) Long id) {
        return start(id, null);
    }

    @CliCommand(value = "stack start --name", help = "Start the stack by its name")
    public String startByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return start(null, name);
    }

    @CliCommand(value = "stack node --ADD", help = "Add new nodes to the cluster")
    public String addNode(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") InstanceGroup instanceGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be added to the stack") Integer adjustment,
            @CliOption(key = "withClusterUpScale", help = "Do the upscale with the cluster together",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean withClusterUpScale,
            @CliOption(key = "wait", help = "Wait until the operation completes",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true", mandatory = false) Long timeout) {
        try {
            if (adjustment < 1) {
                throw shellContext.exceptionTransformer().transformToRuntimeException("The adjustment value in case of node addition should be at least 1");
            }
            UpdateStackJson updateStackJson = new UpdateStackJson();
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setScalingAdjustment(adjustment);
            instanceGroupAdjustmentJson.setWithClusterEvent(withClusterUpScale);
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup.getName());
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            String stackIdStr = shellContext.getStackId();
            Long stackId = Long.valueOf(stackIdStr);
            cloudbreakShellUtil.checkResponse("upscaleStack", shellContext.cloudbreakClient().stackEndpoint().put(stackId, updateStackJson));
            if (!wait) {
                return "Stack upscale started with id: " + stackIdStr;
            }

            waitUntilStackAvailable(stackId, "Stack upscale failed: ", timeout);
            if (!withClusterUpScale) {
                return "Stack upscale finished with id: " + stackIdStr;
            }

            waitUntilClusterAvailable(stackId, "Cluster upscale failed: ", timeout);
            return "Stack and cluster upscale finished with id " + stackIdStr;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "stack node --REMOVE", help = "Remove nodes from the cluster")
    public String removeNode(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") InstanceGroup instanceGroup,
            @CliOption(key = "adjustment", mandatory = true, help = "Count of the nodes which will be removed from the stack") Integer adjustment,
            @CliOption(key = "wait", help = "Wait until the operation completes",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true", mandatory = false) Long timeout) {
        try {
            if (adjustment > -1) {
                throw shellContext.exceptionTransformer().transformToRuntimeException("The adjustment value in case of node removal should be negative");
            }
            UpdateStackJson updateStackJson = new UpdateStackJson();
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setScalingAdjustment(adjustment);
            instanceGroupAdjustmentJson.setWithClusterEvent(false);
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup.getName());
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            String stackIdStr = shellContext.getStackId();
            Long stackId = Long.valueOf(stackIdStr);
            cloudbreakShellUtil.checkResponse("downscaleStack", shellContext.cloudbreakClient().stackEndpoint().put(stackId, updateStackJson));
            if (!wait) {
                return "Stack downscale started with id: " + stackIdStr;
            }
            waitUntilStackAvailable(stackId, "Stack downscale failed: ", timeout);
            return "Stack downscale finished with id: " + stackIdStr;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = "stack metadata")
    public boolean metadataAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "stack metadata", help = "Shows the stack metadata")
    public String metadata(
            @CliOption(key = "id", help = "Id of the stack") Long id,
            @CliOption(key = "name", help = "Name of the stack") String name,
            @CliOption(key = "instancegroup", help = "Instancegroup of the stack") String group,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) {
        try {
            outPutType = outPutType == null ? OutPutType.RAW : outPutType;
            StackResponse stackResponse = getStackResponse(name, id);
            if (stackResponse != null && stackResponse.getInstanceGroups() != null) {
                Map<String, InstanceMetaDataJson> metadata = collectMetadata(
                        stackResponse.getInstanceGroups() == null ? new ArrayList<>() : stackResponse.getInstanceGroups(), group);
                return shellContext.outputTransformer().render(outPutType, metadata, "instanceId");
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No stack specified");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = "stack sync")
    public boolean syncAvailable() {
        return shellContext.isStackAvailable() && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "stack sync", help = "Sync the stack")
    public String sync() {
        try {
            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setStatus(StatusRequest.SYNC);
            cloudbreakShellUtil.checkResponse("syncStack",
                    shellContext.cloudbreakClient().stackEndpoint().put(Long.valueOf(shellContext.getStackId()), updateStackJson));
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
            return shellContext.cloudbreakClient().stackEndpoint().getPublic(name, new HashSet<>());
        } else if (id != null) {
            return shellContext.cloudbreakClient().stackEndpoint().get(id, new HashSet<>());
        }
        return null;
    }

    private Map<String, InstanceMetaDataJson> collectMetadata(List<InstanceGroupResponse> instanceGroups, final String group) {
        final Map<String, InstanceMetaDataJson> returnValues = new HashMap<>();
        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            for (InstanceMetaDataJson instanceMetaDataJson : instanceGroup.getMetadata()) {
                if (everyGroupDataNeeded(group) || instanceMetaDataJson.getInstanceGroup().equals(group)) {
                    returnValues.put(instanceMetaDataJson.getInstanceId(), instanceMetaDataJson);
                }
            }
        }
        return returnValues;
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
        if (networkId == null || !shellContext.getNetworksByProvider().get(networkId).equals(shellContext.getActiveCloudPlatform())) {
            throw new ValidationException("A network must be selected with the same cloud platform as the credential!");
        }
    }

    private void validateRegion(StackRegion region) {
        Collection<String> regionsByPlatform = shellContext.getRegionsByPlatform(shellContext.getActiveCloudPlatform());
        if (regionsByPlatform != null && !regionsByPlatform.isEmpty() && !regionsByPlatform.contains(region.getName())) {
            throw new ValidationException("Region is not available for the selected platform.");
        }
    }

    private void validateInstanceGroups(String platform, String region, String availabilityZone) {
        shellContext.getInstanceGroups().values()
                .stream().filter(i -> "GATEWAY".equals(i.getType()))
                .findAny()
                .orElseThrow(() -> new ValidationException("You must specify where to install ambari server to with '--ambariServer true' on instancegroup"));
        if (!platform.equals(OpenStackCommands.PLATFORM)) {
            Map<String, Collection<VmTypeJson>> vmTypesPerZones = shellContext.getVmTypesPerZones().get(platform);
            Map<Long, TemplateResponse> templateMap = shellContext.getTemplateMap();
            String azone = availabilityZone == null ? shellContext.getAvailabilityZonesByRegion(platform, region).iterator().next() : availabilityZone;
            Collection<VmTypeJson> vmTypes = vmTypesPerZones.get(azone);
            for (Map.Entry<String, InstanceGroupEntry> ig : shellContext.getInstanceGroups().entrySet()) {
                TemplateResponse template = templateMap.get(ig.getValue().getTemplateId());
                String instanceType = template.getInstanceType();
                if (!vmTypes.stream().anyMatch(vm -> vm.getValue().equals(instanceType))) {
                    throw new ValidationException("The " + instanceType + " instencetype is not supported for the " + ig.getKey() + " instancegroup which using "
                            + template.getName() + " template in [" + region + "] region and [" + availabilityZone + "] availabilty zone"
                            + " Supported instancetypes in this region / availability zone: "
                            + vmTypes.stream().map(vm -> vm.getValue()).collect(Collectors.toList()));
                }
            }
        }
    }

    private void prepareCluster(String stackId) {
        try {
            ClusterResponse cluster = shellContext.cloudbreakClient().clusterEndpoint().get(Long.valueOf(stackId));
            if (cluster != null) {
                String blueprintId = cluster.getBlueprintId().toString();
                shellContext.addBlueprint(blueprintId);
            } else {
                shellContext.removeBlueprintId();
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Waits until stack becomes available after some operation.
     *
     * @throws RuntimeException if the operation fails
     */
    public void waitUntilStackAvailable(Long stackId, String errorMessagePrefix, Long timeout) {
        CloudbreakShellUtil.WaitResult waitResult = cloudbreakShellUtil.waitAndCheckStackStatus(stackId, Status.AVAILABLE.name(), timeout);
        throwIfWaitFailed(errorMessagePrefix, waitResult);
    }

    /**
     * Waits until cluster becomes available after some operation.
     *
     * @throws RuntimeException if the operation fails
     */
    public void waitUntilClusterAvailable(Long stackId, String errorMessagePrefix, Long timeout) {
        CloudbreakShellUtil.WaitResult waitResult = cloudbreakShellUtil.waitAndCheckClusterStatus(stackId, Status.AVAILABLE.name(), timeout);
        throwIfWaitFailed(errorMessagePrefix, waitResult);
    }

    private void throwIfWaitFailed(String errorMessagePrefix, CloudbreakShellUtil.WaitResult waitResult) {
        if (CloudbreakShellUtil.WaitResultStatus.FAILED.equals(waitResult.getWaitResultStatus())) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(errorMessagePrefix + waitResult.getReason());
        }
    }

}
