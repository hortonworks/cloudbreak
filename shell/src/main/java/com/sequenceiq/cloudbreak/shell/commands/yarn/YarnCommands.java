package com.sequenceiq.cloudbreak.shell.commands.yarn;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.shell.completion.ConstraintName;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.FocusType;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.model.YarnHostgroupEntry;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class YarnCommands implements CommandMarker {

    private static final String BYOS = "BYOS";

    @Inject
    private ShellContext shellContext;

    @Inject
    private CloudbreakClient cloudbreakClient;

    @Inject
    private ExceptionTransformer exceptionTransformer;

    @Inject
    private ResponseTransformer responseTransformer;

    @CliAvailabilityIndicator({ "yarnmode" })
    public boolean isHintCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator({ "yarn" })
    public boolean isYarnHintCommandAvailable() {
        return shellContext.isYarnMode();
    }

    @CliCommand(value = "mode --YARN", help = "Change to yarn mode")
    public void yarnMode() {
        if (shellContext.getSelectedYarnStackName() == null) {
            shellContext.setFocus(null, FocusType.YARN);
        } else {
            shellContext.setFocus(shellContext.getSelectedYarnStackName(), FocusType.YARN);
        }
        shellContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
        shellContext.setHint(Hints.YARN_STACK);
    }

    @CliCommand(value = "mode --DEFAULT", help = "Change to the default, non-yarn mode")
    public void rootMode() {
        shellContext.resetFocus();
        shellContext.setHint(Hints.CREATE_CREDENTIAL);
    }

    @CliCommand(value = "yarn stack create", help = "Import a yarn stack")
    public String createYarnStack(
            @CliOption(key = "name", mandatory = true, help = "Name of the yarn stack") String name,
            @CliOption(key = "yarnEndpoint", mandatory = true, help = "Endpoint of the yarn") String yarnEndpoint) {
        try {
            StackRequest stackRequest = new StackRequest();
            OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
            orchestratorRequest.setApiEndpoint(yarnEndpoint);
            orchestratorRequest.setType("YARN");
            stackRequest.setName(name);
            stackRequest.setOrchestrator(orchestratorRequest);
            return String.format("Yarn stack imported with id: '%d' and name: '%s'",
                    cloudbreakClient.stackEndpoint().postPublic(stackRequest).getId(), name);
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "yarn stack show", help = "Show a yarn stack")
    public String showYarnStack(
            @CliOption(key = "name", help = "Name of the yarn stack") String name,
            @CliOption(key = "id", help = "Id of the yarn stack") Long id) {
        try {
            StackResponse response = null;
            if (id != null) {
                response = cloudbreakClient.stackEndpoint().get(id);
            } else if (name != null) {
                response = cloudbreakClient.stackEndpoint().getPublic(name);
            }
            if (response == null || !BYOS.equals(response.getPlatformVariant())) {
                return "No yarn stack specified (select a yarn stack by --id or --name)";
            } else {
                return shellContext.outputTransformer().render(responseTransformer.transformObjectToStringMap(response.getOrchestrator()), "FIELD", "VALUE");
            }
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "yarn stack select", help = "Select a yarn stack")
    public String selectYarnStack(
            @CliOption(key = "name", help = "Name of the yarn stack") String name,
            @CliOption(key = "id", help = "Id of the yarn stack") Long id) {
        try {
            StackResponse response = null;
            if (id != null) {
                response = cloudbreakClient.stackEndpoint().get(id);
            } else if (name != null) {
                response = cloudbreakClient.stackEndpoint().getPublic(name);
            }
            if (response == null) {
                return "Yarn stack not exist or not a yarn stack was specified.";
            } else {
                shellContext.setSelectedYarnStackId(response.getId());
                shellContext.setSelectedYarnStackName(response.getName());
                shellContext.resetYarnHostGroups();
                shellContext.setFocus(response.getName(), FocusType.YARN);
                shellContext.setHint(Hints.SELECT_BLUEPRINT);
                return "Yarn stack selected with id: " + response.getId();
            }
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "yarn stack list", help = "List of yarn stacks")
    public String listYarnStack() {
        try {
            Set<StackResponse> responses = new HashSet<>();
            for (StackResponse aPublic : cloudbreakClient.stackEndpoint().getPublics()) {
                if (BYOS.equals(aPublic.getPlatformVariant())) {
                    responses.add(aPublic);
                }
            }
            return shellContext.outputTransformer().render(responseTransformer.transformToMap(responses, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "yarn stack delete", help = "Terminate a yarn stack")
    public String deleteYarnStack(
            @CliOption(key = "name", help = "Name of the yarn stack") String name,
            @CliOption(key = "id", help = "Id of the yarn stack") Long id) {
        try {
            if (id != null) {
                cloudbreakClient.stackEndpoint().delete(id, true, false);
                if (Objects.equals(id, shellContext.getSelectedYarnStackId())) {
                    shellContext.resetSelectedYarnStackId();
                    shellContext.setHint(Hints.YARN_CLUSTER);
                }
                return String.format("Yarn stack has been deleted, id: %s", id);
            } else if (name != null) {
                StackResponse aPublic = cloudbreakClient.stackEndpoint().getPublic(name);
                cloudbreakClient.stackEndpoint().deletePublic(name, true, false);
                if (Objects.equals(aPublic.getId(), shellContext.getSelectedYarnStackId())) {
                    shellContext.resetSelectedYarnStackId();
                }
                return String.format("Yarn has been deleted, name: %s", name);
            }
            return "No yarn stack was specified.";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "yarn constraint create", help = "Create a new yarn constraint")
    public String createYarnTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the yarn constraint") String name,
            @CliOption(key = "cores", mandatory = true, help = "Cpu cores of the yarn constraint (0.1 - 64 core)") Double cpuCores,
            @CliOption(key = "memory", mandatory = true, help = "Memory in Mb of the yarn constraint (16mb - 128Gb)") Double memory,
            @CliOption(key = "diskSize", mandatory = true, help = "Disk in Gb of the yarn constraint (10Gb - 1000Gb)") Double disk,
            @CliOption(key = "description", help = "Description of the yarn stack") String description,
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
            constraintTemplateRequest.setOrchestratorType(OrchestratorConstants.YARN);
            if (publicInAccount) {
                id = cloudbreakClient.constraintTemplateEndpoint().postPublic(constraintTemplateRequest).getId();
            } else {
                id = cloudbreakClient.constraintTemplateEndpoint().postPrivate(constraintTemplateRequest).getId();
            }
            return "Yarn template was created with id: " + id;
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "yarn constraint list", help = "Shows the currently available yarn constraints")
    public String listYarnTemplates() {
        try {
            Set<ConstraintTemplateResponse> publics = cloudbreakClient.constraintTemplateEndpoint().getPublics();
            shellContext.setConstraints(publics);
            return shellContext.outputTransformer().render(responseTransformer.transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "yarn constraint delete", help = "Delete the yarn constraint by its id or name")
    public Object deleteYarnTemplate(
            @CliOption(key = "id", help = "Id of the yarn template") String id,
            @CliOption(key = "name", help = "Name of the yarn template") String name) {
        try {
            if (id != null) {
                cloudbreakClient.constraintTemplateEndpoint().delete(Long.valueOf(id));
                shellContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
                return String.format("Yarn constraint has been deleted, id: %s", id);
            } else if (name != null) {
                cloudbreakClient.constraintTemplateEndpoint().deletePublic(name);
                shellContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
                return String.format("Yarn constraint has been deleted, name: %s", name);
            }
            return "No constraint specified.";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "yarn constraint show", help = "Shows the yarn constraint by its id or name")
    public Object showTemplate(
            @CliOption(key = "id", help = "Id of the yarn constraint") Long id,
            @CliOption(key = "name", help = "Name of the yarn constraint") String name) {
        try {
            ConstraintTemplateResponse aPublic = getConstraintTemplateResponse(id, name);
            if (aPublic != null) {
                return shellContext.outputTransformer().render(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
            }
            return "No constraint was found.";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    private ConstraintTemplateResponse getConstraintTemplateResponse(Long id, String name) {
        if (id != null) {
            return cloudbreakClient.constraintTemplateEndpoint().get(id);
        } else {
            return cloudbreakClient.constraintTemplateEndpoint().getPublic(name);
        }
    }

    @CliCommand(value = "yarn hostgroup configure", help = "configure hostgroups")
    public String createHostGroup(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostgroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Count of the nodes in the hostgroup") Integer nodecount,
            @CliOption(key = "constraintName", mandatory = true, help = "Name of the constraint") ConstraintName constraintTemplateName) {
        try {
            ConstraintTemplateResponse constraintTemplateResponse = getConstraintTemplateResponse(null, constraintTemplateName.getName());
            if (constraintTemplateResponse != null) {
                shellContext.putYarnHostGroup(hostgroup.getName(), new YarnHostgroupEntry(nodecount, constraintTemplateName.getName()));
                int yarnHostGroupsConfigured = shellContext.getYarnHostGroups().size();
                int totalHostGroupsToConfigure = shellContext.getActiveHostGroups().size();
                if (yarnHostGroupsConfigured == totalHostGroupsToConfigure) {
                    shellContext.setHint(Hints.YARN_CLUSTER);
                }
                return shellContext.outputTransformer().render(shellContext.getYarnHostGroups(), "hostgroup");
            } else {
                return "Constraint was not found.";
            }
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "yarn hostgroup list", help = "list hostgroups")
    public String listHostGroup() {
        try {
            return shellContext.outputTransformer().render(shellContext.getYarnHostGroups(), "hostgroup");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }


}
