package com.sequenceiq.cloudbreak.controller.validation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;

@RunWith(MockitoJUnitRunner.class)
public class LdapConfigValidatorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private LdapConfigValidator underTest;

    @Before
    public void setUp() {

    }

    @Test
    public void testInvalidLdapConnection() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid name: /localhost:389");
        underTest.validateLdapConnection(TestUtil.ldapConfig());
    }
}
