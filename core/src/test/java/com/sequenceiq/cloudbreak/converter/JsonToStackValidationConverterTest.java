package com.sequenceiq.cloudbreak.converter;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.StackValidationRequest;
import com.sequenceiq.cloudbreak.domain.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroupType;
import com.sequenceiq.cloudbreak.domain.StackValidation;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.NetworkRepository;

public class JsonToStackValidationConverterTest extends AbstractJsonConverterTest<StackValidationRequest> {

    @InjectMocks
    private JsonToStackValidationConverter underTest;

    @Mock
    private BlueprintRepository blueprintRepository;
    @Mock
    private NetworkRepository networkRepository;
    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new JsonToStackValidationConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        Set<HostGroup> hostGroups = new HashSet<>();
        instanceGroups.add(TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.awsTemplate(1L)));
        hostGroups.add(TestUtil.hostGroup());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(instanceGroups);
        given(networkRepository.findOne(1L)).willReturn(new AwsNetwork());
        given(blueprintRepository.findOne(2L)).willReturn(new Blueprint());
        // WHEN
        StackValidation result = underTest.convert(getRequest("stack/stack-validation-request.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<StackValidationRequest> getRequestClass() {
        return StackValidationRequest.class;
    }

}
