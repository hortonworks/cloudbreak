package com.sequenceiq.cloudbreak.service.decorator

import org.springframework.util.StringUtils.isEmpty

import java.util.Optional

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.ConstraintJson
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.Recipe
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ConstraintRepository
import com.sequenceiq.cloudbreak.repository.ConstraintTemplateRepository
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService
import com.sequenceiq.cloudbreak.service.recipe.RecipeService
import com.sequenceiq.cloudbreak.service.stack.StackService

@Component
class HostGroupDecorator : Decorator<HostGroup> {

    private enum class DecorationData {
        STACK_ID,
        USER,
        CONSTRAINT,
        RECIPE_IDS,
        REQUEST_TYPE
    }

    @Inject
    private val instanceGroupRepository: InstanceGroupRepository? = null

    @Inject
    private val constraintTemplateRepository: ConstraintTemplateRepository? = null

    @Inject
    private val constraintRepository: ConstraintRepository? = null

    @Inject
    private val hostGroupService: HostGroupService? = null

    @Inject
    private val stackService: StackService? = null

    @Inject
    private val recipeService: RecipeService? = null

    @Inject
    private val conversionService: ConversionService? = null

    @Inject
    private val clusterService: ClusterService? = null


    override fun decorate(subject: HostGroup, vararg data: Any): HostGroup {
        var subject = subject
        if (null == data || data.size != DecorationData.values().size) {
            throw IllegalArgumentException("Invalid decoration data provided. Cluster: " + subject.name)
        }
        val stackId = data[DecorationData.STACK_ID.ordinal] as Long
        val user = data[DecorationData.USER.ordinal] as CbUser
        val constraintJson = data[DecorationData.CONSTRAINT.ordinal] as ConstraintJson
        val recipeIds = data[DecorationData.RECIPE_IDS.ordinal] as Set<Long>
        val postRequest = data[DecorationData.REQUEST_TYPE.ordinal] as Boolean

        LOGGER.debug("Decorating hostgroup on [{}] request.", if (postRequest) "POST" else "PUT")
        var constraint = conversionService!!.convert<Constraint>(constraintJson, Constraint::class.java)
        if (postRequest) {
            constraint = decorateConstraint(stackId, user, constraint, constraintJson.instanceGroupName, constraintJson.constraintTemplateName)
            subject.constraint = constraint
        } else {
            subject = getHostGroup(stackId, constraint, constraintJson, subject, user)
        }

        subject.recipes.clear()
        if (recipeIds != null) {
            for (recipeId in recipeIds) {
                val recipe = recipeService!!.get(recipeId)
                subject.recipes.add(recipe)
            }
        }

        return subject
    }

    private fun decorateConstraint(stackId: Long?, user: CbUser, constraint: Constraint, instanceGroupName: String?, constraintTemplateName: String?): Constraint {
        if (instanceGroupName != null) {
            val instanceGroup = instanceGroupRepository!!.findOneByGroupNameInStack(stackId, instanceGroupName)
            if (instanceGroup == null) {
                LOGGER.error("Instance group not found: {}", instanceGroupName)
                throw BadRequestException(String.format("Instance group '%s' not found on stack.", instanceGroupName))
            }
            constraint.instanceGroup = instanceGroup
        }
        if (constraintTemplateName != null) {
            val constraintTemplate = constraintTemplateRepository!!.findByNameInAccount(constraintTemplateName,
                    user.account, user.userId) ?: throw BadRequestException(String.format("Couldn't find constraint template with name: %s", constraintTemplateName))
            constraint.constraintTemplate = constraintTemplate
        }
        return constraint
    }

    private fun getHostGroup(stackId: Long?, constraint: Constraint, constraintJson: ConstraintJson?, subject: HostGroup, user: CbUser): HostGroup {
        var result = subject
        val instanceGroupName = constraintJson!!.instanceGroupName
        val constraintTemplateName = constraintJson.constraintTemplateName
        val cluster = clusterService!!.retrieveClusterByStackId(stackId)
        if (constraintJson == null) {
            throw BadRequestException("The constraint field must be set in the reinstall request!")
        }
        val decoratedConstraint = decorateConstraint(stackId, user, constraint, instanceGroupName, constraintTemplateName)
        if (!isEmpty(instanceGroupName)) {
            result = getHostGroupByInstanceGroupName(decoratedConstraint, subject, cluster, instanceGroupName)
        } else if (!isEmpty(constraintTemplateName)) {
            subject.constraint = constraintRepository!!.save(constraint)
        } else {
            throw BadRequestException("The constraint field must contain the 'constraintTemplateName' or 'instanceGroupName' parameter!")
        }
        return result
    }

    private fun getHostGroupByInstanceGroupName(constraint: Constraint, subject: HostGroup, cluster: Cluster, instanceGroupName: String): HostGroup {
        var result = subject
        val hostGroups = hostGroupService!!.getByCluster(cluster.id)
        if (hostGroups.isEmpty()) {
            val stack = cluster.stack
            if (stack == null) {
                val msg = String.format("There is no stack associated to cluster (id:'%s', name: '%s')!", cluster.id, cluster.name)
                throw BadRequestException(msg)
            } else {
                subject.constraint = constraint
            }
        } else {
            result = getDetailsFromExistingHostGroup(constraint, subject, instanceGroupName, hostGroups)
        }
        return result
    }

    private fun getDetailsFromExistingHostGroup(constraint: Constraint, subject: HostGroup, instanceGroupName: String, hostGroups: Set<HostGroup>): HostGroup {
        val hostGroupOptional = hostGroups.stream().filter({ input -> input.getConstraint().getInstanceGroup().getGroupName() == instanceGroupName }).findFirst()
        if (hostGroupOptional.isPresent()) {
            val hostGroup = hostGroupOptional.get()
            val instanceGroupNodeCount = hostGroup.getConstraint().getInstanceGroup().getNodeCount()
            if (constraint.hostCount > instanceGroupNodeCount) {
                throw BadRequestException(String.format("The 'hostCount' of host group '%s' constraint could not be more than '%s'!"))
            }
            hostGroup.getConstraint().setHostCount(constraint.hostCount)
            hostGroup.setName(subject.name)
            return hostGroup
        } else {
            throw BadRequestException(String.format("Invalid 'instanceGroupName'! Could not find instance group with name: '%s'", instanceGroupName))
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HostGroupDecorator::class.java)
    }
}
