package com.sequenceiq.cloudbreak.shell.commands.provider;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.InstanceProfileStrategy;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands;
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.completion.AwsInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.AwsOrchestratorType;
import com.sequenceiq.cloudbreak.shell.completion.AwsVolumeType;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class AwsCommands implements CommandMarker {

    public static final String PLATFORM = "AWS";

    private ShellContext shellContext;
    private CredentialCommands baseCredentialCommands;
    private NetworkCommands baseNetworkCommands;
    private TemplateCommands baseTemplateCommands;
    private PlatformCommands basePlatformCommands;
    private StackCommands stackCommands;

    public AwsCommands(ShellContext shellContext,
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

    @CliAvailabilityIndicator(value = "stack create --AWS")
    public boolean createStackAvailable() {
        return stackCommands.createStackAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "template create --AWS")
    public boolean createTemplateAvailable() {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "platform create --AWS")
    public boolean createPlatformAvailable() {
        return basePlatformCommands.createPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = { "network create --AWS --NEW_SUBNET", "network create --AWS --NEW", "network create --AWS --EXISTING_SUBNET" })
    public boolean createNetworkAvailable() {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = { "credential create --AWS", "template create --EC2" })
    public boolean createCredentialAvailable() {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM);
    }

    @CliCommand(value = "credential create --AWS", help = "Create a new AWS credential")
    public String createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "roleArn", mandatory = false, help = "roleArn for assuming roles or use access and secret based authentication") String roleArn,
            @CliOption(key = "accessKey", mandatory = false, help = "accessKey of the credential") String accessKey,
            @CliOption(key = "secretKey", mandatory = false, help = "secretKey of the credential") String secretKey,
            @CliOption(key = "sshKeyPath", mandatory = false, help = "path of a public SSH key file") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", mandatory = false, help = "URL of a public SSH key file") String sshKeyUrl,
            @CliOption(key = "sshKeyString", mandatory = false, help = "Raw data of a public SSH key file") String sshKeyString,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the credential is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the credential belongs to") Long platformId,
            @CliOption(key = "existingKeyPairName", mandatory = false, help = "Name of an existing SSH key pair that should be exist on EC2") String keyPairName
    ) {
        Map<String, Object> parameters = new HashMap<>();
        if (roleArn != null) {
            parameters.put("selector", "role-based");
            parameters.put("roleArn", roleArn);
        } else if (accessKey != null && secretKey != null) {
            parameters.put("selector", "key-based");
            parameters.put("accessKey", accessKey);
            parameters.put("secretKey", secretKey);
        } else {
            return "Please specify the roleArn or both the access and secret key";
        }
        if (!StringUtils.isEmpty(keyPairName)) {
            parameters.put("existingKeyPairName", keyPairName);
        }
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --AWS --NEW", help = "Create an Aws network configuration with a new network and a new subnet")
    public String createNewNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --AWS --NEW_SUBNET", help = "Create an Aws network configuration with a new subnet in an existing network")
    public String createNetworkWithNewSubnet(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "vpcID", mandatory = true, help = "The ID of the virtual private cloud (VPC)") String vpcId,
            @CliOption(key = "internetGatewayID", mandatory = true,
                    help = "The ID of the internet gateway that is attached to the VPC (configured via 'vpcID' option)") String internetGatewayId,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("vpcId", vpcId);
        parameters.put("internetGatewayId", internetGatewayId);
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --AWS --EXISTING_SUBNET", help = "Create an Aws network which use an existing subnet in an existing network")
    public String createNetworkWithExistingSubnet(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "vpcID", mandatory = true, help = "The ID of the virtual private cloud (VPC)") String vpcId,
            @CliOption(key = "subnetId", mandatory = true, help = "The ID of the subnet which belongs to the custom VPC") String subnetId,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("vpcId", vpcId);
        parameters.put("subnetId", subnetId);
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "template create --AWS", help = "Create a new AWS template")
    public String createTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") AwsInstanceType instanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") AwsVolumeType volumeType,
            @CliOption(key = "encrypted", mandatory = false, help = "use encrypted disks") Boolean encrypted,
            @CliOption(key = "spotPrice", mandatory = false, help = "spotPrice of the template") Double spotPrice,
            @CliOption(key = "sshLocation", mandatory = false, specifiedDefaultValue = "0.0.0.0/0", help = "sshLocation of the template") String sshLocation,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "topologyId", mandatory = false, help = "Id of a topology the template belongs to") Long platformId
    ) {
        return createEc2Template(name, instanceType, volumeCount, volumeSize, volumeType, encrypted, spotPrice, sshLocation, publicInAccount,
                description, platformId);
    }

    @CliCommand(value = "template create --EC2", help = "Create a new AWS template")
    public String createEc2Template(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") AwsInstanceType instanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") AwsVolumeType volumeType,
            @CliOption(key = "encrypted", mandatory = false, help = "use encrypted disks") Boolean encrypted,
            @CliOption(key = "spotPrice", mandatory = false, help = "spotPrice of the template") Double spotPrice,
            @CliOption(key = "sshLocation", mandatory = false, specifiedDefaultValue = "0.0.0.0/0", help = "sshLocation of the template") String sshLocation,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") Long platformId
    ) {
        Map<String, Object> params = new HashMap<>();
        params.put("sshLocation", sshLocation == null ? "0.0.0.0/0" : sshLocation);
        params.put("spotPrice", spotPrice == null ? null : spotPrice.toString());
        params.put("encrypted", encrypted == null ? null : encrypted.toString());
        return baseTemplateCommands.create(name, instanceType.getName(), volumeCount, volumeSize, volumeType == null ? "gp2" : volumeType.getName(),
                publicInAccount, description, params, platformId, PLATFORM);
    }

    @CliCommand(value = "platform create --AWS", help = "Create a new AWS platform configuration")
    public String createPlatform(
            @CliOption(key = "name", mandatory = true, help = "Name of the platform") String name,
            @CliOption(key = "description", mandatory = false, help = "Description of the platform") String description) {
        try {
            return basePlatformCommands.create(name, description, PLATFORM, Collections.<String, String>emptyMap());
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "stack create --AWS", help = "Create a new AWS stack based on a template")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") String name,
            @CliOption(key = "region", mandatory = true, help = "region of the stack") StackRegion region,
            @CliOption(key = "availabilityZone", mandatory = false, help = "availabilityZone of the stack") StackAvailabilityZone availabilityZone,
            @CliOption(key = "publicInAccount", mandatory = false, help = "marks the stack as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "onFailureAction", mandatory = false, help = "onFailureAction which is ROLLBACK or DO_NOTHING.") OnFailureAction onFailureAction,
            @CliOption(key = "adjustmentType", mandatory = false, help = "adjustmentType which is EXACT or PERCENTAGE.") AdjustmentType adjustmentType,
            @CliOption(key = "threshold", mandatory = false, help = "threshold of failure") Long threshold,
            @CliOption(key = "platformVariant", mandatory = false, help = "select platform variant version") PlatformVariant platformVariant,
            @CliOption(key = "orchestrator", mandatory = false, help = "select orchestrator variant version") AwsOrchestratorType orchestratorType,
            @CliOption(key = "dedicatedInstances", mandatory = false, help = "request dedicated instances on AWS") Boolean dedicatedInstances,
            @CliOption(key = "instanceProfileStrategy", mandatory = false, help = "seamless S3 access type", specifiedDefaultValue = "false")
            InstanceProfileStrategy instanceProfileStrategy,
            @CliOption(key = "s3Role", mandatory = false, help = "seamless S3 access role", specifiedDefaultValue = "false") String s3Role,
            @CliOption(key = "wait", mandatory = false, help = "Wait for stack creation", specifiedDefaultValue = "false") Boolean wait) {

        Map<String, String> params = new HashMap<>();
        if (dedicatedInstances != null) {
            params.put("dedicatedInstances", dedicatedInstances.toString());
        }
        if (instanceProfileStrategy != null) {
            params.put("instanceProfileStrategy", instanceProfileStrategy.toString());
        }
        if (s3Role != null && InstanceProfileStrategy.USE_EXISTING.equals(instanceProfileStrategy)) {
            params.put("s3Role", s3Role.toString());
        }
        if (s3Role != null && !InstanceProfileStrategy.USE_EXISTING.equals(instanceProfileStrategy)) {
            return "Please specify the role for S3 connection if you are using 'USE_EXISTING' profile type.";
        }
        return stackCommands.create(name, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType, threshold, false,
                wait, platformVariant, orchestratorType == null ? "SALT" : orchestratorType.getName(), PLATFORM, params);
    }
}
