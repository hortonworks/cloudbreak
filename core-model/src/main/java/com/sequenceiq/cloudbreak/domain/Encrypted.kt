package com.sequenceiq.cloudbreak.domain

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Indicates that a String field is encrypted, thus it needs to be decrypted before use.
 * The decryption happens on the converter side.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Encrypted