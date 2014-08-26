package com.sequenceiq.periscope.service;

import java.io.IOException;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private Configuration freemarkerConfiguration;

    @Value("${periscope.smtp.from}")
    private String msgFrom;

    @Async
    public void sendMail(String[] to, String subject, String template, Map<String, String> properties)
            throws IOException, TemplateException {
        String messageBody = generateMessageBody(template, properties);
        MimeMessagePreparator message = prepareMessage(to, subject, messageBody);
        mailSender.send(message);
    }

    private String generateMessageBody(String templateName, Map<String, String> model) throws IOException, TemplateException {
        Template template = freemarkerConfiguration.getTemplate(templateName, "UTF-8");
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
    }

    private MimeMessagePreparator prepareMessage(final String[] to, final String subject, final String text) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text, true);
            }
        };
    }
}
