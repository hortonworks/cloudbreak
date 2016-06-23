package com.sequenceiq.cloudbreak.logger

import org.junit.Assert.assertEquals

import org.junit.Test
import org.slf4j.MDC

import com.sequenceiq.cloudbreak.TestUtil
import com.sequenceiq.cloudbreak.domain.Stack

class MDCBuilderTest {

    @Test
    fun buildSimpleContext() {
        MDCBuilder.buildMdcContext(null)
        assertEquals("cloudbreak", MDC.get(LoggerContextKey.OWNER_ID.toString()))
        assertEquals("cloudbreakLog", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()))
        assertEquals("undefined", MDC.get(LoggerContextKey.RESOURCE_ID.toString()))
        assertEquals("cb", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()))
    }

    @Test
    fun buildSimpleContextWithNull() {
        MDCBuilder.buildMdcContext()
        assertEquals("cloudbreak", MDC.get(LoggerContextKey.OWNER_ID.toString()))
        assertEquals("cloudbreakLog", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()))
        assertEquals("undefined", MDC.get(LoggerContextKey.RESOURCE_ID.toString()))
        assertEquals("cb", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()))
    }

    @Test
    fun buildContextWithStack() {
        MDCBuilder.buildMdcContext(TestUtil.stack())
        assertEquals("userid", MDC.get(LoggerContextKey.OWNER_ID.toString()))
        assertEquals("STACK", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()))
        assertEquals("1", MDC.get(LoggerContextKey.RESOURCE_ID.toString()))
        assertEquals("simplestack", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()))
    }

    @Test
    fun buildContextWithStackAndUndefinedValues() {
        val stack = TestUtil.stack()
        stack.owner = null
        MDCBuilder.buildMdcContext(stack)
        assertEquals("undefined", MDC.get(LoggerContextKey.OWNER_ID.toString()))
        assertEquals("STACK", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()))
        assertEquals("1", MDC.get(LoggerContextKey.RESOURCE_ID.toString()))
        assertEquals("simplestack", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()))
    }

    @Test
    fun buildUserMdcContextWithUser() {
        MDCBuilder.buildUserMdcContext(TestUtil.cbAdminUser())
        assertEquals("userid", MDC.get(LoggerContextKey.OWNER_ID.toString()))
    }

}