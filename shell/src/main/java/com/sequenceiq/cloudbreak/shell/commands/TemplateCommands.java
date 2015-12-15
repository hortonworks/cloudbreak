package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.TemplateEndpoint;
import com.sequenceiq.cloudbreak.model.IdJson;
import com.sequenceiq.cloudbreak.model.TemplateRequest;
import com.sequenceiq.cloudbreak.model.TemplateResponse;
import com.sequenceiq.cloudbreak.shell.completion.AwsInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.AwsVolumeType;
import com.sequenceiq.cloudbreak.shell.completion.AzureInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.GcpInstanceType;
import com.sequenceiq.cloudbreak.shell.completion.GcpVolumeType;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class TemplateCommands implements CommandMarker {

    public static final int BUFFER = 1024;
    public static final int VOLUME_COUNT_MIN = 1;
    public static final int VOLUME_COUNT_MAX = 8;
    public static final int VOLUME_SIZE_MIN = 1;
    public static final int VOLUME_SIZE_MAX = 1024;
    @Autowired
    private CloudbreakContext context;
    @Autowired
    private TemplateEndpoint templateEndpoint;
    @Autowired
    private ResponseTransformer responseTransformer;

    @CliAvailabilityIndicator(value = "template list")
    public boolean isTemplateListCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "template show")
    public boolean isTemplateShowCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "template delete")
    public boolean isTemplateDeleteCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator({ "template create --GCP", "template create --EC2", "template create --AZURE", "template create --OPENSTACK" })
    public boolean isTemplateEc2CreateCommandAvailable() {
        return true;
    }

    @CliCommand(value = "template list", help = "Shows the currently available cloud templates")
    public String listTemplates() {
        try {
            Set<TemplateResponse> publics = templateEndpoint.getPublics();
            return renderSingleMap(responseTransformer.transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @CliCommand(value = "template create --OPENSTACK", help = "Create a new OPENSTACK template")
    public String createOpenStackTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "instanceType of the template") String instanceType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description
    ) {
        try {
            if (volumeCount < VOLUME_COUNT_MIN || volumeCount > VOLUME_COUNT_MAX) {
                return "volumeCount has to be between 1 and 8.";
            }
            if (volumeSize < VOLUME_SIZE_MIN || volumeSize > VOLUME_SIZE_MAX) {
                return "VolumeSize has to be between 1 and 1024.";
            }
            IdJson id;
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform("OPENSTACK");
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(instanceType);
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);

            if (publicInAccount(publicInAccount)) {
                id = templateEndpoint.postPublic(templateRequest);
            } else {
                id = templateEndpoint.postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return "Template created, id: " + id.getId().toString();
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "template create --EC2", help = "Create a new EC2 template")
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
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description
    ) {
        try {
            if (volumeCount < VOLUME_COUNT_MIN || volumeCount > VOLUME_COUNT_MAX) {
                return "volumeCount has to be between 1 and 8.";
            }
            if (volumeSize < VOLUME_SIZE_MIN || volumeSize > VOLUME_SIZE_MAX) {
                return "VolumeSize has to be between 1 and 1024.";
            }
            IdJson id;
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform("AWS");
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(instanceType.getName());
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);
            templateRequest.setVolumeType(volumeType == null ? "gp2" : volumeType.getName());

            Map<String, Object> params = new HashMap<>();
            params.put("sshLocation", sshLocation == null ? "0.0.0.0/0" : sshLocation);
            params.put("spotPrice", spotPrice == null ? null : spotPrice.toString());
            params.put("encrypted", encrypted == null ? false : encrypted);
            templateRequest.setParameters(params);

            if (publicInAccount(publicInAccount)) {
                id = templateEndpoint.postPublic(templateRequest);
            } else {
                id = templateEndpoint.postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return "Template created, id: " + id.getId();
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private String getDescription(String description, String cloudPlatform) {
        return description == null ? cloudPlatform + " template was created by the cloudbreak-shell" : description;
    }


    @CliCommand(value = "template create --AZURE", help = "Create a new AZURE template")
    public String createAzureTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the template") String name,
            @CliOption(key = "instanceType", mandatory = true, help = "type of the VM") AzureInstanceType vmType,
            @CliOption(key = "volumeCount", mandatory = true, help = "volumeCount of the template") Integer volumeCount,
            @CliOption(key = "volumeSize", mandatory = true, help = "volumeSize(GB) of the template") Integer volumeSize,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the template is public in the account") Boolean publicInAccount,
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description
    ) {
        if (volumeCount < VOLUME_COUNT_MIN || volumeCount > VOLUME_COUNT_MAX) {
            return "volumeCount has to be between 1 and 8.";
        }
        if (volumeSize < VOLUME_SIZE_MIN || volumeSize > VOLUME_SIZE_MAX) {
            return "VolumeSize has to be between 1 and 1024.";
        }
        try {
            IdJson id;
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform("AZURE");
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(vmType.getName());
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);

            if (publicInAccount(publicInAccount)) {
                id = templateEndpoint.postPublic(templateRequest);
            } else {
                id = templateEndpoint.postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return "Template created, id: " + id.getId().toString();
        } catch (Exception ex) {
            return ex.toString();
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
            @CliOption(key = "description", mandatory = false, help = "Description of the template") String description
    ) {
        if (volumeCount < VOLUME_COUNT_MIN || volumeCount > VOLUME_COUNT_MAX) {
            return "volumeCount has to be between 1 and 8.";
        }
        if (volumeSize < VOLUME_SIZE_MIN || volumeSize > VOLUME_SIZE_MAX) {
            return "VolumeSize has to be between 1 and 1024.";
        }
        try {
            IdJson id;
            TemplateRequest templateRequest = new TemplateRequest();
            templateRequest.setCloudPlatform("GCP");
            templateRequest.setName(name);
            templateRequest.setDescription(description);
            templateRequest.setInstanceType(instanceType.getName());
            templateRequest.setVolumeCount(volumeCount);
            templateRequest.setVolumeSize(volumeSize);
            templateRequest.setVolumeType(volumeType == null ? "pd-standard" : volumeType.getName());

            if (publicInAccount(publicInAccount)) {
                id = templateEndpoint.postPublic(templateRequest);
            } else {
                id = templateEndpoint.postPrivate(templateRequest);
            }
            createOrSelectBlueprintHint();
            return "Template created, id: " + id.getId().toString();
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private boolean publicInAccount(Boolean publicInAccount) {
        return publicInAccount == null ? false : publicInAccount;
    }


    @CliCommand(value = "template show", help = "Shows the template by its id or name")
    public Object showTemplate(
            @CliOption(key = "id", mandatory = false, help = "Id of the template") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the template") String name) {
        try {
            if (id != null) {
                return renderSingleMap(responseTransformer.transformObjectToStringMap(templateEndpoint.get(Long.valueOf(id))), "FIELD", "VALUE");
            } else if (name != null) {
                TemplateResponse aPublic = templateEndpoint.getPublic(name);
                if (aPublic != null) {
                    return renderSingleMap(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
                }
            }
            return "No template specified.";
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @CliCommand(value = "template delete", help = "Shows the template by its id or name")
    public Object deleteTemplate(
            @CliOption(key = "id", mandatory = false, help = "Id of the template") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the template") String name) {
        try {
            if (id != null) {
                templateEndpoint.delete(Long.valueOf(id));
                return String.format("Template has been selected, id: %s", id);
            } else if (name != null) {
                templateEndpoint.deletePublic(name);
                return String.format("Tempalte has been selected, name: %s", name);
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
