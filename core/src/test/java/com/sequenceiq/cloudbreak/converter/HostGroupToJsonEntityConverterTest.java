package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Recipe;

public class HostGroupToJsonEntityConverterTest extends AbstractEntityConverterTest<HostGroup> {

    private HostGroupToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new HostGroupToJsonConverter();
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
        getSource().setRecipes(new HashSet<Recipe>());
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
