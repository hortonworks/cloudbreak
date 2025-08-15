package com.sequenceiq.environment.tags.service;

import static com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService.DEFAULT_KEY_ACCOUNT_TAG_PATTERN;
import static com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService.DEFAULT_VALUE_ACCOUNT_TAG_PATTERN;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.environment.tags.domain.AccountTag;

@ExtendWith(MockitoExtension.class)
public class DefaultInternalAccountTagServiceTest {

    private final DefaultInternalAccountTagService underTest = new DefaultInternalAccountTagService(new CloudbreakResourceReaderService());

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
                { "Pear",                       "apple",                        false },
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
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "applyInternalTags", false);
        ReflectionTestUtils.setField(underTest, "keyAccountTagPattern", DEFAULT_KEY_ACCOUNT_TAG_PATTERN);
        ReflectionTestUtils.setField(underTest, "valueAccountValueTagPattern", DEFAULT_VALUE_ACCOUNT_TAG_PATTERN);
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
}