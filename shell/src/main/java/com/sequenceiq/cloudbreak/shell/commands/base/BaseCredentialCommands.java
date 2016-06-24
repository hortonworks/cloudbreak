package com.sequenceiq.cloudbreak.shell.commands.base;

import static com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class BaseCredentialCommands implements BaseCommands, CredentialCommands {

    private static final String FILE_NOT_FOUND = "File not found with ssh key.";
    private static final String URL_NOT_FOUND = "Url not Available for ssh key.";
    private static final String CREATE_SUCCESS_MESSAGE = "Credential created with id: '%d' and name: '%s'";

    private ShellContext shellContext;

    public BaseCredentialCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @Override
    @CliAvailabilityIndicator(value = { "credential delete --id", "credential delete --name" })
    public boolean deleteAvailable() {
        return !shellContext.isMarathonMode();
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
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    @CliAvailabilityIndicator(value = { "credential select --id", "credential select --name" })
    public boolean selectAvailable() {
        return shellContext.isCredentialAccessible() && !shellContext.isMarathonMode();
    }

    @CliCommand(value = "credential select --id", help = "Delete the credential by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "credential select --name", help = "Delete the credential by its name")
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
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    @CliAvailabilityIndicator(value = "credential list")
    public boolean listAvailable() {
        return !shellContext.isMarathonMode();
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
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "credential show --id", help = "Show the credential by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "credential show --name", help = "Show the credential by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @Override
    public String show(Long id, String name) {
        try {
            if (id != null) {
                CredentialResponse credentialResponse = shellContext.cloudbreakClient().credentialEndpoint().get(id);
                Map<String, String> map = shellContext.responseTransformer().transformObjectToStringMap(credentialResponse);
                return shellContext.outputTransformer().render(map, "FIELD", "VALUE");
            } else if (name != null) {
                CredentialResponse aPublic = shellContext.cloudbreakClient().credentialEndpoint().getPublic(name);
                if (aPublic != null) {
                    return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public boolean createCredentialAvailable(String platform) {
        return !shellContext.isMarathonMode();
    }

    @Override
    public String create(String name, File sshKeyPath, String sshKeyUrl, String sshKeyString, String description, Boolean publicInAccount, Long platformId,
            Map<String, Object> parameters, String platform) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty()) && sshKeyString == null) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl or --sshKeyString";
        }
        String sshKey;
        if (sshKeyPath != null) {
            try {
                sshKey = IOUtils.toString(new FileReader(new File(sshKeyPath.getPath())));
            } catch (IOException ex) {
                throw shellContext.exceptionTransformer().transformToRuntimeException(FILE_NOT_FOUND);
            }
        } else if (sshKeyUrl != null) {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException ex) {
                throw shellContext.exceptionTransformer().transformToRuntimeException(URL_NOT_FOUND);
            }
        } else {
            sshKey = sshKeyString;
        }
        try {

            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform(platform);
            credentialRequest.setPublicKey(sshKey);
            credentialRequest.setParameters(parameters);

            if (platformId != null) {
                checkTopologyForResource(shellContext.cloudbreakClient().topologyEndpoint().getPublics(), platformId, platform);
            }
            credentialRequest.setTopologyId(platformId);
            IdJson id;
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().credentialEndpoint().postPublic(credentialRequest);
            } else {
                id = shellContext.cloudbreakClient().credentialEndpoint().postPrivate(credentialRequest);
            }
            shellContext.setCredential(id.getId().toString());
            createOrSelectTemplateHint();
            return String.format(CREATE_SUCCESS_MESSAGE, id.getId(), name);
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

    protected String readUrl(String url) throws IOException {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        String str;
        StringBuilder sb = new StringBuilder();
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }
        in.close();
        return sb.toString();
    }
}
