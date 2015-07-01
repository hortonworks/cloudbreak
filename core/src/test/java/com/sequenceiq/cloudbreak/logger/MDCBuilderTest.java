package com.sequenceiq.cloudbreak.logger;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.Stack;

public class MDCBuilderTest {

    @Test
    public void buildSimpleContext() {
        MDCBuilder.buildMdcContext(null);
        assertEquals("cloudbreak", MDC.get(LoggerContextKey.OWNER_ID.toString()));
        assertEquals("cloudbreakLog", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
        assertEquals("undefined", MDC.get(LoggerContextKey.RESOURCE_ID.toString()));
        assertEquals("cb", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
    }

    @Test
    public void buildSimpleContextWithNull() {
        MDCBuilder.buildMdcContext();
        assertEquals("cloudbreak", MDC.get(LoggerContextKey.OWNER_ID.toString()));
        assertEquals("cloudbreakLog", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
        assertEquals("undefined", MDC.get(LoggerContextKey.RESOURCE_ID.toString()));
        assertEquals("cb", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
    }

    @Test
    public void buildContextWithStack() {
        MDCBuilder.buildMdcContext(TestUtil.stack());
        assertEquals("userid", MDC.get(LoggerContextKey.OWNER_ID.toString()));
        assertEquals("STACK", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
        assertEquals("1", MDC.get(LoggerContextKey.RESOURCE_ID.toString()));
        assertEquals("simplestack", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
    }

    @Test
    public void buildContextWithStackAndUndefinedValues() {
        Stack stack = TestUtil.stack();
        stack.setOwner(null);
        MDCBuilder.buildMdcContext(stack);
        assertEquals("undefined", MDC.get(LoggerContextKey.OWNER_ID.toString()));
        assertEquals("STACK", MDC.get(LoggerContextKey.RESOURCE_TYPE.toString()));
        assertEquals("1", MDC.get(LoggerContextKey.RESOURCE_ID.toString()));
        assertEquals("simplestack", MDC.get(LoggerContextKey.RESOURCE_NAME.toString()));
    }

    @Test
    public void buildUserMdcContextWithUser() {
        MDCBuilder.buildUserMdcContext(TestUtil.cbUser());
        assertEquals("userid", MDC.get(LoggerContextKey.OWNER_ID.toString()));
    }

}