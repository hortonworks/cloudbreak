package com.sequenceiq.cloudbreak.shell.commands.base;

import static com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource;

import java.util.Map;
import java.util.Set;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class BaseCredentialCommands implements BaseCommands, CredentialCommands {

    private static final String CREATE_SUCCESS_MESSAGE = "Credential created with id: '%d' and name: '%s'";

    private ShellContext shellContext;

    public BaseCredentialCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @Override
    @CliAvailabilityIndicator(value = { "credential delete --id", "credential delete --name" })
    public boolean deleteAvailable() {
        return true;
    }

    @CliCommand(value = "credential delete --id", help = "Delete the credential by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "credential delete --name", help = "Delete the credential by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public String delete(Long id, String name) {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().credentialEndpoint().delete(id);
                return String.format("Credential deleted, id: %s", id);
            } else if (name != null) {
                shellContext.cloudbreakClient().credentialEndpoint().deletePublic(name);
                return String.format("Credential deleted, name: %s", name);
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No credential specified (select a credential by --id or --name)");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    @CliAvailabilityIndicator(value = { "credential select --id", "credential select --name" })
    public boolean selectAvailable() {
        return true;
    }

    @CliCommand(value = "credential select --id", help = "Select the credential by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "credential select --name", help = "Select the credential by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return select(null, name);
    }

    @Override
    public String select(Long id, String name) {
        try {
            if (id != null) {
                if (shellContext.cloudbreakClient().credentialEndpoint().get(id) != null) {
                    shellContext.setCredential(id.toString());
                    createOrSelectTemplateHint();
                    return "Credential selected, id: " + id;
                }
            } else if (name != null) {
                CredentialResponse aPublic = shellContext.cloudbreakClient().credentialEndpoint().getPublic(name);
                shellContext.setCredential(aPublic.getId().toString());
                createOrSelectTemplateHint();
                return "Credential selected, name: " + name;
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No credential specified (select a credential by --id or --name)");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    @CliAvailabilityIndicator(value = "credential list")
    public boolean listAvailable() {
        return true;
    }

    @Override
    @CliCommand(value = "credential list", help = "Shows all of your credentials")
    public String list() {
        try {
            Set<CredentialResponse> publics = shellContext.cloudbreakClient().credentialEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    @CliAvailabilityIndicator(value = { "credential show --id", "credential show --name" })
    public boolean showAvailable() {
        return true;
    }

    @CliCommand(value = "credential show --id", help = "Show the credential by its id")
    @Override
    public String showById(
            @CliOption(key = "", mandatory = true) Long id,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(id, null, outPutType);
    }

    @CliCommand(value = "credential show --name", help = "Show the credential by its name")
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
            if (id != null) {
                CredentialResponse credentialResponse = shellContext.cloudbreakClient().credentialEndpoint().get(id);
                Map<String, String> map = shellContext.responseTransformer().transformObjectToStringMap(credentialResponse);
                return shellContext.outputTransformer().render(outPutType, map, "FIELD", "VALUE");
            } else if (name != null) {
                CredentialResponse aPublic = shellContext.cloudbreakClient().credentialEndpoint().getPublic(name);
                if (aPublic != null) {
                    return shellContext.outputTransformer().render(outPutType,
                            shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No credential specified (select a credential by --id or --name)");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public boolean createCredentialAvailable(String platform) {
        return true;
    }

    @Override
    public String create(String name, String description, boolean publicInAccount, Long platformId,
            Map<String, Object> parameters, String platform) {
        try {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform(platform);
            credentialRequest.setParameters(parameters);

            if (platformId != null) {
                checkTopologyForResource(shellContext.cloudbreakClient().topologyEndpoint().getPublics(), platformId, platform);
            }
            credentialRequest.setTopologyId(platformId);
            Long id;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().credentialEndpoint().postPublic(credentialRequest).getId();
            } else {
                id = shellContext.cloudbreakClient().credentialEndpoint().postPrivate(credentialRequest).getId();
            }
            shellContext.setCredential(id.toString());
            createOrSelectTemplateHint();
            return String.format(CREATE_SUCCESS_MESSAGE, id, name);
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }

    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    protected void createOrSelectTemplateHint() {
        if (shellContext.cloudbreakClient().credentialEndpoint().getPublics().isEmpty()) {
            shellContext.setHint(Hints.ADD_BLUEPRINT);
        } else {
            shellContext.setHint(Hints.SELECT_BLUEPRINT);
        }
    }
}
