package com.sequenceiq.environment.tags.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.environment.tags.domain.AccountTag;

@ExtendWith(MockitoExtension.class)
public class DefaultInternalAccountTagServiceTest {

    private DefaultInternalAccountTagService underTest = new DefaultInternalAccountTagService(new CloudbreakResourceReaderService());

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "applyInternalTags", false);
    }

    @Test
    public void testValidateWhenKeyAndValueIsFineShouldDoNothing() {
        underTest.validate(accountTags("apple", "apple"));
    }

    @Test
    public void testValidateWhenKeyContainsMicrosoftShouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> underTest.validate(accountTags("microsoftapple", "apple")));
    }

    @Test
    public void testValidateWhenKeyContainsAwsShouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> underTest.validate(accountTags("awsapple", "apple")));
    }

    @Test
    public void testValidateWhenKeyContainsAwsButNotStartingWithShouldBeFine() {
        underTest.validate(accountTags("appleaws", "apple"));
    }

    @Test
    public void testValidateWhenValueContainsMicrosoftShouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> underTest.validate(accountTags("apple", "microsoftapple")));
    }

    @Test
    public void testValidateWhenValueContainsAwsShouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> underTest.validate(accountTags("apple", "awsapple")));
    }

    private List<AccountTag> accountTags(String key, String value) {
        List<AccountTag> accountTagList = new ArrayList<>();

        AccountTag accountTag = new AccountTag();
        accountTag.setTagValue(key);
        accountTag.setTagKey(value);

        accountTagList.add(accountTag);
        return accountTagList;
    }

}