package com.sequenceiq.cloudbreak.service.email;

import java.util.Map;

import org.springframework.mail.javamail.MimeMessagePreparator;

public interface EmailService {

    String messageText(Map<String, Object> model, String template);

    MimeMessagePreparator messagePreparator(final String emailTo, final String emailFrom, final String subject, final String emailText);

    void sendEmail(final MimeMessagePreparator preparator);

}
