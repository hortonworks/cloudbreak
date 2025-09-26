package com.sequenceiq.cloudbreak.cloud.model.generic;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.ExternalResourceAttributes;
import com.sequenceiq.cloudbreak.cloud.model.NetworkAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.OutboundType;

class DynamicModelTest {

    private DynamicModel underTest;

    @BeforeEach
    void setUp() {
        underTest = new DynamicModel();
    }

    @Test
    void constructorTestWhenDefault() {
        assertThat(underTest.getParameters()).isEmpty();
    }

    @Test
    void constructorTestWhenInitialEmptyMapAndEmpty() {
        DynamicModel underTest = new DynamicModel(Map.of());

        assertThat(underTest.getParameters()).isEmpty();
    }

    @Test
    void constructorTestWhenInitialEmptyImmutableMapAndMutability() {
        DynamicModel underTest = new DynamicModel(Map.of());
        underTest.putParameter("key", "value");

        assertThat(underTest.getParameters()).containsOnly(entry("key", "value"));
    }

    @Test
    void constructorTestWhenInitialMapAndParameters() {
        DynamicModel underTest = new DynamicModel(Map.ofEntries(entry("key1", "value1"), entry("key2", "value2")));

        assertThat(underTest.getParameters()).containsOnly(entry("key1", "value1"), entry("key2", "value2"));
    }

    @Test
    void constructorTestWhenInitialMapAndParametersAndMutability() {
        DynamicModel underTest = new DynamicModel(Map.ofEntries(entry("key1", "value1"), entry("key2", "value2")));
        underTest.putParameter("key3", "value3");

        assertThat(underTest.getParameters()).containsOnly(entry("key1", "value1"), entry("key2", "value2"), entry("key3", "value3"));
    }

    @Test
    void constructorTestWhenNullInitialMap() {
        DynamicModel underTest = new DynamicModel(null);

        assertThat(underTest.getParameters()).isEmpty();
    }

    @Test
    void getParameterTestWhenKeyAndClassAndSuccess() {
        String value1 = "value1";
        underTest.putParameter("key1", value1);
        Integer value2 = 23;
        underTest.putParameter("key2", value2);
        Object value3 = new Object();
        underTest.putParameter("key3", value3);

        assertThat(underTest.getParameter("key_missing", String.class)).isNull();
        assertThat(underTest.getParameter("key1", String.class)).isSameAs(value1);
        assertThat(underTest.getParameter("key2", Integer.class)).isSameAs(value2);
        assertThat(underTest.getParameter("key3", Object.class)).isSameAs(value3);
    }

    @Test
    void getParameterTestWhenKeyAndClassAndCastFailure() {
        underTest.putParameter("key", "value");

        assertThrows(CloudbreakServiceException.class, () -> underTest.getParameter("key", Integer.class));
    }

    @Test
    void getParameterTestWhenKeyAndClassAndCastResultsInNullObject() {
        ExternalResourceAttributes ea = new ExternalResourceAttributes();
        underTest.putParameter("key", ea);

        NetworkAttributes na = underTest.getParameter("key", NetworkAttributes.class);
        assertNull(na.getCloudPlatform());
        assertNull(na.getNetworkId());
        assertEquals(OutboundType.NOT_DEFINED, na.getOutboundType());
        assertEquals(ExternalResourceAttributes.class, na.getAttributeType());
        assertNull(na.getSubnetId());
    }

    @Test
    void getParameterTestWhenKeyAndNullClass() {
        underTest.putParameter("key", "value");

        assertThrows(NullPointerException.class, () -> underTest.getParameter("key", null));
    }

    @Test
    void getParameterTestWhenClassAndSuccess() {
        underTest.putParameter("java.lang.String", "value");

        assertThat(underTest.getParameter(String.class)).isSameAs("value");
    }

    @Test
    void getParameterTestWhenClassAndCastFailure() {
        underTest.putParameter("java.lang.Integer", "value");

        assertThrows(CloudbreakServiceException.class, () -> underTest.getParameter(Integer.class));
    }

    @Test
    void getParameterTestWhenNullClass() {
        underTest.putParameter("key", "value");

        assertThrows(NullPointerException.class, () -> underTest.getParameter(null));
    }

    @Test
    void getStringParameterTestWhenSuccess() {
        underTest.putParameter("key", "value");

        assertThat(underTest.getStringParameter("key")).isSameAs("value");
    }

    @Test
    void getStringParameterTestWhenCastFailure() {
        underTest.putParameter("key", 12);

        assertEquals("12", underTest.getParameter("key", String.class));
    }

    @Test
    void putParameterTestWhenKeyAndNullValue() {
        underTest.putParameter("key", null);

        assertThat(underTest.getParameter("key", Object.class)).isNull();
    }

    @Test
    void putParameterTestWhenKeyAndValueAndExisting() {
        underTest.putParameter("key", "old_value");
        String newValue = "new_value";
        underTest.putParameter("key", newValue);

        assertThat(underTest.getParameter("key", String.class)).isSameAs(newValue);
    }

    @Test
    void putParameterTestWhenClassAndValue() {
        String value = "value";
        underTest.putParameter(String.class, value);
        underTest.putParameter(Integer.class, null);

        assertThat(underTest.getParameter(String.class)).isSameAs(value);
        assertThat(underTest.getParameter(Integer.class)).isNull();
        assertThat(underTest.getParameters()).containsOnly(entry("java.lang.String", value), new SimpleEntry<>("java.lang.Integer", null));
    }

    @Test
    void putParameterTestWhenClassAndValueAndExisting() {
        underTest.putParameter(String.class, "old_value");
        String newValue = "new_value";
        underTest.putParameter(String.class, newValue);

        assertThat(underTest.getParameter(String.class)).isSameAs(newValue);
    }

    @Test
    void putParameterTestWhenNullClassAndValue() {
        assertThrows(NullPointerException.class, () -> underTest.putParameter((Class<?>) null, "value"));
    }

    @Test
    void getParametersTestWhenContentsCheck() {
        String value1 = "value1";
        underTest.putParameter("key1", value1);
        Integer value2 = 23;
        underTest.putParameter("key2", value2);
        Object value3 = new Object();
        underTest.putParameter("key3", value3);
        underTest.putParameter("key4", null);
        String value5 = "value5";
        underTest.putParameter(String.class, value5);
        Long value6 = 456L;
        underTest.putParameter(Long.class, value6);
        underTest.putParameter(Integer.class, null);

        assertThat(underTest.getParameters()).containsOnly(entry("key1", value1), entry("key2", value2), entry("key3", value3),
                new SimpleEntry<>("key4", null), entry("java.lang.String", value5), entry("java.lang.Long", value6),
                new SimpleEntry<>("java.lang.Integer", null));
    }

    @Test
    void getParametersTestWhenImmutabilityCheck() {
        Map<String, Object> parameters = underTest.getParameters();

        assertThat(parameters).isNotNull();
        assertThat(parameters).isEmpty();
        assertThrows(UnsupportedOperationException.class, () -> parameters.put("key", "value"));
    }

    @Test
    void getParametersTestWhenUpdatesCheck() {
        Map<String, Object> parameters = underTest.getParameters();

        assertThat(parameters).isNotNull();
        assertThat(parameters).isEmpty();

        String value = "value";
        underTest.putParameter("key", value);

        assertThat(parameters).containsOnly(entry("key", value));
        assertThat(underTest.getParameter("key", String.class)).isSameAs(value);
    }

    @Test
    void getParametersTestWhenNewInstanceCheck() {
        Map<String, Object> parameters1 = underTest.getParameters();
        Map<String, Object> parameters2 = underTest.getParameters();

        assertThat(parameters1).isNotNull();
        assertThat(parameters1).isEmpty();
        assertThat(parameters2).isNotNull();
        assertThat(parameters2).isEmpty();
        assertThat(parameters1).isNotSameAs(parameters2);
    }

    @Test
    void hasParameterTest() {
        underTest.putParameter("key", "value");

        assertThat(underTest.hasParameter("key")).isTrue();
        assertThat(underTest.hasParameter("key_missing")).isFalse();
    }

    @Test
    void toStringTest() {
        underTest.putParameter("key1", "value1");
        underTest.putParameter("key2", "value2");
        String result = underTest.toString();

        assertThat(result).contains("key1");
        assertThat(result).contains("value1");
        assertThat(result).contains("key2");
        assertThat(result).contains("value2");
    }

}