package com.sequenceiq.cloudbreak.service.cluster.flow;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

import freemarker.template.Configuration;

@Service
public class EmailSenderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSenderService.class);

    @Value("${cb.smtp.sender.from: no-reply@sequenceiq.com}")
    private String msgFrom;

    @Value("${cb.success.cluster.installer.mail.template.path:templates/cluster-installer-mail-success.ftl}")
    private String successClusterInstallerMailTemplatePath;

    @Value("${cb.failed.cluster.installer.mail.template.path:templates/cluster-installer-mail-fail.ftl}")
    private String failedClusterInstallerMailTemplatePath;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private Configuration freemarkerConfiguration;

    @Autowired
    private UserDetailsService userDetailsService;

    @Async
    public void sendSuccessEmail(String email, String ambariServer) {
        MDCBuilder.buildMdcContext();
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, successClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "SUCCESS", ambariServer));
    }

    @Async
    public void sendFailureEmail(String email) {
        MDCBuilder.buildMdcContext();
        CbUser user = userDetailsService.getDetails(email, UserFilterField.USERID);
        sendEmail(user, failedClusterInstallerMailTemplatePath, getEmailModel(user.getGivenName(), "FAILED", null));
    }

    private void sendEmail(CbUser user, String template, Map<String, Object> model) {
        try {
            String emailBody = processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
            ((JavaMailSender) mailSender).send(prepareMessage(user, "Cloudbreak - stack installation", emailBody));
        } catch (Exception e) {
            LOGGER.error("Couldn't send stack installation email");
            throw new CloudbreakRuntimeException("Exception during cluster install failure email sending.", e);
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
