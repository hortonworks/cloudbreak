package com.sequenceiq.cloudbreak.controller.validation;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;

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
        LdapConfig ldapConfig = TestUtil.ldapConfig();
        ldapConfig.setProtocol("ldap://");
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Invalid name: /localhost:389");
        underTest.validateLdapConnection(ldapConfig);
    }
}
