package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CsdParcelNameMatcherTest {

    @InjectMocks
    private CsdParcelNameMatcher underTest;

    @ParameterizedTest(name = "csdurl {0} & parcelName {1} should match {2}")
    @MethodSource("testCsdAndNameMatchingData")
    public void testMatcher(String csdUrl, String parcelName, boolean expectedToMatch) {

        boolean result = underTest.matching(csdUrl, parcelName);

        assertEquals(expectedToMatch, result);
    }

    static Object[][] testCsdAndNameMatchingData() {
        return new Object[][]{
                {
                        null,
                        "FLINK",
                        false
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/FLINK-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        null,
                        false
                },
                // FLINK
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/FLINK-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "FLINK",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/FLINK-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "flink",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/flink-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "FLINK",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/FLINK-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "ZLINK",
                        false
                },
                // SQL_STREAM_BUILDER
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/sql_stream_builder-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "FLINK",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/SQL_STREAM_BUILDER-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "SQL_STREAM_BUILDER",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/SQL_STREAM_BUILDER-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "SQLSTREAM_BUILDER",
                        false
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/SQL_STREAM_BUILDER-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "sql_stream_builder",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/sql_stream_builder-1.13.2-csadh1.5.0.0-cdh7.2.12.0-35-17431883.jar",
                        "SQL_STREAM_BUILDER",
                        true
                },
                // NIFI
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFI-1.13.2.2.2.3.0-56.jar",
                        "NIFI",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFI-1.13.2.2.2.3.0-56.jar",
                        "FIFI",
                        false
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFI-1.13.2.2.2.3.0-56.jar",
                        "nifi",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/nifi-1.13.2.2.2.3.0-56.jar",
                        "NIFI",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFI-1.13.2.2.2.3.0-56.jar",
                        "CFM",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFI-1.13.2.2.2.3.0-56.jar",
                        "CFM",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFI-1.13.2.2.2.3.0-56.jar",
                        "CFM",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/nifi-1.13.2.2.2.3.0-56.jar",
                        "CFM",
                        true
                },
                // NIFIREGISTRY
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFIREGISTRY-0.8.0.2.2.3.0-56.jar",
                        "NIFIREGISTRY",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFIREGISTRY-0.8.0.2.2.3.0-56.jar",
                        "NIFI_REGISTRY",
                        false
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFIREGISTRY-0.8.0.2.2.3.0-56.jar",
                        "nifiregistry",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/nifiregistry-0.8.0.2.2.3.0-56.jar",
                        "NIFIREGISTRY",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFIREGISTRY-0.8.0.2.2.3.0-56.jar",
                        "CFM",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFIREGISTRY-0.8.0.2.2.3.0-56.jar",
                        "CFM",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/NIFIREGISTRY-0.8.0.2.2.3.0-56.jar",
                        "CFM",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/nifiregistry-0.8.0.2.2.3.0-56.jar",
                        "CFM",
                        true
                },
                // SPARK3
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/spark3-0.8.0.2.2.3.0-56.jar",
                        "SPARK3",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/SPARK3-0.8.0.2.2.3.0-56.jar",
                        "spark3",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/spark3-0.8.0.2.2.3.0-56.jar",
                        "SPARK3",
                        true
                },
                {
                        "http://cloudera-build-us-west-1.vpc.cloudera.com/spark3-0.8.0.2.2.3.0-56.jar",
                        "spark2",
                        false
                },

        };
    }
}