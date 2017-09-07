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
import com.sequenceiq.cloudbreak.shell.commands.InstanceGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.SecurityGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.completion.OpenStackFacing;
import com.sequenceiq.cloudbreak.shell.completion.OpenStackOrchestratorType;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupName;
import com.sequenceiq.cloudbreak.shell.completion.SecurityRules;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.util.TagParser;

public class OpenStackCommands implements CommandMarker {

    public static final String PLATFORM = "OPENSTACK";

    private static final int TEN = 10;

    private ShellContext shellContext;

    private final CredentialCommands baseCredentialCommands;

    private final NetworkCommands baseNetworkCommands;

    private final SecurityGroupCommands baseSecurityGroupCommands;

    private final TemplateCommands baseTemplateCommands;

    private final PlatformCommands basePlatformCommands;

    private final StackCommands stackCommands;

    private final InstanceGroupCommands baseInstanceGroupCommands;

    public OpenStackCommands(ShellContext shellContext,
            CredentialCommands baseCredentialCommands,
            NetworkCommands baseNetworkCommands,
            SecurityGroupCommands baseSecurityGroupCommands,
            TemplateCommands baseTemplateCommands,
            PlatformCommands basePlatformCommands,
            StackCommands stackCommands,
            InstanceGroupCommands baseInstanceGroupCommands) {
        this.baseCredentialCommands = baseCredentialCommands;
        this.baseNetworkCommands = baseNetworkCommands;
        this.baseSecurityGroupCommands = baseSecurityGroupCommands;
        this.shellContext = shellContext;
        this.baseTemplateCommands = baseTemplateCommands;
        this.basePlatformCommands = basePlatformCommands;
        this.stackCommands = stackCommands;
        this.baseInstanceGroupCommands = baseInstanceGroupCommands;
    }

    @CliAvailabilityIndicator("stack create --OPENSTACK")
    public boolean createStackAvailable() {
        return stackCommands.createStackAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("template create --OPENSTACK")
    public boolean createTemplateAvailable() {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("platform create --OPENSTACK")
    public boolean createPlatformAvailable() {
        return basePlatformCommands.createPlatformAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator({"network create --OPENSTACK --NEW", "network create --OPENSTACK --EXISTING_SUBNET",
            "network create --OPENSTACK --NEW_SUBNET"})
    public boolean createNetworkAvailable() {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("securitygroup create --OPENSTACK --NEW")
    public boolean createSecurityGroupAvailable() {
        return baseSecurityGroupCommands.createSecurityGroupAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("credential create --OPENSTACK")
    public boolean createCredentialAvailable() {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("instancegroup configure --OPENSTACK")
    public boolean configureInstanceGroupAvailable() {
        return baseInstanceGroupCommands.createInstanceGroupAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliCommand(value = "credential create --OPENSTACK", help = "Create a new OpenStack credential")
    public String createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "userName", mandatory = true, help = "Username of the credential") String userName,
            @CliOption(key = "password", mandatory = true, help = "password of the credential") String password,
            @CliOption(key = "endPoint", mandatory = true, help = "endPoint of the credential") String endPoint,
            @CliOption(key = "tenantName", help = "tenantName of the credential for cb-keystone-v2") String tenantName,
            @CliOption(key = "userDomain", help = "userDomain of the credential for cb-keystone-v3*") String userDomain,
            @CliOption(key = "keystoneAuthScope", help = "keystoneAuthScope of the credential for cb-keystone-v3*") String keystoneAuthScope,
            @CliOption(key = "domainName", help = "domainName of the credential for cb-keystone-v3-default-scope") String domainName,
            @CliOption(key = "projectDomainName", help = "projectDomainName of the credential for cb-keystone-v3-project-scope")
                    String projectDomainName,
            @CliOption(key = "projectName", help = "projectName of the credential for cb-keystone-v3-project-scope") String projectName,
            @CliOption(key = "sshKeyPath", help = "path of a public SSH key file") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", help = "URL of a public SSH key file") String sshKeyUrl,
            @CliOption(key = "sshKeyString", help = "Raw data of a public SSH key file") String sshKeyString,
            @CliOption(key = "facing", help = "URL perspective in which the API is accessing data") OpenStackFacing facing,
            @CliOption(key = "description", help = "Description of the credential") String description,
            @CliOption(key = "publicInAccount", help = "flags if the credential is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "platformId", help = "Id of a platform the credential belongs to") Long platformId
    ) {
        String keyStoneVersion = null;
        Map<String, Object> parameters = new HashMap<>();
        if (tenantName != null) {
            keystoneAuthScope = "cb-keystone-v2";
            keyStoneVersion = "cb-keystone-v2";
            parameters.put("selector", "cb-keystone-v2");
        }
        if (userDomain != null && keystoneAuthScope != null) {
            if (domainName != null) {
                keystoneAuthScope = "cb-keystone-v3-domain-scope";
            } else if (projectDomainName != null && projectName != null) {
                keystoneAuthScope = "cb-keystone-v3-project-scope";
            } else {
                keystoneAuthScope = "cb-keystone-v3-default-scope";
            }
            parameters.put("selector", keystoneAuthScope);
            keyStoneVersion = "cb-keystone-v3";
        }
        if (keystoneAuthScope == null || keyStoneVersion == null) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("keystoneAuthScope not found for specified parameters");
        }
        parameters.put("userName", userName);
        parameters.put("password", password);
        parameters.put("endpoint", endPoint);
        parameters.put("keystoneVersion", keyStoneVersion);
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

    @CliCommand(value = "network create --OPENSTACK --EXISTING_SUBNET",
            help = "Create an OpenStack network which uses an existing subnet in an existing network")
    public String createExisitngNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "networkId", mandatory = true, help = "ID of the custom network to use") String networkId,
            @CliOption(key = "subnetId", mandatory = true, help = "ID of the custom subnet to use") String subnetId,
            @CliOption(key = "publicNetID", help = "ID of the available and desired OpenStack public network") String publicNetID,
            @CliOption(key = "publicInAccount", help = "Marks the network as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the network") String description,
            @CliOption(key = "platformId", help = "Id of a platform the network belongs to") Long platformId,
            @CliOption(key = "networkingOption", help = "Networking option: self-service, provider",
                    unspecifiedDefaultValue = "self-service") String networkingOption
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("networkId", networkId);
        parameters.put("subnetId", subnetId);
        parameters.put("networkingOption", networkingOption);
        if (publicNetID != null) {
            parameters.put("publicNetId", publicNetID);
        }
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --OPENSTACK --NEW", help = "Create an OpenStack network configuration with a new network and a new subnet")
    public String createNewNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "publicNetID", help = "ID of the available and desired OpenStack public network") String publicNetID,
            @CliOption(key = "publicInAccount", help = "Marks the network as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the network") String description,
            @CliOption(key = "platformId", help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        if (publicNetID != null) {
            parameters.put("publicNetId", publicNetID);
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --OPENSTACK --NEW_SUBNET", help = "Create an OpenStack network configuration with a new subnet in an existing network")
    public String createNetworkWithNewSubnet(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "networkId", mandatory = true, help = "ID of the custom network to use") String networkId,
            @CliOption(key = "routerId", mandatory = true, help = "ID of the custom router to use") String routerId,
            @CliOption(key = "publicNetID", help = "ID of the available and desired OpenStack public network") String publicNetID,
            @CliOption(key = "publicInAccount", help = "Marks the network as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the network") String description,
            @CliOption(key = "platformId", help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("networkId", networkId);
        parameters.put("routerId", routerId);
        if (publicNetID != null) {
            parameters.put("publicNetId", publicNetID);
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "securitygroup create --OPENSTACK --NEW", help = "Create an OPENSTACK security group")
    public String createNewSecurityGroup(
            @CliOption(key = "name", mandatory = true, help = "Name of the security group") String name,
            @CliOption(key = "description", help = "Description of the security group") String description,
            @CliOption(key = "rules", mandatory = true,
                    help = "Security rules in the following format: ';' separated list of <cidr>:<protocol>:<comma separated port list>") SecurityRules rules,
            @CliOption(key = "publicInAccount", help = "Marks the securitygroup as visible for all members of the account",
                    specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") Boolean publicInAccount) {
        return baseSecurityGroupCommands.create(name, description, null, PLATFORM, rules, publicInAccount);
    }

    @CliCommand(value = "template create --OPENSTACK", help = "Create a new OpenStack template")
    public String createTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") String instanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "publicInAccount", help = "flags if the template is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the template") String description,
            @CliOption(key = "platformId", help = "Id of a platform the template belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();

        if (volumeCount == 0) {
            volumeSize = null;
        }

        if (volumeCount < 0) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("Count of volumes could not be smaller than 0");
        }

        if (volumeCount > 0) {
            if (volumeSize == null) {
                throw shellContext.exceptionTransformer().transformToRuntimeException("volumeSize parameter must be specified");
            } else if (volumeSize < TEN) {
                throw shellContext.exceptionTransformer().transformToRuntimeException("Size of volumes could not be smaller than 10 Gb");
            }
        }
        return baseTemplateCommands.create(name, instanceType, volumeCount, volumeSize, "HDD", publicInAccount, description, parameters, platformId, PLATFORM);
    }

    @CliCommand(value = "platform create --OPENSTACK", help = "Create a new OpenStack platform configuration")
    public String createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") String name,
            @CliOption(key = "description", help = "Description of the platform") String description,
            @CliOption(key = "url", help = "URL of the topology mapping file to download from") String url,
            @CliOption(key = "file", help = "File which contains the topology mapping") File file
    ) {
        try {
            return basePlatformCommands.create(name, description, "OPENSTACK", basePlatformCommands.convertMappingFile(file, url));
        } catch (RuntimeException e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "instancegroup configure --OPENSTACK", help = "Configure instance groups")
    public String createInstanceGroup(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") InstanceGroup instanceGroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Nodecount for instanceGroup") Integer nodeCount,
            @CliOption(key = "ambariServer", mandatory = true, help = "Ambari server will be installed here if true") boolean ambariServer,
            @CliOption(key = "templateId", help = "TemplateId of the instanceGroup") InstanceGroupTemplateId instanceGroupTemplateId,
            @CliOption(key = "templateName", help = "TemplateName of the instanceGroup") InstanceGroupTemplateName instanceGroupTemplateName,
            @CliOption(key = "securityGroupId", help = "SecurityGroupId of the instanceGroup") SecurityGroupId instanceGroupSecurityGroupId,
            @CliOption(key = "securityGroupName", help = "SecurityGroupName of the instanceGroup") SecurityGroupName instanceGroupSecurityGroupName) {

        Map<String, Object> parameters = new HashMap<>();

        return baseInstanceGroupCommands.create(instanceGroup, nodeCount, ambariServer, instanceGroupTemplateId, instanceGroupTemplateName,
                instanceGroupSecurityGroupId, instanceGroupSecurityGroupName, parameters);
    }

    @CliCommand(value = "stack create --OPENSTACK", help = "Create a new OpenStack stack based on a template")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") String name,
            @CliOption(key = "region", mandatory = true, help = "region of the stack") StackRegion region,
            @CliOption(key = "availabilityZone", mandatory = true, help = "availabilityZone of the stack") StackAvailabilityZone availabilityZone,
            @CliOption(key = "publicInAccount", help = "marks the stack as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "ambariVersion", help = "Ambari version") String ambariVersion,
            @CliOption(key = "hdpVersion", help = "HDP version") String hdpVersion,
            @CliOption(key = "imageCatalog", help = "custom image catalog URL") String imageCatalog,
            @CliOption(key = "onFailureAction", help = "onFailureAction which is ROLLBACK or DO_NOTHING.") OnFailureAction onFailureAction,
            @CliOption(key = "adjustmentType", help = "adjustmentType which is EXACT or PERCENTAGE.") AdjustmentType adjustmentType,
            @CliOption(key = "threshold", help = "threshold of failure") Long threshold,
            @CliOption(key = "orchestrator", help = "select orchestrator variant version") OpenStackOrchestratorType orchestratorType,
            @CliOption(key = "platformVariant", help = "select platform variant version") PlatformVariant platformVariant,
            @CliOption(key = "customImage", help = "select customImage for cluster") String customImage,
            @CliOption(key = "tags", help = "created resources will be tagged with these key=value pairs, format: key1=value1,key2=value2") String tags,
            @CliOption(key = "wait", help = "Wait for stack creation", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true") Long timeout,
            @CliOption(key = "customDomain", help = "Custom domain for the nodes in the stack") String customDomain,
            @CliOption(key = "customHostname", help = "Custom hostname for the nodes in the stack") String customHostname,
            @CliOption(key = "clusterNameAsSubdomain", help = "Using the cluster name for subdomain", unspecifiedDefaultValue = "false",
                    specifiedDefaultValue = "true") boolean clusterNameAsSubdomain,
            @CliOption(key = "hostgroupNameAsHostname", help = "Using the hostgroup names to create hostnames", unspecifiedDefaultValue = "false",
                    specifiedDefaultValue = "true") boolean hostgroupNameAsHostname) {
        Map<String, String> params = new HashMap<>();
        return stackCommands.create(name, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType, threshold,
                wait, platformVariant, orchestratorType == null ? "SALT" : orchestratorType.getName(), PLATFORM,
                ambariVersion, hdpVersion, imageCatalog, params, TagParser.parseTagsIntoMap(tags), customImage, timeout,
                customDomain, customHostname, clusterNameAsSubdomain, hostgroupNameAsHostname);
    }
}
