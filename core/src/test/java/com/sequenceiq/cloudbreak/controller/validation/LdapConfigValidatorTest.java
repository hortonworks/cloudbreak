package com.sequenceiq.cloudbreak.controller.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;

@RunWith(MockitoJUnitRunner.class)
public class LdapConfigValidatorTest {

    @InjectMocks
    private LdapConfigValidator underTest;

    @Before
    public void setUp() {

    }

    @Test(expected = BadRequestException.class)
    public void testInvalidLdapConnection() {
        underTest.validateLdapConnection(TestUtil.ldapConfig());
    }
}
