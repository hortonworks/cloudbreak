package com.sequenceiq.cloudbreak.service.cluster.flow

import javax.mail.internet.MimeMessage

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.mail.javamail.MimeMessagePreparator
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.CbUser

@Service
class EmailMimeMessagePreparator {

    @Value("${cb.smtp.sender.from:}")
    private val msgFrom: String? = null

    fun prepareMessage(user: CbUser, subject: String, body: String): MimeMessagePreparator {
        return MimeMessagePreparator { mimeMessage ->
            val message = MimeMessageHelper(mimeMessage)
            message.setFrom(msgFrom)
            message.setTo(user.username)
            message.setSubject(subject)
            message.setText(body, true)
        }
    }
}
