package com.sequenceiq.notification.sender.mail;

import static jakarta.mail.Message.RecipientType.TO;

import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.notification.domain.ChannelType;
import com.sequenceiq.notification.sender.LocalEmailProvider;
import com.sequenceiq.notification.sender.NotificationSenderService;
import com.sequenceiq.notification.sender.dto.NotificationDto;

@Service
public class LocalSenderService implements NotificationSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalSenderService.class);

    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";

    private static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    private static final String MAIL_SMTP_HOST = "mail.smtp.host";

    private static final String MAIL_SMTP_PORT = "mail.smtp.port";

    private static final String TRUE_VALUE = "true";

    private static final String SMTP_HOST_VALUE = "smtp.gmail.com";

    private static final String SMTP_PORT_VALUE = "587";

    private static final Properties PROPERTIES;

    static {
        PROPERTIES = new Properties();
        PROPERTIES.put(MAIL_SMTP_AUTH, TRUE_VALUE);
        PROPERTIES.put(MAIL_SMTP_STARTTLS_ENABLE, TRUE_VALUE);
        PROPERTIES.put(MAIL_SMTP_HOST, SMTP_HOST_VALUE);
        PROPERTIES.put(MAIL_SMTP_PORT, SMTP_PORT_VALUE);
    }

    private final LocalEmailProvider localEmailProvider;

    public LocalSenderService(
            LocalEmailProvider localEmailProvider) {
        this.localEmailProvider = localEmailProvider;
    }

    @Override
    public void send(NotificationDto notificationDto) {
        try {
            if (localEmailProvider.emailSendingConfigured()) {
                sendEmail(notificationDto);
            } else {
                LOGGER.info("Skip sending emails");
            }
        } catch (MessagingException e) {
            LOGGER.error("Cloud not sent the message: ", e);
        }

        LOGGER.debug("Sending {} email notification for resourceCrn: {} with type: {}",
                notificationDto.getSeverity(),
                notificationDto.getResourceCrn(),
                notificationDto.getType());
    }

    private void sendEmail(NotificationDto notificationDto) throws MessagingException {
        Message message = new MimeMessage(Session.getInstance(PROPERTIES, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        localEmailProvider.getEmailFromAddress(),
                        localEmailProvider.getEmailApplicationSecret()
                );
            }
        }));
        message.setFrom(InternetAddress.parse(localEmailProvider.getEmailFromAddress())[0]);
        message.setRecipients(TO, InternetAddress.parse(localEmailProvider.getEmailToAddress()));
        message.setSubject(String.format(notificationDto.getType().getSubjectTemplate(), notificationDto.getName()));
        message.setContent(notificationDto.getMessage(), "text/html; charset=utf-8");

        Transport.send(message);
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.LOCAL_EMAIL;
    }
}
