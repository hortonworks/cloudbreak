package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.CredentialEndpoint;
import com.sequenceiq.cloudbreak.model.CredentialRequest;
import com.sequenceiq.cloudbreak.model.CredentialResponse;
import com.sequenceiq.cloudbreak.model.IdJson;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class CredentialCommands implements CommandMarker {

    private List<Map> maps = new ArrayList<>();

    @Autowired
    private CloudbreakContext context;
    @Autowired
    private CredentialEndpoint credentialEndpoint;
    @Autowired
    private ResponseTransformer responseTransformer;

    @CliAvailabilityIndicator(value = "credential list")
    public boolean isCredentialListCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "credential delete")
    public boolean isCredentialDeleteCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "credential show")
    public boolean isCredentialShowCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "credential select")
    public boolean isCredentialSelectCommandAvailable() throws Exception {
        return context.isCredentialAccessible();
    }

    @CliAvailabilityIndicator({ "credential create --GCP", "credential create --EC2", "credential create --AZURE", "credential create --OPENSTACK" })
    public boolean isCredentialEc2CreateCommandAvailable() {
        return true;
    }

    @CliCommand(value = "credential show", help = "Shows the credential by its id")
    public Object showCredential(
            @CliOption(key = "id", mandatory = false, help = "Id of the credential") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the credential") String name) {
        try {
            if (id != null) {
                return renderSingleMap(responseTransformer.transformObjectToStringMap(credentialEndpoint.get(Long.valueOf(id))), "FIELD", "VALUE");
            } else if (name != null) {
                CredentialResponse aPublic = credentialEndpoint.getPublic(name);
                if (aPublic != null) {
                    return renderSingleMap(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "credential delete", help = "Delete the credential by its id")
    public Object deleteCredential(
            @CliOption(key = "id", mandatory = false, help = "Id of the credental") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the credential") String name) {
        try {
            if (id != null) {
                credentialEndpoint.delete(Long.valueOf(id));
                return String.format("Credential deleted, id: %s", id);
            } else if (name != null) {
                credentialEndpoint.deletePublic(name);
                return String.format("Credential deleted, name: %s", name);
            }
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "credential list", help = "Shows all of your credentials")
    public String listCredentials() {
        try {
            return renderSingleMap(responseTransformer.transformToMap(credentialEndpoint.getPublics(), "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "credential select", help = "Select the credential by its id or name")
    public String selectCredential(
            @CliOption(key = "id", mandatory = false, help = "Id of the credential") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the credential") String name) {
        try {

            if (id != null) {
                if (credentialEndpoint.get(Long.valueOf(id)) != null) {
                    context.setCredential(id);
                    createOrSelectTemplateHint();
                    return "Credential selected, id: " + id;
                }
            } else if (name != null) {
                CredentialResponse aPublic = credentialEndpoint.getPublic(name);
                context.setCredential(aPublic.getId().toString());
                createOrSelectTemplateHint();
                return "Credential selected, name: " + name;
            }
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "credential create --OPENSTACK", help = "Create a new OPENSTACK credential")
    public String createOpenStackCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "userName", mandatory = true, help = "Username of the credential") String userName,
            @CliOption(key = "password", mandatory = true, help = "password of the credential") String password,
            @CliOption(key = "endPoint", mandatory = true, help = "endPoint of the credential") String endPoint,
            @CliOption(key = "tenantName", mandatory = false, help = "tenantName of the credential for cb-keystone-v2") String tenantName,
            @CliOption(key = "userDomain", mandatory = false, help = "userDomain of the credential for cb-keystone-v3*") String userDomain,
            @CliOption(key = "keystoneAuthScope", mandatory = false, help = "keystoneAuthScope of the credential for cb-keystone-v3*") String keystoneAuthScope,
            @CliOption(key = "domainName", mandatory = false, help = "domainName of the credential for cb-keystone-v3-default-scope") String domainName,
            @CliOption(key = "projectDomainName", mandatory = false, help = "projectDomainName of the credential for cb-keystone-v3-project-scope")
            String projectDomainName,
            @CliOption(key = "projectName", mandatory = false, help = "projectName of the credential for cb-keystone-v3-project-scope") String projectName,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key file") String sshKeyUrl,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") String description,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount
    ) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty())) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl";
        }
        String selector = null;
        String keyStoneVersion = null;
        if (tenantName != null) {
            selector = "cb-keystone-v2";
            keyStoneVersion = "cb-keystone-v2";
        }
        if (userDomain != null && keystoneAuthScope != null) {
            if (domainName != null) {
                selector = "cb-keystone-v3-domain-scope";
            } else if (projectDomainName != null && projectName != null) {
                selector = "cb-keystone-v3-project-scope";
            } else {
                selector = "cb-keystone-v3-default-scope";
            }
            keyStoneVersion = "cb-keystone-v3";
        }
        if (selector == null || keyStoneVersion == null) {
            return "Selector not found for specified parameters.";
        }
        String sshKey;
        if (sshKeyPath != null) {
            try {
                sshKey = new String(Files.readAllBytes(Paths.get(sshKeyPath.getPath()))).replaceAll("\n", "");
            } catch (IOException e) {
                return "File not found with ssh key.";
            }
        } else {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException e) {
                return "Url not found with ssh key.";
            }
        }

        try {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform("OPENSTACK");
            credentialRequest.setPublicKey(sshKey);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("userName", userName);
            parameters.put("password", password);
            parameters.put("endpoint", endPoint);
            parameters.put("keystoneVersion", keyStoneVersion);
            parameters.put("selector", selector);
            parameters.put("tenantName", tenantName);
            parameters.put("userDomain", userDomain);
            parameters.put("keystoneAuthScope", keystoneAuthScope);
            parameters.put("domainName", domainName);
            parameters.put("projectDomainName", projectDomainName);
            parameters.put("projectName", projectName);

            credentialRequest.setParameters(parameters);

            IdJson idJson;
            if (publicInAccount) {
                idJson = credentialEndpoint.postPublic(credentialRequest);
            } else {
                idJson = credentialEndpoint.postPrivate(credentialRequest);
            }
            context.setCredential(idJson.getId().toString());
            createOrSelectTemplateHint();
            return "Credential created, id: " + idJson.getId().toString();
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "credential create --EC2", help = "Create a new EC2 credential")
    public String createEc2Credential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "roleArn", mandatory = true, help = "roleArn of the credential") String roleArn,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key file") String sshKeyUrl,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description
    ) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty())) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl";
        }
        String sshKey;
        if (sshKeyPath != null) {
            try {
                sshKey = new String(Files.readAllBytes(Paths.get(sshKeyPath.getPath()))).replaceAll("\n", "");
            } catch (IOException e) {
                return "File not found with ssh key.";
            }
        } else {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException e) {
                return "Url not found with ssh key.";
            }
        }
        try {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform("AWS");
            credentialRequest.setPublicKey(sshKey);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("roleArn", roleArn);

            credentialRequest.setParameters(parameters);

            IdJson id;
            if (publicInAccount) {
                id = credentialEndpoint.postPublic(credentialRequest);
            } else {
                id = credentialEndpoint.postPrivate(credentialRequest);
            }
            context.setCredential(id.getId().toString());
            createOrSelectTemplateHint();
            return "Credential created, id: " + id.getId().toString();
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "credential create --GCP", help = "Create a new Gcp credential")
    public String createGcpCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "projectId", mandatory = true, help = "projectId of the credential") String projectId,
            @CliOption(key = "serviceAccountId", mandatory = true, help = "serviceAccountId of the credential") String serviceAccountId,
            @CliOption(key = "serviceAccountPrivateKeyPath", mandatory = true, help = "path of a service account private key (p12) file")
            File serviceAccountPrivateKeyPath,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key url") String sshKeyUrl,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") String description
    ) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty())) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl";
        }
        String sshKey;
        if (sshKeyPath != null) {
            try {
                sshKey = new String(Files.readAllBytes(Paths.get(sshKeyPath.getPath()))).replaceAll("\n", "");
            } catch (IOException e) {
                return "File not found with ssh key.";
            }
        } else {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException e) {
                return "Url not found with ssh key.";
            }
        }

        String serviceAccountPrivateKey;

        try {
            serviceAccountPrivateKey = Base64.encodeBase64String(Files.readAllBytes(serviceAccountPrivateKeyPath.toPath())).replaceAll("\n", "");
        } catch (IOException e) {
            return "File not found with service account private key (p12) file.";
        }

        try {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform("GCP");
            credentialRequest.setPublicKey(sshKey);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("projectId", projectId);
            parameters.put("serviceAccountId", serviceAccountId);
            parameters.put("serviceAccountPrivateKey", serviceAccountPrivateKey);

            credentialRequest.setParameters(parameters);

            IdJson id;
            if (publicInAccount) {
                id = credentialEndpoint.postPublic(credentialRequest);
            } else {
                id = credentialEndpoint.postPrivate(credentialRequest);
            }
            context.setCredential(id.getId().toString());
            createOrSelectTemplateHint();
            return "Credential created, id: " + id.getId().toString();
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private String readUrl(String url) throws IOException {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        String str;
        StringBuffer sb = new StringBuffer();
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }
        in.close();
        return sb.toString();
    }

    @CliCommand(value = "credential create --AZURE", help = "Create a new AZURE credential")
    public String createAzureRmCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "subscriptionId", mandatory = true, help = "subscriptionId of the credential") String subscriptionId,
            @CliOption(key = "tenantId", mandatory = true, help = "tenantId of the credential") String tenantId,
            @CliOption(key = "appId", mandatory = true, help = "appId of the credential") String appId,
            @CliOption(key = "password", mandatory = true, help = "password of the credential") String password,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "sshKeyPath of the template") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "sshKeyUrl of the template") String sshKeyUrl,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") String description
    ) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty())) {
            return "SshKey cannot be null if password null";
        }
        String sshKey;
        if (sshKeyPath != null) {
            try {
                sshKey = IOUtils.toString(new FileReader(new File(sshKeyPath.getPath())));
            } catch (IOException e) {
                return "File not found with ssh key.";
            }
        } else {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException e) {
                return "Url not found with ssh key.";
            }
        }
        try {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform("AZURE_RM");
            credentialRequest.setPublicKey(sshKey);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("subscriptionId", subscriptionId);
            parameters.put("secretKey", appId);
            parameters.put("tenantId", tenantId);
            parameters.put("accessKey", password);

            credentialRequest.setParameters(parameters);

            IdJson id;
            if (publicInAccount) {
                id = credentialEndpoint.postPublic(credentialRequest);
            } else {
                id = credentialEndpoint.postPrivate(credentialRequest);
            }
            context.setCredential(id.getId().toString());
            createOrSelectTemplateHint();
            return "Credential created, id: " + id.getId().toString();
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private void createOrSelectTemplateHint() throws Exception {
        if (credentialEndpoint.getPublics().isEmpty()) {
            context.setHint(Hints.ADD_BLUEPRINT);
        } else {
            context.setHint(Hints.SELECT_BLUEPRINT);
        }
    }
}
