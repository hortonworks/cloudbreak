package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.it.IntegrationTestContext;

public class TemplateAdditionHelper {

    public static final int WITH_TYPE_LENGTH = 3;

    public List<TemplateAddition> parseTemplateAdditions(String additionString) {
        List<TemplateAddition> additions = new ArrayList<>();
        String[] additionsArray = additionString.split(";");
        for (String additionsString : additionsArray) {
            String[] additionArray = additionsString.split(",");
            String type = additionArray.length == WITH_TYPE_LENGTH ? additionArray[WITH_TYPE_LENGTH - 1] : "CORE";
            additions.add(new TemplateAddition(additionArray[0], Integer.valueOf(additionArray[1]), type));
        }
        return additions;
    }

    public List<String[]> parseCommaSeparatedRows(String source) {
        List<String[]> result = new ArrayList<>();
        String[] rows = source.split(";");
        for (String row : rows) {
            result.add(row.split(","));
        }
        return result;
    }

    public void handleTemplateAdditions(IntegrationTestContext itContext, String templateId, List<TemplateAddition> additions) {
        List<InstanceGroup> instanceGroups = itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
        if (instanceGroups == null) {
            instanceGroups = new ArrayList<>();
            itContext.putContextParam(CloudbreakITContextConstants.TEMPLATE_ID, instanceGroups, true);
        }
        List<HostGroup> hostGroups = itContext.getContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, List.class);
        if (hostGroups == null) {
            hostGroups = new ArrayList<>();
            itContext.putContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, hostGroups, true);
        }
        for (TemplateAddition addition : additions) {
            String groupName = addition.getGroupName();
            instanceGroups.add(new InstanceGroup(templateId, addition.getGroupName(), addition.getNodeCount(), addition.getType()));
            if ("CORE".equals(addition.getType())) {
                hostGroups.add(new HostGroup(groupName, groupName));
            }
        }
    }
}
