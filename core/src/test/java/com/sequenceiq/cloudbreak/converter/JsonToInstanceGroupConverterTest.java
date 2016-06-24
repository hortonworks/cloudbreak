package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

public class JsonToInstanceGroupConverterTest extends AbstractJsonConverterTest<InstanceGroupJson> {

    @InjectMocks
    private JsonToInstanceGroupConverter underTest;

    @Mock
    private TemplateService templateService;

    @Mock
    private SecurityGroupService securityGroupService;

    @Before
    public void setUp() {
        underTest = new JsonToInstanceGroupConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(templateService.get(anyLong())).willReturn(TestUtil.gcpTemplate(51L));
        given(securityGroupService.get(anyLong())).willReturn(TestUtil.securityGroup(1L));
        // WHEN
        InstanceGroup instanceGroup = underTest.convert(getRequest("stack/instance-group.json"));
        // THEN
        assertAllFieldsNotNull(instanceGroup, Collections.singletonList("stack"));
    }

    @Test(expected = AccessDeniedException.class)
    public void testConvertWhenAccessDenied() {
        // GIVEN
        given(templateService.get(anyLong())).willThrow(new AccessDeniedException("exception"));
        // WHEN
        underTest.convert(getRequest("stack/instance-group.json"));
    }

    @Override
    public Class<InstanceGroupJson> getRequestClass() {
        return InstanceGroupJson.class;
    }
}
