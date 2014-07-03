package com.sequenceiq.cloudbreak.service.user;


import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailMessage;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class SimpleUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private MailMessage message;

    @Value("${host.addr}")
    private String hostAddress;

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

    @Async
    private void sendConfirmationEmail(User user) {
        LOGGER.info("Sending confirmation email ...");
        message.setTo(user.getEmail());
        message.setText(hostAddress + "/users/confirm/" + user.getConfToken());
        mailSender.send((SimpleMailMessage) message);
        LOGGER.info("Confirmation email sent...");
    }

    private String generateRegistrationId(User user) {
        LOGGER.debug("Generating registration id ...");
        String textToDigest = user.getEmail() + user.getPassword();
        return DigestUtils.md5DigestAsHex(textToDigest.getBytes());
    }

}
