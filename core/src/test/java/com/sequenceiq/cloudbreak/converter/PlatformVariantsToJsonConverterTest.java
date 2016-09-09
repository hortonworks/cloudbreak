package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.api.client.util.Maps;
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class PlatformVariantsToJsonConverterTest extends AbstractEntityConverterTest<PlatformVariants> {

    private static final Platform PLATFORM = Platform.platform("PLATFORM");
    private static final Variant VARIANT_1 = Variant.variant("VARIANT1");
    private static final Variant VARIANT_2 = Variant.variant("VARIANT2");

    private PlatformVariantsToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new PlatformVariantsToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        PlatformVariantsJson result = underTest.convert(getSource());
        // THEN
        assertTrue(result.getPlatformToVariants().get(PLATFORM.value()).contains(VARIANT_1.value()));
        assertEquals(VARIANT_2.value(), result.getDefaultVariants().get(PLATFORM.value()));
        assertAllFieldsNotNull(result);
    }

    @Override
    public PlatformVariants createSource() {
        Map<Platform, Collection<Variant>> platformToVariants = Maps.newHashMap();
        platformToVariants.put(PLATFORM, Arrays.asList(VARIANT_1, VARIANT_2));
        Map<Platform, Variant> defaultVariants = Maps.newHashMap();
        defaultVariants.put(PLATFORM, VARIANT_2);
        return new PlatformVariants(platformToVariants, defaultVariants);
    }
}
