package com.sequenceiq.cloudbreak.service.user;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.blueprint.DefaultBlueprintLoaderService;
import com.sequenceiq.cloudbreak.service.email.EmailService;

@Service
public class SimpleUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);

    @Autowired
    private UserRepository userRepository;

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
    private EmailService emailService;

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
            Map<String, Object> model = new HashMap<>();
            model.put("user", user);
            model.put("confirm", getConfirmLinkPath() + getRegisterUserConfirmPath() + user.getConfToken());

            String messageText = emailService.messageText(model, "templates/confirmation-email.ftl");
            MimeMessagePreparator msgPreparator = emailService.messagePreparator(user.getEmail(), msgFrom, "Cloudbreak - confirm registration", messageText);
            emailService.sendEmail(msgPreparator);

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

            Map<String, Object> model = new HashMap<>();
            model.put("user", user);
            model.put("confirm", getConfirmLinkPath() + getResetPasswordConfirmPath() + user.getConfToken());

            String emailText = emailService.messageText(model, getResetTemplate());
            MimeMessagePreparator messagePreparator = emailService.messagePreparator(user.getEmail(), msgFrom, "Cloudbreak - reset password", emailText);
            emailService.sendEmail(messagePreparator);
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
        invitedUser.setFirstName(UUID.randomUUID().toString());
        invitedUser.setLastName(UUID.randomUUID().toString());
        invitedUser.setPassword(UUID.randomUUID().toString());

        invitedUser = userRepository.save(invitedUser);

        Map<String, Object> model = new HashMap<>();

        model.put("user", adminUser);
        model.put("invite", getInviteRegistrationPath() + invitedUser.getConfToken());
        String emailText = emailService.messageText(model, getInviteTemplate());

        MimeMessagePreparator messagePreparator = emailService.messagePreparator(invitedUser.getEmail(), msgFrom, "Cloudbreak - invitation", emailText);
        emailService.sendEmail(messagePreparator);

        return inviteHash;
    }

    @Override
    public User invitedUser(String inviteToken) {
        LOGGER.debug("Registering upon invitation. Token: {}", inviteToken);
        User invitedUser = userRepository.findUserByConfToken(inviteToken);
        if (!UserStatus.INVITED.equals(invitedUser.getStatus())) {
            throw new IllegalStateException("The user has already been registered!");
        }
        invitedUser.setRegistrationDate(new Date());
        invitedUser.setFirstName("");
        invitedUser.setLastName("");
        invitedUser.setPassword("");

        return invitedUser;
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
        return uiEnabled ? "#?inviteToken=" : "/admin/users/invite/";
    }


    private String getResetPasswordConfirmPath() {
        return uiEnabled ? "#?resetToken=" : "/password/reset/";
    }

    private String generateConfToken(User user) {
        LOGGER.debug("Generating registration id ...");
        String textToDigest = user.getEmail() + user.getPassword();
        return DigestUtils.md5DigestAsHex(textToDigest.getBytes());
    }

    private String generateInviteToken(String accountName, String email) {
        LOGGER.debug("Generating registration id ...");
        String textToDigest = accountName + email;
        return DigestUtils.md5DigestAsHex(textToDigest.getBytes());
    }

    private String getConfirmLinkPath() {
        return uiEnabled ? uiAddress : hostAddress;
    }

}
