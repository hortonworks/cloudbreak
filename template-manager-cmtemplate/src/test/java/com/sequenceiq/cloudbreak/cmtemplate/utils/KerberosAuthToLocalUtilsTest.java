package com.sequenceiq.cloudbreak.cmtemplate.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KerberosAuthToLocalUtilsTest {

    @InjectMocks
    private KerberosAuthToLocalUtils underTest;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testGenerateForTrustedRealm() {
        String result = underTest.generateForTrustedRealm("H5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE");
        assertThat(result).isEqualTo("""
                RULE:[1:$1@$0](.*@\\QH5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE\\E$)s/@\\QH5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE\\E$//
                RULE:[2:$1@$0](.*@\\QH5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE\\E$)s/@\\QH5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE\\E$//
                DEFAULT""");
    }

    @Test
    void generateEscapedForTrustedRealm() {
        String result = underTest.generateEscapedForTrustedRealm("H5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE");
        assertThat(result).isEqualTo(
                "RULE:[1:$1@$0](.*@\\\\QH5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE\\\\E$)s/@\\\\QH5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE\\\\E$//\\n"
                + "RULE:[2:$1@$0](.*@\\\\QH5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE\\\\E$)s/@\\\\QH5-DBAJZ.XCU2-8Y8X.WL.CLOUDERA.SITE\\\\E$//\\n"
                + "DEFAULT");
    }

}
