package com.sequenceiq.cloudbreak.service.user;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SimpleUserServiceTest {

    private static final String DUMMY_EMAIL = "gipszjakab@myemail.com";
    private static final String DUMMY_PASSWORD = "test123";
    private static final String DUMMY_TOKEN = "KJ3fws";
    private static final String DUMMY_NEW_PASSWORD = "newPass";


    @InjectMocks
    @Spy
    private SimpleUserService underTest;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MimeMessagePreparator mimeMessagePreparator;

    private User user;

    @Before
    public void setUp() {
        underTest = new SimpleUserService();
        MockitoAnnotations.initMocks(this);
        user = createUser();
    }

    @Test
    public void testGenerateResetPasswordToken() {
        // GIVEN
        given(userRepository.findByEmail(anyString())).willReturn(user);
        given(userRepository.save(user)).willReturn(user);
        doReturn(mimeMessagePreparator).when(underTest).prepareMessage(any(User.class), anyString(), anyString(), anyString());
        doNothing().when(underTest).sendConfirmationEmail(mimeMessagePreparator);
        // WHEN
        underTest.generatePasswordResetToken(DUMMY_EMAIL);
        // THEN
        verify(underTest, times(1)).sendConfirmationEmail(mimeMessagePreparator);

    }

    @Test(expected = NotFoundException.class)
    public void testGenerateResetPasswordTokenWhenNoUserFoundForEmailShouldNotSendEmail() {
        // GIVEN
        given(userRepository.findByEmail(anyString())).willReturn(null);
        // WHEN
        underTest.generatePasswordResetToken(DUMMY_EMAIL);
    }

    @Test
    public void resetPassword() {
        // GIVEN
        given(userRepository.findUserByConfToken(DUMMY_TOKEN)).willReturn(user);
        given(passwordEncoder.encode(anyString())).willReturn(DUMMY_NEW_PASSWORD);
        given(userRepository.save(user)).willReturn(user);
        // WHEN
        underTest.resetPassword(DUMMY_TOKEN, Base64Coder.encodeString(DUMMY_NEW_PASSWORD));
        // THEN
        verify(userRepository, times(1)).save(user);

    }

    @Test(expected = NotFoundException.class)
    public void resetPasswordWhenUserNotFound() {
        // GIVEN
        given(userRepository.findUserByConfToken(DUMMY_TOKEN)).willReturn(null);
        // WHEN
        underTest.resetPassword(DUMMY_TOKEN, Base64Coder.encodeString(DUMMY_NEW_PASSWORD));
    }

    private User createUser() {
        User user = new User();
        user.setEmail(DUMMY_EMAIL);
        user.setPassword(DUMMY_PASSWORD);
        user.setConfToken(DUMMY_TOKEN);
        return user;
    }
}
