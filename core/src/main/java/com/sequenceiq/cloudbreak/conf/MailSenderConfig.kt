package com.sequenceiq.cloudbreak.conf

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Properties

import javax.mail.internet.MimeMessage

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessagePreparator
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean
import org.springframework.util.StringUtils

import freemarker.template.TemplateException

@Configuration
class MailSenderConfig {
    @Value("${cb.smtp.sender.host:}")
    private val host: String? = null

    @Value("${cb.smtp.sender.port:}")
    private val port: Int = 0

    @Value("${cb.smtp.sender.username:}")
    private val userName: String? = null

    @Value("${cb.smtp.sender.password:}")
    private val password: String? = null

    @Value("${cb.smtp.sender.from:}")
    private val msgFrom: String? = null

    @Value("${cb.mail.smtp.auth:}")
    private val smtpAuth: String? = null

    @Value("${cb.mail.smtp.starttls.enable:}")
    private val smtpStarttlsEnable: String? = null

    @Value("${cb.mail.smtp.type:}")
    private val smtpType: String? = null

    @Bean
    fun mailSender(): JavaMailSender {
        var mailSender: JavaMailSender? = null
        if (isMailSendingConfigured) {
            mailSender = JavaMailSenderImpl()
            mailSender.host = host
            mailSender.port = port
            if (!StringUtils.isEmpty(userName)) {
                mailSender.username = userName
            }
            if (!StringUtils.isEmpty(password)) {
                mailSender.password = password
            }
            mailSender.javaMailProperties = javaMailProperties
        } else {
            mailSender = DummyEmailSender()
        }

        return mailSender
    }

    private // some SMTP servers don't need username/password
    val isMailSendingConfigured: Boolean
        get() = !StringUtils.isEmpty(host) && !StringUtils.isEmpty(msgFrom)

    private fun missingVars(): String {
        val missingVars = ArrayList()
        if (StringUtils.isEmpty(host)) {
            missingVars.add("cb.smtp.sender.host")
        }
        if (StringUtils.isEmpty(userName)) {
            missingVars.add("cb.smtp.sender.username")
        }
        if (StringUtils.isEmpty(password)) {
            missingVars.add("cb.smtp.sender.password")
        }
        if (StringUtils.isEmpty(msgFrom)) {
            missingVars.add("cb.smtp.sender.from")
        }
        return StringUtils.collectionToDelimitedString(missingVars, ",", "[", "]")
    }

    private val javaMailProperties: Properties
        get() {
            val props = Properties()
            props.put("mail.transport.protocol", smtpType)
            props.put("mail.smtp.auth", smtpAuth)
            props.put("mail.smtp.starttls.enable", smtpStarttlsEnable)
            props.put("mail.debug", true)
            return props
        }

    @Bean
    @Throws(IOException::class, TemplateException::class)
    fun freemarkerConfiguration(): freemarker.template.Configuration {
        val factoryBean = FreeMarkerConfigurationFactoryBean()
        factoryBean.setPreferFileSystemAccess(false)
        factoryBean.setTemplateLoaderPath("classpath:/")
        factoryBean.afterPropertiesSet()
        return factoryBean.`object`
    }

    private inner class DummyEmailSender : JavaMailSender {
        private val logger = LoggerFactory.getLogger(DummyEmailSender::class.java)
        private val msg = "SMTP not configured! Related configuration entries: " + missingVars()

        override fun createMimeMessage(): MimeMessage? {
            return null
        }

        @Throws(MailException::class)
        override fun createMimeMessage(contentStream: InputStream): MimeMessage? {
            return null
        }

        @Throws(MailException::class)
        override fun send(mimeMessage: MimeMessage) {
            logger.info(msg)
        }

        @Throws(MailException::class)
        override fun send(mimeMessages: Array<MimeMessage>) {
            logger.info(msg)
        }

        @Throws(MailException::class)
        override fun send(mimeMessagePreparator: MimeMessagePreparator) {
            logger.info(msg)
        }

        @Throws(MailException::class)
        override fun send(mimeMessagePreparators: Array<MimeMessagePreparator>) {
            logger.info(msg)
        }

        @Throws(MailException::class)
        override fun send(simpleMessage: SimpleMailMessage) {
            logger.info(msg)
        }

        @Throws(MailException::class)
        override fun send(simpleMessages: Array<SimpleMailMessage>) {
            logger.info(msg)
        }
    }

}
