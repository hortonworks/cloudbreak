package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;
import static com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.completion.AwsInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.AwsVolumeType;
import com.sequenceiq.cloudbreak.shell.completion.AzureInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.AzureVolumeType;
import com.sequenceiq.cloudbreak.shell.completion.GcpInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.GcpVolumeType;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class TemplateCommands implements CommandMarker {

    public static final int BUFFER = 1024;
    public static final int VOLUME_COUNT_MIN = 1;
    public static final int VOLUME_COUNT_MAX = 8;
    public static final int VOLUME_SIZE_MIN = 1;
    public static final int VOLUME_SIZE_MAX = 1024;

    private static final String CREATE_SUCCESS_MESSAGE = "Template created with id: '%d' and name: '%s'";

    @Inject
    private CloudbreakContext context;
    @Inject
    private CloudbreakClient cloudbreakClient;
    @Inject
    private ResponseTransformer responseTransformer;
    @Inject
    private ExceptionTransformer exceptionTransformer;

    @CliAvailabilityIndicator(value = "template list")
    public boolean isTemplateListCommandAvailable() {
        return !context.isMarathonMode();
    }

    @CliAvailabilityIndicator(value = "template show")
    public boolean isTemplateShowCommandAvailable() {
        return !context.isMarathonMode();
    }

    @CliAvailabilityIndicator(value = "template delete")
    public boolean isTemplateDeleteCommandAvailable() {
        return !context.isMarathonMode();
    }

    @CliAvailabilityIndicator({ "template create --GCP", "template create --EC2", "template create --AWS",
            "template create --AZURE", "template create --OPENSTACK" })
    public boolean isTemplateEc2CreateCommandAvailable() {
        return !context.isMarathonMode();
    }

    @CliCommand(value = "template list", help = "Shows the currently available cloud templates")
    public String listTemplates() {
        try {
            Set<TemplateResponse> publics = cloudbreakClient.templateEndpoint().getPublics();
            return renderSingleMap(responseTransformer.transformToMap(publics, "id", "name"), true, "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "template create --OPENSTACK", help = "Create a new OPENSTACK template")
    public String createOpenStackTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") String instanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description,
            @CliOption(key = "platformId", mandatory = false, help = "Id of a platform the template belongs to") Long platformId
    ) {
        try {
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (volumeCount < VOLUME_COUNT_MIN || volumeCount > VOLUME_COUNT_MAX) {
                return "volumeCount has to be between 1 and 8.";
            }
            if (volumeSize < VOLUME_SIZE_MIN || volumeSize > VOLUME_SIZE_MAX) {
                return "VolumeSize has to be between 1 and 1024.";
            }
            String cloudPlatform = "OPENSTACK";
            IdJson id;
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform(cloudPlatform);
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(instanceType);
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);
            if (platformId != null) {
                checkTopologyForResource(cloudbreakClient.topologyEndpoint().getPublics(), platformId, cloudPlatform);
            }
            templateRequest.setTopologyId(platformId);

            if (publicInAccount) {
                id = cloudbreakClient.templateEndpoint().postPublic(templateRequest);
            } else {
                id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return String.format(CREATE_SUCCESS_MESSAGE, id.getId(), name);
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }


    @CliCommand(value = "template create --AWS", help = "Create a new AWS template")
    public String createAwsTemplate(
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
            @CliOption(key = "topologyId", mandatory = false, help = "Id of a topology the template belongs to") Long topologyId
    ) {
        return createEc2Template(name, instanceType, volumeCount, volumeSize, volumeType, encrypted,
                spotPrice, sshLocation, publicInAccount, description, topologyId);
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
        try {
            String cloudPlatform = "AWS";
            publicInAccount = publicInAccount == null ? false : publicInAccount;
            if (volumeCount < VOLUME_COUNT_MIN || volumeCount > VOLUME_COUNT_MAX) {
                return "volumeCount has to be between 1 and 8.";
            }
            if (volumeSize < VOLUME_SIZE_MIN || volumeSize > VOLUME_SIZE_MAX) {
                return "VolumeSize has to be between 1 and 1024.";
            }
            IdJson id;
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform(cloudPlatform);
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(instanceType.getName());
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);
            templateRequest.setVolumeType(volumeType == null ? "gp2" : volumeType.getName());
            if (platformId != null) {
                checkTopologyForResource(cloudbreakClient.topologyEndpoint().getPublics(), platformId, cloudPlatform);
            }
            templateRequest.setTopologyId(platformId);

            Map<String, Object> params = new HashMap<>();
            params.put("sshLocation", sshLocation == null ? "0.0.0.0/0" : sshLocation);
            params.put("spotPrice", spotPrice == null ? null : spotPrice.toString());
            params.put("encrypted", encrypted == null ? false : encrypted);
            templateRequest.setParameters(params);

            if (publicInAccount) {
                id = cloudbreakClient.templateEndpoint().postPublic(templateRequest);
            } else {
                id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return String.format(CREATE_SUCCESS_MESSAGE, id.getId(), name);
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    private String getDescription(String description, String cloudPlatform) {
        return description == null ? cloudPlatform + " template was created by the cloudbreak-shell" : description;
    }


    @CliCommand(value = "template create --AZURE", help = "Create a new AZURE template")
    public String createAzureTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "type of the VM") AzureInstanceType vmType,
            @CliOption(key = "volumeType", mandatory = false, help = "volumeType of the template") AzureVolumeType volumeType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
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
        try {
            IdJson id;
            String cloudPlatform = "AZURE_RM";
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform(cloudPlatform);
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(vmType.getName());
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);
            templateRequest.setVolumeType(volumeType == null ? "Standard_LRS" : volumeType.getName());
            if (platformId != null) {
                checkTopologyForResource(cloudbreakClient.topologyEndpoint().getPublics(), platformId, cloudPlatform);
            }
            templateRequest.setTopologyId(platformId);

            if (publicInAccount) {
                id = cloudbreakClient.templateEndpoint().postPublic(templateRequest);
            } else {
                id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return String.format(CREATE_SUCCESS_MESSAGE, id.getId(), name);
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
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
        try {
            String cloudPlatform = "GCP";
            IdJson id;
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform(cloudPlatform);
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(instanceType.getName());
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);
            templateRequest.setVolumeType(volumeType == null ? "pd-standard" : volumeType.getName());
            if (platformId != null) {
                checkTopologyForResource(cloudbreakClient.topologyEndpoint().getPublics(), platformId, cloudPlatform);
            }
            templateRequest.setTopologyId(platformId);

            if (publicInAccount) {
                id = cloudbreakClient.templateEndpoint().postPublic(templateRequest);
            } else {
                id = cloudbreakClient.templateEndpoint().postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return String.format(CREATE_SUCCESS_MESSAGE, id.getId(), name);
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "template show", help = "Shows the template by its id or name")
    public Object showTemplate(
            @CliOption(key = "id", mandatory = false, help = "Id of the template") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the template") String name) {
        try {
            if (id != null) {
                return renderSingleMap(
                        responseTransformer.transformObjectToStringMap(cloudbreakClient.templateEndpoint().get(Long.valueOf(id))), "FIELD", "VALUE");
            } else if (name != null) {
                TemplateResponse aPublic = cloudbreakClient.templateEndpoint().getPublic(name);
                if (aPublic != null) {
                    return renderSingleMap(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            return "No template specified.";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "template delete", help = "Shows the template by its id or name")
    public Object deleteTemplate(
            @CliOption(key = "id", mandatory = false, help = "Id of the template") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the template") String name) {
        try {
            if (id != null) {
                cloudbreakClient.templateEndpoint().delete(Long.valueOf(id));
                return String.format("Template has been deleted, id: %s", id);
            } else if (name != null) {
                cloudbreakClient.templateEndpoint().deletePublic(name);
                return String.format("Template has been deleted, name: %s", name);
            }
            return "No template specified.";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private String readFileAsString(String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath));
        char[] buf = new char[BUFFER];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    private void createOrSelectBlueprintHint() throws Exception {
        if (context.isCredentialAccessible() && context.isBlueprintAccessible()) {
            context.setHint(Hints.CONFIGURE_INSTANCEGROUP);
        } else if (!context.isBlueprintAccessible()) {
            context.setHint(Hints.SELECT_BLUEPRINT);
        } else if (!context.isCredentialAccessible()) {
            context.setHint(Hints.SELECT_CREDENTIAL);
        } else if (context.isCredentialAvailable()
                && (context.getActiveHostGroups().size() == context.getInstanceGroups().size()
                && context.getActiveHostGroups().size() != 0)) {
            context.setHint(Hints.CREATE_STACK);
        } else if (context.isStackAccessible()) {
            context.setHint(Hints.CREATE_STACK);
        } else {
            context.setHint(Hints.NONE);
        }
    }
}
