package com.sequenceiq.cloudbreak.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupV4RequestToSecurityGroupConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;

public class SecurityGroupRequestToSecurityGroupConverterTest extends AbstractJsonConverterTest<SecurityGroupV4Request> {

    @InjectMocks
    private SecurityGroupV4RequestToSecurityGroupConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new SecurityGroupV4RequestToSecurityGroupConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(Sets.newConcurrentHashSet(Collections.singletonList(new SecurityRule())));
        // WHEN
        SecurityGroup result = underTest.convert(getRequest("security-group/security-group.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("id", "owner", "account", "securityGroupId"));
    }

    @Override
    public Class<SecurityGroupV4Request> getRequestClass() {
        return SecurityGroupV4Request.class;
    }
}
