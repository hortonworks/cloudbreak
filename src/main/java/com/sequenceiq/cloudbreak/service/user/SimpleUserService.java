package com.sequenceiq.cloudbreak.service.user;


import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import freemarker.template.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.DigestUtils;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;

@Service
public class SimpleUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private Configuration freemarkerConfiguration;

    @Value("${host.addr}")
    private String hostAddress;

    @Value("${mail.sender.from}")
    private String msgFrom;


    @Override
    public Long registerUser(User user) {
        String confToken = generateRegistrationId(user);
        user.setConfToken(confToken);
        User savedUser = userRepository.save(user);
        LOGGER.info("User {} successfully saved", user);
        sendConfirmationEmail(user);
        return savedUser.getId();
    }

    @Override
    public void confirmRegistration(String confToken) {
        User user = userRepository.findUserByConfToken(confToken);
        if (null != user) {
            user.setConfToken(null);
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } else {
            LOGGER.warn("There's no user registration pending for confToken: {}", confToken);
        }
    }

    private String generateRegistrationId(User user) {
        LOGGER.debug("Generating registration id ...");
        String textToDigest = user.getEmail() + user.getPassword();
        return DigestUtils.md5DigestAsHex(textToDigest.getBytes());
    }

    private String getEmailBody(User user) {
        String text = null;
        try {
            String template = "templates/confirmation-email.ftl";
            Map<String, Object> model = new HashMap<>();
            model.put("user", user);
            model.put("confirm", hostAddress + "/users/confirm/" + user.getConfToken());
            text = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
        } catch (Exception e) {
            LOGGER.error("Confirmation email assembling failed. Exception: {}", e);
        }
        return text;
    }

    public void sendConfirmationEmail(final User user) {
        LOGGER.info("Sending confirmation email ...");
        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(user.getEmail());
                message.setSubject("Cloudbreak - confirm registration");
                message.setText(getEmailBody(user), true);
            }
        };
        ((JavaMailSender) mailSender).send(preparator);
        LOGGER.info("Confirmation email sent");
    }

}
