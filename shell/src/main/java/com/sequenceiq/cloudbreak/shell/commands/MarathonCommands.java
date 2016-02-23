package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderObjectValueMap;
import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.shell.completion.ConstraintName;
import com.sequenceiq.cloudbreak.shell.completion.HostGroup;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.FocusType;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.MarathonContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class MarathonCommands implements CommandMarker {

    private static final String BYOS = "BYOS";
    @Inject
    private CloudbreakContext context;
    @Inject
    private MarathonContext marathonContext;
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
        return context.isMarathonMode();
    }

    @CliCommand(value = "mode --MARATHON", help = "Change to marathon mode")
    public void marathonMode() {
        if (marathonContext.getSelectedMarathonStackName() == null) {
            context.setFocus(null, FocusType.MARATHON);
        } else {
            context.setFocus(marathonContext.getSelectedMarathonStackName(), FocusType.MARATHON);
        }
        marathonContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
        context.setHint(Hints.MARATHON_STACK);
    }

    @CliCommand(value = "mode --DEFAULT", help = "Change to Marathon mode")
    public void rootMode() {
        context.resetFocus();
        context.setHint(Hints.CREATE_CREDENTIAL);
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
            return String.format("Marathon stack imported with id: %s", cloudbreakClient.stackEndpoint().postPublic(stackRequest).getId());
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon show", help = "Show a marathon stack")
    public String showMarathonStack(
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon stack") String name,
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon stack") Long id) {
        try {
            StackResponse response = null;
            if (id != null) {
                response = cloudbreakClient.stackEndpoint().get(Long.valueOf(id));
            } else if (name != null) {
                response = cloudbreakClient.stackEndpoint().getPublic(name);
            }
            if (response == null || !BYOS.equals(response.getPlatformVariant())) {
                return "No marathon stack specified (select a marathon stack by --id or --name)";
            } else {
                return renderSingleMap(responseTransformer.transformObjectToStringMap(response.getOrchestrator()), "FIELD", "VALUE");
            }
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon select", help = "Select a marathon stack")
    public String selectMarathonStack(
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon stack") String name,
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon stack") Long id) {
        try {
            StackResponse response = null;
            if (id != null) {
                response = cloudbreakClient.stackEndpoint().get(Long.valueOf(id));
            } else if (name != null) {
                response = cloudbreakClient.stackEndpoint().getPublic(name);
            }
            if (response == null) {
                if (!BYOS.equals(response.getPlatformVariant())) {
                    return "Not a marathon stack was specified.";
                }
                return "Marathon stack not exist.";
            } else {
                marathonContext.setSelectedMarathonStackId(response.getId());
                marathonContext.setSelectedMarathonStackName(response.getName());
                marathonContext.resetHostGroups();
                context.setFocus(response.getName(), FocusType.MARATHON);
                context.setHint(Hints.SELECT_BLUEPRINT);
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
            return renderSingleMap(responseTransformer.transformToMap(responses, "id", "name"), true, "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon terminate", help = "Terminate a marathon stack")
    public String deleteMarathonStack(
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon stack") String name,
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon stack") Long id) {
        try {
            if (id != null) {
                cloudbreakClient.stackEndpoint().delete(Long.valueOf(id), true);
                if (id == marathonContext.getSelectedMarathonStackId()) {
                    marathonContext.resetSelectedMarathonStackId();
                    context.setHint(Hints.MARATHON_CLUSTER);
                }
                return String.format("Marathon stack has been deleted, id: %s", id);
            } else if (name != null) {
                StackResponse aPublic = cloudbreakClient.stackEndpoint().getPublic(name);
                cloudbreakClient.stackEndpoint().deletePublic(name, true);
                if (aPublic.getId() == marathonContext.getSelectedMarathonStackId()) {
                    marathonContext.resetSelectedMarathonStackId();
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
            @CliOption(key = "description", mandatory = false, help = "Description of the marathon stack") String description,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the constraint is public in the account") Boolean publicInAccount) {
        IdJson idJson = null;
        try {
            ConstraintTemplateRequest constraintTemplateRequest = new ConstraintTemplateRequest();
            constraintTemplateRequest.setName(name);
            constraintTemplateRequest.setCpu(cpuCores);
            constraintTemplateRequest.setDescription(description);
            constraintTemplateRequest.setDisk(disk);
            constraintTemplateRequest.setMemory(memory);
            constraintTemplateRequest.setPublicInAccount(publicInAccount == null ? false : publicInAccount);
            if (constraintTemplateRequest.isPublicInAccount()) {
                idJson = cloudbreakClient.constraintTemplateEndpoint().postPublic(constraintTemplateRequest);
            } else {
                idJson = cloudbreakClient.constraintTemplateEndpoint().postPrivate(constraintTemplateRequest);
            }
        } catch (Exception ex) {
            exceptionTransformer.transformToRuntimeException(ex);
        }
        return "Marathon template was created with id: " + idJson.getId();
    }

    @CliCommand(value = "marathon constraint list", help = "Shows the currently available marathon constraints")
    public String listMarathonTemplates() {
        try {
            Set<ConstraintTemplateResponse> publics = cloudbreakClient.constraintTemplateEndpoint().getPublics();
            marathonContext.setConstraints(publics);
            return renderSingleMap(responseTransformer.transformToMap(publics, "id", "name"), true, "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon constraint delete", help = "Delete the marathon constraint by its id or name")
    public Object deleteMarathonTemplate(
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon template") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon template") String name) {
        try {
            if (id != null) {
                cloudbreakClient.constraintTemplateEndpoint().delete(Long.valueOf(id));
                marathonContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
                return String.format("Marathon constraint has been deleted, id: %s", id);
            } else if (name != null) {
                cloudbreakClient.constraintTemplateEndpoint().deletePublic(name);
                marathonContext.setConstraints(cloudbreakClient.constraintTemplateEndpoint().getPublics());
                return String.format("Marathon constraint has been deleted, name: %s", name);
            }
            return "No constraint specified.";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "marathon constraint show", help = "Shows the marathon constraint by its id or name")
    public Object showTemplate(
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon constraint") Long id,
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon constraint") String name) {
        try {
            ConstraintTemplateResponse aPublic = getConstraintTemplateResponse(id, name);
            if (aPublic != null) {
                return renderSingleMap(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
            }
            return "No constraint was found.";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    private ConstraintTemplateResponse getConstraintTemplateResponse(Long id, String name) {
        if (id != null) {
            return cloudbreakClient.constraintTemplateEndpoint().get(Long.valueOf(id));
        } else {
            return cloudbreakClient.constraintTemplateEndpoint().getPublic(name);
        }
    }

    @CliCommand(value = "marathon hostgroup configure", help = "Configure hostgroups")
    public String createHostGroup(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") HostGroup hostgroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Count of the nodes in the hostgroup") Integer nodecount,
            @CliOption(key = "constraintName", mandatory = true, help = "Name of the constraint") ConstraintName constraintTemplateName)
            throws Exception {
        try {
            ConstraintTemplateResponse constraintTemplateResponse = getConstraintTemplateResponse(null, constraintTemplateName.getName());
            if (constraintTemplateResponse != null) {
                marathonContext.putHostGroup(hostgroup.getName(), new HostgroupEntry(nodecount, constraintTemplateName.getName()));
                if (marathonContext.getHostGroups().size() == context.getActiveHostGroups().size()) {
                    context.setHint(Hints.MARATHON_CLUSTER);
                }
                return renderObjectValueMap(marathonContext.getHostGroups(), "hostgroup");
            } else {
                return "Constraint was not found.";
            }
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "marathon hostgroup show", help = "Show hostgroups")
    public String listHostGroup() throws Exception {
        try {
            return renderObjectValueMap(marathonContext.getHostGroups(), "hostgroup");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }


}
