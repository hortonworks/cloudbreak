package com.sequenceiq.cloudbreak.cloud.aws

import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.StringUtils.isNoneEmpty

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.google.common.annotations.VisibleForTesting
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsGroupView
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate

import freemarker.template.Configuration
import freemarker.template.TemplateException

@Service("CloudFormationTemplateBuilder")
class CloudFormationTemplateBuilder {
    @Inject
    private var freemarkerConfiguration: Configuration? = null

    fun build(context: ModelContext): String {
        val model = HashMap<String, Any>()
        val awsInstanceProfileView = AwsInstanceProfileView(context.stack!!.parameters)
        val awsGroupViews = ArrayList<AwsGroupView>()
        for (group in context.stack!!.groups) {
            val instanceTemplate = group.instances[0].template
            var encrypted: Boolean? = instanceTemplate.getParameter<Boolean>("encrypted", Boolean::class.java)
            encrypted = if (encrypted == null) java.lang.Boolean.FALSE else encrypted
            awsGroupViews.add(
                    AwsGroupView(
                            group.instances.size,
                            group.type.name,
                            instanceTemplate.flavor,
                            group.name,
                            instanceTemplate.volumes.size,
                            encrypted == java.lang.Boolean.TRUE,
                            instanceTemplate.volumeSize,
                            instanceTemplate.volumeType,
                            getSpotPrice(instanceTemplate)))
        }
        model.put("instanceGroups", awsGroupViews)
        model.put("existingVPC", context.existingVPC)
        model.put("existingIGW", context.existingIGW)
        model.put("existingSubnet", isNoneEmpty(context.existingSubnetCidr))
        model.put("securityRules", context.stack!!.security)
        model.put("enableInstanceProfile", context.enableInstanceProfile || context.s3RoleAvailable)
        model.put("existingRole", context.s3RoleAvailable)
        model.put("cbSubnet", if (isBlank(context.existingSubnetCidr)) context.stack!!.network.subnet.cidr else context.existingSubnetCidr)
        model.put("dedicatedInstances", areDedicatedInstancesRequested(context.stack))
        model.put("availabilitySetNeeded", if (context.ac!!.cloudContext.location!!.availabilityZone.value() == null) false else true)
        model.put("mapPublicIpOnLaunch", context.mapPublicIpOnLaunch)
        if (isNoneEmpty(context.snapshotId)) {
            model.put("snapshotId", context.snapshotId)
        }
        if (context.s3RoleAvailable) {
            model.put("roleName", awsInstanceProfileView.s3Role)
        }
        try {
            return processTemplateIntoString(freemarkerConfiguration!!.getTemplate(context.templatePath, "UTF-8"), model)
        } catch (e: IOException) {
            throw CloudConnectorException("Failed to process CloudFormation freemarker template", e)
        } catch (e: TemplateException) {
            throw CloudConnectorException("Failed to process CloudFormation freemarker template", e)
        }

    }

    fun areDedicatedInstancesRequested(cloudStack: CloudStack): Boolean {
        var result = false
        if (isDedicatedInstancesParamExistAndTrue(cloudStack)) {
            result = true
        }
        return result
    }

    private fun isDedicatedInstancesParamExistAndTrue(stack: CloudStack): Boolean {
        return stack.parameters.containsKey("dedicatedInstances") && java.lang.Boolean.valueOf(stack.parameters["dedicatedInstances"])!!
    }

    private fun getSpotPrice(instanceTemplate: InstanceTemplate): Double? {
        try {
            return instanceTemplate.getParameter<Double>("spotPrice", Double::class.java)
        } catch (e: ClassCastException) {
            return instanceTemplate.getParameter<Int>("spotPrice", Int::class.java).toDouble()
        }

    }

    @VisibleForTesting
    internal fun setFreemarkerConfiguration(freemarkerConfiguration: Configuration) {
        this.freemarkerConfiguration = freemarkerConfiguration
    }

    class ModelContext {
        private var ac: AuthenticatedContext? = null
        private var stack: CloudStack? = null
        private var snapshotId: String? = null
        private var existingVPC: Boolean = false
        private var existingIGW: Boolean = false
        private var existingSubnetCidr: String? = null
        private var mapPublicIpOnLaunch: Boolean = false
        private var templatePath: String? = null
        private var enableInstanceProfile: Boolean = false
        private var s3RoleAvailable: Boolean = false

        fun withAuthenticatedContext(ac: AuthenticatedContext): ModelContext {
            this.ac = ac
            return this
        }

        fun withStack(stack: CloudStack): ModelContext {
            this.stack = stack
            return this
        }

        fun withSnapshotId(snapshotId: String): ModelContext {
            this.snapshotId = snapshotId
            return this
        }

        fun withExistingVpc(existingVpc: Boolean): ModelContext {
            this.existingVPC = existingVpc
            return this
        }

        fun withExistingIGW(existingIGW: Boolean): ModelContext {
            this.existingIGW = existingIGW
            return this
        }

        fun withExistingSubnetCidr(cidr: String): ModelContext {
            this.existingSubnetCidr = cidr
            return this
        }

        fun mapPublicIpOnLaunch(mapPublicIpOnLaunch: Boolean): ModelContext {
            this.mapPublicIpOnLaunch = mapPublicIpOnLaunch
            return this
        }

        fun withEnableInstanceProfile(enableInstanceProfile: Boolean): ModelContext {
            this.enableInstanceProfile = enableInstanceProfile
            return this
        }

        fun withS3RoleAvailable(s3RoleAvailable: Boolean): ModelContext {
            this.s3RoleAvailable = s3RoleAvailable
            return this
        }

        fun withTemplatePath(templatePath: String): ModelContext {
            this.templatePath = templatePath
            return this
        }
    }
}