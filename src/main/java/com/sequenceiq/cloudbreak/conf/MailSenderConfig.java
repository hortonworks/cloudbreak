package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailSenderConfig {
    @Value("${mail.sender.host:email-smtp.eu-west-1.amazonaws.com}")
    private String host;

    @Value("${mail.sender.port:587}")
    private int port;

    @Value("${mail.sender.username:AKIAIPUYXRHAMCDNUSLA}")
    private String userName;

    @Value("${mail.sender.password:Aviq2rAqS6wYNzVlBmDlHUQc/pkFgnuhaKEyxY3JWUNC}")
    private String password;

    @Value("${mail.sender.from:no-reply@sequenceiq.com}")
    private String msgFrom;

    @Bean
    public MailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(userName);
        mailSender.setPassword(password);
        mailSender.setJavaMailProperties(getJavaMailProperties());
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
    public MailMessage mailMessage() {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(msgFrom);
        msg.setSubject("Cloudbreak - confirm registration");
        return msg;
    }

}
