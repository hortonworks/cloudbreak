package com.sequenceiq.distrox.api.v1.distrox.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.volume.RootVolumeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.validation.volume.RootVolumeSizeProvider;

@ExtendWith(MockitoExtension.class)
public class RootVolumeSizeValidatorTest {

    @InjectMocks
    private RootVolumeSizeValidator underTest;

    @Mock
    private RootVolumeSizeProvider rootVolumeSizeProvider;

    @BeforeEach
    public void setup() {
        underTest.setRootVolumeSizeProvider(Optional.of(rootVolumeSizeProvider));
    }

    @ParameterizedTest
    @MethodSource("isValidSource")
    public void testIsValid(boolean valid, int size) {
        InstanceTemplateV1Request templateV1Request = new InstanceTemplateV1Request();
        templateV1Request.setAws(new AwsInstanceTemplateV1Parameters());
        RootVolumeV1Request rootVolume = new RootVolumeV1Request();
        rootVolume.setSize(size);
        templateV1Request.setRootVolume(rootVolume);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder builder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        when(rootVolumeSizeProvider.getForPlatform("AWS")).thenReturn(10);
        if (!valid) {
            when(context.buildConstraintViolationWithTemplate("Group root volume (" + size + "GB) couldn't be less than 10GB")).thenReturn(builder);
        }
        InstanceGroupV1Request value = new InstanceGroupV1Request();
        value.setName("group");
        value.setTemplate(templateV1Request);
        boolean actual = underTest.isValid(value, context);

        Assertions.assertEquals(actual, valid);
        int times = valid ? 0 : 1;
        verify(context, times(times)).buildConstraintViolationWithTemplate("Group root volume (" + size + "GB) couldn't be less than 10GB");
    }

    static Object[][] isValidSource() {
        return new Object[][]{
                {false, 1},
                {false, 9},
                {true, 10},
                {true, 11}
        };
    }
}
