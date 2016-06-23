package com.sequenceiq.cloudbreak.service.cluster.flow


import org.mockito.Matchers.any
import org.mockito.Matchers.anyString
import org.mockito.Mockito.`when`

import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Date
import java.util.Properties

import javax.mail.Message
import javax.mail.MessagingException

import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean

import com.google.common.io.CharStreams
import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import com.icegreen.greenmail.util.ServerSetupTest
import com.sequenceiq.cloudbreak.common.type.CbUserRole
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.service.user.UserDetailsService
import com.sequenceiq.cloudbreak.service.user.UserFilterField

import freemarker.template.Configuration
import freemarker.template.TemplateException

@RunWith(MockitoJUnitRunner::class)
class EmailSenderHostServiceTypeTest {
    private var greenMail: GreenMail? = null

    @Mock
    private val userDetailsService: UserDetailsService? = null

    @InjectMocks
    private val emailSenderService = EmailSenderService()

    private var cbUser: CbUser? = null

    @Mock
    private val emailMimeMessagePreparator: EmailMimeMessagePreparator? = null

    @Before
    @Throws(IOException::class, TemplateException::class)
    fun before() {
        greenMail = GreenMail(ServerSetup(3465, null, ServerSetup.PROTOCOL_SMTP))
        greenMail!!.setUser("demouser", "demopwd")
        greenMail!!.start()

        cbUser = CbUser("sdf", "testuser", "testaccount", ArrayList<CbUserRole>(), "familyname", "givenName", Date())
        ReflectionTestUtils.setField(emailSenderService, "msgFrom", "no-reply@sequenceiq.com")
        ReflectionTestUtils.setField(emailSenderService, "freemarkerConfiguration", freemarkerConfiguration())

        ReflectionTestUtils.setField(emailSenderService, "successClusterMailTemplatePath", "templates/launch-cluster-installer-mail-success.ftl")
        ReflectionTestUtils.setField(emailSenderService, "failedClusterMailTemplatePath", "templates/launch-cluster-installer-mail-fail.ftl")

        val mailSender = JavaMailSenderImpl()
        mailSender.host = "localhost"
        mailSender.port = 3465
        mailSender.username = "demouser2"
        mailSender.password = "demopwd2"

        val props = Properties()
        props.put("mail.transport.protocol", "smtp")
        props.put("mail.smtp.auth", false)
        props.put("mail.smtp.starttls.enable", false)
        props.put("mail.debug", false)

        mailSender.javaMailProperties = props

        ReflectionTestUtils.setField(emailSenderService, "mailSender", mailSender)

        val mmp = EmailMimeMessagePreparator()
        ReflectionTestUtils.setField(mmp, "msgFrom", "marci@test.com")

        ReflectionTestUtils.setField(emailSenderService, "emailMimeMessagePreparator", mmp)

        `when`(userDetailsService!!.getDetails(anyString(), any<UserFilterField>(UserFilterField::class.java))).thenReturn(cbUser)

    }

    @After
    fun tearDown() {
        greenMail!!.stop()
    }

    @Test
    @Throws(IOException::class, MessagingException::class)
    fun testSendTerminationSuccessEmail() {
        // GIVEN
        val content = getFileContent("mail/termination-success-email").replace("\\n".toRegex(), "")
        val subject = String.format("Cloudbreak - %s cluster termination", NAME_OF_THE_CLUSTER)
        // WHEN
        emailSenderService.sendTerminationSuccessEmail("xxx", "123.123.123.123", NAME_OF_THE_CLUSTER)
        // THEN
        greenMail!!.waitForIncomingEmail(5000, 1)
        val messages = greenMail!!.receivedMessages

        Assert.assertEquals(1, messages.size.toLong())
        Assert.assertEquals(subject, messages[0].subject)
        Assert.assertTrue(messages[0].content.toString().replace("\\n".toRegex(), "").replace("\\r".toRegex(), "").contains(content))

    }

    @Test
    @Throws(IOException::class, MessagingException::class)
    fun testSendTerminationFailureEmail() {
        // GIVEN
        val content = getFileContent("mail/termination-failure-email").replace("\\n".toRegex(), "")
        val subject = String.format("Cloudbreak - %s cluster termination", NAME_OF_THE_CLUSTER)
        //WHEN
        emailSenderService.sendTerminationFailureEmail("xxx", "123.123.123.123", NAME_OF_THE_CLUSTER)
        //THEN
        greenMail!!.waitForIncomingEmail(5000, 1)
        val messages = greenMail!!.receivedMessages

        Assert.assertEquals(1, messages.size.toLong())
        Assert.assertEquals(subject, messages[0].subject)
        Assert.assertTrue(messages[0].content.toString().replace("\\n".toRegex(), "").replace("\\r".toRegex(), "").contains(content))

    }

    @Test
    @Throws(IOException::class, MessagingException::class)
    fun testSendProvisioningFailureEmail() {
        //GIVEN
        val content = getFileContent("mail/provisioning-failure-email").replace("\\n".toRegex(), "")
        val subject = String.format("Cloudbreak - %s cluster installation", NAME_OF_THE_CLUSTER)
        //WHEN
        emailSenderService.sendProvisioningFailureEmail("xxx", NAME_OF_THE_CLUSTER)
        //THEN
        greenMail!!.waitForIncomingEmail(5000, 1)
        val messages = greenMail!!.receivedMessages

        Assert.assertEquals(1, messages.size.toLong())
        Assert.assertEquals(subject, messages[0].subject)
        Assert.assertTrue(messages[0].content.toString().replace("\\n".toRegex(), "").replace("\\r".toRegex(), "").contains(content))
    }

    @Ignore
    @Test
    @Throws(IOException::class, MessagingException::class)
    fun testSendProvisioningSuccessEmailSMTPS() {
        //To run this test, please download the greenmail.jks from the following link:
        // https://github.com/greenmail-mail-test/greenmail/blob/master/greenmail-core/src/main/resources/greenmail.jks
        // and put into the /cert/trusted directory, and set the mail.transport.protocol variable to smtps.
        greenMail = GreenMail(ServerSetupTest.SMTPS)
        greenMail!!.start()
        //GIVEN
        val content = getFileContent("mail/provisioning-success-email").replace("\\n".toRegex(), "")
        val subject = String.format("Cloudbreak - %s cluster installation", NAME_OF_THE_CLUSTER)
        //WHEN
        emailSenderService.sendProvisioningSuccessEmail("test@example.com", "123.123.123.123", NAME_OF_THE_CLUSTER)

        //THEN
        greenMail!!.waitForIncomingEmail(5000, 1)
        val messages = greenMail!!.receivedMessages

        Assert.assertEquals(1, messages.size.toLong())
        Assert.assertEquals(subject, messages[0].subject)
        Assert.assertTrue(messages[0].content.toString().replace("\\n".toRegex(), "").replace("\\r".toRegex(), "").contains(content))
    }

    @Test
    @Throws(IOException::class, MessagingException::class)
    fun testSendProvisioningSuccessEmailSmtp() {
        //GIVEN
        val content = getFileContent("mail/provisioning-success-email").replace("\\n".toRegex(), "")
        val subject = String.format("Cloudbreak - %s cluster installation", NAME_OF_THE_CLUSTER)
        emailSenderService.sendProvisioningSuccessEmail("xxx@alma.com", "123.123.123.123", NAME_OF_THE_CLUSTER)

        //THEN
        greenMail!!.waitForIncomingEmail(5000, 1)
        val messages = greenMail!!.receivedMessages

        Assert.assertEquals(1, messages.size.toLong())
        Assert.assertEquals(subject, messages[0].subject)
        Assert.assertTrue(messages[0].content.toString().replace("\\n".toRegex(), "").replace("\\r".toRegex(), "").contains(content))

    }

    @Throws(IOException::class, TemplateException::class)
    internal fun freemarkerConfiguration(): Configuration {
        val factoryBean = FreeMarkerConfigurationFactoryBean()
        factoryBean.setPreferFileSystemAccess(false)
        factoryBean.setTemplateLoaderPath("classpath:/")
        factoryBean.afterPropertiesSet()
        return factoryBean.`object`
    }

    @Throws(IOException::class)
    private fun getFileContent(path: String): String {
        return CharStreams.toString(InputStreamReader(ClassPathResource(path).inputStream))
    }

    companion object {
        private val NAME_OF_THE_CLUSTER = "name-of-the-cluster"
    }

}