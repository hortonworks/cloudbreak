package com.sequenceiq.cloudbreak.cloud.model.objectstorage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ObjectStoragePathLengthValidatorTest {

    ObjectStoragePathLengthValidator underTest = new ObjectStoragePathLengthValidator();

    @Test
    public void testObjectStoragePathLengthValidatorWhenNullValueShouldReturnFalse() {
        assertThat(underTest.isValid(null, null)).isFalse();
    }

    @Test
    public void testObjectStoragePathLengthValidatorWhenEmptyValueShouldReturnFalse() {
        assertThat(underTest.isValid("", null)).isFalse();
    }

    @Test
    public void testObjectStoragePathLengthValidatorWhenLongTextValueShouldReturnFalse() {
        assertThat(underTest.isValid(new String(new char[99999999]).replace('a', ' '), null)).isFalse();
    }

    @Test
    public void testObjectStoragePathLengthValidatorWhenValidTextValueShouldReturnTrue() {
        assertThat(underTest.isValid("asdfsdf", null)).isTrue();
    }

}