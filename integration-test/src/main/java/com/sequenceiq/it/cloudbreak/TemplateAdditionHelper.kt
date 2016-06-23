package com.sequenceiq.it.cloudbreak

import java.util.ArrayList

import com.sequenceiq.it.IntegrationTestContext

class TemplateAdditionHelper {

    fun parseTemplateAdditions(additionString: String): List<TemplateAddition> {
        val additions = ArrayList<TemplateAddition>()
        val additionsArray = additionString.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        for (additionsString in additionsArray) {
            val additionArray = additionsString.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val type = if (additionArray.size == WITH_TYPE_LENGTH) additionArray[WITH_TYPE_LENGTH - 1] else "CORE"
            additions.add(TemplateAddition(additionArray[0], Integer.valueOf(additionArray[1])!!, type))
        }
        return additions
    }

    fun parseCommaSeparatedRows(source: String): List<Array<String>> {
        val result = ArrayList<Array<String>>()
        val rows = source.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        for (row in rows) {
            result.add(row.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
        }
        return result
    }

    fun handleTemplateAdditions(itContext: IntegrationTestContext, templateId: String, additions: List<TemplateAddition>) {
        var instanceGroups: MutableList<InstanceGroup>? = itContext.getContextParam<List<Any>>(CloudbreakITContextConstants.TEMPLATE_ID, List<Any>::class.java)
        if (instanceGroups == null) {
            instanceGroups = ArrayList<InstanceGroup>()
            itContext.putContextParam(CloudbreakITContextConstants.TEMPLATE_ID, instanceGroups, true)
        }
        var hostGroups: MutableList<HostGroup>? = itContext.getContextParam<List<Any>>(CloudbreakITContextConstants.HOSTGROUP_ID, List<Any>::class.java)
        if (hostGroups == null) {
            hostGroups = ArrayList<HostGroup>()
            itContext.putContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, hostGroups, true)
        }
        for (addition in additions) {
            val groupName = addition.groupName
            instanceGroups.add(InstanceGroup(templateId, addition.groupName, addition.nodeCount, addition.type))
            if ("CORE" == addition.type) {
                hostGroups.add(HostGroup(groupName, groupName, addition.nodeCount))
            }
        }
    }

    companion object {

        val WITH_TYPE_LENGTH = 3
    }
}
