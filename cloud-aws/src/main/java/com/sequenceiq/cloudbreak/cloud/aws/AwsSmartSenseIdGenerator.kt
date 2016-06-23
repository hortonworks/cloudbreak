package com.sequenceiq.cloudbreak.cloud.aws

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView

@Component
class AwsSmartSenseIdGenerator {

    @Value("${cb.smartsense.configure:false}")
    private val configureSmartSense: Boolean = false

    @Value("${cb.smartsense.id.pattern:}")
    private val smartSenseIdPattern: String? = null

    fun getSmartSenseId(credentialView: AwsCredentialView): String {
        var result = ""
        if (configureSmartSense) {
            result = getSmartSenseId(credentialView.roleArn, credentialView.accessKey, credentialView.secretKey)
        }
        return result
    }

    private fun getSmartSenseId(roleArn: String, accessKey: String, secretKey: String): String {
        var smartSenseId = ""
        try {
            if (StringUtils.isNoneEmpty(roleArn)) {
                smartSenseId = getSmartSenseIdFromArn(roleArn)
            } else if (StringUtils.isNoneEmpty(accessKey) && StringUtils.isNoneEmpty(secretKey)) {
                try {
                    val iamClient = AmazonIdentityManagementClient(BasicAWSCredentials(accessKey, secretKey))
                    val arn = iamClient.user.user.arn
                    smartSenseId = getSmartSenseIdFromArn(arn)
                } catch (e: Exception) {
                    LOGGER.error("Could not get ARN of IAM user from AWS.", e)
                }

            }
        } catch (e: Exception) {
            LOGGER.error("Could not get SmartSense Id from AWS credential.", e)
        }

        return smartSenseId
    }

    private fun getSmartSenseIdFromArn(roleArn: String): String {
        var smartSenseId = ""
        val m = Pattern.compile("arn:aws:iam::(?<accountId>[0-9]{12}):.*").matcher(roleArn)
        if (m.matches()) {
            val accountId = m.group("accountId")
            val firstPart = accountId.substring(0, FIRST_PART_LENGTH)
            val secondPart = accountId.substring(FIRST_PART_LENGTH)
            smartSenseId = String.format(smartSenseIdPattern, firstPart, secondPart)
        }
        return smartSenseId
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AwsSmartSenseIdGenerator::class.java)
        private val FIRST_PART_LENGTH = 4
    }
}
