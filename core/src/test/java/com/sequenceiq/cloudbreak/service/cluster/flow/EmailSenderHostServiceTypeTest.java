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
import javax.mail.MessagingException;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class EmailSenderHostServiceTypeTest {
    private static final String NAME_OF_THE_CLUSTER = "name-of-the-cluster";
    private GreenMail greenMail;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private EmailSenderService emailSenderService = new EmailSenderService();

    private CbUser cbUser;

    @Mock
    private EmailMimeMessagePreparator emailMimeMessagePreparator;

    @Before
    public void before() throws IOException, TemplateException {
        greenMail = new GreenMail(new ServerSetup(3465, null, ServerSetup.PROTOCOL_SMTP));
        greenMail.setUser("demouser", "demopwd");
        greenMail.start();

        cbUser = new CbUser("sdf", "testuser", "testaccount", new ArrayList<>(), "familyname", "givenName", new Date());
        ReflectionTestUtils.setField(emailSenderService, "msgFrom", "no-reply@sequenceiq.com");
        ReflectionTestUtils.setField(emailSenderService, "freemarkerConfiguration", freemarkerConfiguration());

        ReflectionTestUtils.setField(emailSenderService, "successClusterMailTemplatePath", "templates/cluster-installer-mail-success.ftl");
        ReflectionTestUtils.setField(emailSenderService, "failedClusterMailTemplatePath", "templates/cluster-installer-mail-fail.ftl");

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

    @After
    public void tearDown() {
        greenMail.stop();
    }

    @Test
    public void testSendTerminationSuccessEmail() throws IOException, MessagingException {
        // GIVEN
        String subject = "Your cluster has been terminated";
        // WHEN
        emailSenderService.sendTerminationSuccessEmail("xxx", "xxx", "123.123.123.123", NAME_OF_THE_CLUSTER);
        // THEN
        greenMail.waitForIncomingEmail(5000, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1, messages.length);
        Assert.assertEquals(subject, messages[0].getSubject());
        Assert.assertThat(String.valueOf(messages[0].getContent()), Matchers.containsString("successfully terminated"));
    }

    @Test
    public void testSendTerminationFailureEmail() throws IOException, MessagingException {
        // GIVEN
        String subject = "Cluster termination failed";
        //WHEN
        emailSenderService.sendTerminationFailureEmail("xxx", "xxx", "123.123.123.123", NAME_OF_THE_CLUSTER);
        //THEN
        greenMail.waitForIncomingEmail(5000, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1, messages.length);
        Assert.assertEquals(subject, messages[0].getSubject());
        Assert.assertThat(String.valueOf(messages[0].getContent()), Matchers.containsString("Failed to terminate your cluster"));
    }

    @Test
    public void testSendProvisioningFailureEmail() throws IOException, MessagingException {
        //GIVEN
        String subject = "Cluster install failed";
        //WHEN
        emailSenderService.sendProvisioningFailureEmail("xxx", "xxx", NAME_OF_THE_CLUSTER);
        //THEN
        greenMail.waitForIncomingEmail(5000, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1, messages.length);
        Assert.assertEquals(subject, messages[0].getSubject());
        Assert.assertThat(String.valueOf(messages[0].getContent()), Matchers.containsString("Something went terribly wrong"));
    }

    @Ignore
    @Test
    public void testSendProvisioningSuccessEmailSMTPS() throws IOException, MessagingException {
        //To run this test, please download the greenmail.jks from the following link:
        // https://github.com/greenmail-mail-test/greenmail/blob/master/greenmail-core/src/main/resources/greenmail.jks
        // and put into the /cert/trusted directory, and set the mail.transport.protocol variable to smtps.
        greenMail = new GreenMail(ServerSetupTest.SMTPS);
        greenMail.start();
        //GIVEN
        String content = getFileContent("mail/cluster-installer-mail-success").replaceAll("\\n", "");
        String subject = String.format("%s cluster installation", NAME_OF_THE_CLUSTER);
        //WHEN
        emailSenderService.sendProvisioningSuccessEmail("test@example.com", "xxx", "123.123.123.123", NAME_OF_THE_CLUSTER);

        //THEN
        greenMail.waitForIncomingEmail(5000, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1, messages.length);
        Assert.assertEquals(subject, messages[0].getSubject());
        Assert.assertTrue(String.valueOf(messages[0].getContent()).replaceAll("\\n", "").replaceAll("\\r", "").contains(content));
    }

    @Test
    public void testSendProvisioningSuccessEmailSmtp() throws IOException, MessagingException {
        //GIVEN
        String subject = "Your cluster is ready";
        emailSenderService.sendProvisioningSuccessEmail("xxx@alma.com", "xxx", "123.123.123.123", NAME_OF_THE_CLUSTER);

        //THEN
        greenMail.waitForIncomingEmail(5000, 1);
        Message[] messages = greenMail.getReceivedMessages();

        Assert.assertEquals(1, messages.length);
        Assert.assertEquals("Your cluster '" + NAME_OF_THE_CLUSTER + "' is ready", messages[0].getSubject());
        Assert.assertThat(String.valueOf(messages[0].getContent()), Matchers.containsString("Your cluster '" + NAME_OF_THE_CLUSTER + "' is ready"));
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