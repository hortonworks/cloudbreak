package com.sequenceiq.cloudbreak.shell.commands.base;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.SecurityGroupRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupResponse;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.commands.SecurityGroupCommands;
import com.sequenceiq.cloudbreak.shell.completion.SecurityRules;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class BaseSecurityGroupCommands implements BaseCommands, SecurityGroupCommands {
    private static final String CREATE_SUCCESS_MSG = "Security group created and selected successfully, with id: '%d' and name: '%s'";

    private ShellContext shellContext;

    public BaseSecurityGroupCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @Override
    public boolean createSecurityGroupAvailable(String platform) {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String create(String name, String description, String existingSecurityGroupId, String platform,
            SecurityRules rules, Boolean publicInAccount) {
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
            Long id;
            SecurityGroupRequest securityGroupRequest = new SecurityGroupRequest();
            securityGroupRequest.setName(name);
            securityGroupRequest.setDescription(description);
            securityGroupRequest.setCloudPlatform(platform);
            List<SecurityRuleRequest> securityRuleRequestList = new ArrayList<>();
            if (existingSecurityGroupId != null && !existingSecurityGroupId.isEmpty()) {
                securityGroupRequest.setSecurityGroupId(existingSecurityGroupId);
            }

            for (Entry<String, String> stringStringEntry : tcpRules.entrySet()) {
                SecurityRuleRequest securityRuleRequest = new SecurityRuleRequest();
                securityRuleRequest.setPorts(stringStringEntry.getValue());
                securityRuleRequest.setSubnet(stringStringEntry.getKey());
                securityRuleRequest.setProtocol("tcp");
                securityRuleRequestList.add(securityRuleRequest);
            }

            for (Entry<String, String> stringStringEntry : udpRules.entrySet()) {
                SecurityRuleRequest securityRuleRequest = new SecurityRuleRequest();
                securityRuleRequest.setPorts(stringStringEntry.getValue());
                securityRuleRequest.setSubnet(stringStringEntry.getKey());
                securityRuleRequest.setProtocol("udp");
                securityRuleRequestList.add(securityRuleRequest);
            }

            securityGroupRequest.setSecurityRules(securityRuleRequestList);

            if (publicInAccount) {
                id = shellContext.cloudbreakClient().securityGroupEndpoint().postPublic(securityGroupRequest).getId();
            } else {
                id = shellContext.cloudbreakClient().securityGroupEndpoint().postPrivate(securityGroupRequest).getId();
            }
            shellContext.putSecurityGroup(id, name);
            setHint();
            return String.format(CREATE_SUCCESS_MSG, id, name);
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator({"securitygroup delete --id", "securitygroup delete --name"})
    @Override
    public boolean deleteAvailable() {
        return !shellContext.getSecurityGroups().isEmpty() && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String delete(Long securityGroupId, String securityGroupName) {
        try {
            Long id = securityGroupId;
            String name = securityGroupName;
            if (id != null) {
                shellContext.cloudbreakClient().securityGroupEndpoint().delete(id);
                refreshSecurityGroupsInContext();
                return String.format("SecurityGroup deleted with %s id", name);
            } else if (name != null) {
                shellContext.cloudbreakClient().securityGroupEndpoint().deletePublic(name);
                refreshSecurityGroupsInContext();
                return String.format("SecurityGroup deleted with %s name", name);
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("No security group specified");
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

    @CliCommand(value = "securitygroup select --id", help = "Select the securitygroup by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "securitygroup select --name", help = "Select the securitygroup by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return select(null, name);
    }

    @CliAvailabilityIndicator("securitygroup list")
    @Override
    public boolean listAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @CliCommand(value = "securitygroup list", help = "Shows the currently available security groups")
    @Override
    public String list() {
        try {
            Set<SecurityGroupResponse> publics = shellContext.cloudbreakClient().securityGroupEndpoint().getPublics();
            Map<Long, String> updatedGroups = new HashMap<>();
            for (SecurityGroupResponse aPublic : publics) {
                updatedGroups.put(aPublic.getId(), aPublic.getName());
            }
            shellContext.setSecurityGroups(updatedGroups);
            return shellContext.outputTransformer().render(updatedGroups, "ID", "NAME");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator({"securitygroup show --id", "securitygroup show --name"})
    @Override
    public boolean showAvailable() {
        return !shellContext.getSecurityGroups().isEmpty() && !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public String show(Long groupId, String groupName, OutPutType outPutType) {
        try {
            outPutType = outPutType == null ? OutPutType.RAW : outPutType;
            Long id = groupId;
            String name = groupName;
            if (id != null) {
                SecurityGroupResponse securityGroupResponse = shellContext.cloudbreakClient().securityGroupEndpoint().get(id);
                return shellContext.outputTransformer().render(outPutType,
                        shellContext.responseTransformer().transformObjectToStringMap(securityGroupResponse),
                        "FIELD", "VALUE");
            } else if (name != null) {
                SecurityGroupResponse aPublic = shellContext.cloudbreakClient().securityGroupEndpoint().getPublic(name);
                return shellContext.outputTransformer().render(outPutType,
                        shellContext.responseTransformer().transformObjectToStringMap(aPublic),
                        "FIELD", "VALUE");
            }
            throw shellContext.exceptionTransformer().transformToRuntimeException("Security group could not be found");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "securitygroup show --id", help = "Show the securitygroup by its id")
    @Override
    public String showById(
            @CliOption(key = "", mandatory = true) Long id,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(id, null, outPutType);
    }

    @CliCommand(value = "securitygroup show --name", help = "Show the securitygroup by its name")
    @Override
    public String showByName(
            @CliOption(key = "", mandatory = true) String name,
            @CliOption(key = "outputType", help = "OutputType of the response") OutPutType outPutType) throws Exception {
        return show(null, name, outPutType);
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    private void refreshSecurityGroupsInContext() {
        shellContext.getSecurityGroups().clear();
        Set<SecurityGroupResponse> publics = shellContext.cloudbreakClient().securityGroupEndpoint().getPublics();
        for (SecurityGroupResponse securityGroup : publics) {
            shellContext.putSecurityGroup(securityGroup.getId(), securityGroup.getName());
        }
    }

    private void setHint() {
        shellContext.setHint(Hints.CREATE_STACK);
    }
}
