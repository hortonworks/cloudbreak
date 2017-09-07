package com.sequenceiq.cloudbreak.shell.commands.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.completion.ConstraintName;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.MarathonHostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

@Component
public class MarathonCommands implements CommandMarker {

    public static final String PLATFORM = "MARATHON";

    public static final String BYOS = "BYOS";

    private ShellContext shellContext;

    private final CredentialCommands baseCredentialCommands;

    private final StackCommands stackCommands;

    public MarathonCommands(ShellContext shellContext,
            CredentialCommands baseCredentialCommands,
            StackCommands stackCommands) {
        this.baseCredentialCommands = baseCredentialCommands;
        this.shellContext = shellContext;
        this.stackCommands = stackCommands;
    }

    @CliAvailabilityIndicator("stack create --MARATHON")
    public boolean createStackAvailable() {
        return shellContext.isPlatformAvailable(BYOS) &&  shellContext.getActiveHostGroups().size() == shellContext.getMarathonHostGroups().size();
    }

    @CliAvailabilityIndicator("credential create --MARATHON")
    public boolean createCredentialAvailable() {
        return shellContext.isPlatformAvailable(BYOS);
    }

    @CliCommand(value = "credential create --MARATHON", help = "Create a new MARATHON credential")
    public String createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "publicInAccount", help = "flags if the credential is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the credential") String description,
            @CliOption(key = "apiEndpoint", help = "ApiEndpoint of the credential") String apiEndpoint
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", PLATFORM);
        parameters.put("apiEndpoint", apiEndpoint);
        return baseCredentialCommands.create(name, null, null, null, description, publicInAccount, null, parameters, BYOS);
    }

    @CliCommand(value = "stack create --MARATHON", help = "Create a new MARATHON stack")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") String name,
            @CliOption(key = "publicInAccount", help = "flags if the stack is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "wait", help = "Wait for stack creation", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true") Long timeout) {
        Set<Entry<String, MarathonHostgroupEntry>> entries = shellContext.getMarathonHostGroups().entrySet();
        StackRequest stackRequest = new StackRequest();
        OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
        orchestratorRequest.setApiEndpoint(shellContext.getApiEndpoint());
        orchestratorRequest.setType(PLATFORM);
        stackRequest.setName(name);
        stackRequest.setOrchestrator(orchestratorRequest);
        stackRequest.setRegion("LOCAL");
        stackRequest.setCloudPlatform(BYOS);
        stackRequest.setCredentialId(Long.valueOf(shellContext.getCredentialId()));

        for (Entry<String, MarathonHostgroupEntry> entry : entries) {
            InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
            instanceGroupRequest.setGroup(entry.getKey());
            instanceGroupRequest.setType(InstanceGroupType.CORE);
            instanceGroupRequest.setNodeCount(entry.getValue().getNodeCount());
            stackRequest.getInstanceGroups().add(instanceGroupRequest);
        }


        StackResponse stackResponse = stackCommands.create(stackRequest, publicInAccount, wait, timeout);
        shellContext.setSelectedMarathonStackId(stackResponse.getId());
        shellContext.setSelectedMarathonStackName(stackResponse.getName());
        return String.format("Stack creation started with id: '%s' and name: '%s'", stackResponse.getId(), stackResponse.getName());
    }

    @CliCommand(value = "marathon constraint create", help = "Create a new marathon constraint")
    public String createMarathonTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the marathon constraint") String name,
            @CliOption(key = "cores", mandatory = true, help = "Cpu cores of the marathon constraint (0.1 - 64 core)") Double cpuCores,
            @CliOption(key = "memory", mandatory = true, help = "Memory in Mb of the marathon constraint (16mb - 128Gb)") Double memory,
            @CliOption(key = "diskSize", mandatory = true, help = "Disk in Gb of the marathon constraint (10Gb - 1000Gb)") Double disk,
            @CliOption(key = "description", help = "Description of the marathon stack") String description,
            @CliOption(key = "publicInAccount", help = "flags if the constraint is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount) {
        Long id;
        try {
            ConstraintTemplateRequest constraintTemplateRequest = new ConstraintTemplateRequest();
            constraintTemplateRequest.setName(name);
            constraintTemplateRequest.setCpu(cpuCores);
            constraintTemplateRequest.setDescription(description);
            constraintTemplateRequest.setDisk(disk);
            constraintTemplateRequest.setMemory(memory);
            constraintTemplateRequest.setOrchestratorType(OrchestratorConstants.MARATHON);
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().constraintTemplateEndpoint().postPublic(constraintTemplateRequest).getId();
            } else {
                id = shellContext.cloudbreakClient().constraintTemplateEndpoint().postPrivate(constraintTemplateRequest).getId();
            }
            return "Marathon template was created with id: " + id;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon constraint list", help = "Shows the currently available marathon constraints")
    public String listMarathonTemplates() {
        try {
            Set<ConstraintTemplateResponse> publics = shellContext.cloudbreakClient().constraintTemplateEndpoint().getPublics();
            shellContext.setConstraints(publics);
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon constraint delete", help = "Delete the marathon constraint by its id or name")
    public Object deleteMarathonTemplate(
            @CliOption(key = "id", help = "Id of the marathon template") String id,
            @CliOption(key = "name", help = "Name of the marathon template") String name) {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().constraintTemplateEndpoint().delete(Long.valueOf(id));
                shellContext.setConstraints(shellContext.cloudbreakClient().constraintTemplateEndpoint().getPublics());
                return String.format("Marathon constraint has been deleted, id: %s", id);
            } else if (name != null) {
                shellContext.cloudbreakClient().constraintTemplateEndpoint().deletePublic(name);
                shellContext.setConstraints(shellContext.cloudbreakClient().constraintTemplateEndpoint().getPublics());
                return String.format("Marathon constraint has been deleted, name: %s", name);
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No constraint specified");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon constraint show", help = "Shows the marathon constraint by its id or name")
    public Object showTemplate(
            @CliOption(key = "id", help = "Id of the marathon constraint") Long id,
            @CliOption(key = "name", help = "Name of the marathon constraint") String name) {
        try {
            ConstraintTemplateResponse aPublic = getConstraintTemplateResponse(id, name);
            if (aPublic != null) {
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE");
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No constraint was found");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    private ConstraintTemplateResponse getConstraintTemplateResponse(Long id, String name) {
        if (id != null) {
            return shellContext.cloudbreakClient().constraintTemplateEndpoint().get(id);
        } else {
            return shellContext.cloudbreakClient().constraintTemplateEndpoint().getPublic(name);
        }
    }

    @CliCommand(value = "marathon hostgroup list", help = "list hostgroups")
    public String listHostGroup() {
        try {
            return shellContext.outputTransformer().render(shellContext.getMarathonHostGroups(), "hostgroup");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon hostgroup configure", help = "configure hostgroups")
    public String createHostGroup(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostgroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Count of the nodes in the hostgroup") Integer nodecount,
            @CliOption(key = "constraintName", mandatory = true, help = "Name of the constraint") ConstraintName constraintTemplateName) {
        try {
            ConstraintTemplateResponse constraintTemplateResponse = getConstraintTemplateResponse(null, constraintTemplateName.getName());
            if (constraintTemplateResponse != null) {
                shellContext.putMarathonHostGroup(hostgroup.getName(), new MarathonHostgroupEntry(nodecount, constraintTemplateName.getName()));
                int marathonHostGroupsConfigured = shellContext.getMarathonHostGroups().size();
                int totalHostGroupsToConfigure = shellContext.getActiveHostGroups().size();
                if (marathonHostGroupsConfigured == totalHostGroupsToConfigure) {
                    shellContext.setHint(Hints.MARATHON_CLUSTER);
                }
                return shellContext.outputTransformer().render(shellContext.getMarathonHostGroups(), "hostgroup");
            } else {
                throw shellContext.exceptionTransformer().transformToRuntimeException("Constraint was not found");
            }
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }


}
