package com.sequenceiq.cloudbreak.shell.commands.base;

import static com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource;

import java.util.Map;
import java.util.Set;

import org.apache.http.MethodNotSupportedException;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class BaseTemplateCommands implements BaseCommands, TemplateCommands {

    private static final String CREATE_SUCCESS_MESSAGE = "Template created with id: '%d' and name: '%s'";

    private ShellContext shellContext;

    public BaseTemplateCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator("template list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "template list", help = "Shows the currently available cloud templates")
    @Override
    public String list() throws Exception {
        try {
            Set<TemplateResponse> publics = shellContext.cloudbreakClient().templateEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator("template show")
    @Override
    public boolean showAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String show(Long id, String name, OutPutType outPutType) throws Exception {
        try {
            outPutType = outPutType == null ? OutPutType.RAW : outPutType;
            if (id != null) {
                return shellContext.outputTransformer().render(outPutType, shellContext.responseTransformer()
                        .transformObjectToStringMap(shellContext.cloudbreakClient().templateEndpoint().get(id)), "FIELD", "VALUE");
            } else if (name != null) {
                TemplateResponse aPublic = shellContext.cloudbreakClient().templateEndpoint().getPublic(name);
                if (aPublic != null) {
                    return shellContext.outputTransformer()
                            .render(outPutType, shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No template specified");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "template show --id", help = "Shows the template by its id")
    @Override
    public String showById(
            @CliOption(key = "", mandatory = true) Long id,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(id, null, outPutType);
    }

    @CliCommand(value = "template show --name", help = "Shows the template by its name")
    @Override
    public String showByName(
            @CliOption(key = "", mandatory = true) String name,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(null, name, outPutType);
    }

    @Override
    public boolean selectAvailable() {
        return false;
    }

    @Override
    public String select(Long id, String name) throws Exception {
        throw new MethodNotSupportedException("Select is not supported on templates");
    }

    @Override
    public String selectById(Long id) throws Exception {
        return select(id, null);
    }

    @Override
    public String selectByName(String name) throws Exception {
        return select(null, name);
    }

    @CliAvailabilityIndicator("template delete")
    @Override
    public boolean deleteAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String delete(Long id, String name) throws Exception {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().templateEndpoint().delete(id);
                return String.format("Template has been deleted, id: %s", id);
            } else if (name != null) {
                shellContext.cloudbreakClient().templateEndpoint().deletePublic(name);
                return String.format("Template has been deleted, name: %s", name);
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No template specified");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "template delete --id", help = "Deletes the template by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "template delete --name", help = "Deletes the template by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public boolean createTemplateAvailable(String platform) {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String create(String name, String instanceType, Integer volumeCount, Integer volumeSize, String volumeType, boolean publicInAccount,
            String description, Map<String, Object> parameters, Long platformId, String platform) {
        try {
            Long id;
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform(platform);
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(instanceType);
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);
            templateRequest.setVolumeType(volumeType);
            templateRequest.setParameters(parameters);
            if (platformId != null) {
                checkTopologyForResource(shellContext.cloudbreakClient().topologyEndpoint().getPublics(), platformId, platform);
            }
            templateRequest.setTopologyId(platformId);

            TemplateResponse templateResponse;
            if (publicInAccount) {
                templateResponse = shellContext.cloudbreakClient().templateEndpoint().postPublic(templateRequest);
            } else {
                templateResponse = shellContext.cloudbreakClient().templateEndpoint().postPrivate(templateRequest);
            }
            shellContext.putTemplate(templateResponse);
            createOrSelectBlueprintHint();
            return String.format(CREATE_SUCCESS_MESSAGE, templateResponse.getId(), name);
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    private void createOrSelectBlueprintHint() {
        if (shellContext.isCredentialAccessible() && shellContext.isBlueprintAccessible()) {
            shellContext.setHint(Hints.CONFIGURE_INSTANCEGROUP);
        } else if (!shellContext.isBlueprintAccessible()) {
            shellContext.setHint(Hints.SELECT_BLUEPRINT);
        } else if (!shellContext.isCredentialAccessible()) {
            shellContext.setHint(Hints.SELECT_CREDENTIAL);
        } else if (shellContext.isCredentialAvailable()
                && (shellContext.getActiveHostGroups().size() == shellContext.getInstanceGroups().size()
                && !shellContext.getActiveHostGroups().isEmpty()) || shellContext.isStackAccessible()) {
            shellContext.setHint(Hints.CREATE_STACK);
        } else {
            shellContext.setHint(Hints.NONE);
        }
    }

}
