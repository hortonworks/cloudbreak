package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_SMTP_SENDER_FROM;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CbUser;

@Service
public class EmailMimeMessagePreparator {

    @Value("${cb.smtp.sender.from:" + CB_SMTP_SENDER_FROM + "}")
    private String msgFrom;

    public MimeMessagePreparator prepareMessage(final CbUser user, final String subject, final String body) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(user.getUsername());
                message.setSubject(subject);
                message.setText(body, true);
            }
        };
    }
}
