package com.sequenceiq.cloudbreak.shell.commands.provider;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands;
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.completion.AzureInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.ArmOrchestratorType;
import com.sequenceiq.cloudbreak.shell.completion.AzureVolumeType;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class AzureCommands implements CommandMarker {

    public static final String PLATFORM = "AZURE_RM";

    private ShellContext shellContext;
    private CredentialCommands baseCredentialCommands;
    private NetworkCommands baseNetworkCommands;
    private TemplateCommands baseTemplateCommands;
    private PlatformCommands basePlatformCommands;
    private StackCommands stackCommands;

    public AzureCommands(ShellContext shellContext,
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

    @CliAvailabilityIndicator(value = "stack create --AZURE")
    public boolean createStackAvailable() {
        return stackCommands.createStackAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "template create --AZURE")
    public boolean createTemplateAvailable() {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "platform create --AZURE")
    public boolean createPlatformAvailable() {
        return basePlatformCommands.createPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "network create --AZURE")
    public boolean createNetworkAvailable() {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "credential create --AZURE")
    public boolean createCredentialAvailable() {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM);
    }


    @CliCommand(value = "credential create --AZURE", help = "Create a new AZURE credential")
    public String createCredential(
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
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the credential belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("subscriptionId", subscriptionId);
        parameters.put("secretKey", password);
        parameters.put("tenantId", tenantId);
        parameters.put("accessKey", appId);
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --AZURE", help = "Create a new AZURE network configuration")
    public String createNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "addressPrefix", mandatory = true, help = "The address prefix of the Azure virtual network in CIDR format") String addressPrefix,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "resourceGroupName", mandatory = false,
                    help = "Name of the custom resource group in case of existing virtual network and subnet") String rgName,
            @CliOption(key = "networkId", mandatory = false, help = "Name of the custom network within the custom resource group") String networkId,
            @CliOption(key = "subnetId", mandatory = false, help = "Name of the custom subnet within the custom resource group") String subnetId,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("addressPrefix", addressPrefix);
        if (rgName != null && networkId != null && subnetId != null) {
            parameters.put("resourceGroupName", rgName);
            parameters.put("networkId", networkId);
            parameters.put("subnetId", subnetId);
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "template create --AZURE", help = "Create a new AZURE template")
    public String createTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "type of the VM") AzureInstanceType instanceType,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") AzureVolumeType volumeType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        return baseTemplateCommands.create(name, instanceType.getName(), volumeCount, volumeSize, volumeType.getName(), publicInAccount, description,
                parameters, platformId, PLATFORM);
    }

    @CliCommand(value = "cluster fileSystem --DASH", help = "Set Windows Azure Blob Storage filesystem with DASH on cluster")
    public String setAzureRmFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default filesystem") Boolean defaultFileSystem,
            @CliOption(key = "accountName", mandatory = true, help = "accountName of the DASH service") String accountName,
            @CliOption(key = "accountKey", mandatory = true, help = "access key of the DASH service") String accountKey) {
        shellContext.setDefaultFileSystem(defaultFileSystem);
        shellContext.setFileSystemType(FileSystemType.DASH);
        Map<String, Object> props = new HashMap<>();
        props.put("accountName", accountName);
        props.put("accountKey", accountKey);
        shellContext.setFileSystemParameters(props);
        return "Windows Azure Blob Storage with DASH configured as the filesystem";
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

    @CliCommand(value = "platform create --AZURE", help = "Create a new Azure platform configuration")
    public String createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") String name,
            @CliOption(key = "description", mandatory = false, help = "Description of the platform") String description
    ) {
        try {
            return basePlatformCommands.create(name, description, PLATFORM, Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "stack create --AZURE", help = "Create a new stack based on a template")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") String name,
            @CliOption(key = "region", mandatory = true, help = "region of the stack") StackRegion region,
            @CliOption(key = "availabilityZone", mandatory = false, help = "availabilityZone of the stack") StackAvailabilityZone availabilityZone,
            @CliOption(key = "publicInAccount", mandatory = false, help = "marks the stack as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "onFailureAction", mandatory = false, help = "onFailureAction which is ROLLBACK or DO_NOTHING.") OnFailureAction onFailureAction,
            @CliOption(key = "adjustmentType", mandatory = false, help = "adjustmentType which is EXACT or PERCENTAGE.") AdjustmentType adjustmentType,
            @CliOption(key = "threshold", mandatory = false, help = "threshold of failure") Long threshold,
            @CliOption(key = "diskPerStorage", mandatory = false, help = "disk per Storage Account on Azure") Integer diskPerStorage,
            @CliOption(key = "platformVariant", mandatory = false, help = "select platform variant version") PlatformVariant platformVariant,
            @CliOption(key = "relocateDocker", mandatory = false, help = "relocate docker in startup time") Boolean relocateDocker,
            @CliOption(key = "orchestrator", mandatory = false, help = "select orchestrator variant version") ArmOrchestratorType orchestratorType,
            @CliOption(key = "attachedStorageType", mandatory = false, help = "type of the storage creation") ArmAttachedStorageOption attachedStorageOption,
            @CliOption(key = "persistentStorage", mandatory = false, help = "name of the persistent storage")
            String persistentStorage,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") Boolean wait) {

            relocateDocker = relocateDocker == null ? true : relocateDocker;

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
                relocateDocker == null ? true : relocateDocker,
                wait, platformVariant, orchestratorType == null ? "ON_HOST" : orchestratorType.getName(), PLATFORM, params);
    }
}
