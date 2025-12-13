package com.sequenceiq.cloudbreak.cloud.aws.conf;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.config.AwsConfig;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetFilterStrategyMultiplePreferPublic;
import com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetSelectorService;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { AwsConfig.class, SubnetFilterStrategyMultiplePreferPublic.class, SubnetSelectorService.class})
class AwsConfigTagValueValidatorRegexpTest {

    @Inject
    private AwsConfig awsConfig;

    private Pattern awsTagValueValidatorPattern;

    private Pattern awsTagKeyValidatorPattern;

    private static Stream<Arguments> provideData() {
        return Stream.of(
                Arguments.of("awsprefixed-simpletagvalue", true, false),
                Arguments.of("aws-prefixed-simpletagvalue", true, false),
                Arguments.of("  \\n", false, false),
                Arguments.of("aws```tag-invalid-value", false, false),
                Arguments.of("simple tag value", true, true),
                Arguments.of("simple-tag-value with allowed special characters like + - = . _ : / @.", true, true)
        );
    }

    @BeforeEach
    void setUp() {
        TagSpecification tagSpecification = awsConfig.getTagSpecification();
        awsTagValueValidatorPattern = Pattern.compile(tagSpecification.getValueValidator());
        awsTagKeyValidatorPattern = Pattern.compile(tagSpecification.getKeyValidator());
    }

    @ParameterizedTest
    @MethodSource("provideData")
    void testAwsTagValueRegexp(String tagValue, boolean expectedForTagValue, boolean expectedForTagKey) {

        boolean tagValueResult = awsTagValueValidatorPattern.matcher(tagValue)
                .matches();

        boolean tagKeyResult = awsTagKeyValidatorPattern.matcher(tagValue)
                .matches();

        assertEquals(expectedForTagValue, tagValueResult);
        assertEquals(expectedForTagKey, tagKeyResult);
    }
}
