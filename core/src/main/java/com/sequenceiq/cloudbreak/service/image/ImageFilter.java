package com.sequenceiq.cloudbreak.service.image;

import java.util.Set;
import java.util.function.Predicate;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

public class ImageFilter {

    private final ImageCatalog imageCatalog;

    private final Set<ImageCatalogPlatform> platforms;

    private final boolean baseImageEnabled;

    private final Set<String> operatingSystems;

    private final String clusterVersion;

    private final Architecture architecture;

    private final Predicate<Image> additionalPredicate;

    private final boolean defaultOnly;

    private final boolean includeUnversionedImages;

    private ImageFilter(ImageCatalog imageCatalog, Set<ImageCatalogPlatform> platforms, boolean baseImageEnabled,
            Set<String> operatingSystems, String clusterVersion, Architecture architecture, Predicate<Image> additionalPredicate, boolean defaultOnly,
            boolean includeUnversionedImages) {
        this.imageCatalog = imageCatalog;
        this.platforms = platforms;
        this.baseImageEnabled = baseImageEnabled;
        this.operatingSystems = operatingSystems;
        this.clusterVersion = clusterVersion;
        this.architecture = architecture;
        this.additionalPredicate = additionalPredicate;
        this.defaultOnly = defaultOnly;
        this.includeUnversionedImages = includeUnversionedImages;
    }

    public ImageCatalog getImageCatalog() {
        return imageCatalog;
    }

    public Set<ImageCatalogPlatform> getPlatforms() {
        return platforms;
    }

    public boolean isBaseImageEnabled() {
        return baseImageEnabled;
    }

    public Set<String> getOperatingSystems() {
        return operatingSystems;
    }

    public String getClusterVersion() {
        return clusterVersion;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public Predicate<Image> getAdditionalPredicate() {
        return additionalPredicate;
    }

    public boolean isDefaultOnly() {
        return defaultOnly;
    }

    public boolean isIncludingUnversionedImages() {
        return includeUnversionedImages;
    }

    @Override
    public String toString() {
        return "ImageFilter{" +
                "imageCatalog=" + imageCatalog +
                ", platforms=" + platforms +
                ", baseImageEnabled=" + baseImageEnabled +
                ", operatingSystems=" + operatingSystems +
                ", clusterVersion='" + clusterVersion + '\'' +
                ", architecture=" + architecture +
                ", additionalPredicate=" + additionalPredicate +
                ", includeUnversionedImages=" + includeUnversionedImages +
                '}';
    }

    public static ImageFilterBuilder builder() {
        return new ImageFilterBuilder();
    }

    public static final class ImageFilterBuilder {

        private Predicate<Image> additionalPredicate;

        private ImageCatalog imageCatalog;

        private Set<ImageCatalogPlatform> platforms;

        private boolean baseImageEnabled;

        private Set<String> operatingSystems;

        private String clusterVersion;

        private Architecture architecture;

        private boolean defaultOnly;

        private boolean includeUnversionedImages;

        private ImageFilterBuilder() {
        }

        public ImageFilterBuilder withAdditionalPredicate(Predicate<Image> additionalPredicate) {
            this.additionalPredicate = additionalPredicate;
            return this;
        }

        public ImageFilterBuilder withImageCatalog(ImageCatalog imageCatalog) {
            this.imageCatalog = imageCatalog;
            return this;
        }

        public ImageFilterBuilder withPlatforms(Set<ImageCatalogPlatform> platforms) {
            this.platforms = platforms;
            return this;
        }

        public ImageFilterBuilder withBaseImageEnabled(boolean baseImageEnabled) {
            this.baseImageEnabled = baseImageEnabled;
            return this;
        }

        public ImageFilterBuilder withOperatingSystems(Set<String> operatingSystems) {
            this.operatingSystems = operatingSystems;
            return this;
        }

        public ImageFilterBuilder withClusterVersion(String clusterVersion) {
            this.clusterVersion = clusterVersion;
            return this;
        }

        public ImageFilterBuilder withArchitecture(Architecture architecture) {
            this.architecture = architecture;
            return this;
        }

        public ImageFilterBuilder withDefaultOnly(boolean defaultOnly) {
            this.defaultOnly = defaultOnly;
            return this;
        }

        public ImageFilterBuilder withIncludeUnversionedImages(boolean includeUnversionedImages) {
            this.includeUnversionedImages = includeUnversionedImages;
            return this;
        }

        public ImageFilter build() {
            return new ImageFilter(imageCatalog, platforms, baseImageEnabled, operatingSystems, clusterVersion, architecture, additionalPredicate,
                    defaultOnly, includeUnversionedImages);
        }
    }
}
