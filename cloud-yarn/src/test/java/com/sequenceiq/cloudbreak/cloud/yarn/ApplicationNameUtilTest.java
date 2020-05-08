package com.sequenceiq.cloudbreak.cloud.yarn;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class ApplicationNameUtilTest {

    private ApplicationNameUtil underTest = new ApplicationNameUtil();

    @Test
    public void testDecorateNameWhenNameContainsPostfix() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 30);
        String actual = underTest.decorateName("smtg-name-cb-username", "username");
        Assertions.assertEquals("smtg-name-cb-username", actual);
    }

    @Test
    public void testDecorateNameWhenNameContainsCbOnly() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 30);
        String actual = underTest.decorateName("smtg-name-cb", "username");
        Assertions.assertEquals("smtg-name-cb-username", actual);
    }

    @Test
    public void testDecorateNameWhenNameDoesNotContainsPostfix() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 30);
        String actual = underTest.decorateName("smtg-name", "username");
        Assertions.assertEquals("smtg-name-cb-username", actual);
    }

    @Test
    public void testDecorateNameWhenNameIsTooLong() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 15);
        String actual = underTest.decorateName("smtg-name-smtg-name-smtg-name-smtg-name-smtg-name-smtg-name", "username");
        Assertions.assertEquals("smt-cb-username", actual);
    }

    @Test
    public void testDecorateNameWhenNameIsEqualsChars() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", 21);
        String actual = underTest.decorateName("smtg-name-cb-username", "username");
        Assertions.assertEquals("smtg-name-cb-username", actual);
    }
}
