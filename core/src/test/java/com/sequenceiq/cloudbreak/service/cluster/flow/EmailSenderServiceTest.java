package com.sequenceiq.cloudbreak.service.cluster.flow;


import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class EmailSenderServiceTest {

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private EmailSenderService emailSenderService = new EmailSenderService();

    @Before
    public void before() throws IOException, TemplateException {
        ReflectionTestUtils.setField(emailSenderService, "msgFrom", "no-reply@sequenceiq.com");
        ReflectionTestUtils.setField(emailSenderService, "freemarkerConfiguration", freemarkerConfiguration());

        ReflectionTestUtils.setField(emailSenderService, "successClusterMailTemplatePath", "templates/launch-cluster-installer-mail-success.ftl");
        ReflectionTestUtils.setField(emailSenderService, "failedClusterMailTemplatePath", "templates/launch-cluster-installer-mail-fail.ftl");

        ReflectionTestUtils.setField(emailSenderService, "mailSender", mailSender());
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class)))
                .thenReturn(new CbUser("sdf", "testuser", "testaccount", new ArrayList<CbUserRole>(), "familyname", "givenName", new Date()));

    }

    public JavaMailSender mailSender() {
        JavaMailSender mailSender = null;
        mailSender = new JavaMailSenderImpl();
        ((JavaMailSenderImpl) mailSender).setHost("smtp.gmail.com");
        ((JavaMailSenderImpl) mailSender).setPort(587);
        ((JavaMailSenderImpl) mailSender).setUsername("xxx");
        ((JavaMailSenderImpl) mailSender).setPassword("xxx");
        ((JavaMailSenderImpl) mailSender).setJavaMailProperties(getJavaMailProperties());
        return mailSender;
    }

    private Properties getJavaMailProperties() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.debug", true);
        return props;
    }

    Configuration freemarkerConfiguration() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    @Test
    public void test1() {
        emailSenderService.sendProvisioningSuccessEmail("rdoktorics@hortonworks.com", "123.123.123.123");
    }

}