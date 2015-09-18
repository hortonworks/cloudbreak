package com.sequenceiq.cloudbreak.service.cluster.flow;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.io.CharStreams;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class EmailSenderServiceTest {

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailSenderService emailSenderService = new EmailSenderService();

    private CbUser cbUser;
    @Mock
    private MimeMessagePreparator mimeMessagePreparator;

    @Mock
    private EmailMimeMessagePreparator emailMimeMessagePreparator;

    @Before
    public void before() throws IOException, TemplateException {
        cbUser = new CbUser("sdf", "testuser", "testaccount", new ArrayList<CbUserRole>(), "familyname", "givenName", new Date());
        ReflectionTestUtils.setField(emailSenderService, "msgFrom", "no-reply@sequenceiq.com");
        ReflectionTestUtils.setField(emailSenderService, "freemarkerConfiguration", freemarkerConfiguration());

        ReflectionTestUtils.setField(emailSenderService, "successClusterMailTemplatePath", "templates/launch-cluster-installer-mail-success.ftl");
        ReflectionTestUtils.setField(emailSenderService, "failedClusterMailTemplatePath", "templates/launch-cluster-installer-mail-fail.ftl");

        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class)))
                .thenReturn(cbUser);

    }

    @Test
    public void testSendTerminationSuccessEmail() throws IOException {
        // GIVEN
        String content = getFileContent("mail/termination-success-email");
        when(emailMimeMessagePreparator.prepareMessage(cbUser, "Cloudbreak - cloudbreak cluster termination", content)).thenReturn(mimeMessagePreparator);
        // WHEN
        emailSenderService.sendTerminationSuccessEmail("xxx", "123.123.123.123");
        // THEN
        verify(emailMimeMessagePreparator, times(1)).prepareMessage(cbUser, "Cloudbreak - cloudbreak cluster termination",
                getFileContent("mail/termination-success-email"));
        verify(mailSender, times(1)).send(mimeMessagePreparator);
    }

    @Test
    public void testSendTerminationFailureEmail() throws IOException {
        // GIVEN
        String content = getFileContent("mail/termination-failure-email");
        when(emailMimeMessagePreparator.prepareMessage(cbUser, "Cloudbreak - cloudbreak cluster termination", content)).thenReturn(mimeMessagePreparator);
        //WHEN
        emailSenderService.sendTerminationFailureEmail("xxx", "123.123.123.123");
        //THEN
        verify(emailMimeMessagePreparator, times(1)).prepareMessage(cbUser, "Cloudbreak - cloudbreak cluster termination",
                getFileContent("mail/termination-failure-email"));
        verify(mailSender, times(1)).send(mimeMessagePreparator);

    }

    @Test
    public void testSendProvisioningFailureEmail() throws IOException {
        //GIVEN
        String contenct = getFileContent("mail/provisioning-failure-email");
        when(emailMimeMessagePreparator.prepareMessage(cbUser, "Cloudbreak - cloudbreak cluster installation", contenct)).thenReturn(mimeMessagePreparator);
        //WHEN
        emailSenderService.sendProvisioningFailureEmail("xxx");
        //THEN
        verify(emailMimeMessagePreparator, times(1)).prepareMessage(cbUser, "Cloudbreak - cloudbreak cluster installation",
                getFileContent("mail/provisioning-failure-email"));
        verify(mailSender, times(1)).send(mimeMessagePreparator);

    }

    @Test
    public void testSendProvisioningSuccessEmail() throws IOException {
        //GIVEN
        String contenct = getFileContent("mail/provisioning-success-email");
        when(emailMimeMessagePreparator.prepareMessage(cbUser, "Cloudbreak - cloudbreak cluster installation", contenct)).thenReturn(mimeMessagePreparator);
        //WHEN
        emailSenderService.sendProvisioningSuccessEmail("xxx", "123.123.123.123");
        //THEN
        verify(emailMimeMessagePreparator, times(1)).prepareMessage(cbUser, "Cloudbreak - cloudbreak cluster installation",
                getFileContent("mail/provisioning-success-email"));
        verify(mailSender, times(1)).send(mimeMessagePreparator);

    }

    Configuration freemarkerConfiguration() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    private String getFileContent(String path) throws IOException {
        return CharStreams.toString(new InputStreamReader(new ClassPathResource(path).getInputStream()));
    }

}