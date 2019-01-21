package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupConstraintV4Request;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.updates.HostGroupToHostGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;

public class HostGroupToJsonEntityConverterTest extends AbstractEntityConverterTest<HostGroup> {

    @InjectMocks
    private HostGroupToHostGroupV4ResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new HostGroupToHostGroupV4ResponseConverter();
        MockitoAnnotations.initMocks(this);
        when(conversionService.convert(any(Constraint.class), any())).thenReturn(new HostGroupConstraintV4Request());
        when(conversionService.convert(any(HostMetadata.class), any())).thenReturn(new HostMetadataResponse());
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        HostGroupResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, result.getMetadata().size());
        assertTrue(result.getRecipeIds().contains(1L));
        assertEquals("dummyName", result.getName());
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutRecipes() {
        // GIVEN
        getSource().setRecipes(new HashSet<>());
        // WHEN
        HostGroupResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, result.getMetadata().size());
        assertFalse(result.getRecipeIds().contains(1L));
        assertEquals("dummyName", result.getName());
        assertAllFieldsNotNull(result);
    }

    @Override
    public HostGroup createSource() {
        return TestUtil.hostGroup();
    }
}
