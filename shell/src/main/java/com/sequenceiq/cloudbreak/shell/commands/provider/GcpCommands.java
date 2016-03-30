package com.sequenceiq.cloudbreak.shell.commands.provider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.OnFailureAction;
import com.sequenceiq.cloudbreak.shell.commands.CredentialCommands;
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.completion.GcpInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.GcpVolumeType;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class GcpCommands implements CommandMarker {

    public static final String PLATFORM = "GCP";
    private static final String P12_FILE_NOT_FOUND = "File not found with service account private key (p12) file.";
    private static final int VOLUME_COUNT_MIN = 1;
    private static final int VOLUME_COUNT_MAX = 12;
    private static final int VOLUME_SIZE_MIN = 1;
    private static final int VOLUME_SIZE_MAX = 1024;

    private ShellContext shellContext;
    private CredentialCommands baseCredentialCommands;
    private NetworkCommands baseNetworkCommands;
    private TemplateCommands baseTemplateCommands;
    private PlatformCommands basePlatformCommands;
    private StackCommands stackCommands;

    public GcpCommands(ShellContext shellContext,
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

    @CliAvailabilityIndicator(value = "stack create --GCP")
    public boolean createStackAvailable() {
        return stackCommands.createStackAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "template create --GCP")
    public boolean createTemplateAvailable() {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "platform create --GCP")
    public boolean createPlatformAvailable() {
        return basePlatformCommands.createPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "network create --GCP")
    public boolean createNetworkAvailable() {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "credential create --GCP")
    public boolean createCredentialAvailable() {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM);
    }

    @CliCommand(value = "credential create --GCP", help = "Create a new Gcp credential")
    public String createCredential(
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
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the credential belongs to") Long platformId
    ) {
        String serviceAccountPrivateKey;

        try {
            serviceAccountPrivateKey = Base64.encodeBase64String(Files.readAllBytes(serviceAccountPrivateKeyPath.toPath())).replaceAll("\n", "");
        } catch (IOException ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(P12_FILE_NOT_FOUND);
        }
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("projectId", projectId);
        parameters.put("serviceAccountId", serviceAccountId);
        parameters.put("serviceAccountPrivateKey", serviceAccountPrivateKey);
        return baseCredentialCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, description, publicInAccount, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --GCP", help = "Create a new GCP network configuration")
    public String createNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = false, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "networkId", mandatory = false, help = "Id of a custom network") String networkId,
            @CliOption(key = "subnetId", mandatory = false, help = "Id of a custom subnet") String subnetId,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the network as visible for all members of the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the network") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        if (networkId != null) {
            parameters.put("networkId", networkId);
        }
        if (subnetId != null) {
            parameters.put("subnetId", subnetId);
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "template create --GCP", help = "Create a new GCP template")
    public String createGcpTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "type of the VM") GcpInstanceType instanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") GcpVolumeType volumeType,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") Long platformId
    ) {
        publicInAccount = publicInAccount == null ? false : publicInAccount;
        if (volumeCount < VOLUME_COUNT_MIN || volumeCount > VOLUME_COUNT_MAX) {
            return "volumeCount has to be between 1 and 8.";
        }
        if (volumeSize < VOLUME_SIZE_MIN || volumeSize > VOLUME_SIZE_MAX) {
            return "VolumeSize has to be between 1 and 1024.";
        }
        Map<String, Object> parameters = new HashMap<>();
        return baseTemplateCommands.create(name, instanceType.getName(), volumeCount, volumeSize, volumeType == null ? "pd-standard" : volumeType.getName(),
                publicInAccount, description, parameters, platformId, PLATFORM);
    }

    @CliCommand(value = "cluster fileSystem --GCS", help = "Set GCS fileSystem on cluster")
    public String setGcsFileSystem(
            @CliOption(key = "defaultFileSystem", mandatory = true, help = "Use as default fileSystem") Boolean defaultFileSystem,
            @CliOption(key = "projectId", mandatory = true, help = "projectId of the GCS") String projectId,
            @CliOption(key = "serviceAccountEmail", mandatory = true, help = "serviceAccountEmail of the GCS") String serviceAccountEmail,
            @CliOption(key = "privateKeyEncoded", mandatory = true, help = "privateKeyEncoded of the GCS") String privateKeyEncoded,
            @CliOption(key = "defaultBucketName", mandatory = true, help = "defaultBucketName of the GCS") String defaultBucketName) {
        shellContext.setDefaultFileSystem(defaultFileSystem);
        shellContext.setFileSystemType(FileSystemType.GCS);
        Map<String, Object> props = new HashMap<>();
        props.put("projectId", projectId);
        props.put("serviceAccountEmail", serviceAccountEmail);
        props.put("privateKeyEncoded", privateKeyEncoded);
        props.put("defaultBucketName", defaultBucketName);
        shellContext.setFileSystemParameters(props);
        return "GCS filesystem configured";
    }

    @CliCommand(value = "platform create --GCP", help = "Create a new GCP platform configuration")
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

    @CliCommand(value = "stack create --GCP", help = "Create a new stack based on a template")
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
