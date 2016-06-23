package com.sequenceiq.cloudbreak.service.credential

import java.security.PublicKey

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.Credential

@Component
class RsaPublicKeyValidator {

    fun validate(credential: Credential) {
        try {
            val load = PublicKeyReaderUtil.load(credential.publicKey)
        } catch (e: Exception) {
            val errorMessage = String.format("Could not validate publickey certificate [certificate: '%s'], detailed message: %s",
                    credential.publicKey, e.message)
            LOGGER.error(errorMessage, e)
            throw BadRequestException(errorMessage, e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(RsaPublicKeyValidator::class.java)
    }
}
