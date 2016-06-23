package com.sequenceiq.cloudbreak.service.messages

import java.util.Locale

import javax.inject.Inject

import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

/**
 * Wraps a message source used by the cloudbreak core.
 * It provides defaults and extends the interface with application specific methods if any
 */
@Component
class CloudbreakMessagesService {

    @Inject
    private val messageSource: MessageSource? = null

    fun getMessage(code: String): String {
        return messageSource!!.getMessage(code, null, Locale.getDefault())
    }

    fun getMessage(code: String, args: List<Any>): String {
        return messageSource!!.getMessage(code, args.toArray(), Locale.getDefault())
    }
}
