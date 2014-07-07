package com.sequenceiq.cloudbreak.service.user;


import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.DigestUtils;

import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SimpleUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private Configuration freemarkerConfiguration;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        MimeMessagePreparator msgPreparator = prepareMessage(user, "templates/confirmation-email.ftl", "/users/confirm/");
        sendConfirmationEmail(msgPreparator);
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

    @Override
    public void disableUser(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setStatus(UserStatus.DISABLED);
            user.setPassword(UUID.randomUUID().toString());
            String confToken = generateRegistrationId(user);
            user.setConfToken(confToken);
            userRepository.save(user);
            MimeMessagePreparator msgPreparator = prepareMessage(user, "templates/reset-email.ftl", "/users/reset/");
            sendConfirmationEmail(msgPreparator);
        } else {
            LOGGER.warn("There's no user for email: {} ", email);
        }
    }

    @Override
    public void resetPassword(String confToken, String password) {
        User user = userRepository.findUserByConfToken(confToken);
        if (user.getStatus().equals(UserStatus.DISABLED)) {
            user.setPassword(passwordEncoder.encode(password));
            user.setConfToken(null);
            userRepository.save(user);
        } else {
            LOGGER.warn("There's no user for token: {}", confToken);
        }
    }

    @Override
    public boolean validateResetPassword(String confToken) {
        return userRepository.findUserByConfToken(confToken) != null;
    }

    private String generateRegistrationId(User user) {
        LOGGER.debug("Generating registration id ...");
        String textToDigest = user.getEmail() + user.getPassword();
        return DigestUtils.md5DigestAsHex(textToDigest.getBytes());
    }

    private String getEmailBody(User user, String template, String confirmPath) {
        String text = null;
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("user", user);
            model.put("confirm", hostAddress + confirmPath + user.getConfToken());
            text = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
        } catch (Exception e) {
            LOGGER.error("Confirmation email assembling failed. Exception: {}", e);
            throw new InternalServerException("Failed to assemble confirmation email message", e);
        }
        return text;
    }

    @VisibleForTesting
    protected MimeMessagePreparator prepareMessage(final User user, final String template, final String confirmPath) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(user.getEmail());
                message.setSubject("Cloudbreak - confirm registration");
                message.setText(getEmailBody(user, template, confirmPath), true);
            }
        };
    }

    @Async
    public void sendConfirmationEmail(final MimeMessagePreparator preparator) {
        LOGGER.info("Sending confirmation email ...");
        ((JavaMailSender) mailSender).send(preparator);
        LOGGER.info("Confirmation email sent");
    }

}
