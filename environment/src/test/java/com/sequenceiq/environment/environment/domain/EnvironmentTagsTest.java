package com.sequenceiq.environment.environment.domain;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;

class EnvironmentTagsTest {

    @Test
    void fromJsonTestWhenNullJson() {
        EnvironmentTags result = EnvironmentTags.fromJson(null);

        verifyEmptyResult(result);
    }

    private void verifyEmptyResult(EnvironmentTags result) {
        assertThat(result).isNotNull();
        assertThat(result.getDefaultTags()).isNotNull();
        assertThat(result.getDefaultTags()).isEmpty();
        assertThat(result.getUserDefinedTags()).isNotNull();
        assertThat(result.getUserDefinedTags()).isEmpty();
    }

    @Test
    void fromJsonTestWhenNullJsonValue() {
        Json json = new Json(null);

        EnvironmentTags result = EnvironmentTags.fromJson(json);

        verifyEmptyResult(result);
    }

    @Test
    void fromJsonTestWhenInvalidJsonValue() {
        Json json = new Json("");

        EnvironmentTags result = EnvironmentTags.fromJson(json);

        verifyEmptyResult(result);
    }

    @Test
    void fromJsonTestWhenJsonValueEmptyObject() {
        Json json = new Json("{}");

        EnvironmentTags result = EnvironmentTags.fromJson(json);

        assertThat(result).isNotNull();
        assertThat(result.getDefaultTags()).isNull();
        assertThat(result.getUserDefinedTags()).isNull();
    }

    @Test
    void fromJsonTestWhenSuccess() {
        Map<String, String> userDefinedTags = Map.ofEntries(entry("userKey1", "userValue1"), entry("userKey2", "userValue2"));
        Map<String, String> defaultTags = Map.ofEntries(entry("defaultKey1", "defaultValue1"), entry("defaultKey2", "defaultValue2"));
        Json json = new Json(new EnvironmentTags(userDefinedTags, defaultTags));

        EnvironmentTags result = EnvironmentTags.fromJson(json);

        assertThat(result).isNotNull();
        assertThat(result.getDefaultTags()).isNotNull();
        assertThat(result.getDefaultTags()).isEqualTo(defaultTags);
        assertThat(result.getUserDefinedTags()).isNotNull();
        assertThat(result.getUserDefinedTags()).isEqualTo(userDefinedTags);
    }

    @Test
    void removeUserDefinedTagsRemovesSpecifiedKeys() {
        EnvironmentTags environmentTags = new EnvironmentTags(
                new HashMap<>(Map.of("owner", "john doe", "project", "atlas")),
                new HashMap<>(Map.of("creation-timestamp", "1773042126")));

        EnvironmentTags result = environmentTags.removeUserDefinedTags(Set.of("owner"));

        assertThat(result.getUserDefinedTags()).containsExactly(Map.entry("project", "atlas"));
        assertThat(result.getDefaultTags()).containsExactly(Map.entry("creation-timestamp", "1773042126"));
    }

    @Test
    void removeUserDefinedTagsIsNoOpForMissingKeys() {
        EnvironmentTags environmentTags = new EnvironmentTags(Map.of("project", "atlas"), Map.of());

        EnvironmentTags result = environmentTags.removeUserDefinedTags(Set.of("missing"));

        assertThat(result.getUserDefinedTags()).containsExactly(Map.entry("project", "atlas"));
    }

    @Test
    void removeUserDefinedTagsHandlesNullUserDefinedTags() {
        EnvironmentTags environmentTags = new EnvironmentTags(null, Map.of("creation-timestamp", "1773042126"));

        EnvironmentTags result = environmentTags.removeUserDefinedTags(Set.of("owner"));

        assertThat(result.getUserDefinedTags()).isEmpty();
        assertThat(result.getDefaultTags()).containsExactly(Map.entry("creation-timestamp", "1773042126"));
    }

}