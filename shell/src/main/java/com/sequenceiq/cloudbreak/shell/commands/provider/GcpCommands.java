package com.sequenceiq.cloudbreak.shell.commands.provider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
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
import com.sequenceiq.cloudbreak.shell.commands.InstanceGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.NetworkCommands;
import com.sequenceiq.cloudbreak.shell.commands.PlatformCommands;
import com.sequenceiq.cloudbreak.shell.commands.SecurityGroupCommands;
import com.sequenceiq.cloudbreak.shell.commands.StackCommands;
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands;
import com.sequenceiq.cloudbreak.shell.completion.GcpInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.GcpOrchestratorType;
import com.sequenceiq.cloudbreak.shell.completion.GcpVolumeType;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.completion.PlatformVariant;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupName;
import com.sequenceiq.cloudbreak.shell.completion.SecurityRules;
import com.sequenceiq.cloudbreak.shell.completion.StackAvailabilityZone;
import com.sequenceiq.cloudbreak.shell.completion.StackRegion;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;
import com.sequenceiq.cloudbreak.shell.util.TagParser;

public class GcpCommands implements CommandMarker {

    public static final String PLATFORM = "GCP";

    private static final String P12_FILE_NOT_FOUND = "File not found with service account private key (p12) file.";

    private ShellContext shellContext;

    private CredentialCommands baseCredentialCommands;

    private NetworkCommands baseNetworkCommands;

    private SecurityGroupCommands baseSecurityGroupCommands;

    private TemplateCommands baseTemplateCommands;

    private PlatformCommands basePlatformCommands;

    private StackCommands stackCommands;

    private InstanceGroupCommands baseInstanceGroupCommands;

    public GcpCommands(ShellContext shellContext,
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

    @CliAvailabilityIndicator(value = "stack create --GCP")
    public boolean createStackAvailable() {
        return stackCommands.createStackAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "template create --GCP")
    public boolean createTemplateAvailable() {
        return baseTemplateCommands.createTemplateAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "platform create --GCP")
    public boolean createPlatformAvailable() {
        return basePlatformCommands.createPlatformAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = {"network create --GCP --NEW", "network create --GCP --NEW_SUBNET",
            "network create --GCP --EXISTING_SUBNET", "network create --GCP --LEGACY"})
    public boolean createNetworkAvailable() {
        return baseNetworkCommands.createNetworkAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = {"securitygroup create --GCP --NEW"})
    public boolean createSecurityGroupAvailable() {
        return baseSecurityGroupCommands.createSecurityGroupAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "credential create --GCP")
    public boolean createCredentialAvailable() {
        return baseCredentialCommands.createCredentialAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliAvailabilityIndicator(value = "instancegroup configure --GCP")
    public boolean configureInstanceGroupAvailable() {
        return baseInstanceGroupCommands.createInstanceGroupAvailable(PLATFORM) && shellContext.isPlatformAvailable(PLATFORM);
    }

    @CliCommand(value = "credential create --GCP", help = "Create a new GCP credential")
    public String createCredential(
            @CliOption(key = "name", mandatory = true, help = "Name of the credential") String name,
            @CliOption(key = "projectId", mandatory = true, help = "projectId of the credential") String projectId,
            @CliOption(key = "serviceAccountId", mandatory = true, help = "serviceAccountId of the credential") String serviceAccountId,
            @CliOption(key = "serviceAccountPrivateKeyPath", mandatory = true, help = "path of a service account private key (p12) file")
            File serviceAccountPrivateKeyPath,
            @CliOption(key = "publicInAccount", help = "flags if the credential is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the credential") String description,
            @CliOption(key = "platformId", help = "Id of a platform the credential belongs to") Long platformId
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
        return baseCredentialCommands.create(name, description, publicInAccount, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --GCP --NEW", help = "Create a GCP network configuration with a new network and a new subnet")
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

    @CliCommand(value = "network create --GCP --NEW_SUBNET", help = "Create a GCP network configuration with a new subnet in an existing network")
    public String createNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "subnet", mandatory = true, help = "Subnet of the network in CIDR format") String subnet,
            @CliOption(key = "networkId", mandatory = true, help = "Id of a custom network") String networkId,
            @CliOption(key = "publicInAccount", help = "Marks the network as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the network") String description,
            @CliOption(key = "platformId", help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        if (networkId != null) {
            parameters.put("networkId", networkId);
        }
        return baseNetworkCommands.create(name, subnet, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "network create --GCP --EXISTING_SUBNET", help = "Create a GCP network which uses an existing subnet in an existing network")
    public String createExistingSubnetNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "networkId", mandatory = true, help = "Id of a custom network") String networkId,
            @CliOption(key = "subnetId", mandatory = true, help = "Id of a custom subnet") String subnetId,
            @CliOption(key = "publicInAccount", help = "Marks the network as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the network") String description,
            @CliOption(key = "platformId", help = "Id of a platform the network belongs to") Long platformId,
            @CliOption(key = "noPublicIp", help = "If true, no public IP is created for the instances") Boolean noPublicIp,
            @CliOption(key = "noFirewallRules", help = "If true, no new firewall rules will be created for the network") Boolean noFirewallRules
    ) {
        Map<String, Object> parameters = new HashMap<>();
        if (networkId != null) {
            parameters.put("networkId", networkId);
        }
        if (subnetId != null) {
            parameters.put("subnetId", subnetId);
        }
        if (noPublicIp != null) {
            parameters.put("noPublicIp", noPublicIp);
        }
        if (noFirewallRules != null) {
            parameters.put("noFirewallRules", noFirewallRules);
        }
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "securitygroup create --GCP --NEW", help = "Create an GCP security group")
    public String createNewSecurityGroup(
            @CliOption(key = "name", mandatory = true, help = "Name of the security group") String name,
            @CliOption(key = "description", help = "Description of the security group") String description,
            @CliOption(key = "rules", mandatory = true,
                    help = "Security rules in the following format: ';' separated list of <cidr>:<protocol>:<comma separated port list>") SecurityRules rules,
            @CliOption(key = "publicInAccount", help = "Marks the securitygroup as visible for all members of the account",
                    specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") Boolean publicInAccount) {
        return baseSecurityGroupCommands.create(name, description, null, PLATFORM, rules, publicInAccount);
    }

    @CliCommand(value = "network create --GCP --LEGACY", help = "Create a legacy GCP network configuration without subnet")
    public String createLegacyNetwork(
            @CliOption(key = "name", mandatory = true, help = "Name of the network") String name,
            @CliOption(key = "networkId", help = "Id of a custom network") String networkId,
            @CliOption(key = "publicInAccount", help = "Marks the network as visible for all members of the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the network") String description,
            @CliOption(key = "platformId", help = "Id of a platform the network belongs to") Long platformId
    ) {
        Map<String, Object> parameters = new HashMap<>();
        if (networkId != null) {
            parameters.put("networkId", networkId);
        }
        return baseNetworkCommands.create(name, null, publicInAccount, description, platformId, parameters, PLATFORM);
    }

    @CliCommand(value = "template create --GCP", help = "Create a new GCP template")
    public String createGcpTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "type of the VM") GcpInstanceType instanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "volumeType", help = "volumeType of the template") GcpVolumeType volumeType,
            @CliOption(key = "publicInAccount", help = "flags if the template is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount,
            @CliOption(key = "description", help = "Description of the template") String description,
            @CliOption(key = "platformId", help = "Id of a platform the template belongs to") Long platformId,
            @CliOption(key = "preemptible", help = "flags if the template is preemptible") Boolean preemptible
    ) {
        Map<String, Object> parameters = new HashMap<>();
        if (preemptible != null) {
            parameters.put("preemptible", preemptible);
        }
        return baseTemplateCommands.create(name, instanceType.getName(), volumeCount, volumeSize, volumeType == null ? "pd-standard" : volumeType.getName(),
                publicInAccount, description, parameters, platformId, PLATFORM);
    }

    @CliCommand(value = "instancegroup configure --GCP", help = "Configure instance groups")
    public String createInstanceGroup(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") InstanceGroup instanceGroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Nodecount for instanceGroup") Integer nodeCount,
            @CliOption(key = "ambariServer", mandatory = true, help = "Ambari server will be installed here if true") boolean ambariServer,
            @CliOption(key = "templateId", help = "TemplateId of the instanceGroup") InstanceGroupTemplateId instanceGroupTemplateId,
            @CliOption(key = "templateName", help = "TemplateName of the instanceGroup") InstanceGroupTemplateName instanceGroupTemplateName,
            @CliOption(key = "securityGroupId", help = "SecurityGroupId of the instanceGroup") SecurityGroupId instanceGroupSecurityGroupId,
            @CliOption(key = "securityGroupName", help = "SecurityGroupName of the instanceGroup") SecurityGroupName instanceGroupSecurityGroupName)
            throws Exception {

        Map<String, Object> parameters = new HashMap<>();

        return baseInstanceGroupCommands.create(instanceGroup, nodeCount, ambariServer, instanceGroupTemplateId, instanceGroupTemplateName,
                instanceGroupSecurityGroupId, instanceGroupSecurityGroupName, parameters);
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
            @CliOption(key = "description", help = "Description of the platform") String description
    ) {
        try {
            return basePlatformCommands.create(name, description, PLATFORM, Collections.emptyMap());
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @CliCommand(value = "stack create --GCP", help = "Create a new GCP stack based on a template")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the stack") String name,
            @CliOption(key = "sshKeyPath", help = "path of a public SSH key file") File sshKeyPath,
            @CliOption(key = "sshKeyUrl", help = "URL of a public SSH key url") String sshKeyUrl,
            @CliOption(key = "sshKeyString", help = "Raw data of a public SSH key file") String sshKeyString,
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
            @CliOption(key = "orchestrator", help = "select orchestrator variant version") GcpOrchestratorType orchestratorType,
            @CliOption(key = "platformVariant", help = "select platform variant version") PlatformVariant platformVariant,
            @CliOption(key = "customImage", help = "select customImage for cluster") String customImage,
            @CliOption(key = "tags", help = "created resources will be tagged with these key=value pairs, format: key1=value1,key2=value2") String tags,
            @CliOption(key = "wait", help = "Wait for stack creation", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean wait,
            @CliOption(key = "timeout", help = "Wait timeout if wait=true") Long timeout,
            @CliOption(key = "customDomain", help = "Custom domain for the nodes in the stack", mandatory = false) String customDomain,
            @CliOption(key = "customHostname", help = "Custom hostname for the nodes in the stack", mandatory = false) String customHostname,
            @CliOption(key = "clusterNameAsSubdomain", help = "Using the cluster name for subdomain", unspecifiedDefaultValue = "false",
                    specifiedDefaultValue = "true", mandatory = false) boolean clusterNameAsSubdomain,
            @CliOption(key = "hostgroupNameAsHostname", help = "Using the hostgroup names to create hostnames", unspecifiedDefaultValue = "false",
                    specifiedDefaultValue = "true", mandatory = false) boolean hostgroupNameAsHostname) {
        Map<String, String> params = new HashMap<>();
        if (availabilityZone == null) {
            Collection<String> availabilityZonesByRegion = shellContext.getAvailabilityZonesByRegion(shellContext.getActiveCloudPlatform(), region.getName());
            if (availabilityZonesByRegion == null || availabilityZonesByRegion.isEmpty()) {
                throw shellContext.exceptionTransformer().transformToRuntimeException(String.format("Availability zone for %s not found", region.getName()));
            }
            availabilityZone = new StackAvailabilityZone(availabilityZonesByRegion.iterator().next());
        }
        return stackCommands.create(name, sshKeyPath, sshKeyUrl, sshKeyString, region, availabilityZone, publicInAccount, onFailureAction, adjustmentType,
                threshold, wait, platformVariant, orchestratorType == null ? "SALT" : orchestratorType.getName(), PLATFORM, ambariVersion,
                hdpVersion, imageCatalog, params, TagParser.parseTagsIntoMap(tags), customImage, timeout,
                customDomain, customHostname, clusterNameAsSubdomain, hostgroupNameAsHostname);
    }
}
