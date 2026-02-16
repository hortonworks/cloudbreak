package com.sequenceiq.notification.scheduled.register.dto;

import static com.sequenceiq.notification.util.NotificationUtil.toCamelCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class BaseNotificationRegisterAdditionalDataDtoTest {

    private final TestDto dto = new TestDto();

    @Test
    void testNullInputReturnsNull() {
        assertNull(dto.camel(null));
    }

    @Test
    void testEmptyInputReturnsEmpty() {
        assertEquals("", dto.camel(""));
    }

    @Test
    void testSingleWord() {
        assertEquals("Test", dto.camel("test"));
        assertEquals("Word", dto.camel("WORD"));
    }

    @Test
    void testMultipleWordsUppercase() {
        assertEquals("Hello World From Us", dto.camel("HELLO_WORLD_FROM_US"));
    }

    @Test
    void testConsecutiveAndLeadingTrailingUnderscores() {
        assertEquals("Some Value", dto.camel("___SOME__VALUE___"));
    }

    @Test
    void testMixedCaseInput() {
        assertEquals("Mixed Case Input", dto.camel("miXeD_CaSe_InPut"));
    }

    @Test
    void testOnlyUnderscores() {
        assertEquals("", dto.camel("____"));
    }

    @Test
    void testDigitsInside() {
        assertEquals("User 123 Status", dto.camel("USER_123_STATUS"));
    }

    // Concrete test subclass to expose the protected method
    private static class TestDto extends BaseNotificationRegisterAdditionalDataDto {

        TestDto() {
            super("name", "crn");
        }

        String camel(String input) {
            return toCamelCase(input);
        }
    }
}

