package com.sequenceiq.cloudbreak.service.email;

import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import freemarker.template.Configuration;

@Service
public class DefaultEmailService implements EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEmailService.class);

    @Autowired
    private MailSender mailSender;

    @Autowired
    private Configuration freemarkerConfiguration;

    @Override
    public String messageText(Map<String, Object> model, String template) {
        MDCBuilder.buildMdcContext();
        String emailBody = null;
        try {
            emailBody = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Could not assemble message body; template: %s", template));
        }
        return emailBody;
    }

    @Override
    public MimeMessagePreparator messagePreparator(final String emailTo, final String emailFrom, final String subject, final String emailText) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(emailFrom);
                message.setTo(emailTo);
                message.setSubject(subject);
                message.setText(emailText, true);
            }
        };
    }

    @Override
    @Async
    public void sendEmail(final MimeMessagePreparator preparator) {
        MDCBuilder.buildMdcContext();
        LOGGER.info("Sending confirmation email ...");
        ((JavaMailSender) mailSender).send(preparator);
        LOGGER.info("Confirmation email sent");
    }

}
