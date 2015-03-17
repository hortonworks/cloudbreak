package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import freemarker.template.TemplateException;

@Configuration
public class MailSenderConfig {
    @Value("${cb.smtp.sender.host}")
    private String host;

    @Value("${cb.smtp.sender.port}")
    private int port;

    @Value("${cb.smtp.sender.username}")
    private String userName;

    @Value("${cb.smtp.sender.password}")
    private String password;

    @Value("${cb.smtp.sender.from}")
    private String msgFrom;
    @Bean
    public MailSender mailSender() {
        MailSender mailSender = new JavaMailSenderImpl();
        ((JavaMailSenderImpl) mailSender).setHost(host);
        ((JavaMailSenderImpl) mailSender).setPort(port);
        ((JavaMailSenderImpl) mailSender).setUsername(userName);
        ((JavaMailSenderImpl) mailSender).setPassword(password);
        ((JavaMailSenderImpl) mailSender).setJavaMailProperties(getJavaMailProperties());
        return mailSender;
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

}
