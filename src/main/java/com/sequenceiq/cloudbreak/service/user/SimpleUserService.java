package com.sequenceiq.cloudbreak.service.user;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.converter.BlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

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

    @Value("${host.addr}")
    private String hostAddress;

    @Value("${mail.sender.from}")
    private String msgFrom;

    @Value("${uluwatu.addr}")
    private String uiAddress;

    @Value("#{'${blueprint.defaults}'.split(',')}")
    private List<String> blueprintArray;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @Autowired
    private JsonHelper jsonHelper;


    @Override
    public Long registerUser(User user) {
        if (userRepository.findByEmail(user.getEmail()) == null) {
            String confToken = generateRegistrationId(user);
            user.setConfToken(confToken);
            for (String blueprintName : blueprintArray) {
                try {
                    BlueprintJson blueprintJson = new BlueprintJson();
                    blueprintJson.setBlueprintName(blueprintName);
                    blueprintJson.setName(blueprintName);
                    blueprintJson.setDescription(blueprintName);
                    blueprintJson.setAmbariBlueprint(
                            jsonHelper.createJsonFromString(FileReaderUtils.readFileFromClasspath(String.format("blueprints/%s.bp", blueprintName)))
                    );

                    Blueprint bp = blueprintConverter.convert(blueprintJson);
                    bp.setUser(user);

                    user.getBlueprints().add(bp);
                } catch (IOException ex) {
                    LOGGER.error(blueprintName + " blueprint is not available.");
                }
            }
            User savedUser = userRepository.save(user);
            LOGGER.info("User {} successfully saved", user);
            MimeMessagePreparator msgPreparator = prepareMessage(user, "templates/confirmation-email.ftl", "#?confirmSignUpToken=");
            sendConfirmationEmail(msgPreparator);
            return savedUser.getId();
        } else {
            throw new BadRequestException("User already exists.");
        }
    }

    @Override
    public String confirmRegistration(String confToken) {
        User user = userRepository.findUserByConfToken(confToken);
        if (null != user) {
            user.setConfToken(null);
            user.setStatus(UserStatus.ACTIVE);
            user.setRegistrationDate(new Date());
            userRepository.save(user);
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
            userRepository.save(user);
            MimeMessagePreparator msgPreparator = prepareMessage(user, "templates/reset-email.ftl", "#?resetToken=");
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
        String decodedPassword = Base64Coder.decodeString(password);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(decodedPassword));
            user.setConfToken(null);
            userRepository.save(user);
            return confToken;
        } else {
            LOGGER.warn("There's no user for token: {}", confToken);
            throw new NotFoundException("There's no user for token: " + confToken);
        }
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
            model.put("confirm", getConfirmLinkPath() + confirmPath + user.getConfToken());
            text = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), model);
        } catch (Exception e) {
            LOGGER.error("Confirmation email assembling failed. Exception: {}", e);
            throw new BadRequestException("Failed to assemble confirmation email message", e);
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

    private String getConfirmLinkPath() {
        return uiAddress == null ? hostAddress : uiAddress;
    }

    @Async
    public void sendConfirmationEmail(final MimeMessagePreparator preparator) {
        LOGGER.info("Sending confirmation email ...");
        ((JavaMailSender) mailSender).send(preparator);
        LOGGER.info("Confirmation email sent");
    }

}
