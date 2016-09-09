package com.sequenceiq.cloudbreak.shell.commands.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.google.common.primitives.Longs;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupName;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry;
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class InstanceGroupCommands implements CommandMarker {

    private ShellContext shellContext;

    public InstanceGroupCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "instancegroup configure")
    public boolean createAvailable() {
        return (shellContext.isBlueprintAvailable() && shellContext.isCredentialAvailable()) && !shellContext.isMarathonMode();
    }

    @CliAvailabilityIndicator(value = "instancegroup show")
    public boolean showAvailable() {
        return !shellContext.isMarathonMode();
    }

    @CliCommand(value = "instancegroup configure", help = "Configure instance groups")
    public String create(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") InstanceGroup instanceGroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Nodecount for instanceGroup") Integer nodeCount,
            @CliOption(key = "ambariServer", mandatory = true, help = "Ambari server will be installed here if true") boolean ambariServer,
            @CliOption(key = "templateId", mandatory = false, help = "TemplateId of the instanceGroup") InstanceGroupTemplateId instanceGroupTemplateId,
            @CliOption(key = "templateName", mandatory = false, help = "TemplateName of the instanceGroup") InstanceGroupTemplateName instanceGroupTemplateName,
            @CliOption(key = "securityGroupId", mandatory = false, help = "SecurityGroupId of the instanceGroup") SecurityGroupId instanceGroupSecurityGroupId,
            @CliOption(key = "securityGroupName", mandatory = false, help = "SecurityGroupName of the instanceGroup")
            SecurityGroupName instanceGroupSecurityGroupName) throws Exception {
        try {
            String templateId;
            if (instanceGroupTemplateId != null) {
                templateId = instanceGroupTemplateId.getName();
            } else if (instanceGroupTemplateName != null) {
                TemplateResponse aPublic = shellContext.cloudbreakClient().templateEndpoint().getPublic(instanceGroupTemplateName.getName());
                if (aPublic != null) {
                    templateId = aPublic.getId().toString();
                } else {
                    return String.format("Template not found by name: %s", instanceGroupTemplateName.getName());
                }
            } else {
                return "Template name or id is not defined for instanceGroup (use --templateName or --templateId)";
            }
            String securityGroupId = null;
            if (instanceGroupSecurityGroupId != null) {
                securityGroupId = instanceGroupSecurityGroupId.getName();
            } else if (instanceGroupSecurityGroupName != null) {
                SecurityGroupJson aPublic = shellContext.cloudbreakClient().securityGroupEndpoint().getPublic(instanceGroupSecurityGroupName.getName());
                if (aPublic != null) {
                    securityGroupId = aPublic.getId().toString();
                } else {
                    return String.format("SecurityGroup not found by name: %s", instanceGroupSecurityGroupName.getName());
                }
            } else {
                return "SecurityGroup name or id is not defined for instanceGroup (use --securityGroupName or --securityGroupId)";
            }

            Long parsedTemplateId = Longs.tryParse(templateId);
            Long parsedsecurityGroupId = Longs.tryParse(securityGroupId);
            if (parsedTemplateId != null && parsedsecurityGroupId != null) {
                Map<Long, Integer> map = new HashMap<>();
                map.put(parsedTemplateId, nodeCount);
                if (ambariServer) {
                    boolean ambariSpecified = shellContext.getInstanceGroups().values()
                            .stream().filter(e -> e.getType().equals("GATEWAY")).findAny().isPresent();
                    if (ambariSpecified) {
                        return "Ambari server is already specified";
                    }
                    if (nodeCount != 1) {
                        return "Allowed node count for Ambari server: 1";
                    }
                    shellContext.putInstanceGroup(instanceGroup.getName(),
                            new InstanceGroupEntry(parsedTemplateId, parsedsecurityGroupId, nodeCount, "GATEWAY"));
                } else {
                    shellContext.putInstanceGroup(instanceGroup.getName(),
                            new InstanceGroupEntry(parsedTemplateId, parsedsecurityGroupId, nodeCount, "CORE"));
                }
                shellContext.putHostGroup(instanceGroup.getName(), new HostgroupEntry(nodeCount, new HashSet<>()));
                if (shellContext.getActiveHostGroups().size() == shellContext.getInstanceGroups().size()
                        && shellContext.getActiveHostGroups().size() != 0) {
                    shellContext.setHint(Hints.SELECT_NETWORK);
                } else {
                    shellContext.setHint(Hints.CONFIGURE_HOSTGROUP);
                }
                return shellContext.outputTransformer().render(shellContext.getInstanceGroups(), "instanceGroup");
            } else {
                return "TemplateId is not a number.";
            }
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "instancegroup show", help = "Configure instance groups")
    public String show() throws Exception {
        if (shellContext.getInstanceGroups().isEmpty()) {
            return "List of instance groups is empty currently.";
        } else {
            return shellContext.outputTransformer().render(shellContext.getInstanceGroups(), "instanceGroup");
        }
    }
}
