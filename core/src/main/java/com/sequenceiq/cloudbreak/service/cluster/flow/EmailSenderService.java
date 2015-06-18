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
    private String successClusterInstallerMailTemplatePath;

    @Value("${cb.failed.cluster.installer.mail.template.path:" + CB_FAILED_CLUSTER_INSTALLER_MAIL_TEMPLATE_PATH + "}")
    private String failedClusterInstallerMailTemplatePath;

    @Inject
    private JavaMailSender mailSender;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private UserDetailsService userDetailsService;

    @Async
    public void sendProvisioningSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendProvisioningFailureEmail(String email) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, failedClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "FAILED", null));
    }

    @Async
    public void sendStartSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendStartFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendStopSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendStopFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendUpscaleSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendUpscaleFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendDownScaleSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendDownScaleFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendTerminationSuccessEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendTerminationFailureEmail(String email, String ambariServer) {
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }


    private void sendEmail(CbUser user, String template, Map<String, Object> model) {
        try {
            String emailBody = processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
            LOGGER.debug("Sending email. Content: {}", emailBody);
            mailSender.send(prepareMessage(user, "Cloudbreak - stack installation", emailBody));
        } catch (Exception e) {
            LOGGER.error("Could not send email. User: {}", user.getUserId());
            throw new CloudbreakServiceException(e);
        }
    }

    private Map<String, Object> getEmailModel(String name, String status, String server) {
        Map<String, Object> model = new HashMap<>();
        model.put("status", status);
        model.put("server", server);
        model.put("name", name);
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
