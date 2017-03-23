package com.sequenceiq.cloudbreak.shell.commands;

import java.util.Map;

import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId;
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupId;
import com.sequenceiq.cloudbreak.shell.completion.SecurityGroupName;

public interface InstanceGroupCommands {

    String create(InstanceGroup instanceGroup, Integer nodeCount, boolean ambariServer, InstanceGroupTemplateId instanceGroupTemplateId,
            InstanceGroupTemplateName instanceGroupTemplateName, SecurityGroupId instanceGroupSecurityGroupId,
            SecurityGroupName instanceGroupSecurityGroupName, Map<String, Object> parameters);

    boolean createInstanceGroupAvailable(String platform);
}
