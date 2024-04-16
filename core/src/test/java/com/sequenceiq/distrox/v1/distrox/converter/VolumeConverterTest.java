package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.VolumeV1Request;

@ExtendWith(MockitoExtension.class)
public class VolumeConverterTest {

    public static final int DEFAULT_ROOT_DISK_SIZE = 200;

    @Mock
    private DefaultRootVolumeSizeProvider rootVolumeSizeProvider;

    @InjectMocks
    private VolumeConverter underTest;

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

    @Test
    public void testRootVolumeSizeConvert() {
        when(rootVolumeSizeProvider.getForPlatform("AWS")).thenReturn(DEFAULT_ROOT_DISK_SIZE);
        RootVolumeV1Request smallRootVolumeSizeRequest = new RootVolumeV1Request();
        smallRootVolumeSizeRequest.setSize(50);
        RootVolumeV4Request convertedRootVolumeV4Request = underTest.convert(smallRootVolumeSizeRequest, "AWS");
        assertEquals(DEFAULT_ROOT_DISK_SIZE, convertedRootVolumeV4Request.getSize());

        RootVolumeV4Request smallRootVolumeV4Request = new RootVolumeV4Request();
        smallRootVolumeV4Request.setSize(50);
        RootVolumeV1Request convertedRootVolumeV1Request = underTest.convert(smallRootVolumeV4Request, "AWS");
        assertEquals(DEFAULT_ROOT_DISK_SIZE, convertedRootVolumeV1Request.getSize());

        RootVolumeV4Request nullRootVolumeV4Request = new RootVolumeV4Request();
        RootVolumeV1Request convertedNullRootVolumeV1Request = underTest.convert(nullRootVolumeV4Request, "AWS");
        assertEquals(DEFAULT_ROOT_DISK_SIZE, convertedNullRootVolumeV1Request.getSize());

        when(rootVolumeSizeProvider.getForPlatform("AWS")).thenReturn(DEFAULT_ROOT_DISK_SIZE);
        RootVolumeV1Request goodRootVolumeV1Request = new RootVolumeV1Request();
        goodRootVolumeV1Request.setSize(300);
        convertedRootVolumeV4Request = underTest.convert(goodRootVolumeV1Request, "AWS");
        assertEquals(300, convertedRootVolumeV4Request.getSize());

        RootVolumeV4Request goodRootVolumeV4Request = new RootVolumeV4Request();
        goodRootVolumeV4Request.setSize(300);
        convertedRootVolumeV1Request = underTest.convert(goodRootVolumeV4Request, "AWS");
        assertEquals(300, convertedRootVolumeV1Request.getSize());
    }

    private VolumeV1Request createVolumeV1request() {
        VolumeV1Request source = new VolumeV1Request();
        source.setSize(100);
        source.setType("HDD");
        return source;
    }

}
