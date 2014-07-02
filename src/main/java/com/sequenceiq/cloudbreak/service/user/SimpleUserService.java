package com.sequenceiq.cloudbreak.service.user;


import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class SimpleUserService implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailSender mailSender;

    @Override
    public Long registerUser(User user) {
        //TODO add column and finder to the user entity
        String registrationId = generateRegistrationId(user);

        //user.setRegistrationId(registrationId);

        User savedUser = userRepository.save(user);
        LOGGER.info("User {} successfully saved", user);

        sendConfirmationEmail(user);

        return savedUser.getId();
    }


    @Override
    public void confirmRegistration(String registrationId) {

        throw new UnsupportedOperationException("Not yet implemented ...");
    }

    private void sendConfirmationEmail(User user) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(user.getEmail());
        msg.setFrom("reg@sequenceiq.com");
        msg.setText("http://localhost:8080/users/confirm/" + user.getToken());
        mailSender.send(msg);

    }

    private String generateRegistrationId(User user) {
        LOGGER.debug("Generating registration id ...");
        String textToDigest = user.getEmail() + user.getPassword();
        return DigestUtils.md5DigestAsHex(textToDigest.getBytes());
    }

}
