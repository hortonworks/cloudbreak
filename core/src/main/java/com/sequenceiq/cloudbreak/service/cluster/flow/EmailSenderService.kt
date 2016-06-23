package com.sequenceiq.cloudbreak.service.cluster.flow

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString

import java.util.HashMap

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField

import freemarker.template.Configuration

@Service
class EmailSenderService {

    @Value("${cb.smtp.sender.from:}")
    private val msgFrom: String? = null

    @Value("${cb.success.cluster.installer.mail.template.path:}")
    private val successClusterMailTemplatePath: String? = null

    @Value("${cb.failed.cluster.installer.mail.template.path:}")
    private val failedClusterMailTemplatePath: String? = null

    @Inject
    private val emailMimeMessagePreparator: EmailMimeMessagePreparator? = null

    @Inject
    private val mailSender: JavaMailSender? = null

    @Inject
    private val freemarkerConfiguration: Configuration? = null

    @Inject
    private val userDetailsService: UserDetailsService? = null

    private enum class State private constructor(private val status: String, private val title: String, private val text: String) {
        PROVISIONING_SUCCESS("SUCCESS", "Cloudbreak Cluster Install Success", "Your cluster '%s' is ready to use. You can log into the Ambari UI %s:8080 using the configured username/password."),
        PROVISIONING_FAILURE("FAILED", "Cloudbreak Cluster Install Failed", "Something went terribly wrong - we are happy to help, please let us know your cluster details, " + "time, etc - and we will check the logs and get a fix for you."),
        START_SUCCESS("SUCCESS", "Cloudbreak Cluster Start Success", "Your cluster '%s' is ready to use after the start. You can log into the Ambari UI %s:8080 using the configured username/password."),
        START_FAILURE("FAILED", "Cloudbreak Cluster Start Failed", "Failed to start your cluster: %s - we are happy to help, please let us know your cluster details, time, etc - and we will check the" + " logs and get a fix for you."),
        STOP_SUCCESS("SUCCESS", "Cloudbreak Cluster Stop Success", "Your cluster '%s' was successfully stopped. If you want to use again just restart."),
        STOP_FAILURE("FAILED", "Cloudbreak Cluster Stop Failed", "Failed to stop your cluster: %s - we are happy to help, please let us know your cluster details, time, etc - and we will check the " + "logs and get a fix for you."),
        UPSCALE_SUCCESS("SUCCESS", "Cloudbreak Cluster Upscale Success", "Your cluster '%s' is ready to use after the upscale. You can log into the Ambari UI %s:8080 using the configured username/password."),
        DOWN_SCALE_SUCCESS("SUCCESS", "Cloudbreak Cluster Downscale Success", "Your cluster '%s' is ready to use after the downscale. You can log into the Ambari UI %s:8080 using the configured username/password."),
        TERMINATION_SUCCESS("SUCCESS", "Cloudbreak Cluster Termination Success", "Your cluster '%s' was successfully terminated."),
        TERMINATION_FAILURE("FAILED", "Cloudbreak Cluster Termination Failed", "Failed to terminate your cluster: '%s'. Please try again... - we are happy to help, please let us know your cluster details, time, " + "etc - and we will check the logs and get a fix for you.")
    }

    @Async
    fun sendProvisioningSuccessEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, successClusterMailTemplatePath, clusterName + " cluster installation", getEmailModel(user.givenName,
                ambariServer, State.PROVISIONING_SUCCESS, clusterName))
    }

    @Async
    fun sendProvisioningFailureEmail(email: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, failedClusterMailTemplatePath, clusterName + " cluster installation", getEmailModel(user.givenName,
                null, State.PROVISIONING_FAILURE, clusterName))
    }

    @Async
    fun sendStartSuccessEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, successClusterMailTemplatePath, clusterName + " cluster start", getEmailModel(user.givenName,
                ambariServer, State.START_SUCCESS, clusterName))
    }

    @Async
    fun sendStartFailureEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, failedClusterMailTemplatePath, clusterName + " cluster start", getEmailModel(user.givenName,
                ambariServer, State.START_FAILURE, clusterName))
    }

    @Async
    fun sendStopSuccessEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, successClusterMailTemplatePath, clusterName + " cluster stop", getEmailModel(user.givenName,
                ambariServer, State.STOP_SUCCESS, clusterName))
    }

    @Async
    fun sendStopFailureEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, failedClusterMailTemplatePath, clusterName + " cluster stop", getEmailModel(user.givenName,
                ambariServer, State.STOP_FAILURE, clusterName))
    }

    @Async
    fun sendUpscaleSuccessEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, successClusterMailTemplatePath, clusterName + " cluster upscale", getEmailModel(user.givenName,
                ambariServer, State.UPSCALE_SUCCESS, clusterName))
    }

    @Async
    fun sendDownScaleSuccessEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, successClusterMailTemplatePath, clusterName + " cluster downscale", getEmailModel(user.givenName,
                ambariServer, State.DOWN_SCALE_SUCCESS, clusterName))
    }

    @Async
    fun sendTerminationSuccessEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, successClusterMailTemplatePath, clusterName + " cluster termination", getEmailModel(user.givenName,
                ambariServer, State.TERMINATION_SUCCESS, clusterName))
    }

    @Async
    fun sendTerminationFailureEmail(email: String, ambariServer: String, clusterName: String) {
        val user = userDetailsService!!.getDetails(email, UserFilterField.USERID)
        sendEmail(user, failedClusterMailTemplatePath, clusterName + " cluster termination", getEmailModel(user.givenName,
                ambariServer, State.TERMINATION_FAILURE, clusterName))
    }


    private fun sendEmail(user: CbUser, template: String, subject: String, model: Map<String, Any>) {
        try {
            val emailBody = processTemplateIntoString(freemarkerConfiguration!!.getTemplate(template, "UTF-8"), model)
            LOGGER.debug("Sending email. Content: {}", emailBody)
            mailSender!!.send(emailMimeMessagePreparator!!.prepareMessage(user, String.format("Cloudbreak - %s", subject), emailBody))
        } catch (e: Exception) {
            LOGGER.error("Could not send email. User: {}", user.userId)
            throw CloudbreakServiceException(e)
        }

    }

    private fun getEmailModel(name: String, server: String?, state: State, clusterName: String): Map<String, Any> {
        val model = HashMap<String, Any>()
        model.put("status", state.status)
        model.put("server", server)
        model.put("name", name)
        model.put("text", String.format(state.text, clusterName, server))
        model.put("title", state.title)
        model.put("state", state)
        model.put("clusterName", clusterName)
        return model
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EmailSenderService::class.java)
    }
}
