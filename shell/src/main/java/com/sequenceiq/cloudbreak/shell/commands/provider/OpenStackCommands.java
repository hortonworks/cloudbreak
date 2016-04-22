package com.sequenceiq.cloudbreak.shell.commands.provider;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands;
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.completion.OpenStackFacing;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class OpenStackCommands implements CommandMarker {

    public static final String PLATFORM = "OPENSTACK";

    private ShellContext shellContext;
    private CredentialCommands baseCredentialCommands;
    private NetworkCommands baseNetworkCommands;
    private TemplateCommands baseTemplateCommands;
    private PlatformCommands basePlatformCommands;
    private StackCommands stackCommands;

    public OpenStackCommands(ShellContext shellContext,
            CredentialCommands baseCredentialCommands,
            NetworkCommands baseNetworkCommands,
            TemplateCommands baseTemplateCommands,
            PlatformCommands basePlatformCommands,
            StackCommands stackCommands) {
        this.baseCredentialCommands = baseCredentialCommands;
        this.baseNetworkCommands = baseNetworkCommands;
        this.shellContext = shellContext;
        this.baseTemplateCommands = baseTemplateCommands;
        this.basePlatformCommands = basePlatformCommands;
        this.stackCommands = stackCommands;
    }

    @CliAvailabilityIndicator(value = "stack create --OPENSTACK")
    public boolean createStackAvailable() {
        return stackCommands.createStackAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "template create --OPENSTACK")
    public boolean createTemplateAvailable() {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "platform create --OPENSTACK")
    public boolean createPlatformAvailable() {
        return basePlatformCommands.createPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "network create --OPENSTACK")
    public boolean createNetworkAvailable() {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "credential create --OPENSTACK")
    public boolean createCredentialAvailable() {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM);
    }

    @CliCommand(value = "credential create --OPENSTACK", help = "Create a new OPENSTACK credential")
    public String createCredential(
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
            @CliOption(key = "facing", mandatory = false, help = "URL perspective in which the API is accessing data") OpenStackFacing facing,
            @CliOption(key = "description", mandatory = false, help = "Description of the credential") String description,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the credential belongs to") Long platformId
    ) {
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
        if (facing != null) {
            parameters.put("facing", facing.getName());
        }
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --OPENSTACK", help = "Create a new OpenStack network configuration")
    public String createNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "publicNetID", mandatory = true, help = "ID of the available and desired OpenStack public network") String publicNetID,
            @CliOption(key = "networkId", mandatory = false, help = "ID of the custom network to use") String networkId,
            @CliOption(key = "routerId", mandatory = false, help = "ID of the custom router to use") String routerId,
            @CliOption(key = "subnetId", mandatory = false, help = "ID of the custom subnet to use") String subnetId,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("publicNetId", publicNetID);
        if (networkId != null && routerId != null) {
            parameters.put("networkId", networkId);
            parameters.put("routerId", routerId);
            if (subnetId != null) {
                parameters.put("subnetId", subnetId);
            }
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "template create --OPENSTACK", help = "Create a new OPENSTACK template")
    public String createTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") String instanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        return baseTemplateCommands.create(name, instanceType, volumeCount, volumeSize, "HDD", publicInAccount, description, parameters, platformId, PLATFORM);
    }

    @CliCommand(value = "platform create --OPENSTACK", help = "Create a new Openstack platform configuration")
    public String createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") String name,
            @CliOption(key = "description", mandatory = false, help = "Description of the platform") String description,
            @CliOption(key = "url", mandatory = false, help = "URL of the topology mapping file to download from") String url,
            @CliOption(key = "file", mandatory = false, help = "File which contains the topology mapping") File file
    ) {
        try {
            return basePlatformCommands.create(name, description, "OPENSTACK", basePlatformCommands.convertMappingFile(file, url));
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "stack create --OPENSTACK", help = "Create a new stack based on a template")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") String name,
            @CliOption(key = "region", mandatory = true, help = "region of the stack") StackRegion region,
            @CliOption(key = "availabilityZone", mandatory = false, help = "availabilityZone of the stack") StackAvailabilityZone availabilityZone,
            @CliOption(key = "publicInAccount", mandatory = false, help = "marks the stack as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "onFailureAction", mandatory = false, help = "onFailureAction which is ROLLBACK or DO_NOTHING.") OnFailureAction onFailureAction,
            @CliOption(key = "adjustmentType", mandatory = false, help = "adjustmentType which is EXACT or PERCENTAGE.") AdjustmentType adjustmentType,
            @CliOption(key = "threshold", mandatory = false, help = "threshold of failure") Long threshold,
            @CliOption(key = "platformVariant", mandatory = false, help = "select platform variant version") PlatformVariant platformVariant,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") Boolean wait) {
        Map<String, String> params = new HashMap<>();
        return stackCommands.create(name, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType, threshold,
                false, wait, platformVariant, PLATFORM, params);
    }
}
