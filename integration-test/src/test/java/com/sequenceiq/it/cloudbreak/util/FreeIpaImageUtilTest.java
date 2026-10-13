package com.sequenceiq.it.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.ImageCatalog;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sun.net.httpserver.HttpServer;

/**
 * The served catalog is a trimmed snapshot of https://cloudbreak-imagecatalog.s3.amazonaws.com/v3-test-freeipa-image-catalog.json
 * (taken on 2026-10-15). It keeps the newest images for every provider / os / architecture / region and the published versions.
 */
public class FreeIpaImageUtilTest {

    private static final String CATALOG_RESOURCE = "/image-catalog/v3-test-freeipa-image-catalog.json";

    private static final String AWS = "AWS";

    private static final String AZURE = "AZURE";

    private static final String GCP = "GCP";

    private static final String AWS_REGION = "eu-central-1";

    private static final String AZURE_REGION = "West US";

    private HttpServer httpServer;

    private String catalogUrl;

    private Set<String> publishedImageIds;

    private FreeIpaImageUtil underTest;

    @BeforeClass
    public void setUp() throws IOException {
        byte[] catalog;
        try (InputStream inputStream = getClass().getResourceAsStream(CATALOG_RESOURCE)) {
            catalog = inputStream.readAllBytes();
        }
        publishedImageIds = new ObjectMapper().readValue(catalog, ImageCatalog.class).getVersions().getFreeIpaVersions().stream()
                .flatMap(freeIpaVersions -> freeIpaVersions.getImageIds().stream())
                .collect(Collectors.toSet());
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/v3-test-freeipa-image-catalog.json", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, catalog.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(catalog);
            }
        });
        httpServer.start();
        catalogUrl = "http://localhost:" + httpServer.getAddress().getPort() + "/v3-test-freeipa-image-catalog.json";
        underTest = new FreeIpaImageUtil();
        ReflectionTestUtils.setField(underTest, "imageCatalogDownloader", new ImageCatalogDownloader());
    }

    @AfterClass
    public void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    public void testGetLastUpgradeableImageReturnsArm64Images() {
        Pair<Image, Image> images = underTest.getLastUpgradeableImage(catalogUrl, AWS, AWS_REGION, Architecture.ARM64, false);

        assertUpgradeableImages(images, AWS, AWS_REGION, Architecture.ARM64);
    }

    @Test
    public void testGetLastUpgradeableImageReturnsPublishedArm64Images() {
        Pair<Image, Image> images = underTest.getLastUpgradeableImage(catalogUrl, AWS, AWS_REGION, Architecture.ARM64, true);

        assertUpgradeableImages(images, AWS, AWS_REGION, Architecture.ARM64);
        assertThat(publishedImageIds).contains(images.getLeft().getUuid(), images.getRight().getUuid());
    }

    @Test
    public void testGetLastUpgradeableImageReturnsX86Images() {
        Pair<Image, Image> images = underTest.getLastUpgradeableImage(catalogUrl, AWS, AWS_REGION, Architecture.X86_64, false);

        assertUpgradeableImages(images, AWS, AWS_REGION, Architecture.X86_64);
    }

    @Test
    public void testGetLastUpgradeableImageReturnsX86ImagesOnAzure() {
        Pair<Image, Image> images = underTest.getLastUpgradeableImage(catalogUrl, AZURE, AZURE_REGION, Architecture.X86_64, false);

        assertUpgradeableImages(images, AZURE, AZURE_REGION, Architecture.X86_64);
    }

    @Test
    public void testGetLastUpgradeableImageFallsBackToX86WhenArchitectureIsNull() {
        Pair<Image, Image> images = underTest.getLastUpgradeableImage(catalogUrl, AWS, AWS_REGION, null, false);

        assertUpgradeableImages(images, AWS, AWS_REGION, Architecture.X86_64);
    }

    @Test
    public void testGetLastUpgradeableImageTreatsImagesWithoutArchitectureAsX86() {
        // us-east-1 only has older images without an architecture field
        Pair<Image, Image> images = underTest.getLastUpgradeableImage(catalogUrl, AWS, "us-east-1", Architecture.X86_64, false);

        assertUpgradeableImages(images, AWS, "us-east-1", Architecture.X86_64);
        assertThat(images.getLeft().getArchitecture()).isNull();
        assertThat(images.getRight().getArchitecture()).isNull();
    }

    @Test
    public void testGetLastUpgradeableImageFailsWhenNoPublishedImageInRegion() {
        assertThatThrownBy(() -> underTest.getLastUpgradeableImage(catalogUrl, AWS, "us-east-1", Architecture.X86_64, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not found at least two x86_64 freeipa images on AWS (region: us-east-1). Please burn more images in %s catalog.", catalogUrl);
    }

    @Test
    public void testGetLastUpgradeableImageFailsWhenNoArm64ImageOnProvider() {
        assertThatThrownBy(() -> underTest.getLastUpgradeableImage(catalogUrl, AZURE, AZURE_REGION, Architecture.ARM64, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not found at least two arm64 freeipa images on AZURE (region: West US). Please burn more images in %s catalog.", catalogUrl);
    }

    @Test
    public void testGetLastUpgradeableImageFailsWhenRegionIsUnknown() {
        assertThatThrownBy(() -> underTest.getLastUpgradeableImage(catalogUrl, AWS, "mars-north-1", Architecture.ARM64, false))
                .isInstanceOf(RuntimeException.class)
                .hasMessageStartingWith("Not found at least two arm64 freeipa images on AWS (region: mars-north-1).");
    }

    @Test
    public void testGetLastAndNewestImageByOsReturnsTwoImagesOfSameOs() {
        LastAndNewestImage images = underTest.getLastAndNewestImageByOs(catalogUrl, Architecture.X86_64, AWS, OsType.RHEL8, OsType.RHEL8);

        assertLastAndNewestImages(images, AWS, Architecture.X86_64, OsType.RHEL8, OsType.RHEL8);
    }

    @Test
    public void testGetLastAndNewestImageByOsReturnsTwoArm64ImagesOfSameOs() {
        LastAndNewestImage images = underTest.getLastAndNewestImageByOs(catalogUrl, Architecture.ARM64, AWS, OsType.RHEL9, OsType.RHEL9);

        assertLastAndNewestImages(images, AWS, Architecture.ARM64, OsType.RHEL9, OsType.RHEL9);
    }

    @Test
    public void testGetLastAndNewestImageByOsReturnsArm64ImagePerOs() {
        LastAndNewestImage images = underTest.getLastAndNewestImageByOs(catalogUrl, Architecture.ARM64, AWS, OsType.RHEL8, OsType.RHEL9);

        assertLastAndNewestImages(images, AWS, Architecture.ARM64, OsType.RHEL8, OsType.RHEL9);
    }

    @Test
    public void testGetLastAndNewestImageByOsReturnsX86ImagePerOsOnAzure() {
        LastAndNewestImage images = underTest.getLastAndNewestImageByOs(catalogUrl, Architecture.X86_64, AZURE, OsType.RHEL8, OsType.RHEL9);

        assertLastAndNewestImages(images, AZURE, Architecture.X86_64, OsType.RHEL8, OsType.RHEL9);
    }

    @Test
    public void testGetLastAndNewestImageByOsTreatsCentos7ImagesWithoutArchitectureAsX86() {
        LastAndNewestImage images = underTest.getLastAndNewestImageByOs(catalogUrl, Architecture.X86_64, GCP, OsType.CENTOS7, OsType.RHEL8);

        assertLastAndNewestImages(images, GCP, Architecture.X86_64, OsType.CENTOS7, OsType.RHEL8);
        assertThat(images.last().getArchitecture()).isNull();
    }

    @Test
    public void testGetLastAndNewestImageByOsFailsWhenNoImageOfSourceOs() {
        assertThatThrownBy(() -> underTest.getLastAndNewestImageByOs(catalogUrl, Architecture.ARM64, AZURE, OsType.RHEL8, OsType.RHEL9))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not found any 'redhat8' freeipa image on AZURE. Please burn an image in %s catalog.", catalogUrl);
    }

    @Test
    public void testGetLastAndNewestImageByOsFailsWhenLessThanTwoImagesOfSameOs() {
        assertThatThrownBy(() -> underTest.getLastAndNewestImageByOs(catalogUrl, Architecture.ARM64, AWS, OsType.CENTOS7, OsType.CENTOS7))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not found at least two 'CENTOS7' freeipa images on AWS. Please burn more images in %s catalog.", catalogUrl);
    }

    @Test
    public void testGetNewestImageIdReturnsNewestImageOfProviderOsAndArchitecture() {
        // newer Azure redhat8, AWS redhat9 and AWS redhat8 arm64 images are in the catalog
        String imageId = underTest.getNewestImageId(catalogUrl, AWS, AWS_REGION, OsType.RHEL8.getOs(), Architecture.X86_64);

        assertThat(imageId).isEqualTo("4dc6eb19-60da-4f5e-a4bc-6addaf8f5790");
    }

    @Test
    public void testGetNewestImageIdReturnsNewestArm64Image() {
        // a newer AWS redhat9 x86_64 image is in the catalog
        String imageId = underTest.getNewestImageId(catalogUrl, AWS, AWS_REGION, OsType.RHEL9.getOs(), Architecture.ARM64);

        assertThat(imageId).isEqualTo("f6d1d145-0f04-44d6-98c5-780e00844c5e");
    }

    @Test
    public void testGetNewestImageIdReturnsNewestImageInRegion() {
        // us-east-1 only has older images without an architecture field
        String imageId = underTest.getNewestImageId(catalogUrl, AWS, "us-east-1", OsType.RHEL8.getOs(), Architecture.X86_64);

        assertThat(imageId).isEqualTo("1015105b-4c87-43be-98fd-561e578d158b");
    }

    @Test
    public void testGetNewestImageIdFailsWhenNoImageMatches() {
        assertThatThrownBy(() -> underTest.getNewestImageId(catalogUrl, AZURE, AZURE_REGION, OsType.RHEL9.getOs(), Architecture.ARM64))
                .isInstanceOf(TestFailException.class)
                .hasMessage("Not found any 'redhat9' arm64 freeipa image on AZURE (region: West US). Please burn an image in %s catalog.", catalogUrl);
    }

    private void assertUpgradeableImages(Pair<Image, Image> images, String cloudProvider, String region, Architecture architecture) {
        assertHasImage(images.getLeft(), cloudProvider, architecture);
        assertHasImage(images.getRight(), cloudProvider, architecture);
        assertThat(images.getLeft().getImageSetsByProvider().get(cloudProvider.toLowerCase())).containsKey(region);
        assertThat(images.getRight().getImageSetsByProvider().get(cloudProvider.toLowerCase())).containsKey(region);
        assertThat(images.getLeft().getUuid()).isNotEqualTo(images.getRight().getUuid());
        assertThat(OsType.getByOs(images.getLeft().getOs())).isLessThanOrEqualTo(OsType.getByOs(images.getRight().getOs()));
    }

    private void assertLastAndNewestImages(LastAndNewestImage images, String cloudProvider, Architecture architecture, OsType sourceOs,
            OsType destinationOs) {
        assertHasImage(images.last(), cloudProvider, architecture);
        assertHasImage(images.newest(), cloudProvider, architecture);
        assertThat(images.last().getOs()).isEqualTo(sourceOs.getOs());
        assertThat(images.newest().getOs()).isEqualTo(destinationOs.getOs());
        assertThat(images.last().getUuid()).isNotEqualTo(images.newest().getUuid());
        if (sourceOs == destinationOs) {
            assertThat(images.newest().getDate()).isGreaterThanOrEqualTo(images.last().getDate());
        }
    }

    private void assertHasImage(Image image, String cloudProvider, Architecture architecture) {
        assertThat(image).isNotNull();
        assertThat(image.getImageSetsByProvider()).containsKey(cloudProvider.toLowerCase());
        assertThat(image.getImageSetsByProvider().get(cloudProvider.toLowerCase())).isNotEmpty();
        assertThat(Architecture.fromStringWithFallback(image.getArchitecture())).isEqualTo(architecture);
    }
}
