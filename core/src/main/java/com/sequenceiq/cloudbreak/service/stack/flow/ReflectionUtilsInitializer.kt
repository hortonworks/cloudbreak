package com.sequenceiq.cloudbreak.service.stack.flow

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor
import org.springframework.stereotype.Component

@Component
class ReflectionUtilsInitializer {

    @Inject
    private val encryptor: PBEStringCleanablePasswordEncryptor? = null

    @PostConstruct
    fun init() {
        ReflectionUtils.setEncryptor(encryptor)
    }

}
