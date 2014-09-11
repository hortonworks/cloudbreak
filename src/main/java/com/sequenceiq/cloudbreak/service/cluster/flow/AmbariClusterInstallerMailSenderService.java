package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.User;

import freemarker.template.Configuration;

@Service
public class AmbariClusterInstallerMailSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterInstallerMailSenderService.class);

    // @Value("${cb.smtp.sender.from}")
    private String msgFrom = "no-reply@sequenceiq.com";

    @Autowired
    private MailSender mailSender;

    @Autowired
    private Configuration freemarkerConfiguration;

    @VisibleForTesting
    protected MimeMessagePreparator prepareSuccessMessage(final User user, final String template, final String status,
            final String server) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(user.getEmail());
                message.setSubject("Cloudbreak - stack installation");
                message.setText(getEmailBody(status, server, template), true);
            }
        };
    }

    @VisibleForTesting
    protected MimeMessagePreparator prepareFailMessage(final String email, final String template, final String status) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(email);
                message.setSubject("Cloudbreak - stack installation");
//                message.setText(getEmailBody(user.getFirstName(), status, "", template), true);
                message.setText(getEmailBody(status, "", template), true);
            }
        };
    }

    public void sendSuccessEmail(User user, String ambariServer) {
        MimeMessagePreparator mimeMessagePreparator = prepareSuccessMessage(user, "templates/cluster-installer-mail-success.ftl", "SUCCESS", ambariServer);
        sendInstallationEmail(mimeMessagePreparator);
    }

    public void sendFailEmail(String email) {
        MimeMessagePreparator mimeMessagePreparator = prepareFailMessage(email, "templates/cluster-installer-mail-fail.ftl", "FAILED");
        sendInstallationEmail(mimeMessagePreparator);
    }

    private void sendInstallationEmail(final MimeMessagePreparator preparator) {
        LOGGER.info("Sending cluster installation email ...");
        ((JavaMailSender) mailSender).send(preparator);
        LOGGER.info("Cluster installation email sent");
    }

    private String getEmailBody(String status, String server, String template) {
        String text = null;
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("status", status);
            model.put("server", server);
            text = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
        } catch (Exception e) {
            LOGGER.error("Cluster installer email assembling failed. Exception: {}", e);
            throw new BadRequestException("Failed to assemble cluster installer email message", e);
        }
        return text;
    }
}
