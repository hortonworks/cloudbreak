package com.sequenceiq.cloudbreak.shell.commands.marathon;

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
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.shell.completion.ConstraintName;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.FocusType;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.MarathonHostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class MarathonCommands implements CommandMarker {

    private static final String BYOS = "BYOS";
    @Inject
    private ShellContext shellContext;
    @Inject
    private CloudbreakClient cloudbreakClient;
    @Inject
    private ExceptionTransformer exceptionTransformer;
    @Inject
    private ResponseTransformer responseTransformer;

    @CliAvailabilityIndicator({ "mode" })
    public boolean isHintCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator({ "marathon" })
    public boolean isMarathonHintCommandAvailable() {
        return shellContext.isMarathonMode();
    }

    @CliCommand(value = "mode --MARATHON", help = "Change to marathon mode")
    public void marathonMode() {
        if (shellContext.getSelectedMarathonStackName() == null) {
            shellContext.setFocus(null, FocusType.MARATHON);
        } else {
            shellContext.setFocus(shellContext.getSelectedMarathonStackName(), FocusType.MARATHON);
        }
        shellContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
        shellContext.setHint(Hints.MARATHON_STACK);
    }

    @CliCommand(value = "mode --DEFAULT", help = "Change to Marathon mode")
    public void rootMode() {
        shellContext.resetFocus();
        shellContext.setHint(Hints.CREATE_CREDENTIAL);
    }

    @CliCommand(value = "marathon import", help = "Import a marathon stack")
    public String createMarathonStack(
            @CliOption(key = "name", mandatory = true, help = "Name of the marathon stack") String name,
            @CliOption(key = "marathonEndpoint", mandatory = true, help = "Endpoint of the marathon") String marathonEndpoint) {
        try {
            StackRequest stackRequest = new StackRequest();
            OrchestratorRequest orchestratorRequest = new OrchestratorRequest();
            orchestratorRequest.setApiEndpoint(marathonEndpoint);
            orchestratorRequest.setType("MARATHON");
            stackRequest.setName(name);
            stackRequest.setOrchestrator(orchestratorRequest);
            return String.format("Marathon stack imported with id: '%d' and name: '%s'",
                    cloudbreakClient.stackEndpoint().postPublic(stackRequest).getId(), name);
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon show", help = "Show a marathon stack")
    public String showMarathonStack(
            @CliOption(key = "name", help = "Name of the marathon stack") String name,
            @CliOption(key = "id", help = "Id of the marathon stack") Long id) {
        try {
            StackResponse response = null;
            if (id != null) {
                response = cloudbreakClient.stackEndpoint().get(id);
            } else if (name != null) {
                response = cloudbreakClient.stackEndpoint().getPublic(name);
            }
            if (response == null || !BYOS.equals(response.getPlatformVariant())) {
                return "No marathon stack specified (select a marathon stack by --id or --name)";
            } else {
                return shellContext.outputTransformer().render(responseTransformer.transformObjectToStringMap(response.getOrchestrator()), "FIELD", "VALUE");
            }
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon select", help = "Select a marathon stack")
    public String selectMarathonStack(
            @CliOption(key = "name", help = "Name of the marathon stack") String name,
            @CliOption(key = "id", help = "Id of the marathon stack") Long id) {
        try {
            StackResponse response = null;
            if (id != null) {
                response = cloudbreakClient.stackEndpoint().get(id);
            } else if (name != null) {
                response = cloudbreakClient.stackEndpoint().getPublic(name);
            }
            if (response == null) {
                return "Marathon stack not exist or not a marathon stack was specified.";
            } else {
                shellContext.setSelectedMarathonStackId(response.getId());
                shellContext.setSelectedMarathonStackName(response.getName());
                shellContext.resetMarathonHostGroups();
                shellContext.setFocus(response.getName(), FocusType.MARATHON);
                shellContext.setHint(Hints.SELECT_BLUEPRINT);
                return "Marathon stack selected with id: " + response.getId();
            }
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon list", help = "List of marathon stacks")
    public String listMarathonStack() {
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

    @CliCommand(value = "marathon terminate", help = "Terminate a marathon stack")
    public String deleteMarathonStack(
            @CliOption(key = "name", help = "Name of the marathon stack") String name,
            @CliOption(key = "id", help = "Id of the marathon stack") Long id) {
        try {
            if (id != null) {
                cloudbreakClient.stackEndpoint().delete(id, true);
                if (Objects.equals(id, shellContext.getSelectedMarathonStackId())) {
                    shellContext.resetSelectedMarathonStackId();
                    shellContext.setHint(Hints.MARATHON_CLUSTER);
                }
                return String.format("Marathon stack has been deleted, id: %s", id);
            } else if (name != null) {
                StackResponse aPublic = cloudbreakClient.stackEndpoint().getPublic(name);
                cloudbreakClient.stackEndpoint().deletePublic(name, true);
                if (Objects.equals(aPublic.getId(), shellContext.getSelectedMarathonStackId())) {
                    shellContext.resetSelectedMarathonStackId();
                }
                return String.format("Marathon has been deleted, name: %s", name);
            }
            return "No marathon stack was specified.";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon constraint create", help = "Create a new marathon constraint")
    public String createMarathonTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the marathon constraint") String name,
            @CliOption(key = "cores", mandatory = true, help = "Cpu cores of the marathon constraint (0.1 - 64 core)") Double cpuCores,
            @CliOption(key = "memory", mandatory = true, help = "Memory in Mb of the marathon constraint (16mb - 128Gb)") Double memory,
            @CliOption(key = "diskSize", mandatory = true, help = "Disk in Gb of the marathon constraint (10Gb - 1000Gb)") Double disk,
            @CliOption(key = "description", help = "Description of the marathon stack") String description,
            @CliOption(key = "publicInAccount", help = "flags if the constraint is public in the account") Boolean publicInAccount) {
        IdJson idJson;
        try {
            ConstraintTemplateRequest constraintTemplateRequest = new ConstraintTemplateRequest();
            constraintTemplateRequest.setName(name);
            constraintTemplateRequest.setCpu(cpuCores);
            constraintTemplateRequest.setDescription(description);
            constraintTemplateRequest.setDisk(disk);
            constraintTemplateRequest.setMemory(memory);
            constraintTemplateRequest.setOrchestratorType(OrchestratorConstants.MARATHON);
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (publicInAccount) {
                idJson = cloudbreakClient.constraintTemplateEndpoint().postPublic(constraintTemplateRequest);
            } else {
                idJson = cloudbreakClient.constraintTemplateEndpoint().postPrivate(constraintTemplateRequest);
            }
            return "Marathon template was created with id: " + idJson.getId();
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon constraint list", help = "Shows the currently available marathon constraints")
    public String listMarathonTemplates() {
        try {
            Set<ConstraintTemplateResponse> publics = cloudbreakClient.constraintTemplateEndpoint().getPublics();
            shellContext.setConstraints(publics);
            return shellContext.outputTransformer().render(responseTransformer.transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon constraint delete", help = "Delete the marathon constraint by its id or name")
    public Object deleteMarathonTemplate(
            @CliOption(key = "id", help = "Id of the marathon template") String id,
            @CliOption(key = "name", help = "Name of the marathon template") String name) {
        try {
            if (id != null) {
                cloudbreakClient.constraintTemplateEndpoint().delete(Long.valueOf(id));
                shellContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
                return String.format("Marathon constraint has been deleted, id: %s", id);
            } else if (name != null) {
                cloudbreakClient.constraintTemplateEndpoint().deletePublic(name);
                shellContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
                return String.format("Marathon constraint has been deleted, name: %s", name);
            }
            return "No constraint specified.";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "marathon constraint show", help = "Shows the marathon constraint by its id or name")
    public Object showTemplate(
            @CliOption(key = "id", help = "Id of the marathon constraint") Long id,
            @CliOption(key = "name", help = "Name of the marathon constraint") String name) {
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

    @CliCommand(value = "marathon hostgroup configure", help = "Configure hostgroups")
    public String createHostGroup(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostgroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Count of the nodes in the hostgroup") Integer nodecount,
            @CliOption(key = "constraintName", mandatory = true, help = "Name of the constraint") ConstraintName constraintTemplateName) {
        try {
            ConstraintTemplateResponse constraintTemplateResponse = getConstraintTemplateResponse(null, constraintTemplateName.getName());
            if (constraintTemplateResponse != null) {
                shellContext.putMarathonHostGroup(hostgroup.getName(), new MarathonHostgroupEntry(nodecount, constraintTemplateName.getName()));
                if (shellContext.getHostGroups().size() == shellContext.getActiveHostGroups().size()) {
                    shellContext.setHint(Hints.MARATHON_CLUSTER);
                }
                return shellContext.outputTransformer().render(shellContext.getHostGroups(), "hostgroup");
            } else {
                return "Constraint was not found.";
            }
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon hostgroup show", help = "Show hostgroups")
    public String listHostGroup() {
        try {
            return shellContext.outputTransformer().render(shellContext.getHostGroups(), "hostgroup");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }


}
