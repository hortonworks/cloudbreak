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
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.logger.CbLoggerFactory;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

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

    @Autowired
    private UserDetailsService userDetailsService;

    @VisibleForTesting
    protected MimeMessagePreparator prepareSuccessMessage(final String userId, final String template, final String status,
            final String server) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                CbUser user = userDetailsService.getDetails(userId, UserFilterField.USERID);
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(user.getUsername());
                message.setSubject("Cloudbreak - stack installation");
                message.setText(getEmailBody(user.getGivenName(), status, server, template), true);
            }
        };
    }

    @VisibleForTesting
    protected MimeMessagePreparator prepareFailMessage(final String userId, final String template, final String status) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                CbUser user = userDetailsService.getDetails(userId, UserFilterField.USERID);
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(user.getUsername());
                message.setSubject("Cloudbreak - stack installation");
                message.setText(getEmailBody(user.getGivenName(), status, "", template), true);
            }
        };
    }

    public void sendSuccessEmail(String email, String ambariServer) {
        MimeMessagePreparator mimeMessagePreparator = prepareSuccessMessage(email, "templates/cluster-installer-mail-success.ftl", "SUCCESS", ambariServer);
        sendInstallationEmail(mimeMessagePreparator);
    }

    public void sendFailEmail(String email) {
        MimeMessagePreparator mimeMessagePreparator = prepareFailMessage(email, "templates/cluster-installer-mail-fail.ftl", "FAILED");
        sendInstallationEmail(mimeMessagePreparator);
    }

    private void sendInstallationEmail(final MimeMessagePreparator preparator) {
        CbLoggerFactory.buildMdvContext();
        LOGGER.info("Sending cluster installation email ...");
        ((JavaMailSender) mailSender).send(preparator);
        LOGGER.info("Cluster installation email sent");
    }

    private String getEmailBody(String name, String status, String server, String template) {
        CbLoggerFactory.buildMdvContext();
        String text = null;
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("status", status);
            model.put("server", server);
            model.put("name", name);
            text = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
        } catch (Exception e) {
            LOGGER.error("Cluster installer email assembling failed. Exception: {}", e);
            throw new BadRequestException("Failed to assemble cluster installer email message", e);
        }
        return text;
    }
}
