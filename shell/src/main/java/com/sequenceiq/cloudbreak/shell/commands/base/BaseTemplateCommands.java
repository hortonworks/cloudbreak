package com.sequenceiq.cloudbreak.shell.commands.base;

import static com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource;

import java.util.Map;
import java.util.Set;

import org.apache.http.MethodNotSupportedException;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class BaseTemplateCommands implements BaseCommands, TemplateCommands {

    private static final String CREATE_SUCCESS_MESSAGE = "Template created with id: '%d' and name: '%s'";

    private ShellContext shellContext;

    public BaseTemplateCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "template list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode();
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

    @CliAvailabilityIndicator(value = "template show")
    @Override
    public boolean showAvailable() {
        return !shellContext.isMarathonMode();
    }

    @Override
    public String show(Long id, String name) throws Exception {
        try {
            if (id != null) {
                return shellContext.outputTransformer().render(shellContext.responseTransformer()
                        .transformObjectToStringMap(shellContext.cloudbreakClient().templateEndpoint().get(id)), "FIELD", "VALUE");
            } else if (name != null) {
                TemplateResponse aPublic = shellContext.cloudbreakClient().templateEndpoint().getPublic(name);
                if (aPublic != null) {
                    return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            return "No template specified.";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "template show --id", help = "Shows the template by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "template show --name", help = "Shows the template by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
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

    @CliAvailabilityIndicator(value = "template delete")
    @Override
    public boolean deleteAvailable() {
        return !shellContext.isMarathonMode();
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
            return "No template specified.";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "template delete --id", help = "Shows the template by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "template delete --name", help = "Shows the template by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public boolean createTemplateAvailable(String platform) {
        return !shellContext.isMarathonMode();
    }

    @Override
    public String create(String name, String instanceType, Integer volumeCount, Integer volumeSize, String volumeType, Boolean publicInAccount,
            String description, Map<String, Object> parameters, Long platformId, String platform) {
        publicInAccount = publicInAccount == null ? false : publicInAccount;

        try {
            IdJson id;
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

            if (publicInAccount) {
                id = shellContext.cloudbreakClient().templateEndpoint().postPublic(templateRequest);
            } else {
                id = shellContext.cloudbreakClient().templateEndpoint().postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return String.format(CREATE_SUCCESS_MESSAGE, id.getId(), name);
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
                && shellContext.getActiveHostGroups().size() != 0)) {
            shellContext.setHint(Hints.CREATE_STACK);
        } else if (shellContext.isStackAccessible()) {
            shellContext.setHint(Hints.CREATE_STACK);
        } else {
            shellContext.setHint(Hints.NONE);
        }
    }

}
