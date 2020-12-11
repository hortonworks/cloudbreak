package com.sequenceiq.environment.tags.service;

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

    private DefaultInternalAccountTagService underTest = new DefaultInternalAccountTagService(new CloudbreakResourceReaderService());

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "applyInternalTags", false);
        ReflectionTestUtils.setField(underTest, "accountTagPattern", "^(?!microsoft|azure|aws|windows|\\s)[a-zA-Z0-9\\{\\-\\_\\}]*[^\\-\\_]$");
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

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] tagValidationDataProvider() {
        return new Object[][] {
                { "microsoftapple",         "apple",            false },
                { "awsapple",               "apple",            false },
                { "appleaws-",              "apple",            false },
                { "apple",                  "microsoftapple",   false },
                { "apple",                  "awsapple",         false },
                { "apple",                  "{{{{pear}}}",      false },
                { "{{{{apple}}}",           "pear",             false },
                { "test<>'\"\n",            "pear",             false },
                { "pear",                   "{{{creatorcrn}}}", false },
                { "car--",                  "car",              false },
                { "car-car_",               "car",              false },
                { "{{{creatorcrn}}}",       "pear",             false },
                { "{{{creatorCrn}}}_",       "pear",            false },
                { "{{{creatorCrn}}}-",       "pear",            false },

                { "appleaws",               "apple",            true  },
                { "car-car_car",            "car",              true  },
                { "car-car",                "car",              true  },
                { "{{{creatorCrn}}}",       "pear",             true  },
                { "apple",                  "apple",            true  },
                { "car-_car",               "car",              true },


        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    private List<AccountTag> accountTags(String key, String value) {
        List<AccountTag> accountTagList = new ArrayList<>();

        AccountTag accountTag = new AccountTag();
        accountTag.setTagValue(key);
        accountTag.setTagKey(value);

        accountTagList.add(accountTag);
        return accountTagList;
    }

}