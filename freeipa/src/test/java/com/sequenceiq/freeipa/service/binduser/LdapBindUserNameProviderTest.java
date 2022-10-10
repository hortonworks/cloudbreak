package com.sequenceiq.freeipa.service.binduser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LdapBindUserNameProviderTest {

    private LdapBindUserNameProvider underTest = new LdapBindUserNameProvider();

    @Test
    public void testCreateBindUserName() {
        String result = underTest.createBindUserName("asdf");

        assertEquals("ldapbind-asdf", result);
    }
}