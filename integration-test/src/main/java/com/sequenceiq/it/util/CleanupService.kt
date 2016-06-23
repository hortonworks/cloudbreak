package com.sequenceiq.it.util

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse
import com.sequenceiq.cloudbreak.api.model.CredentialResponse
import com.sequenceiq.cloudbreak.api.model.NetworkJson
import com.sequenceiq.cloudbreak.api.model.RecipeResponse
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.api.model.TemplateResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.it.cloudbreak.CloudbreakUtil
import com.sequenceiq.it.cloudbreak.WaitResult
import com.sequenceiq.it.cloudbreak.config.ITProps

@Component
class CleanupService {

    private var cleanedUp: Boolean = false

    @Value("${integrationtest.cleanup.retryCount}")
    private val cleanUpRetryCount: Int = 0
    @Inject
    private val itProps: ITProps? = null

    @Synchronized @Throws(Exception::class)
    fun deleteTestStacksAndResources(cloudbreakClient: CloudbreakClient) {
        if (cleanedUp) {
            return
        }
        cleanedUp = true
        val stacks = cloudbreakClient.stackEndpoint().privates
        for (stack in stacks) {
            if (stack.name!!.startsWith("it-")) {
                deleteStackAndWait(cloudbreakClient, stack.id.toString())
            }
        }
        val templates = cloudbreakClient.templateEndpoint().privates
        for (template in templates) {
            if (template.name!!.startsWith("it-")) {
                deleteTemplate(cloudbreakClient, template.id.toString())
            }
        }
        val networks = cloudbreakClient.networkEndpoint().privates
        for (network in networks) {
            if (network.name!!.startsWith("it-")) {
                deleteNetwork(cloudbreakClient, network.id.toString())
            }
        }
        val secgroups = cloudbreakClient.securityGroupEndpoint().privates
        for (secgroup in secgroups) {
            if (secgroup.name!!.startsWith("it-")) {
                deleteSecurityGroup(cloudbreakClient, secgroup.id.toString())
            }
        }
        val blueprints = cloudbreakClient.blueprintEndpoint().privates
        for (blueprint in blueprints) {
            if (blueprint.name!!.startsWith("it-")) {
                deleteBlueprint(cloudbreakClient, blueprint.id.toString())
            }
        }
        val recipes = cloudbreakClient.recipeEndpoint().privates
        for (recipe in recipes) {
            if (recipe.name!!.startsWith("it-")) {
                deleteRecipe(cloudbreakClient, recipe.id)
            }
        }
        val credentials = cloudbreakClient.credentialEndpoint().privates
        for (credential in credentials) {
            if ("AZURE_RM" == credential.cloudPlatform && credential.name!!.startsWith("its") || "AZURE_RM" != credential.cloudPlatform && credential.name!!.startsWith("its-")) {
                deleteCredential(cloudbreakClient, credential.id.toString())
            }
        }
    }

    @Throws(Exception::class)
    fun deleteCredential(cloudbreakClient: CloudbreakClient, credentialId: String?): Boolean {
        var result = false
        if (credentialId != null) {
            cloudbreakClient.credentialEndpoint().delete(java.lang.Long.valueOf(credentialId))
            result = true
        }
        return result
    }

    @Throws(Exception::class)
    fun deleteTemplate(cloudbreakClient: CloudbreakClient, templateId: String?): Boolean {
        var result = false
        if (templateId != null) {
            cloudbreakClient.templateEndpoint().delete(java.lang.Long.valueOf(templateId))
            result = true
        }
        return result
    }

    @Throws(Exception::class)
    fun deleteNetwork(cloudbreakClient: CloudbreakClient, networkId: String?): Boolean {
        var result = false
        if (networkId != null) {
            cloudbreakClient.networkEndpoint().delete(java.lang.Long.valueOf(networkId))
            result = true
        }
        return result
    }

    @Throws(Exception::class)
    fun deleteSecurityGroup(cloudbreakClient: CloudbreakClient, securityGroupId: String?): Boolean {
        var result = false
        if (securityGroupId != null) {
            val securityGroupEndpoint = cloudbreakClient.securityGroupEndpoint()
            val securityGroupJson = securityGroupEndpoint[java.lang.Long.valueOf(securityGroupId)]
            if (securityGroupJson.name != itProps!!.defaultSecurityGroup) {
                securityGroupEndpoint.delete(java.lang.Long.valueOf(securityGroupId))
                result = true
            }
        }
        return result
    }

    @Throws(Exception::class)
    fun deleteBlueprint(cloudbreakClient: CloudbreakClient, blueprintId: String?): Boolean {
        var result = false
        if (blueprintId != null) {
            cloudbreakClient.blueprintEndpoint().delete(java.lang.Long.valueOf(blueprintId))
            result = true
        }
        return result
    }

    @Throws(Exception::class)
    fun deleteStackAndWait(cloudbreakClient: CloudbreakClient, stackId: String): Boolean {
        var deleted = false
        for (i in 0..cleanUpRetryCount - 1) {
            if (deleteStack(cloudbreakClient, stackId)) {
                val waitResult = CloudbreakUtil.waitForStackStatus(cloudbreakClient, stackId, "DELETE_COMPLETED")
                if (waitResult == WaitResult.SUCCESSFUL) {
                    deleted = true
                    break
                }
                try {
                    Thread.sleep(DELETE_SLEEP.toLong())
                } catch (e: InterruptedException) {
                    LOG.warn("interrupted ex", e)
                }

            }
        }
        return deleted
    }

    @Throws(Exception::class)
    fun deleteStack(cloudbreakClient: CloudbreakClient, stackId: String?): Boolean {
        var result = false
        if (stackId != null) {
            cloudbreakClient.stackEndpoint().delete(java.lang.Long.valueOf(stackId), false)
            result = true
        }
        return result
    }

    @Throws(Exception::class)
    fun deleteRecipe(cloudbreakClient: CloudbreakClient, recipeId: Long?): Boolean {
        cloudbreakClient.recipeEndpoint().delete(recipeId)
        return true
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CleanupService::class.java)
        private val DELETE_SLEEP = 30000
    }
}
