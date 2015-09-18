package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_FAILED_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_SMTP_SENDER_FROM;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_SUCCESS_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

import freemarker.template.Configuration;

@Service
public class EmailSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderService.class);

    @Value("${cb.smtp.sender.from:" + CB_SMTP_SENDER_FROM + "}")
    private String msgFrom;

    @Value("${cb.success.cluster.installer.mail.template.path:" + CB_SUCCESS_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH + "}")
    private String successClusterMailTemplatePath;

    @Value("${cb.failed.cluster.installer.mail.template.path:" + CB_FAILED_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH + "}")
    private String failedClusterMailTemplatePath;

    @Inject
    private EmailMimeMessagePreparator emailMimeMessagePreparator;

    @Inject
    private JavaMailSender mailSender;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private UserDetailsService userDetailsService;

    private enum State {
        PROVISIONING_SUCCESS("SUCCESS", "Cloudbreak Cluster Install Success",
                "Your cluster is ready to use. You can log into the Ambari UI %s:8080 using the configured username/password."),
        PROVISIONING_FAILURE("FAILED", "Cloudbreak Cluster Install Failed",
                "Something went terribly wrong - we are happy to help, please let us know your cluster details, "
                        + "time, etc - and we will check the logs and get a fix for you."),
        START_SUCCESS("SUCCESS", "Cloudbreak Cluster Start Success",
                "Your cluster is ready to use after the start. You can log into the Ambari UI %s:8080 using the configured username/password."),
        START_FAILURE("FAILED", "Cloudbreak Cluster Start Failed",
                "Your cluster start failed. - we are happy to help, please let us know your cluster details, time, etc - and we will check the"
                        + " logs and get a fix for you."),
        STOP_SUCCESS("SUCCESS", "Cloudbreak Cluster Stop Success",
                "Your cluster stop was success. If you want to use again just restart."),
        STOP_FAILURE("FAILED", "Cloudbreak Cluster Stop Failed",
                "Your cluster stop failed. - we are happy to help, please let us know your cluster details, time, etc - and we will check the "
                        + "logs and get a fix for you."),
        UPSCALE_SUCCESS("SUCCESS", "Cloudbreak Cluster Upscale Success",
                "Your cluster is ready to use after the upscale. You can log into the Ambari UI %s:8080 using the configured username/password."),
        DOWN_SCALE_SUCCESS("SUCCESS", "Cloudbreak Cluster Downscale Success",
                "Your cluster is ready to use after the dwnscale. You can log into the Ambari UI %s:8080 using the configured username/password."),
        TERMINATION_SUCCESS("SUCCESS", "Cloudbreak Cluster Termination Success",
                "Your cluster termination was success."),
        TERMINATION_FAILURE("FAILED", "Cloudbreak Cluster Termination Failed",
                "Your cluster termination failed. Please try again...");

        private final String status;
        private final String title;
        private final String text;

        State(String status, String title, String text) {
            this.status = status;
            this.title = title;
            this.text = text;
        }
    }

    @Async
    public void sendProvisioningSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster installation", getEmailModel(user.getGivenName(),
                ambariServer, State.PROVISIONING_SUCCESS));
    }

    @Async
    public void sendProvisioningFailureEmail(String email) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, failedClusterMailTemplatePath, "cloudbreak cluster installation", getEmailModel(user.getGivenName(),
                null, State.PROVISIONING_FAILURE));
    }

    @Async
    public void sendStartSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster start", getEmailModel(user.getGivenName(),
                ambariServer, State.START_SUCCESS));
    }

    @Async
    public void sendStartFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, failedClusterMailTemplatePath, "cloudbreak cluster start", getEmailModel(user.getGivenName(),
                ambariServer, State.START_FAILURE));
    }

    @Async
    public void sendStopSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster stop", getEmailModel(user.getGivenName(),
                ambariServer, State.STOP_SUCCESS));
    }

    @Async
    public void sendStopFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, failedClusterMailTemplatePath, "cloudbreak cluster stop", getEmailModel(user.getGivenName(),
                ambariServer, State.STOP_FAILURE));
    }

    @Async
    public void sendUpscaleSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster upscale", getEmailModel(user.getGivenName(),
                ambariServer, State.UPSCALE_SUCCESS));
    }

    @Async
    public void sendDownScaleSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster downscale", getEmailModel(user.getGivenName(),
                ambariServer, State.DOWN_SCALE_SUCCESS));
    }

    @Async
    public void sendTerminationSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster termination", getEmailModel(user.getGivenName(),
                ambariServer, State.TERMINATION_SUCCESS));
    }

    @Async
    public void sendTerminationFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, failedClusterMailTemplatePath, "cloudbreak cluster termination", getEmailModel(user.getGivenName(),
                ambariServer, State.TERMINATION_FAILURE));
    }


    private void sendEmail(CbUser user, String template, String subject, Map<String, Object> model) {
        try {
            String emailBody = processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
            LOGGER.debug("Sending email. Content: {}", emailBody);
            mailSender.send(emailMimeMessagePreparator.prepareMessage(user, String.format("Cloudbreak - %s", subject), emailBody));
        } catch (Exception e) {
            LOGGER.error("Could not send email. User: {}", user.getUserId());
            throw new CloudbreakServiceException(e);
        }
    }

    private Map<String, Object> getEmailModel(String name, String server, State state) {
        Map<String, Object> model = new HashMap<>();
        model.put("status", state.status);
        model.put("server", server);
        model.put("name", name);
        model.put("text", String.format(state.text, server));
        model.put("title", state.title);
        model.put("state", state);
        return model;
    }
}
