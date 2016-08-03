package com.sequenceiq.cloudbreak.shell.commands.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.completion.SecurityRules;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class SecurityGroupCommands implements BaseCommands {
    private static final String CREATE_SUCCESS_MSG = "Security group created and selected successfully, with id: '%d' and name: '%s'";

    private ShellContext shellContext;

    public SecurityGroupCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "securitygroup create")
    public boolean createAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "securitygroup create", help = "Creates a new security group")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the security group") String name,
            @CliOption(key = "description", help = "Description of the security group") String description,
            @CliOption(key = "rules", mandatory = true,
                    help = "Security rules in the following format: ';' separated list of <cidr>:<protocol>:<comma separated port list>") SecurityRules rules,
            @CliOption(key = "publicInAccount", help = "Marks the securitygroup as visible for all members of the account",
                    specifiedDefaultValue = "true", unspecifiedDefaultValue = "false") Boolean publicInAccount) {
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
            publicInAccount = publicInAccount == null ? false : publicInAccount;

            if (publicInAccount) {
                id = shellContext.cloudbreakClient().securityGroupEndpoint().postPublic(securityGroupJson);
            } else {
                id = shellContext.cloudbreakClient().securityGroupEndpoint().postPrivate(securityGroupJson);
            }
            shellContext.putSecurityGroup(id.getId(), name);
            setHint();
            return String.format(CREATE_SUCCESS_MSG, id.getId(), name);
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "securitygroup delete --id", "securitygroup delete --name" })
    @Override
    public boolean deleteAvailable() {
        return !shellContext.getSecurityGroups().isEmpty() && !shellContext.isMarathonMode();
    }

    @Override
    public String delete(Long securityGroupId, String securityGroupName) {
        try {
            Long id = securityGroupId == null ? null : securityGroupId;
            String name = securityGroupName == null ? null : securityGroupName;
            if (id != null) {
                shellContext.cloudbreakClient().securityGroupEndpoint().delete(id);
                refreshSecurityGroupsInContext();
                return String.format("SecurityGroup deleted with %s id", name);
            } else if (name != null) {
                shellContext.cloudbreakClient().securityGroupEndpoint().deletePublic(name);
                refreshSecurityGroupsInContext();
                return String.format("SecurityGroup deleted with %s name", name);
            }
            return "No security group specified.";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "securitygroup delete --id", help = "Delete the securitygroup by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "securitygroup delete --name", help = "Delete the securitygroup by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public boolean selectAvailable() {
        return false;
    }

    @Override
    public String select(Long secId, String secName) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Select is not supported on securitygroups");
    }

    @CliCommand(value = "securitygroup select --id", help = "Delete the securitygroup by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "securitygroup select --name", help = "Delete the securitygroup by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return select(null, name);
    }

    @CliAvailabilityIndicator(value = "securitygroup list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "securitygroup list", help = "Shows the currently available security groups")
    public String list() {
        try {
            Set<SecurityGroupJson> publics = shellContext.cloudbreakClient().securityGroupEndpoint().getPublics();
            Map<Long, String> updatedGroups = new HashMap<>();
            for (SecurityGroupJson aPublic : publics) {
                updatedGroups.put(aPublic.getId(), aPublic.getName());
            }
            shellContext.setSecurityGroups(updatedGroups);
            return shellContext.outputTransformer().render(updatedGroups, "ID", "NAME");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "securitygroup show --id", "securitygroup show --name" })
    @Override
    public boolean showAvailable() {
        return !shellContext.getSecurityGroups().isEmpty() && !shellContext.isMarathonMode();
    }

    @Override
    public String show(Long groupId, String groupName) {
        try {
            Long id = groupId == null ? null : groupId;
            String name = groupName == null ? null : groupName;
            if (id != null) {
                SecurityGroupJson securityGroupJson = shellContext.cloudbreakClient().securityGroupEndpoint().get(id);
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(securityGroupJson),
                        "FIELD", "VALUE");
            } else if (name != null) {
                SecurityGroupJson aPublic = shellContext.cloudbreakClient().securityGroupEndpoint().getPublic(name);
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE");
            }
            return "Security group could not be found!";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "securitygroup show --id", help = "Show the securitygroup by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "securitygroup show --name", help = "Show the securitygroup by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    private void refreshSecurityGroupsInContext() {
        shellContext.getSecurityGroups().clear();
        Set<SecurityGroupJson> publics = shellContext.cloudbreakClient().securityGroupEndpoint().getPublics();
        for (SecurityGroupJson securityGroup : publics) {
            shellContext.putSecurityGroup(securityGroup.getId(), securityGroup.getName());
        }
    }

    private void setHint() {
        shellContext.setHint(Hints.CREATE_STACK);
    }

}
