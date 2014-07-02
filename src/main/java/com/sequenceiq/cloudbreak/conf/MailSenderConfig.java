package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailSenderConfig {
    @Value("${mail.sender.host:email-smtp.eu-west-1.amazonaws.com}")
    private String host;

    @Value("${mail.sender.port:465}")
    private int port;

    @Value("${mail.sender.username:AKIAJGTOIVXD7OAQG6IA}")
    private String userName;

    @Value("${mail.sender.password:AuBcRIu3HKTzGxJBcY8T89aA2x6RjNjL1KfDkrlRSPnq}")
    private String password;

    @Bean
    public MailSender mailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(userName);
        mailSender.setPassword(password);
        return mailSender;
    }
}
