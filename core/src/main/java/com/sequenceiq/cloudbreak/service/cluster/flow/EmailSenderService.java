package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_FAILED_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_SMTP_SENDER_FROM;
import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_SUCCESS_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH;
import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
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
    private JavaMailSender mailSender;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private UserDetailsService userDetailsService;

    @Async
    public void sendProvisioningSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster is ready to use. You can log into the Ambari UI %s:8080 using the configured username/password.",
                ambariServer);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster installation", getEmailModel(user.getGivenName(), "SUCCESS", ambariServer, text,
                "Cloudbreak Cluster Install Success"));
    }

    @Async
    public void sendProvisioningFailureEmail(String email) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = "Something went terribly wrong "
                + "- we are happy to help, please let us know your cluster details, time, etc - and we will check the logs and get a fix for you.";
        sendEmail(user, failedClusterMailTemplatePath, "cloudbreak cluster installation", getEmailModel(user.getGivenName(), "FAILED", null, text,
                "Cloudbreak Cluster Install Failed"));
    }

    @Async
    public void sendStartSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster is ready to use after the start. "
                        + "You can log into the Ambari UI %s:8080 using the configured username/password.", ambariServer);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster start", getEmailModel(user.getGivenName(), "SUCCESS", ambariServer, text,
                "Cloudbreak Cluster Start Success"));
    }

    @Async
    public void sendStartFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster start failed. "
                        + "- we are happy to help, please let us know your cluster details, time, etc - and we will check the logs and get a fix for you.");
        sendEmail(user, failedClusterMailTemplatePath, "cloudbreak cluster start", getEmailModel(user.getGivenName(), "FAILED", ambariServer, text,
                "Cloudbreak Cluster Start Failed"));
    }

    @Async
    public void sendStopSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster stop was success. If you want to use again just restart.");
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster stop", getEmailModel(user.getGivenName(), "SUCCESS", ambariServer, text,
                "Cloudbreak Cluster Stop Success"));
    }

    @Async
    public void sendStopFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster stop failed. "
                + "- we are happy to help, please let us know your cluster details, time, etc - and we will check the logs and get a fix for you.");
        sendEmail(user, failedClusterMailTemplatePath, "cloudbreak cluster stop", getEmailModel(user.getGivenName(), "FAILED", ambariServer, text,
                "Cloudbreak Cluster Stop Failed"));
    }

    @Async
    public void sendUpscaleSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster is ready to use after the upscale. "
                + "You can log into the Ambari UI %s:8080 using the configured username/password.", ambariServer);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster upscale", getEmailModel(user.getGivenName(), "SUCCESS", ambariServer, text,
                "Cloudbreak Cluster Upscale Success"));
    }

    @Async
    public void sendDownScaleSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster is ready to use after the dwnscale. "
                + "You can log into the Ambari UI %s:8080 using the configured username/password.", ambariServer);
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster downscale", getEmailModel(user.getGivenName(), "SUCCESS", ambariServer, text,
                "Cloudbreak Cluster Downscale Success"));
    }

    @Async
    public void sendTerminationSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster termination was success.");
        sendEmail(user, successClusterMailTemplatePath, "cloudbreak cluster termination", getEmailModel(user.getGivenName(), "SUCCESS", ambariServer, text,
                "Cloudbreak Cluster Termination Success"));
    }

    @Async
    public void sendTerminationFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        String text = String.format("Your cluster termination failed. Please try again...");
        sendEmail(user, failedClusterMailTemplatePath, "cloudbreak cluster termination", getEmailModel(user.getGivenName(), "FAILED", ambariServer, text,
                "Cloudbreak Cluster Termination Failed"));
    }


    private void sendEmail(CbUser user, String template, String subject, Map<String, Object> model) {
        try {
            String emailBody = processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
            LOGGER.debug("Sending email. Content: {}", emailBody);
            mailSender.send(prepareMessage(user, String.format("Cloudbreak - %s", subject), emailBody));
        } catch (Exception e) {
            LOGGER.error("Could not send email. User: {}", user.getUserId());
            throw new CloudbreakServiceException(e);
        }
    }

    private Map<String, Object> getEmailModel(String name, String status, String server, String text, String title) {
        Map<String, Object> model = new HashMap<>();
        model.put("status", status);
        model.put("server", server);
        model.put("name", name);
        model.put("text", text);
        model.put("title", title);
        return model;
    }

    private MimeMessagePreparator prepareMessage(final CbUser user, final String subject, final String body) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(user.getUsername());
                message.setSubject(subject);
                message.setText(body, true);
            }
        };
    }
}
