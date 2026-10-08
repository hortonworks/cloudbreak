package com.sequenceiq.cloudbreak.cloud.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ClouderaManagerCopyTest {

    @Test
    void productCopyPreservesFieldsAndIsolatesMutableValues() {
        ClouderaManagerProduct source = new ClouderaManagerProduct().withName("CDH").withDisplayName("Runtime").withVersion("7.3.2")
                .withParcel("parcel-url").withParcelFileUrl("parcel-file-url").withCsd(new ArrayList<>(List.of("csd-url")));

        ClouderaManagerProduct copy = source.copy();

        assertThat(copy).isNotSameAs(source).usingRecursiveComparison().isEqualTo(source);
        copy.withParcel("rewritten-url");
        copy.getCsd().add("another-csd");
        assertThat(source.getParcel()).isEqualTo("parcel-url");
        assertThat(source.getCsd()).containsExactly("csd-url");
    }

    @Test
    void productCopyPreservesNullCsd() {
        assertThat(new ClouderaManagerProduct().copy().getCsd()).isNull();
    }

    @Test
    void repoCopyPreservesFieldsAndIsolatesUrlChanges() {
        ClouderaManagerRepo source = new ClouderaManagerRepo().withPredefined(true).withVersion("7.13.1")
                .withBuildNumber("123").withBaseUrl("repo-url").withGpgKeyUrl("gpg-url");

        ClouderaManagerRepo copy = source.copy();

        assertThat(copy).isNotSameAs(source).usingRecursiveComparison().isEqualTo(source);
        copy.withBaseUrl("rewritten-url");
        assertThat(source.getBaseUrl()).isEqualTo("repo-url");
    }
}
