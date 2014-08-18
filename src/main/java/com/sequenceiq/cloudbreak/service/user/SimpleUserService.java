package com.sequenceiq.cloudbreak.service.user;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.mail.internet.MimeMessage;

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

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.blueprint.DefaultBlueprintLoaderService;

import freemarker.template.Configuration;

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

    @Value("${cb.host.addr}")
    private String hostAddress;

    @Value("${cb.smtp.sender.from}")
    private String msgFrom;

    @Value("${cb.ui.addr:localhost}")
    private String uiAddress;

    @Value("${cb.mail.ui.enabled:false}")
    private boolean uiEnabled;

    @Autowired
    private DefaultBlueprintLoaderService defaultBlueprintLoaderService;

    @Override
    public Long registerUserInAccount(User user, Account account) {
        if (userRepository.findByEmail(user.getEmail()) == null) {
            String confToken = generateConfToken(user);
            user.setConfToken(confToken);
            user.setBlueprints(defaultBlueprintLoaderService.loadBlueprints(user));
            user.setAccount(account);
            User savedUser = userRepository.save(user);

            LOGGER.info("User '{}' for account '{}' successfully registered", savedUser.getId(), account.getId());
            MimeMessagePreparator msgPreparator = prepareMessage(user, "templates/confirmation-email.ftl",
                    getRegisterUserConfirmPath(), "Cloudbreak - confirm registration");
            sendConfirmationEmail(msgPreparator);
            return savedUser.getId();
        } else {
            throw new BadRequestException(String.format("User with email '%s' already exists.", user.getEmail()));
        }
    }

    @Override
    public String confirmRegistration(String confToken) {
        User user = userRepository.findUserByConfToken(confToken);
        if (null != user) {
            user.setConfToken(null);
            user.setStatus(UserStatus.ACTIVE);
            user.setRegistrationDate(new Date());
            User updatedUser = userRepository.save(user);
            return user.getEmail();
        } else {
            LOGGER.warn("There's no user registration pending for confToken: {}", confToken);
            throw new NotFoundException("There's no user registration pending for confToken: " + confToken);
        }
    }

    @Override
    public String generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setPassword(UUID.randomUUID().toString());
            String confToken = DigestUtils.md5DigestAsHex(UUID.randomUUID().toString().getBytes());
            user.setConfToken(confToken);
            User updatedUser = userRepository.save(user);
            MimeMessagePreparator msgPreparator = prepareMessage(user, getResetTemplate(),
                    getResetPasswordConfirmPath(), "Cloudbreak - reset password");
            sendConfirmationEmail(msgPreparator);
            return email;
        } else {
            LOGGER.warn("There's no user for email: {} ", email);
            throw new NotFoundException("There is no user for " + email);
        }
    }

    @Override
    public String resetPassword(String confToken, String password) {
        User user = userRepository.findUserByConfToken(confToken);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(password));
            user.setConfToken(null);
            User updatedUser = userRepository.save(user);
            return confToken;
        } else {
            LOGGER.warn("There's no user for token: {}", confToken);
            throw new NotFoundException("There's no user for token: " + confToken);
        }
    }

    @Override
    public String inviteUser(User adminUser, String email) {
        String inviteHash = generateInviteToken(adminUser.getAccount().getName(), email);
        // create a new user with the account, email and hash
        User invitedUser = new User();
        invitedUser.setAccount(adminUser.getAccount());
        invitedUser.setEmail(email);
        invitedUser.setConfToken(inviteHash);
        invitedUser.setStatus(UserStatus.INVITED);
        invitedUser.getUserRoles().add(UserRole.ACCOUNT_USER);

        invitedUser = userRepository.save(invitedUser);

        MimeMessagePreparator mimeMessagePreparator = prepareMessage(adminUser, getInviteTemplate(), getInviteRegistrationPath(),
                "Cloudbreak - invitation");
        sendConfirmationEmail(mimeMessagePreparator);

        return inviteHash;
    }

    @Override
    public User registerUserUponInvite(String inviteToken) {
        LOGGER.debug("Registering upon invitation. Token: {}", inviteToken);
        User registeringUser = userRepository.findUserByConfToken(inviteToken);
        if (!UserStatus.INVITED.equals(registeringUser.getStatus())) {
            throw new IllegalStateException("The user has already been registered!");
        }
        registeringUser.setStatus(UserStatus.ACTIVE);
        registeringUser.setConfToken(null);
        registeringUser.setRegistrationDate(new Date());
        registeringUser = userRepository.save(registeringUser);
        return registeringUser;
    }

    @Override
    public User setUserStatus(Long userId, UserStatus userStatus) {
        User user = userRepository.findOne(userId);
        LOGGER.debug("Modifying user: {};  setting status from: [{}] to: [{}]", user.getStatus(), userStatus);
        user.setStatus(userStatus);
        user = userRepository.save(user);
        return user;
    }

    private String getResetTemplate() {
        return uiEnabled ? "templates/reset-email.ftl" : "templates/reset-email-wout-ui.ftl";
    }

    private String getInviteTemplate() {
        return uiEnabled ? "templates/invite-email.ftl" : "templates/invite-email-wout-ui.ftl";
    }


    private String getRegisterUserConfirmPath() {
        return uiEnabled ? "#?confirmSignUpToken=" : "/users/confirm/";
    }

    private String getInviteRegistrationPath() {
        return uiEnabled ? "#?inviteToken=" : "/users/invite/";
    }


    private String getResetPasswordConfirmPath() {
        return uiEnabled ? "#?resetToken=" : "/password/reset/";
    }

    private String generateConfToken(User user) {
        LOGGER.debug("Generating registration id ...");
        String textToDigest = user.getEmail() + user.getPassword();
        return DigestUtils.md5DigestAsHex(textToDigest.getBytes());
    }

    private String getEmailBody(User user, String template, String confirmPath) {
        String text = null;
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("user", user);
            model.put("confirm", getConfirmLinkPath() + confirmPath + user.getConfToken());
            text = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
        } catch (Exception e) {
            LOGGER.error("Confirmation email assembling failed. Exception: {}", e);
            throw new BadRequestException("Failed to assemble confirmation email message", e);
        }
        return text;
    }

    private String generateInviteToken(String accountName, String email) {
        LOGGER.debug("Generating registration id ...");
        String textToDigest = accountName + email;
        return DigestUtils.md5DigestAsHex(textToDigest.getBytes());
    }

    @VisibleForTesting
    protected MimeMessagePreparator prepareMessage(final User user, final String template, final String confirmPath, final String subject) {
        return new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setFrom(msgFrom);
                message.setTo(user.getEmail());
                message.setSubject(subject);
                message.setText(getEmailBody(user, template, confirmPath), true);
            }
        };
    }


    private String getConfirmLinkPath() {
        return uiEnabled ? uiAddress : hostAddress;
    }

    @Async
    public void sendConfirmationEmail(final MimeMessagePreparator preparator) {
        LOGGER.info("Sending confirmation email ...");
        ((JavaMailSender) mailSender).send(preparator);
        LOGGER.info("Confirmation email sent");
    }

}
