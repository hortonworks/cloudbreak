package com.sequenceiq.cloudbreak.cloud.gcp.util

import org.apache.commons.lang3.StringUtils.isAnyEmpty
import org.apache.commons.lang3.StringUtils.isNoneEmpty

import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.PrivateKey
import java.util.Arrays

import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.SecurityUtils
import com.google.api.services.compute.Compute
import com.google.api.services.compute.ComputeScopes
import com.google.api.services.compute.model.Operation
import com.google.api.services.storage.Storage
import com.google.api.services.storage.StorageScopes
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Region

object GcpStackUtil {

    private val LOGGER = LoggerFactory.getLogger(GcpStackUtil::class.java)
    private val JSON_FACTORY = JacksonFactory.getDefaultInstance()
    private val SCOPES = Arrays.asList(ComputeScopes.COMPUTE, StorageScopes.DEVSTORAGE_FULL_CONTROL)
    private val GCP_IMAGE_TYPE_PREFIX = "https://www.googleapis.com/compute/v1/projects/%s/global/images/"
    private val EMPTY_BUCKET = ""
    private val FINISHED = 100
    private val PRIVATE_ID_PART = 2
    private val SERVICE_ACCOUNT = "serviceAccountId"
    private val PRIVATE_KEY = "serviceAccountPrivateKey"
    private val PROJECT_ID = "projectId"
    private val NETWORK_ID = "networkId"
    private val SUBNET_ID = "subnetId"

    fun buildCompute(gcpCredential: CloudCredential): Compute? {
        try {
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val credential = buildCredential(gcpCredential, httpTransport)
            return Compute.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(gcpCredential.name).setHttpRequestInitializer(credential).build()
        } catch (e: Exception) {
            LOGGER.error("Error occurred while building Google Compute access.", e)
        }

        return null
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun buildCredential(gcpCredential: CloudCredential, httpTransport: HttpTransport): GoogleCredential {
        val pk = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                ByteArrayInputStream(Base64.decodeBase64(getServiceAccountPrivateKey(gcpCredential))), "notasecret", "privatekey", "notasecret")
        val credential = GoogleCredential.Builder().setTransport(httpTransport).setJsonFactory(JSON_FACTORY).setServiceAccountId(getServiceAccountId(gcpCredential)).setServiceAccountScopes(SCOPES).setServiceAccountPrivateKey(pk).build()
        return credential
    }

    fun getServiceAccountPrivateKey(credential: CloudCredential): String {
        return credential.getParameter<String>(PRIVATE_KEY, String::class.java)
    }

    fun getServiceAccountId(credential: CloudCredential): String {
        return credential.getParameter<String>(SERVICE_ACCOUNT, String::class.java)
    }

    fun getProjectId(credential: CloudCredential): String {
        return credential.getParameter<String>(PROJECT_ID, String::class.java).toLowerCase().replace("[^A-Za-z0-9 ]".toRegex(), "-")
    }

    @Throws(Exception::class)
    fun analyzeOperation(operation: Operation): Boolean {
        val errorMessage = checkForErrors(operation)
        if (errorMessage != null) {
            throw Exception(errorMessage)
        } else {
            val progress = operation.progress
            return if (progress!!.toInt() != FINISHED) false else true
        }
    }

    fun checkForErrors(operation: Operation?): String? {
        var msg: String? = null
        if (operation == null) {
            LOGGER.error("Operation is null!")
            return msg
        }
        if (operation.error != null) {
            val error = StringBuilder()
            if (operation.error.errors != null) {
                for (errors in operation.error.errors) {
                    error.append(String.format("code: %s -> message: %s %s", errors.code, errors.message, System.lineSeparator()))
                }
                msg = error.toString()
            } else {
                LOGGER.debug("No errors found, Error: {}", operation.error)
            }
        }
        if (operation.httpErrorStatusCode != null) {
            msg += String.format(" HTTP error message: %s, HTTP error status code: %s", operation.httpErrorMessage, operation.httpErrorStatusCode)
        }
        return msg
    }

    @Throws(IOException::class)
    fun globalOperations(compute: Compute, projectId: String, operationName: String): Compute.GlobalOperations.Get {
        return compute.globalOperations().get(projectId, operationName)
    }

    @Throws(IOException::class)
    fun zoneOperations(compute: Compute, projectId: String, operationName: String, region: AvailabilityZone): Compute.ZoneOperations.Get {
        return compute.zoneOperations().get(projectId, region.value(), operationName)
    }

    @Throws(IOException::class)
    fun regionOperations(compute: Compute, projectId: String, operationName: String, region: Region): Compute.RegionOperations.Get {
        return compute.regionOperations().get(projectId, region.value(), operationName)
    }

    fun buildStorage(gcpCredential: CloudCredential, name: String): Storage? {
        try {
            val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
            val credential = buildCredential(gcpCredential, httpTransport)
            return Storage.Builder(
                    httpTransport, JSON_FACTORY, null).setApplicationName(name).setHttpRequestInitializer(credential).build()
        } catch (e: Exception) {
            LOGGER.error("Error occurred while building Google Storage access.", e)
        }

        return null
    }

    fun getBucket(image: String): String {
        if (!StringUtils.isEmpty(image) && createParts(image).size > 1) {
            val parts = createParts(image)
            return StringUtils.join(ArrayUtils.remove(parts, parts.size - 1), "/")
        } else {
            LOGGER.warn("No bucket found in source image path.")
            return EMPTY_BUCKET
        }
    }

    fun getTarName(image: String): String {
        if (!StringUtils.isEmpty(image)) {
            val parts = createParts(image)
            return parts[parts.size - 1]
        } else {
            throw GcpResourceException("Source image path environment variable is not well formed")
        }
    }

    fun getImageName(image: String): String {
        return getTarName(image).replace("(\\.tar|\\.zip|\\.gz|\\.gzip)".toRegex(), "").replace("\\.".toRegex(), "-")
    }

    fun getAmbariImage(projectId: String, image: String): String {
        return String.format(GCP_IMAGE_TYPE_PREFIX + getImageName(image), projectId)
    }

    fun getPrivateId(resourceName: String): Long? {
        try {
            return java.lang.Long.valueOf(resourceName.split("-".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()[PRIVATE_ID_PART])
        } catch (nfe: NumberFormatException) {
            LOGGER.warn("Cannot determine the private id of GCP instance, name: " + resourceName)
            return null
        } catch (e: Exception) {
            LOGGER.warn("Cannot determine the private id of GCP instance, name: " + resourceName, e)
            return null
        }

    }

    fun isExistingNetwork(network: Network): Boolean {
        return isNoneEmpty(getCustomNetworkId(network))
    }

    fun newSubnetInExistingNetwork(network: Network): Boolean {
        return isExistingNetwork(network) && isNoneEmpty(network.subnet.cidr)
    }

    fun newNetworkAndSubnet(network: Network): Boolean {
        return !isExistingNetwork(network)
    }

    fun legacyNetwork(network: Network): Boolean {
        return isAnyEmpty(network.subnet.cidr) && isAnyEmpty(getSubnetId(network))
    }

    fun isExistingSubnet(network: Network): Boolean {
        return isNoneEmpty(getSubnetId(network))
    }

    fun getCustomNetworkId(network: Network): String {
        return network.getStringParameter(NETWORK_ID)
    }

    fun getSubnetId(network: Network): String {
        return network.getStringParameter(SUBNET_ID)
    }

    fun getClusterTag(cloudContext: CloudContext): String {
        return cloudContext.name + cloudContext.id!!
    }

    private fun createParts(splittable: String): Array<String> {
        return splittable.split("/".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
    }

}
