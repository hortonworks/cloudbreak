package com.sequenceiq.cloudbreak.service.cluster.flow;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.google.common.io.CharStreams;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class EmailSenderServiceTest {
    private static final String NAME_OF_THE_CLUSTER = "name-of-the-cluster";

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private EmailSenderService emailSenderService = new EmailSenderService();

    private CbUser cbUser;

    @Mock
    private EmailMimeMessagePreparator emailMimeMessagePreparator;

    @Before
    public void before() throws IOException, TemplateException {
        cbUser = new CbUser("sdf", "testuser", "testaccount", new ArrayList<CbUserRole>(), "familyname", "givenName", new Date());
        ReflectionTestUtils.setField(emailSenderService, "msgFrom", "no-reply@sequenceiq.com");
        ReflectionTestUtils.setField(emailSenderService, "freemarkerConfiguration", freemarkerConfiguration());

        ReflectionTestUtils.setField(emailSenderService, "successClusterMailTemplatePath", "templates/launch-cluster-installer-mail-success.ftl");
        ReflectionTestUtils.setField(emailSenderService, "failedClusterMailTemplatePath", "templates/launch-cluster-installer-mail-fail.ftl");

        JavaMailSender mailSender = new JavaMailSenderImpl();
        ((JavaMailSenderImpl) mailSender).setHost("localhost");
        ((JavaMailSenderImpl) mailSender).setPort(3465);
        ((JavaMailSenderImpl) mailSender).setUsername("demouser2");
        ((JavaMailSenderImpl) mailSender).setPassword("demopwd2");

        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", false);
        props.put("mail.smtp.starttls.enable", false);
        props.put("mail.debug", false);

        ((JavaMailSenderImpl) mailSender).setJavaMailProperties(props);

        ReflectionTestUtils.setField(emailSenderService, "mailSender", mailSender);

        EmailMimeMessagePreparator mmp = new EmailMimeMessagePreparator();
        ReflectionTestUtils.setField(mmp, "msgFrom", "marci@test.com");

        ReflectionTestUtils.setField(emailSenderService, "emailMimeMessagePreparator", mmp);

        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class)))
                .thenReturn(cbUser);

    }

//    @Test
//    public void testSendTerminationSuccessEmail() throws IOException {
//        // GIVEN
//        String content = getFileContent("mail/termination-success-email");
//        String subject = String.format("Cloudbreak - %s cluster termination", NAME_OF_THE_CLUSTER);
//        when(emailMimeMessagePreparator.prepareMessage(cbUser, subject, content)).thenReturn(mimeMessagePreparator);
//        // WHEN
//        emailSenderService.sendTerminationSuccessEmail("xxx", "123.123.123.123", NAME_OF_THE_CLUSTER);
//        // THEN
//        verify(emailMimeMessagePreparator, times(1)).prepareMessage(cbUser, subject,
//                getFileContent("mail/termination-success-email"));
//        verify(mailSender, times(1)).send(mimeMessagePreparator);
//    }
//
//    @Test
//    public void testSendTerminationFailureEmail() throws IOException {
//        // GIVEN
//        String content = getFileContent("mail/termination-failure-email");
//        String subject = String.format("Cloudbreak - %s cluster termination", NAME_OF_THE_CLUSTER);
//        when(emailMimeMessagePreparator.prepareMessage(cbUser, subject, content)).thenReturn(mimeMessagePreparator);
//        //WHEN
//        emailSenderService.sendTerminationFailureEmail("xxx", "123.123.123.123", NAME_OF_THE_CLUSTER);
//        //THEN
//        verify(emailMimeMessagePreparator, times(1)).prepareMessage(cbUser, subject,
//                getFileContent("mail/termination-failure-email"));
//        verify(mailSender, times(1)).send(mimeMessagePreparator);
//
//    }
//
//    @Test
//    public void testSendProvisioningFailureEmail() throws IOException {
//        //GIVEN
//        String contenct = getFileContent("mail/provisioning-failure-email");
//        String subject = String.format("Cloudbreak - %s cluster installation", NAME_OF_THE_CLUSTER);
//        when(emailMimeMessagePreparator.prepareMessage(cbUser, subject, contenct)).thenReturn(mimeMessagePreparator);
//        //WHEN
//        emailSenderService.sendProvisioningFailureEmail("xxx", NAME_OF_THE_CLUSTER);
//        //THEN
//        verify(emailMimeMessagePreparator, times(1)).prepareMessage(cbUser, subject,
//                getFileContent("mail/provisioning-failure-email"));
//        verify(mailSender, times(1)).send(mimeMessagePreparator);
//
//    }

    @Test
    public void testSendProvisioningSuccessEmail() throws IOException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.SMTPS);
        greenMail.setUser("demouser", "demopwd");
        greenMail.start();
        //GIVEN
        String contenct = getFileContent("mail/provisioning-success-email");
        String subject = String.format("Cloudbreak - %s cluster installation", NAME_OF_THE_CLUSTER);
        //WHEN
        emailSenderService.sendProvisioningSuccessEmail("test@example.com", "123.123.123.123", "testcluster");


        //THEN
        greenMail.waitForIncomingEmail(5000, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1, messages.length);
        greenMail.stop();

    }

    @Test
    public void testSendProvisioningSuccessEmailSmtp() throws IOException {
        GreenMail greenMail = new GreenMail(new ServerSetup(3465, null, ServerSetup.PROTOCOL_SMTP));
        greenMail.setUser("demouser", "demopwd");
        greenMail.start();
        //GIVEN
        String contenct = getFileContent("mail/provisioning-success-email");
        String subject = String.format("Cloudbreak - %s cluster installation", NAME_OF_THE_CLUSTER);
//        when(emailMimeMessagePreparator.prepareMessage(cbUser, subject, contenct)).thenReturn(mimeMessagePreparator);
        //WHEN
        emailSenderService.sendProvisioningSuccessEmail("xxx@alma.com", "123.123.123.123", "mialofasz");

        greenMail.waitForIncomingEmail(5000, 1);
        Message[] messages = greenMail.getReceivedMessages();

        //THEN
//        verify(emailMimeMessagePreparator, times(1)).prepareMessage(cbUser, subject,
//                getFileContent("mail/provisioning-success-email"));
//        verify(mailSender, times(1)).send(mimeMessagePreparator);

        greenMail.stop();

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