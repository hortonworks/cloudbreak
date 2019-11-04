package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;

public class HostGroupToJsonEntityConverterTest extends AbstractEntityConverterTest<HostGroup> {

    @InjectMocks
    private HostGroupToHostGroupResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new HostGroupToHostGroupResponseConverter();
        MockitoAnnotations.initMocks(this);
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
