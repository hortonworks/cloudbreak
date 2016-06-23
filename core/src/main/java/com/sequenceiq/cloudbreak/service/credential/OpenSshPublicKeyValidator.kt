package com.sequenceiq.cloudbreak.service.credential

import java.security.PublicKey

import com.sequenceiq.cloudbreak.controller.BadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OpenSshPublicKeyValidator {

    fun validate(publicKey: String) {
        try {
            val load = PublicKeyReaderUtil.loadOpenSsh(publicKey)
        } catch (e: Exception) {
            val errorMessage = String.format("Could not validate publickey certificate [certificate: '%s'], detailed message: %s",
                    publicKey, e.message)
            LOGGER.error(errorMessage, e)
            throw BadRequestException(errorMessage, e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OpenSshPublicKeyValidator::class.java)
    }
}
