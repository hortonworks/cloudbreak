package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupName;
import com.sequenceiq.cloudbreak.shell.completion.SecurityRules;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class SecurityGroupCommands implements CommandMarker {
    private static final String CREATE_SUCCESS_MSG = "Security group created and selected successfully, with id: '%s'";

    @Autowired
    private CloudbreakContext context;
    @Autowired
    private CloudbreakClient cloudbreakClient;
    @Autowired
    private ResponseTransformer responseTransformer;

    @CliAvailabilityIndicator({ "securitygroup create", "securitygroup list" })
    public boolean areSecurityGroupCommandsAvailable() {
        return true;
    }

    @CliAvailabilityIndicator({ "securitygroup delete", "securitygroup select", "securitygroup show" })
    public boolean areExistSecurityGroupCommandsAvailable() {
        return !context.getSecurityGroups().isEmpty();
    }

    @CliCommand(value = "securitygroup create", help = "Creates a new security group")
    public String createSecurityGroup(
            @CliOption(key = "name", mandatory = true, help = "Name of the security group") String name,
            @CliOption(key = "description", mandatory = false, help = "Description of the security group") String description,
            @CliOption(key = "rules", mandatory = true,
                    help = "Security rules in the following format: ';' separated list of <cidr>:<protocol>:<comma separated port list>") SecurityRules rules,
            @CliOption(key = "publicInAccount", mandatory = false, help = "Marks the securitygroup as visible for all members of the account",
                    specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") Boolean publicInAccount) throws Exception {
        try {
            Map<String, String> tcpRules = new HashMap<>();
            Map<String, String> udpRules = new HashMap<>();
            for (Map<String, String> rule : rules.getRules()) {
                if ("tcp".equals(rule.get("protocol"))) {
                    tcpRules.put(rule.get("subnet"), rule.get("ports"));
                } else {
                    udpRules.put(rule.get("subnet"), rule.get("ports"));
                }
            }
            IdJson id;
            SecurityGroupJson securityGroupJson = new SecurityGroupJson();
            securityGroupJson.setName(name);
            securityGroupJson.setDescription(description);
            securityGroupJson.setPublicInAccount(publicInAccount);
            List<SecurityRuleJson> securityRuleJsonList = new ArrayList<>();

            for (Map.Entry<String, String> stringStringEntry : tcpRules.entrySet()) {
                SecurityRuleJson securityRuleJson = new SecurityRuleJson();
                securityRuleJson.setPorts(stringStringEntry.getValue());
                securityRuleJson.setSubnet(stringStringEntry.getKey());
                securityRuleJson.setProtocol("tcp");
                securityRuleJsonList.add(securityRuleJson);
            }

            for (Map.Entry<String, String> stringStringEntry : udpRules.entrySet()) {
                SecurityRuleJson securityRuleJson = new SecurityRuleJson();
                securityRuleJson.setPorts(stringStringEntry.getValue());
                securityRuleJson.setSubnet(stringStringEntry.getKey());
                securityRuleJson.setProtocol("udp");
                securityRuleJsonList.add(securityRuleJson);
            }

            securityGroupJson.setSecurityRules(securityRuleJsonList);

            if (publicInAccount) {
                id = cloudbreakClient.securityGroupEndpoint().postPublic(securityGroupJson);
            } else {
                id = cloudbreakClient.securityGroupEndpoint().postPrivate(securityGroupJson);
            }
            context.putSecurityGroup(id.getId().toString(), name);
            context.setActiveSecurityGroupId(id.getId().toString());
            setHint();
            return String.format(CREATE_SUCCESS_MSG, id);
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "securitygroup list", help = "Shows the currently available security groups")
    public String listSecurityGroups() {
        try {
            Set<SecurityGroupJson> publics = cloudbreakClient.securityGroupEndpoint().getPublics();
            Map<String, String> updatedGroups = new HashMap<>();
            for (SecurityGroupJson aPublic : publics) {
                updatedGroups.put(aPublic.getId().toString(), aPublic.getName());
            }
            context.setSecurityGroups(updatedGroups);
            return renderSingleMap(updatedGroups, "ID", "NAME");
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @CliCommand(value = "securitygroup select", help = "Select a security group by the given id or name")
    public String selectSecurityGroup(
            @CliOption(key = "id", mandatory = false, help = "Id of the security group") SecurityGroupId secId,
            @CliOption(key = "name", mandatory = false, help = "Name of the security group") SecurityGroupName secName) {
        if (secId == null && secName == null) {
            return "Both ID and name cannot be null";
        }
        String message = "No security group has been selected";
        String id = secId == null ? null : secId.getName();
        String name = secName == null ? null : secName.getName();
        Map<String, String> securityGroups = context.getSecurityGroups();
        if (id != null && securityGroups.containsKey(id)) {
            context.setActiveSecurityGroupId(id);
            setHint();
            message = "Security group has been selected with id: " + id;
        } else if (securityGroups.containsValue(name)) {
            for (String groupId : securityGroups.keySet()) {
                if (securityGroups.get(groupId).equals(name)) {
                    context.setActiveSecurityGroupId(groupId);
                    setHint();
                    message = "Security group has been selected with name: " + name;
                    break;
                }
            }
        }
        return message;
    }

    @CliCommand(value = "securitygroup show", help = "Shows the security group by its id or name")
    public Object showSecurityGroup(
            @CliOption(key = "id", mandatory = false, help = "Id of the security group") SecurityGroupId groupId,
            @CliOption(key = "name", mandatory = false, help = "Name of the security group") SecurityGroupName groupName) {
        try {
            String id = groupId == null ? null : groupId.getName();
            String name = groupName == null ? null : groupName.getName();
            if (id != null) {
                SecurityGroupJson securityGroupJson = cloudbreakClient.securityGroupEndpoint().get(Long.valueOf(id));
                return renderSingleMap(responseTransformer.transformObjectToStringMap(securityGroupJson), "FIELD", "VALUE");
            } else if (name != null) {
                SecurityGroupJson aPublic = cloudbreakClient.securityGroupEndpoint().getPublic(name);
                return renderSingleMap(responseTransformer.transformObjectToStringMap(aPublic), "FIELD", "VALUE");
            }
            return "Security group could not be found!";
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @CliCommand(value = "securitygroup delete", help = "Delete security group by the given id or name")
    public Object deleteSecurityGroup(
            @CliOption(key = "id", mandatory = false, help = "Id of the security group") SecurityGroupId securityGroupId,
            @CliOption(key = "name", mandatory = false, help = "Name of the security group") SecurityGroupName securityGroupName) {
        try {
            String id = securityGroupId == null ? null : securityGroupId.getName();
            String name = securityGroupName == null ? null : securityGroupName.getName();
            if (id != null) {
                cloudbreakClient.securityGroupEndpoint().delete(Long.valueOf(id));
                refreshSecurityGroupsInContext();
                return String.format("SecurityGroup deleted with %s id", name);
            } else if (name != null) {
                cloudbreakClient.securityGroupEndpoint().deletePublic(name);
                refreshSecurityGroupsInContext();
                return String.format("SecurityGroup deleted with %s name", name);
            }
            return "No security group specified.";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private void refreshSecurityGroupsInContext() throws Exception {
        context.getSecurityGroups().clear();
        Set<SecurityGroupJson> publics = cloudbreakClient.securityGroupEndpoint().getPublics();
        for (SecurityGroupJson securityGroup : publics) {
            context.putSecurityGroup(securityGroup.getId().toString(), securityGroup.getName());
        }
        if (!context.getSecurityGroups().containsKey(context.getActiveSecurityGroupId())) {
            context.setActiveSecurityGroupId(null);
        }
    }

    private void setHint() {
        context.setHint(Hints.CREATE_STACK);
    }

}
