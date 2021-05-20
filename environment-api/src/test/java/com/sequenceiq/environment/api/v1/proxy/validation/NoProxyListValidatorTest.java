package com.sequenceiq.environment.api.v1.proxy.validation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class NoProxyListValidatorTest {

    private static Validator validator;

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    @ValidNoProxyList
    private String noProxyList;

    @BeforeAll
    public static void setUpClass() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    static Object[][] scenarios() {
        return new Object[][] {
                // value                             isValid
                { "cloudera.com:1024",               true },
                { "cloudera.com:65535",              true },
                { "cloudera.com:65536",              false },
                { "cloudera.com",                    true },
                { "cloudera.com:",                   false },
                { "cloudera.com,google.com",         true },
                { "domain-cloudera.com",             true },
                { ".domain-cloudera.com",            false },
                { "cloudera",                        true },
                { "256.2.3.4",                       false },
                { "1.2.3.4",                         true },
                { "1.2.3.4:",                        false },
                { "1.2.3.4:1024",                    true },
                { "1.2.3.4:65535",                   true },
                { "1.2.3.4:65536",                   false },
                { "1.2.3.4,2.3.4.5:65535",           true },
                { "1.2.3.4:1234,2.3.4.5:65536",      false }
        };
    }

    @ParameterizedTest
    @MethodSource("scenarios")
    void validateTest(String value, boolean valid) {
        noProxyList = value;
        Set<ConstraintViolation<NoProxyListValidatorTest>> violations = validator.validateProperty(this, "noProxyList");
        assertThat(violations.isEmpty()).isEqualTo(valid);
    }

}
