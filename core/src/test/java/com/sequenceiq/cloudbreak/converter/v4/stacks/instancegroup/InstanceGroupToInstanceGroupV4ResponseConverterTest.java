package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.common.api.type.InstanceGroupType;

class InstanceGroupToInstanceGroupV4ResponseConverterTest {

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @InjectMocks
    private InstanceGroupToInstanceGroupV4ResponseConverter underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @ParameterizedTest
    @EnumSource(InstanceGroupType.class)
    @DisplayName("When conversion happens the original type of the InstanceGroup must be copied into the result InstanceGroupV4Response")
    void copyInstanceGroupType(InstanceGroupType originalType) {
        InstanceGroup source = new InstanceGroup();
        source.setInstanceGroupType(originalType);

        InstanceGroupV4Response result = underTest.convert(source);

        assertEquals(originalType, result.getType());
    }

}