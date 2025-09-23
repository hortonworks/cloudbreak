package com.sequenceiq.environment.tags.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.environment.tags.domain.AccountTag;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagServiceTest.TestAppContext;

@SpringBootTest(classes = TestAppContext.class)
public class DefaultInternalAccountTagServiceTest {

    private final DefaultInternalAccountTagService underTest = new DefaultInternalAccountTagService(new CloudbreakResourceReaderService());

    @Value("${environment.account.tag.validator.key}")
    private String keyAccountTagPattern;

    @Value("${environment.account.tag.validator.value}")
    private String valueAccountValueTagPattern;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] tagValidationDataProvider() {
        return new Object[][] {
                { "microsoftapple",             "apple",                        false },
                { "appleaws-",                  "apple",                        false },
                { "apple",                      "{{{{pear}}}",                  false },
                { "{{{{apple}}}",               "pear",                         false },
                { "test<>'\"\n",                "pear",                         false },
                { "pear",                       "{{{creatorcrn}}}",             false },
                { "car--",                      "car",                          false },
                { "car-car_",                   "car",                          false },
                { "{{{creatorcrn}}}",           "pear",                         false },
                { "{{{creatorCrn}}}_",          "pear",                         false },
                { "{{{creatorCrn}}}-",          "pear",                         false },
                { "apple{",                     "car",                          false },
                { "pe}ar",                      "apple",                        false },
                { "pe@r",                       "apple",                        false },
                { "hey!",                       "car",                          false },
                { "why?",                       "apple",                        false },
                { "*pear*",                     "apple",                        false },
                { "{creatorCrn}",               "apple",                        false },
                { "{{creatorCrn}}",             "pear",                         false },
                { "azureapple",                 "apple",                        false },
                { "windowspear",                "car",                          false },
                { "microsoftapple",             "pear",                         false },
                { " apple",                     "car",                          false },
                { "car ",                       "pear",                         false },
                { "-pear ",                     "apple",                        false },
                { "_pear ",                     "apple",                        false },
                { "apple ",                     "apple}",                       false },
                { "pear",                       " car",                         false },
                { "pear",                       "apple ",                       false },

                { "awsapple",                   "apple",                        true  },
                { "apple",                      "awsapple",                     true  },
                { "appleaws",                   "apple",                        true  },
                { "car-car_car",                "car",                          true  },
                { "car-car",                    "car",                          true  },
                { "{{{creatorCrn}}}",           "pear",                         true  },
                { "apple",                      "apple",                        true  },
                { "car-_car",                   "car",                          true  },
                { "apple",                      "1pear1",                       true  },
                { "aws",                        "aws",                          true  },
                { "apple123",                   "car",                          true  },
                { "my{{{time}}}",               "pear",                         true  },
                { "apple",                      "{{{cloudPlatform}}}2",         true  },
                { "{{{userName}}}-{{{time}}}",  "pear",                         true  },
                { "apple",                      "{{{userCrn}}}{{{accountId}}}", true  },
                { "{{{resourceCrn}}}",          "{{{cloudPlatform}}}",          true  },
                { "pear",                       "apple-",                       true  },
                { "apple",                      "pear_",                        true  },
                { "apple",                      "microsoft",                    true  },
                { "apple",                      "microsoftapple",               true  },
                { "apple",                      "azure",                        true  },
                { "apple",                      "windows",                      true  },
                { "Pear",                       "apple",                        true  },
                { "UPPER",                      "CASE",                         true  },
                { "lower",                      "case",                         true  },
                { "aCar",                       "aCar",                         true  },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "applyInternalTags", false);
        ReflectionTestUtils.setField(underTest, "keyAccountTagPattern", keyAccountTagPattern);
        ReflectionTestUtils.setField(underTest, "valueAccountValueTagPattern", valueAccountValueTagPattern);
        ReflectionTestUtils.setField(underTest, "keyPattern", Pattern.compile(keyAccountTagPattern));
        ReflectionTestUtils.setField(underTest, "valuePattern", Pattern.compile(valueAccountValueTagPattern));
    }

    @ParameterizedTest(name = "When key={0} and value={1} should be valid: {2}")
    @MethodSource("tagValidationDataProvider")
    public void testStackUpdateConfigPoller(String key, String value, boolean valid) {
        List<AccountTag> accountTags = accountTags(key, value);
        if (valid) {
            underTest.validate(accountTags);
        } else {
            assertThrows(BadRequestException.class, () -> underTest.validate(accountTags));
        }
    }

    private List<AccountTag> accountTags(String key, String value) {
        List<AccountTag> accountTagList = new ArrayList<>();

        AccountTag accountTag = new AccountTag();
        accountTag.setTagKey(key);
        accountTag.setTagValue(value);

        accountTagList.add(accountTag);
        return accountTagList;
    }

    @Configuration
    @ComponentScan(basePackages = "com.sequenceiq.cloudbreak",
            useDefaultFilters = false,
            includeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                    value = {}
            ))
    @PropertySource("classpath:application.yml")
    static class TestAppContext {
        @Value("${environment.account.tag.validator.key}")
        private String keyAccountTagPattern;

        @Value("${environment.account.tag.validator.value}")
        private String valueAccountValueTagPattern;

        public String getKeyAccountTagPattern() {
            return keyAccountTagPattern;
        }

        public String getValueAccountValueTagPattern() {
            return valueAccountValueTagPattern;
        }
    }
}