package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.freeipa.controller.exception.BadRequestException;

class FreeIpaPasswordValidatorTest {

    private FreeIpaPasswordValidator underTest = new FreeIpaPasswordValidator();

    @BeforeEach
    void before() {
        ReflectionTestUtils.setField(underTest, "minPasswordLength", 8);
        ReflectionTestUtils.setField(underTest, "maxPasswordLength", 16);
        ReflectionTestUtils.setField(underTest, "minCharacterClasses", 3);
    }

    @Test
    void testValidateShouldThrowExceptionWhenThePasswordIsNotLongEnough() {
        String password = "1234567";
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validate(password));

        assertEquals("Password must be between minimum 8 and maximum 16 characters.", exception.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenThePasswordIsTooLong() {
        String password = "123456789123456789";
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validate(password));

        assertEquals("Password must be between minimum 8 and maximum 16 characters.", exception.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenOnlyLowerCaseCharactersArePresent() {
        String password = "asdasdasd";
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validate(password));

        assertEquals("There must be 3 different classes. The classes are: Upper-case characters, Lower-case characters, Digits and Special characters.",
                exception.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenOnlyUpperCaseCharactersArePresent() {
        String password = "ASDASDASD";
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validate(password));

        assertEquals("There must be 3 different classes. The classes are: Upper-case characters, Lower-case characters, Digits and Special characters.",
                exception.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenOnlyUpperAndLowerCaseCharactersArePresent() {
        String password = "ASDASDasd";
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validate(password));

        assertEquals("There must be 3 different classes. The classes are: Upper-case characters, Lower-case characters, Digits and Special characters.",
                exception.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenOnlyDigitsArePresent() {
        String password = "123456789";
        BadRequestException exception = assertThrows(BadRequestException.class, () -> underTest.validate(password));

        assertEquals("There must be 3 different classes. The classes are: Upper-case characters, Lower-case characters, Digits and Special characters.",
                exception.getMessage());
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenThePasswordComplexityIsCorrect() {
        String password = "asd123ASD!";
        underTest.validate(password);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenThePasswordComplexityIs1AndThePasswordContainsOnlyLowerCaseCharacters() {
        ReflectionTestUtils.setField(underTest, "minCharacterClasses", 1);
        String password = "asdasdasd";
        underTest.validate(password);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenThePasswordComplexityIs1AndThePasswordContainsOnlyUpperCaseCharacters() {
        ReflectionTestUtils.setField(underTest, "minCharacterClasses", 1);
        String password = "ASDASDASD";
        underTest.validate(password);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenThePasswordComplexityIs1AndThePasswordContainsOnlyDigits() {
        ReflectionTestUtils.setField(underTest, "minCharacterClasses", 1);
        String password = "12345678";
        underTest.validate(password);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenThePasswordComplexityIs1AndThePasswordContainsOnlySpecialCharacters() {
        ReflectionTestUtils.setField(underTest, "minCharacterClasses", 1);
        String password = "?/!'|]-_=+*@£#^";
        underTest.validate(password);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenThePasswordComplexityIs4AndThePasswordContainsAllClasses() {
        ReflectionTestUtils.setField(underTest, "minCharacterClasses", 4);
        String password = "Asd4asd!22";
        underTest.validate(password);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenThePasswordComplexityIs4AndThePasswordContainsAllClassesWithUnderScore() {
        ReflectionTestUtils.setField(underTest, "minCharacterClasses", 4);
        String password = "Asd4asd_22";
        underTest.validate(password);
    }

}