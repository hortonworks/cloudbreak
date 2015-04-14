package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;
import org.springframework.util.StringUtils;

import freemarker.template.TemplateException;

@Configuration
public class MailSenderConfig {
    @Value("${cb.smtp.sender.host:}")
    private String host;

    @Value("${cb.smtp.sender.port:25}")
    private int port;

    @Value("${cb.smtp.sender.username:}")
    private String userName;

    @Value("${cb.smtp.sender.password:}")
    private String password;

    @Value("${cb.smtp.sender.from:}")
    private String msgFrom;

    @Bean
    public MailSender mailSender() {
        MailSender mailSender = null;
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
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.debug", true);
        return props;
    }

    @Bean
    public freemarker.template.Configuration freemarkerConfiguration() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    private final class DummyEmailSender implements MailSender {
        private final Logger logger = LoggerFactory.getLogger(DummyEmailSender.class);
        private final String msg = "SMTP not configured! Related configuration entries: " + missingVars();

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
