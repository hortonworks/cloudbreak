package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.service.CloudbreakException;

class ConversionStatusTest {

    @Test
    void constructorShouldInitializeEmptyMutableCollections() {
        ConversionStatus underTest = new ConversionStatus();

        assertThat(underTest.cloudbreakExceptions()).isNotNull().isEmpty();
        assertThat(underTest.successes()).isNotNull().isEmpty();

        underTest.cloudbreakExceptions().put("key", new CloudbreakException("err"));
        underTest.successes().add("ok");

        assertThat(underTest.cloudbreakExceptions()).hasSize(1);
        assertThat(underTest.successes()).containsExactly("ok");
    }

    @Test
    void constructorShouldRetainGivenCollectionReferences() {
        Map<String, CloudbreakException> exceptions = new HashMap<>();
        List<String> successes = List.of("a", "b");
        ConversionStatus underTest = new ConversionStatus(exceptions, successes);

        assertThat(underTest.cloudbreakExceptions()).isSameAs(exceptions);
        assertThat(underTest.successes()).isSameAs(successes);
    }

    @Test
    void builderShouldProduceEmptyCollectionsByDefault() {
        ConversionStatus underTest = ConversionStatus.builder().build();

        assertThat(underTest.cloudbreakExceptions()).isNotNull().isEmpty();
        assertThat(underTest.successes()).isNotNull().isEmpty();
    }

    @Test
    void builderShouldUseProvidedCollections() {
        Map<String, CloudbreakException> exceptions = Map.of("ebs-1", new CloudbreakException("failed"));
        List<String> successes = List.of("vol-1");

        ConversionStatus underTest = ConversionStatus.builder()
                .cloudbreakExceptions(exceptions)
                .successes(successes)
                .build();

        assertThat(underTest.cloudbreakExceptions()).isSameAs(exceptions);
        assertThat(underTest.successes()).isSameAs(successes);
    }

    @Test
    void builderShouldShareMutableMapReferenceWithBuiltInstance() {
        Map<String, CloudbreakException> exceptions = new HashMap<>();
        ConversionStatus underTest = ConversionStatus.builder()
                .cloudbreakExceptions(exceptions)
                .build();

        exceptions.put("id", new CloudbreakException("late"));

        assertThat(underTest.cloudbreakExceptions()).containsKey("id");
    }
}
