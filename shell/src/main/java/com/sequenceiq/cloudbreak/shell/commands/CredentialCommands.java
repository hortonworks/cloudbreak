package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;
import static com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class CredentialCommands implements CommandMarker {

    private static final String FILE_NOT_FOUND = "File not found with ssh key.";
    private static final String URL_NOT_FOUND = "Url not Available for ssh key.";
    private static final String P12_FILE_NOT_FOUND = "File not found with service account private key (p12) file.";

    private List<Map> maps = new ArrayList<>();

    @Inject
    private CloudbreakContext context;
    @Inject
    private CloudbreakClient cloudbreakClient;
    @Inject
    private ResponseTransformer responseTransformer;
    @Inject
    private ExceptionTransformer exceptionTransformer;

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

    @CliAvailabilityIndicator({ "credential create --GCP", "credential create --EC2", "credential create --AWS", "credential create --AZURE",
            "credential create --OPENSTACK" })
    public boolean isCredentialEc2CreateCommandAvailable() {
        return true;
    }

    @CliCommand(value = "credential show", help = "Shows the credential by its id")
    public Object showCredential(
            @CliOption(key = "id", mandatory = false, help = "Id of the credential") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the credential") String name) {
        try {
            if (id != null) {
                return renderSingleMap(
                        responseTransformer.transformObjectToStringMap(cloudbreakClient.credentialEndpoint().get(Long.valueOf(id))), "FIELD", "VALUE");
            } else if (name != null) {
                CredentialResponse aPublic = cloudbreakClient.credentialEndpoint().getPublic(name);
                if (aPublic != null) {
                    return renderSingleMap(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "credential delete", help = "Delete the credential by its id")
    public Object deleteCredential(
            @CliOption(key = "id", mandatory = false, help = "Id of the credental") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the credential") String name) {
        try {
            if (id != null) {
                cloudbreakClient.credentialEndpoint().delete(Long.valueOf(id));
                return String.format("Credential deleted, id: %s", id);
            } else if (name != null) {
                cloudbreakClient.credentialEndpoint().deletePublic(name);
                return String.format("Credential deleted, name: %s", name);
            }
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "credential list", help = "Shows all of your credentials")
    public String listCredentials() {
        try {
            return renderSingleMap(responseTransformer.transformToMap(cloudbreakClient.credentialEndpoint().getPublics(), "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "credential select", help = "Select the credential by its id or name")
    public String selectCredential(
            @CliOption(key = "id", mandatory = false, help = "Id of the credential") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the credential") String name) {
        try {

            if (id != null) {
                if (cloudbreakClient.credentialEndpoint().get(Long.valueOf(id)) != null) {
                    context.setCredential(id);
                    createOrSelectTemplateHint();
                    return "Credential selected, id: " + id;
                }
            } else if (name != null) {
                CredentialResponse aPublic = cloudbreakClient.credentialEndpoint().getPublic(name);
                context.setCredential(aPublic.getId().toString());
                createOrSelectTemplateHint();
                return "Credential selected, name: " + name;
            }
            return "No credential specified (select a credential by --id or --name)";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
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
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") String sshKeyString,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") String description,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "topologyId", mandatory = false, help = "Id of a topology the credential belongs to") Long topologyId
    ) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty()) && sshKeyString == null) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl or --sshKeyString";
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
                sshKey = IOUtils.toString(new FileReader(new File(sshKeyPath.getPath())));
            } catch (IOException ex) {
                throw exceptionTransformer.transformToRuntimeException(FILE_NOT_FOUND);
            }
        } else if (sshKeyUrl != null) {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException ex) {
                throw exceptionTransformer.transformToRuntimeException(URL_NOT_FOUND);
            }
        } else {
            sshKey = sshKeyString;
        }

        try {
            String cloudPlatform = "OPENSTACK";
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform(cloudPlatform);
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
            if (topologyId != null) {
                checkTopologyForResource(cloudbreakClient.topologyEndpoint().getPublics(), topologyId, cloudPlatform);
            }
            credentialRequest.setTopologyId(topologyId);

            IdJson idJson;
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (publicInAccount) {
                idJson = cloudbreakClient.credentialEndpoint().postPublic(credentialRequest);
            } else {
                idJson = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest);
            }
            context.setCredential(idJson.getId().toString());
            createOrSelectTemplateHint();
            return "Credential created, id: " + idJson.getId().toString();
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = { "credential create --AWS" },
            help = "Create a new AWS credential")
    public String createAwsCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "roleArn", mandatory = true, help = "roleArn of the credential") String roleArn,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key file") String sshKeyUrl,
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") String sshKeyString,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "topologyId", mandatory = false, help = "Id of a topology the credential belongs to") Long topologyId
    ) {
        return createEc2Credential(name, roleArn, sshKeyPath, sshKeyUrl, sshKeyString, publicInAccount, description, topologyId);
    }


    @CliCommand(value = { "credential create --EC2" },
            help = "Create a new AWS credential ('credential create --EC2' is deprecated will be removed soon)")
    public String createEc2Credential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "roleArn", mandatory = true, help = "roleArn of the credential") String roleArn,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key file") String sshKeyUrl,
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") String sshKeyString,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "topologyId", mandatory = false, help = "Id of a topology the credential belongs to") Long topologyId
    ) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty()) && sshKeyString == null) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl or --sshKeyString";
        }
        String sshKey;
        if (sshKeyPath != null) {
            try {
                sshKey = IOUtils.toString(new FileReader(new File(sshKeyPath.getPath())));
            } catch (IOException ex) {
                throw exceptionTransformer.transformToRuntimeException(FILE_NOT_FOUND);
            }
        } else if (sshKeyUrl != null) {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException ex) {
                throw exceptionTransformer.transformToRuntimeException(URL_NOT_FOUND);
            }
        } else {
            sshKey = sshKeyString;
        }
        try {
            String cloudPlatform = "AWS";
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform(cloudPlatform);
            credentialRequest.setPublicKey(sshKey);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("roleArn", roleArn);

            credentialRequest.setParameters(parameters);
            if (topologyId != null) {
                checkTopologyForResource(cloudbreakClient.topologyEndpoint().getPublics(), topologyId, cloudPlatform);
            }
            credentialRequest.setTopologyId(topologyId);

            IdJson id;
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (publicInAccount) {
                id = cloudbreakClient.credentialEndpoint().postPublic(credentialRequest);
            } else {
                id = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest);
            }
            context.setCredential(id.getId().toString());
            createOrSelectTemplateHint();
            return "Credential created, id: " + id.getId().toString();
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
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
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") String sshKeyString,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") String description,
            @CliOption(key = "topologyId", mandatory = false, help = "Id of a topology the credential belongs to") Long topologyId
    ) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty()) && sshKeyString == null) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl or --sshKeyString";
        }
        String sshKey;
        if (sshKeyPath != null) {
            try {
                sshKey = IOUtils.toString(new FileReader(new File(sshKeyPath.getPath())));
            } catch (IOException ex) {
                throw exceptionTransformer.transformToRuntimeException(FILE_NOT_FOUND);
            }
        } else if (sshKeyUrl != null) {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException ex) {
                throw exceptionTransformer.transformToRuntimeException(URL_NOT_FOUND);
            }
        } else {
            sshKey = sshKeyString;
        }

        String serviceAccountPrivateKey;

        try {
            serviceAccountPrivateKey = Base64.encodeBase64String(Files.readAllBytes(serviceAccountPrivateKeyPath.toPath())).replaceAll("\n", "");
        } catch (IOException ex) {
            throw exceptionTransformer.transformToRuntimeException(P12_FILE_NOT_FOUND);
        }

        try {
            String cloudPlatform = "GCP";
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform(cloudPlatform);
            credentialRequest.setPublicKey(sshKey);
            if (topologyId != null) {
                checkTopologyForResource(cloudbreakClient.topologyEndpoint().getPublics(), topologyId, cloudPlatform);
            }
            credentialRequest.setTopologyId(topologyId);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("projectId", projectId);
            parameters.put("serviceAccountId", serviceAccountId);
            parameters.put("serviceAccountPrivateKey", serviceAccountPrivateKey);

            credentialRequest.setParameters(parameters);

            IdJson id;
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (publicInAccount) {
                id = cloudbreakClient.credentialEndpoint().postPublic(credentialRequest);
            } else {
                id = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest);
            }
            context.setCredential(id.getId().toString());
            createOrSelectTemplateHint();
            return "Credential created, id: " + id.getId().toString();
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
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
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") String sshKeyString,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") String description,
            @CliOption(key = "topologyId", mandatory = false, help = "Id of a topology the credential belongs to") Long topologyId
    ) {
        if ((sshKeyPath == null) && (sshKeyUrl == null || sshKeyUrl.isEmpty()) && sshKeyString == null) {
            return "An SSH public key must be specified either with --sshKeyPath or --sshKeyUrl or --sshKeyString";
        }
        String sshKey;
        if (sshKeyPath != null) {
            try {
                sshKey = IOUtils.toString(new FileReader(new File(sshKeyPath.getPath())));
            } catch (IOException ex) {
                throw exceptionTransformer.transformToRuntimeException(FILE_NOT_FOUND);
            }
        } else if (sshKeyUrl != null) {
            try {
                sshKey = readUrl(sshKeyUrl);
            } catch (IOException ex) {
                throw exceptionTransformer.transformToRuntimeException(URL_NOT_FOUND);
            }
        } else {
            sshKey = sshKeyString;
        }
        try {
            String cloudPlatform = "AZURE_RM";
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setName(name);
            credentialRequest.setDescription(description);
            credentialRequest.setCloudPlatform(cloudPlatform);
            credentialRequest.setPublicKey(sshKey);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("subscriptionId", subscriptionId);
            parameters.put("secretKey", password);
            parameters.put("tenantId", tenantId);
            parameters.put("accessKey", appId);

            credentialRequest.setParameters(parameters);
            if (topologyId != null) {
                checkTopologyForResource(cloudbreakClient.topologyEndpoint().getPublics(), topologyId, cloudPlatform);
            }
            credentialRequest.setTopologyId(topologyId);

            IdJson id;
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (publicInAccount) {
                id = cloudbreakClient.credentialEndpoint().postPublic(credentialRequest);
            } else {
                id = cloudbreakClient.credentialEndpoint().postPrivate(credentialRequest);
            }
            context.setCredential(id.getId().toString());
            createOrSelectTemplateHint();
            return "Credential created, id: " + id.getId().toString();
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    private void createOrSelectTemplateHint() throws Exception {
        if (cloudbreakClient.credentialEndpoint().getPublics().isEmpty()) {
            context.setHint(Hints.ADD_BLUEPRINT);
        } else {
            context.setHint(Hints.SELECT_BLUEPRINT);
        }
    }
}
