package com.sequenceiq.cloudbreak.service.image;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@ExtendWith(MockitoExtension.class)
class PlatformStringTransformerTest {

    @InjectMocks
    private PlatformStringTransformer platformStringTransformer;

    private static Stream<Arguments> variantFlags() {
        return Stream.of(
                Arguments.of(CloudConstants.AWS, AwsConstants.AwsVariant.AWS_VARIANT.variant().value(), "AWS"),
                Arguments.of(CloudConstants.AWS, AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value(), "AWS"),
                Arguments.of(CloudConstants.AWS, AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value(), "AWS_GOV"),
                Arguments.of(CloudConstants.AWS, null, "AWS"),
                Arguments.of(CloudConstants.AWS, "", "AWS"),

                Arguments.of(CloudConstants.GCP, GcpConstants.GCP_VARIANT.value(), "GCP"),
                Arguments.of(CloudConstants.GCP, null, "GCP"),
                Arguments.of(CloudConstants.GCP, "", "GCP"),

                Arguments.of(CloudConstants.AZURE, AzureConstants.VARIANT.value(), "AZURE"),
                Arguments.of(CloudConstants.AZURE, null, "AZURE"),
                Arguments.of(CloudConstants.AZURE, "", "AZURE"),

                Arguments.of(CloudConstants.MOCK, null, "MOCK"),
                Arguments.of(CloudConstants.MOCK, "", "MOCK"),

                Arguments.of(CloudConstants.YARN, null, "YARN"),
                Arguments.of(CloudConstants.YARN, "", "YARN")
        );
    }

    @ParameterizedTest
    @MethodSource("variantFlags")
    public void testUseBaseImageAndDisabledBaseImageShouldReturnError(String platform, String variant, String expected) {
        Assert.assertEquals(expected.toLowerCase(), platformStringTransformer.getPlatformStringForImageCatalog(platform, variant).nameToLowerCase());
    }

}