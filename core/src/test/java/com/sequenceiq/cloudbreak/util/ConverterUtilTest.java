package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.util.ConverterUtilTest.MockConversionService;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ConverterUtil.class, MockConversionService.class })

public class ConverterUtilTest {

    @Inject
    private ConverterUtil underTest;

    @Before
    public void setUp() {
    }

    @Test
    public void convertIterable() {
        Iterable<String> iterable = List.of("1", "2");
        List<Integer> result = underTest.convertAll(iterable, Integer.class);
        List<Integer> expected = List.of(1, 2);
        assertThat(result).hasSameElementsAs(expected);
    }

    @Test
    public void convertColletion() {
        Collection<String> collection = List.of("1", "2");
        List<Integer> result = underTest.convertAll(collection, Integer.class);
        List<Integer> expected = List.of(1, 2);
        assertThat(result).hasSameElementsAs(expected);
    }

    @Test
    public void convertOne() {
        String item = "1";
        int result = underTest.convert(item, Integer.class);
        int expected = 1;
        assertThat(result).isEqualTo(expected);
    }

    @Qualifier("conversionService")
    public static class MockConversionService implements ConversionService {

        @Override
        public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
            return true;
        }

        @Override
        public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
            return true;
        }

        @Override
        public <T> T convert(Object source, Class<T> targetType) {
            return targetType.cast(Integer.parseInt(source.toString()));
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return Integer.parseInt(source.toString());
        }
    }
}