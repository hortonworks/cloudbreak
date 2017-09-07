package com.sequenceiq.cloudbreak.shell.commands.base;

import java.util.HashSet;
import java.util.Map;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.google.common.primitives.Longs;
import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupResponse;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.shell.commands.InstanceGroupCommands;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupName;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class BaseInstanceGroupCommands implements CommandMarker, InstanceGroupCommands {

    private ShellContext shellContext;

    public BaseInstanceGroupCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator("instancegroup configure")
    public boolean createAvailable() {
        return (shellContext.isBlueprintAvailable() && shellContext.isCredentialAvailable())
                && !shellContext.isMarathonMode()
                && !shellContext.isYarnMode();
    }

    @CliAvailabilityIndicator("instancegroup delete")
    public boolean deleteAvailable() {
        return (shellContext.isBlueprintAvailable() && shellContext.isCredentialAvailable())
                && !shellContext.isMarathonMode()
                && !shellContext.isYarnMode();
    }

    @CliAvailabilityIndicator("instancegroup show")
    public boolean showAvailable() {
        return !shellContext.isMarathonMode() && !shellContext.isYarnMode();
    }

    @Override
    public boolean createInstanceGroupAvailable(String platform) {
        return shellContext.isCredentialAvailable() && shellContext.getActiveCloudPlatform().equals(platform);
    }

    @Override
    public String create(InstanceGroup instanceGroup, Integer nodeCount, boolean ambariServer, InstanceGroupTemplateId instanceGroupTemplateId,
            InstanceGroupTemplateName instanceGroupTemplateName, SecurityGroupId instanceGroupSecurityGroupId,
            SecurityGroupName instanceGroupSecurityGroupName, Map<String, Object> parameters) {
        try {
            String templateId;
            if (instanceGroupTemplateId != null) {
                templateId = instanceGroupTemplateId.getName();
            } else if (instanceGroupTemplateName != null) {
                TemplateResponse aPublic = shellContext.cloudbreakClient().templateEndpoint().getPublic(instanceGroupTemplateName.getName());
                if (aPublic != null) {
                    templateId = aPublic.getId().toString();
                } else {
                    throw shellContext.exceptionTransformer().transformToRuntimeException(String.format("Template not found by name: %s",
                            instanceGroupTemplateName.getName()));
                }
            } else {
                throw shellContext.exceptionTransformer().transformToRuntimeException(
                        "Template name or id is not defined for instanceGroup (use --templateName or --templateId)");
            }
            String securityGroupId;
            if (instanceGroupSecurityGroupId != null) {
                securityGroupId = instanceGroupSecurityGroupId.getName();
            } else if (instanceGroupSecurityGroupName != null) {
                SecurityGroupResponse aPublic = shellContext.cloudbreakClient().securityGroupEndpoint().getPublic(instanceGroupSecurityGroupName.getName());
                if (aPublic != null) {
                    securityGroupId = aPublic.getId().toString();
                } else {
                    throw shellContext.exceptionTransformer().transformToRuntimeException(String.format("SecurityGroup not found by name: %s",
                            instanceGroupSecurityGroupName.getName()));
                }
            } else {
                throw shellContext.exceptionTransformer().transformToRuntimeException(
                        "SecurityGroup name or id is not defined for instanceGroup (use --securityGroupName or --securityGroupId)");
            }

            Long parsedTemplateId = Longs.tryParse(templateId);
            Long parsedsecurityGroupId = Longs.tryParse(securityGroupId);
            if (parsedTemplateId != null && parsedsecurityGroupId != null) {
                shellContext.putInstanceGroup(instanceGroup.getName(),
                        new InstanceGroupEntry(parsedTemplateId, parsedsecurityGroupId, nodeCount, ambariServer ? "GATEWAY" : "CORE", parameters));
                shellContext.putHostGroup(instanceGroup.getName(), new HostgroupEntry(nodeCount, new HashSet<>(), RecoveryMode.MANUAL));
                if (shellContext.getActiveHostGroups().size() == shellContext.getInstanceGroups().size()
                        && !shellContext.getActiveHostGroups().isEmpty()) {
                    shellContext.setHint(Hints.SELECT_NETWORK);
                } else {
                    shellContext.setHint(Hints.CONFIGURE_HOSTGROUP);
                }
                return shellContext.outputTransformer().render(shellContext.getInstanceGroups(), "instanceGroup");
            } else {
                throw shellContext.exceptionTransformer().transformToRuntimeException("TemplateId is not a number");
            }
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "instancegroup show", help = "Show the currently available instance groups")
    public String show() throws Exception {
        if (shellContext.getInstanceGroups().isEmpty()) {
            return "List of instance groups is empty currently";
        } else {
            return shellContext.outputTransformer().render(shellContext.getInstanceGroups(), "instanceGroup");
        }
    }

    @CliCommand(value = "instancegroup delete", help = "Delete a currently available instance group")
    public String delete(@CliOption(key = "name", help = "name of the instanceGroup", mandatory = true) String name) throws Exception {
        if (shellContext.getInstanceGroups().isEmpty()) {
            return "List of instance groups is empty currently";
        } else {
            shellContext.getInstanceGroups().remove(name);
            return shellContext.outputTransformer().render(shellContext.getInstanceGroups(), "instanceGroup");
        }
    }
}
