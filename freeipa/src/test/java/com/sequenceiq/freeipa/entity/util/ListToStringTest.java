package com.sequenceiq.freeipa.entity.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

public class ListToStringTest {

    private static final List<TestPojo> TESTPOJO_LIST = List.of(
            new TestPojo("string1", 1L, List.of("internal1.1", "internal.2")),
            new TestPojo("string2", 2L, List.of("internal2.1", "internal2.2"))
    );

    private ListPojoToString converter = new ListPojoToString();

    @Test
    void convertRoundTrip() {
        String convertedString = converter.convertToDatabaseColumn(TESTPOJO_LIST);
        List<TestPojo> convertedList = converter.convertToEntityAttribute(convertedString);
        assertEquals(TESTPOJO_LIST, convertedList);
    }

    @Test
    void convertToEntityAttribute() {
    }

    public static class TestPojo {
        private String strField;

        private Long longField;

        private List<String> listField;

        public TestPojo() {
        }

        public TestPojo(String strField, Long longField, List<String> listField) {
            this.strField = strField;
            this.longField = longField;
            this.listField = listField;
        }

        public String getStrField() {
            return strField;
        }

        public void setStrField(String strField) {
            this.strField = strField;
        }

        public Long getLongField() {
            return longField;
        }

        public void setLongField(Long longField) {
            this.longField = longField;
        }

        public List<String> getListField() {
            return listField;
        }

        public void setListField(List<String> listField) {
            this.listField = listField;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestPojo testPojo = (TestPojo) o;

            return Objects.equals(strField, testPojo.strField)
                    && Objects.equals(longField, testPojo.longField)
                    && Objects.equals(listField, testPojo.listField);
        }

        @Override
        public int hashCode() {
            return Objects.hash(strField, longField, listField);
        }
    }

    public static class ListPojoToString extends ListToString<ListToStringTest.TestPojo> {
        private static final TypeReference<List<ListToStringTest.TestPojo>> TYPE_REFERENCE = new TypeReference<>() { };

        @Override
        public TypeReference<List<ListToStringTest.TestPojo>> getTypeReference() {
            return TYPE_REFERENCE;
        }
    }
}