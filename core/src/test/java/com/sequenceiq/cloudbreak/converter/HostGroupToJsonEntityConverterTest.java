package com.sequenceiq.cloudbreak.converter;

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
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.domain.HostGroup;

public class HostGroupToJsonEntityConverterTest extends AbstractEntityConverterTest<HostGroup> {

    @InjectMocks
    private HostGroupToJsonConverter underTest;
    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        underTest = new HostGroupToJsonConverter();
        MockitoAnnotations.initMocks(this);
        when(conversionService.convert(any(Class.class), any(Class.class))).thenReturn(new ConstraintJson());
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        HostGroupJson result = underTest.convert(getSource());
        // THEN
        assertEquals(1, result.getMetadata().size());
        assertTrue(result.getRecipeIds().contains(1L));
        assertEquals("dummyName", result.getName());
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutRecipes() {
        // GIVEN
        getSource().setRecipes(new HashSet<>());
        // WHEN
        HostGroupJson result = underTest.convert(getSource());
        // THEN
        assertEquals(1, result.getMetadata().size());
        assertFalse(result.getRecipeIds().contains(1L));
        assertEquals("dummyName", result.getName());
        assertAllFieldsNotNull(result);
    }

    @Override
    public HostGroup createSource() {
        return TestUtil.hostGroup();
    }
}
