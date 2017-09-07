package com.sequenceiq.cloudbreak.shell.commands.provider;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands;
import com.sequenceiq.cloudbreak.shell.commands.InstanceGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.SecurityGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.completion.ArmOrchestratorType;
import com.sequenceiq.cloudbreak.shell.completion.AvailabilitySetName;
import com.sequenceiq.cloudbreak.shell.completion.AzureInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.AzureVolumeType;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupName;
import com.sequenceiq.cloudbreak.shell.completion.SecurityRules;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;
import com.sequenceiq.cloudbreak.shell.model.AvailabilitySetEntry;
import com.sequenceiq.cloudbreak.shell.model.AvailabilitySetFaultDomainNumber;
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.util.TagParser;

public class AzureCommands implements CommandMarker {

    public static final String PLATFORM = "AZURE";

    public static final String SALT = "SALT";

    private static final String AVAILABILITY_SET_PATTERN = "^[a-zA-Z0-9][a-zA-Z0-9-_.]{1,78}[a-zA-Z0-9_]$";

    private ShellContext shellContext;

    private final CredentialCommands baseCredentialCommands;

    private final NetworkCommands baseNetworkCommands;

    private final SecurityGroupCommands baseSecurityGroupCommands;

    private final TemplateCommands baseTemplateCommands;

    private final PlatformCommands basePlatformCommands;

    private final StackCommands stackCommands;

    private final InstanceGroupCommands baseInstanceGroupCommands;

    public AzureCommands(ShellContext shellContext,
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

    @CliAvailabilityIndicator("stack create --AZURE")
    public boolean createStackAvailable() {
        return stackCommands.createStackAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("template create --AZURE")
    public boolean createTemplateAvailable() {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("platform create --AZURE")
    public boolean createPlatformAvailable() {
        return basePlatformCommands.createPlatformAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator({"network create --AZURE --NEW", "network create --AZURE --EXISTING_SUBNET"})
    public boolean createNetworkAvailable() {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("securitygroup create --AZURE --NEW")
    public boolean createSecurityGroupAvailable() {
        return baseSecurityGroupCommands.createSecurityGroupAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("credential create --AZURE")
    public boolean createCredentialAvailable() {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator("instancegroup configure --AZURE")
    public boolean configureInstanceGroupAvailable() {
        return baseInstanceGroupCommands.createInstanceGroupAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator({"availabilityset list", "availabilityset create"})
    public boolean configureAvailabilitySetAvailable() {
        return shellContext.isCredentialAvailable() && shellContext.isPlatformAvailable(PLATFORM) && shellContext.getActiveCloudPlatform().equals(PLATFORM);
    }

    @CliAvailabilityIndicator("availabilityset delete")
    public boolean configureAvailabilitySetModificationAvailable() {
        return !shellContext.getAzureAvailabilitySets().isEmpty();
    }

    @CliCommand(value = "credential create --AZURE", help = "Create a new Azure credential")
    public String createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "subscriptionId", mandatory = true, help = "subscriptionId of the credential") String subscriptionId,
            @CliOption(key = "tenantId", mandatory = true, help = "tenantId of the credential") String tenantId,
            @CliOption(key = "appId", mandatory = true, help = "appId of the credential") String appId,
            @CliOption(key = "password", mandatory = true, help = "password of the credential") String password,
            @CliOption(key = "sshKeyPath", help = "sshKeyPath of the template") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", help = "sshKeyUrl of the template") String sshKeyUrl,
            @CliOption(key = "sshKeyString", help = "Raw data of a public SSH key file") String sshKeyString,
            @CliOption(key = "publicInAccount", help = "flags if the credential is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the credential") String description,
            @CliOption(key = "platformId", help = "Id of a platform the credential belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("subscriptionId", subscriptionId);
        parameters.put("secretKey", password);
        parameters.put("tenantId", tenantId);
        parameters.put("accessKey", appId);
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --AZURE --NEW", help = "Create an Azure network configuration with a new network and a new subnet")
    public String createNewNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "publicInAccount", help = "Marks the network as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the network") String description,
            @CliOption(key = "platformId", help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --AZURE --EXISTING_SUBNET", help = "Create an Azure network which uses an existing subnet in an existing network")
    public String createNetworkWithExistingSubnet(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "resourceGroupName", mandatory = true,
                    help = "Name of the custom resource group in case of existing virtual network and subnet") String rgName,
            @CliOption(key = "networkId", mandatory = true, help = "Name of the custom network within the custom resource group") String networkId,
            @CliOption(key = "subnetId", mandatory = true, help = "Name of the custom subnet within the custom resource group") String subnetId,
            @CliOption(key = "publicInAccount", help = "Marks the network as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "noPublicIp", help = "If true, no public IP is created for the instances") Boolean noPublicIp,
            @CliOption(key = "noFirewallRules", help = "If true, no new firewall rules will be created for the network") Boolean noFirewallRules,
            @CliOption(key = "description", help = "Description of the network") String description,
            @CliOption(key = "platformId", help = "Id of a platform the network belongs to") Long platformId
                ) {
            Map<String, Object> parameters = new HashMap<>();
            if (rgName != null && networkId != null && subnetId != null) {
            parameters.put("resourceGroupName", rgName);
            parameters.put("networkId", networkId);
            parameters.put("subnetId", subnetId);
        }
        parameters.put("noPublicIp", noPublicIp != null ? noPublicIp : false);
        parameters.put("noFirewallRules", noFirewallRules != null ? noFirewallRules : false);
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "securitygroup create --AZURE --NEW", help = "Create an AZURE security group")
    public String createNewSecurityGroup(
            @CliOption(key = "name", mandatory = true, help = "Name of the security group") String name,
            @CliOption(key = "description", help = "Description of the security group") String description,
            @CliOption(key = "rules",
                    help = "Security rules in the following format: ';' separated list of <cidr>:<protocol>:<comma separated port list>") SecurityRules rules,
            @CliOption(key = "publicInAccount", help = "Marks the securitygroup as visible for all members of the account",
                    specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") Boolean publicInAccount) {
        return baseSecurityGroupCommands.create(name, description, null, PLATFORM, rules, publicInAccount);
    }

    @CliCommand(value = "template create --AZURE", help = "Create a new Azure template")
    public String createTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "type of the VM") AzureInstanceType instanceType,
            @CliOption(key = "volumeType", mandatory = true, help = "volumeType of the template") AzureVolumeType volumeType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "publicInAccount", help = "flags if the template is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the template") String description,
            @CliOption(key = "platformId", help = "Id of a platform the template belongs to") Long platformId,
            @CliOption(key = "managedDisk", help = "flag if the disks will be managed",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean managedDisk
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("managedDisk", managedDisk);
        if (managedDisk && !"Standard_LRS".equals(volumeType.getName())) {
            throw shellContext.exceptionTransformer()
                    .transformToRuntimeException("Only Standard_LRS supported for managed disks!");
        }
        return baseTemplateCommands.create(name, instanceType.getName(), volumeCount, volumeSize, volumeType.getName(), publicInAccount, description,
                parameters, platformId, PLATFORM);
    }

    @CliCommand(value = "cluster fileSystem --WASB", help = "Set Windows Azure Blob Storage filesystem on cluster")
    public String setWasbFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default filesystem") Boolean defaultFileSystem,
            @CliOption(key = "accountName", mandatory = true, help = "name of the storage account") String accountName,
            @CliOption(key = "accountKey", mandatory = true, help = "primary access key to the storage account") String accountKey) {
        shellContext.setDefaultFileSystem(defaultFileSystem);
        shellContext.setFileSystemType(FileSystemType.WASB);
        Map<String, Object> props = new HashMap<>();
        props.put("accountName", accountName);
        props.put("accountKey", accountKey);
        shellContext.setFileSystemParameters(props);
        return "Windows Azure Blob Storage filesystem configured";
    }

    @CliCommand(value = "cluster fileSystem --ADLS", help = "Set Azure Data Lake Store filesystem on cluster")
    public String setAdlsFileSystem(
            //@CliOption(key = "defaultFileSystem", mandatory = false, specifiedDefaultValue = "false", unspecifiedDefaultValue = "false",
            //        help = "Use as default filesystem") Boolean defaultFileSystem,
            @CliOption(key = "accountName", mandatory = true, help = "name of the storage account") String accountName) {
        shellContext.setDefaultFileSystem(false);
        shellContext.setFileSystemType(FileSystemType.ADLS);

        String credentialId = shellContext.getCredentialId();
        CredentialResponse credential;
        try {
            credential = shellContext.getCredentialById(credentialId);
        } catch (RuntimeException e) {
            throw shellContext.exceptionTransformer()
                    .transformToRuntimeException(e);
        }
        Map<String, Object> parameters = credential.getParameters();
        String tenantId = (String) parameters.get("tenantId");
        String accessKey = (String) parameters.get("accessKey");

        Map<String, Object> props = new HashMap<>();
        props.put("accountName", accountName);
        props.put("tenantId", tenantId);
        props.put("clientId", accessKey);

        if (accountName == null || tenantId == null || accessKey == null) {
            throw shellContext.exceptionTransformer()
                    .transformToRuntimeException("One or more required parameters for ADLS are not set!");
        }
        shellContext.setFileSystemParameters(props);
        return "Azure Data Lake Store filesystem configured";
    }

    @CliCommand(value = "platform create --AZURE", help = "Create a new Azure platform configuration")
    public String createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") String name,
            @CliOption(key = "description", help = "Description of the platform") String description
    ) {
        try {
            return basePlatformCommands.create(name, description, PLATFORM, Collections.emptyMap());
        } catch (RuntimeException e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "availabilityset create", help = "Create an Azure availability set configuration")
    public String createAvailabilitySet(
            @CliOption(key = "name", mandatory = true, help = "Name of the availability set") String name,
            @CliOption(key = "platformFaultDomainCount", mandatory = true, help = "Number of fault domains")
                    AvailabilitySetFaultDomainNumber platformFaultDomainCount) {
        try {
            if (platformFaultDomainCount.number() > AvailabilitySetFaultDomainNumber.THREE.number()
                    || platformFaultDomainCount.number() < AvailabilitySetFaultDomainNumber.TWO.number()) {
                throw shellContext.exceptionTransformer()
                        .transformToRuntimeException("The number of fault domains must be between 2 and 3!");
            }
            if (!Pattern.compile(AVAILABILITY_SET_PATTERN).matcher(name).matches()) {
                throw shellContext.exceptionTransformer()
                        .transformToRuntimeException("Availability set name invalid, it can contain only alphanumeric, underscore and hyphen characters!");
            }
            AvailabilitySetEntry as = new AvailabilitySetEntry();
            as.setName(name);
            as.setFaultDomainCount(platformFaultDomainCount);
            shellContext.putAzureAvailabilitySet(name, as);
            return shellContext.outputTransformer().render(shellContext.getAzureAvailabilitySets(), "Availability sets");

        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "availabilityset list", help = "List the Azure availability sets configured")
    public String listAvailabilitySet() {
        try {
            return shellContext.outputTransformer().render(shellContext.getAzureAvailabilitySets(), "Availability sets");
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "availabilityset delete", help = "Create an Azure availability set configuration")
    public String deleteAvailabilitySet(
            @CliOption(key = "name", mandatory = true, help = "Name of the availability set") String name) {
        try {
            AvailabilitySetEntry as = shellContext.getAzureAvailabilitySets().get(name);
            if (as != null) {
                shellContext.getAzureAvailabilitySets().remove(name);
                return String.format("Azure availability set deleted with %s name", name);
            } else {
                throw shellContext.exceptionTransformer().transformToRuntimeException(String.format("No availability set found with %s name", name));
            }
        } catch (RuntimeException e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "instancegroup configure --AZURE", help = "Configure instance groups")
    public String createInstanceGroup(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") InstanceGroup instanceGroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Nodecount for instanceGroup") Integer nodeCount,
            @CliOption(key = "ambariServer", mandatory = true, help = "Ambari server will be installed here if true") boolean ambariServer,
            @CliOption(key = "templateId", help = "TemplateId of the instanceGroup") InstanceGroupTemplateId instanceGroupTemplateId,
            @CliOption(key = "templateName", help = "TemplateName of the instanceGroup") InstanceGroupTemplateName instanceGroupTemplateName,
            @CliOption(key = "securityGroupId", help = "SecurityGroupId of the instanceGroup") SecurityGroupId instanceGroupSecurityGroupId,
            @CliOption(key = "securityGroupName", help = "SecurityGroupName of the instanceGroup") SecurityGroupName instanceGroupSecurityGroupName,
            @CliOption(key = "availabilitySetName", help = "Availability set name for the instanceGroup") AvailabilitySetName availabilitySetName) {

        Map<String, Object> parameters = new HashMap<>();

        if (availabilitySetName != null) {
            String asName = availabilitySetName.getName();
            for (String otherIgName : shellContext.getInstanceGroups().keySet()) {
                InstanceGroupEntry otherIgEntry = shellContext.getInstanceGroups().get(otherIgName);
                if (instanceGroup.getName().equals(otherIgName)) {
                    continue;
                }
                if (otherIgEntry.getAttributes() != null && otherIgEntry.getAttributes().get("availabilitySet") != null) {
                    Object otherIgAs = otherIgEntry.getAttributes().get("availabilitySet");
                    if (otherIgAs instanceof HashMap) {
                        String otherIgAsName =  (String) ((Map) otherIgAs).get("name");
                        if (asName.equals(otherIgAsName)) {
                            throw shellContext.exceptionTransformer()
                                    .transformToRuntimeException("Cannot use the same availability set for two different instance groups!");
                        }
                    }
                }
            }

            AvailabilitySetEntry as = shellContext.getAzureAvailabilitySets().get(availabilitySetName.getName());
            if (as == null) {
                throw shellContext.exceptionTransformer()
                        .transformToRuntimeException("There is no availability set defined with the name " + availabilitySetName.getName());
            }
            Map<String, Object> map = new HashMap<>();
            map.put("name", as.getName());
            map.put("faultDomainCount", as.getFaultDomainCount().number());
            parameters.put("availabilitySet", map);
        }
        return baseInstanceGroupCommands.create(instanceGroup, nodeCount, ambariServer, instanceGroupTemplateId, instanceGroupTemplateName,
                instanceGroupSecurityGroupId, instanceGroupSecurityGroupName, parameters);
    }

    @CliCommand(value = "stack create --AZURE", help = "Create a new Azure stack based on a template")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") String name,
            @CliOption(key = "region", mandatory = true, help = "region of the stack") StackRegion region,
            @CliOption(key = "availabilityZone", help = "availabilityZone of the stack") StackAvailabilityZone availabilityZone,
            @CliOption(key = "publicInAccount", help = "marks the stack as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "onFailureAction", help = "onFailureAction which is ROLLBACK or DO_NOTHING.") OnFailureAction onFailureAction,
            @CliOption(key = "adjustmentType", help = "adjustmentType which is EXACT or PERCENTAGE.") AdjustmentType adjustmentType,
            @CliOption(key = "ambariVersion", help = "Ambari version") String ambariVersion,
            @CliOption(key = "hdpVersion", help = "HDP version") String hdpVersion,
            @CliOption(key = "imageCatalog", help = "custom image catalog URL") String imageCatalog,
            @CliOption(key = "threshold", help = "threshold of failure") Long threshold,
            @CliOption(key = "diskPerStorage", help = "disk per Storage Account on Azure") Integer diskPerStorage,
            @CliOption(key = "platformVariant", help = "select platform variant version") PlatformVariant platformVariant,
            @CliOption(key = "tags", help = "created resources will be tagged with these key=value pairs, format: key1=value1,key2=value2") String tags,
            @CliOption(key = "orchestrator", help = "select orchestrator variant version") ArmOrchestratorType orchestratorType,
            @CliOption(key = "attachedStorageType", help = "type of the storage creation") ArmAttachedStorageOption attachedStorageOption,
            @CliOption(key = "persistentStorage", help = "name of the persistent storage")
            String persistentStorage,
            @CliOption(key = "customImage", help = "select customImage for cluster") String customImage,
            @CliOption(key = "wait", help = "Wait for stack creation", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true") Long timeout,
            @CliOption(key = "customDomain", help = "Custom domain for the nodes in the stack") String customDomain,
            @CliOption(key = "customHostname", help = "Custom hostname for the nodes in the stack") String customHostname,
            @CliOption(key = "clusterNameAsSubdomain", help = "Using the cluster name for subdomain", unspecifiedDefaultValue = "false",
                    specifiedDefaultValue = "true") boolean clusterNameAsSubdomain,
            @CliOption(key = "hostgroupNameAsHostname", help = "Using the hostgroup names to create hostnames", unspecifiedDefaultValue = "false",
                    specifiedDefaultValue = "true") boolean hostgroupNameAsHostname) {

            orchestratorType = (orchestratorType == null) ? new ArmOrchestratorType(SALT) : orchestratorType;
            Map<String, String> params = new HashMap<>();

            if (diskPerStorage != null) {
                params.put("diskPerStorage", diskPerStorage.toString());
            }
            if (attachedStorageOption != null && shellContext.isAzureActiveCredential()) {
                params.put("attachedStorageOption", attachedStorageOption.name());
            } else if (shellContext.isAzureActiveCredential()) {
                params.put("attachedStorageOption", ArmAttachedStorageOption.SINGLE.name());
            }
            if (persistentStorage != null && shellContext.isAzureActiveCredential()) {
                params.put("persistentStorage", persistentStorage);
            } else if (shellContext.isAzureActiveCredential()) {
                params.put("persistentStorage", "cbstore");
            }
        return stackCommands.create(name, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType, threshold,
                wait, platformVariant, orchestratorType.getName(), PLATFORM, ambariVersion, hdpVersion, imageCatalog, params,
                TagParser.parseTagsIntoMap(tags), customImage, timeout, customDomain, customHostname, clusterNameAsSubdomain, hostgroupNameAsHostname);
    }
}
