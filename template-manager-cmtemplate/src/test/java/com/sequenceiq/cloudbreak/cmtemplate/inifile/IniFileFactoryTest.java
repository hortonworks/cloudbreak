package com.sequenceiq.cloudbreak.cmtemplate.inifile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IniFileFactoryTest {

    private IniFileFactory underTest;

    @BeforeEach
    void setUp() {
        underTest = new IniFileFactory();
    }

    @Test
    void createTest() {
        assertThat(underTest.create()).isNotNull();
    }

}