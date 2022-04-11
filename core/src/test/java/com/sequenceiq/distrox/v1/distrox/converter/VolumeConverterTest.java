package com.sequenceiq.distrox.v1.distrox.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VolumeConverterTest {

    private VolumeConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new VolumeConverter();
    }

    @Test
    public void testV4toV1converterWithoutCount() {
        VolumeV1Request source = createVolumeV1request();
        assertEquals(0, underTest.convert(source).getCount());
    }

    @Test
    public void testV4toV1converterWithCount() {
        VolumeV1Request source = createVolumeV1request();
        source.setCount(1);
        assertEquals(1, underTest.convert(source).getCount());
    }

    private VolumeV1Request createVolumeV1request() {
        VolumeV1Request source = new VolumeV1Request();
        source.setSize(100);
        source.setType("HDD");
        return source;
    }

}
