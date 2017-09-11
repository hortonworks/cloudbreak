package com.sequenceiq.cloudbreak.conf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.util.StringUtils;

@Configuration
public class MailSenderConfig {
    @Value("${cb.smtp.sender.host:}")
    private String host;

    @Value("${cb.smtp.sender.port:}")
    private int port;

    @Value("${cb.smtp.sender.username:}")
    private String userName;

    @Value("${cb.smtp.sender.password:}")
    private String password;

    @Value("${cb.smtp.sender.from:}")
    private String msgFrom;

    @Value("${cb.mail.smtp.auth:}")
    private String smtpAuth;

    @Value("${cb.mail.smtp.starttls.enable:}")
    private String smtpStarttlsEnable;

    @Value("${cb.mail.smtp.type:}")
    private String smtpType;

    @Bean
    public JavaMailSender mailSender() {
        JavaMailSender mailSender;
        if (isMailSendingConfigured()) {
            mailSender = new JavaMailSenderImpl();
            ((JavaMailSenderImpl) mailSender).setHost(host);
            ((JavaMailSenderImpl) mailSender).setPort(port);
            if (!StringUtils.isEmpty(userName)) {
                ((JavaMailSenderImpl) mailSender).setUsername(userName);
            }
            if (!StringUtils.isEmpty(password)) {
                ((JavaMailSenderImpl) mailSender).setPassword(password);
            }
            ((JavaMailSenderImpl) mailSender).setJavaMailProperties(getJavaMailProperties());
        } else {
            mailSender = new DummyEmailSender();
        }

        return mailSender;
    }

    private boolean isMailSendingConfigured() {
        // some SMTP servers don't need username/password
        return !StringUtils.isEmpty(host)
                && !StringUtils.isEmpty(msgFrom);
    }

    private String missingVars() {
        List<String> missingVars = new ArrayList();
        if (StringUtils.isEmpty(host)) {
            missingVars.add("cb.smtp.sender.host");
        }
        if (StringUtils.isEmpty(userName)) {
            missingVars.add("cb.smtp.sender.username");
        }
        if (StringUtils.isEmpty(password)) {
            missingVars.add("cb.smtp.sender.password");
        }
        if (StringUtils.isEmpty(msgFrom)) {
            missingVars.add("cb.smtp.sender.from");
        }
        return StringUtils.collectionToDelimitedString(missingVars, ",", "[", "]");
    }

    private Properties getJavaMailProperties() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", smtpType);
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", smtpStarttlsEnable);
        props.put("mail.debug", true);
        return props;
    }

    private final class DummyEmailSender implements JavaMailSender {
        private final Logger logger = LoggerFactory.getLogger(DummyEmailSender.class);

        private final String msg;

        private DummyEmailSender() {
            msg = "SMTP not configured! Related configuration entries: " + missingVars();
        }

        @Override
        public MimeMessage createMimeMessage() {
            return null;
        }

        @Override
        public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
            return null;
        }

        @Override
        public void send(MimeMessage mimeMessage) throws MailException {
            logger.info(msg);
        }

        @Override
        public void send(MimeMessage[] mimeMessages) throws MailException {
            logger.info(msg);
        }

        @Override
        public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
            logger.info(msg);
        }

        @Override
        public void send(MimeMessagePreparator[] mimeMessagePreparators) throws MailException {
            logger.info(msg);
        }

        @Override
        public void send(SimpleMailMessage simpleMessage) throws MailException {
            logger.info(msg);
        }

        @Override
        public void send(SimpleMailMessage[] simpleMessages) throws MailException {
            logger.info(msg);
        }
    }

}
