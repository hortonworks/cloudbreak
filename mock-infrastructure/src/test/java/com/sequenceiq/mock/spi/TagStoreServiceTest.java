package com.sequenceiq.mock.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TagStoreServiceTest {

    private static final String MOCK_UUID = "crn:cdp:datalake:us-west-1:tenant:datalake:uuid";

    private final TagStoreService underTest = new TagStoreService();

    @Test
    void getWhenUnknownMockUuidThenEmpty() {
        assertThat(underTest.get("unknown")).isEmpty();
    }

    @Test
    void initWhenNullTagsThenEmpty() {
        underTest.init(MOCK_UUID, null);

        assertThat(underTest.get(MOCK_UUID)).isEmpty();
    }

    @Test
    void updateShouldAddNewAndOverwriteExistingTags() {
        underTest.init(MOCK_UUID, Map.of("default", "d", "key1", "v1"));

        underTest.update(MOCK_UUID, Map.of("key1", "v1-new", "key2", "v2"));

        assertThat(underTest.get(MOCK_UUID)).containsExactlyInAnyOrderEntriesOf(Map.of("default", "d", "key1", "v1-new", "key2", "v2"));
    }

    @Test
    void updateWhenNotInitializedThenCreatesEntry() {
        underTest.update(MOCK_UUID, Map.of("key1", "v1"));

        assertThat(underTest.get(MOCK_UUID)).containsExactlyInAnyOrderEntriesOf(Map.of("key1", "v1"));
    }

    @Test
    void deleteShouldRemoveExistingKeysAndIgnoreMissingOnes() {
        underTest.init(MOCK_UUID, Map.of("default", "d", "key1", "v1", "key2", "v2"));

        underTest.delete(MOCK_UUID, Set.of("key1", "missing"));

        assertThat(underTest.get(MOCK_UUID)).containsExactlyInAnyOrderEntriesOf(Map.of("default", "d", "key2", "v2"));
    }

    @Test
    void deleteWhenNotInitializedThenNoop() {
        underTest.delete(MOCK_UUID, Set.of("key1"));

        assertThat(underTest.get(MOCK_UUID)).isEmpty();
    }

    @Test
    void removeShouldDropAllTagsOfMockUuid() {
        underTest.init(MOCK_UUID, Map.of("key1", "v1"));
        underTest.init("other", Map.of("key1", "v1"));

        underTest.remove(MOCK_UUID);

        assertThat(underTest.get(MOCK_UUID)).isEmpty();
        assertThat(underTest.get("other")).containsExactlyInAnyOrderEntriesOf(Map.of("key1", "v1"));
    }
}
