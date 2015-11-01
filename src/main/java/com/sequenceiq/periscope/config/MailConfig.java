package com.sequenceiq.periscope.config;

import java.io.IOException;
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
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.util.StringUtils;

import freemarker.template.TemplateException;

@Configuration
public class MailConfig {

    @Value("${periscope.smtp.host:}")
    private String host;
    @Value("${periscope.smtp.port:25}")
    private int port;
    @Value("${periscope.smtp.username:}")
    private String userName;
    @Value("${periscope.smtp.password:}")
    private String password;
    @Value("${cb.mail.smtp.auth:true}")
    private String smtpAuth;
    @Value("${cb.mail.smtp.starttls.enable:true}")
    private String smtpStarttlsEnable;

    @Bean
    public freemarker.template.Configuration freemarkerConfiguration() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Bean
    public JavaMailSender mailSender() {
        JavaMailSender mailSender = null;
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

    private Properties getJavaMailProperties() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", smtpAuth);
        props.put("mail.smtp.starttls.enable", smtpStarttlsEnable);
        props.put("mail.debug", true);
        return props;
    }

    private String missingVars() {
        List<String> missingVars = new ArrayList();
        if (StringUtils.isEmpty(host)) {
            missingVars.add("periscope.smtp.host");
        }
        if (StringUtils.isEmpty(userName)) {
            missingVars.add("periscope.smtp.username");
        }
        if (StringUtils.isEmpty(password)) {
            missingVars.add("periscope.smtp.password");
        }
        return StringUtils.collectionToDelimitedString(missingVars, ",", "[", "]");
    }

    private boolean isMailSendingConfigured() {
        // some SMTP servers don't need username/password
        return !StringUtils.isEmpty(host);
    }

    private final class DummyEmailSender implements JavaMailSender {
        private final Logger logger = LoggerFactory.getLogger(DummyEmailSender.class);
        private final String msg = "SMTP not configured! Related configuration entries: " + missingVars();

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
