package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class DefaultEnumConverterTest {

    private static final String FIELD_FUTURE = "FIELD_FUTURE";

    private final DefaultEnumConverterTestImpl underTest = new DefaultEnumConverterTestImpl();

    @Test
    void testWhenConvertExistingFieldThenSuccess() {
        TestEnumV1 converted = underTest.convertToEntityAttribute(TestEnumV1.FIELD_FIRST.name());

        assertEquals(TestEnumV1.FIELD_FIRST, converted);
    }

    @Test
    void testWhenConvertFutureFieldThenTryConvertUnknownFieldSucceeds() {
        TestEnumV1 converted = underTest.convertToEntityAttribute(FIELD_FUTURE);

        assertEquals(TestEnumV1.FIELD_MIDDLE, converted);
    }

    @Test
    void testWhenConvertUnknownFieldThenFallbackToDefault() {
        TestEnumV1 converted = underTest.convertToEntityAttribute("FIELD_NOT_EXISTING");

        assertEquals(TestEnumV1.FIELD_LAST, converted);
    }

    private enum TestEnumV1 {
        FIELD_FIRST,
        FIELD_MIDDLE,
        FIELD_LAST
    }

    private static class DefaultEnumConverterTestImpl extends DefaultEnumConverter<TestEnumV1> {

        @Override
        public TestEnumV1 getDefault() {
            return TestEnumV1.FIELD_LAST;
        }

        @Override
        protected Optional<TestEnumV1> tryConvertUnknownField(String attribute) {
            if (FIELD_FUTURE.equals(attribute)) {
                return Optional.of(TestEnumV1.FIELD_MIDDLE);
            }
            return Optional.empty();
        }
    }
}
