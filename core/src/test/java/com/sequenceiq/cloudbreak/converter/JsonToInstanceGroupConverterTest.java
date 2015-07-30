package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.InstanceGroupJson;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

public class JsonToInstanceGroupConverterTest extends AbstractJsonConverterTest<InstanceGroupJson> {

    @InjectMocks
    private JsonToInstanceGroupConverter underTest;

    @Mock
    private TemplateRepository templateRepository;

    @Before
    public void setUp() {
        underTest = new JsonToInstanceGroupConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(templateRepository.findOne(anyLong())).willReturn(TestUtil.gcpTemplate(51L));
        // WHEN
        InstanceGroup instanceGroup = underTest.convert(getRequest("stack/instance-group.json"));
        // THEN
        assertAllFieldsNotNull(instanceGroup, Arrays.asList("stack"));
    }

    @Test(expected = AccessDeniedException.class)
    public void testConvertWhenAccessDenied() {
        // GIVEN
        given(templateRepository.findOne(anyLong())).willThrow(new AccessDeniedException("exception"));
        // WHEN
        underTest.convert(getRequest("stack/instance-group.json"));
    }

    @Override
    public Class<InstanceGroupJson> getRequestClass() {
        return InstanceGroupJson.class;
    }
}
