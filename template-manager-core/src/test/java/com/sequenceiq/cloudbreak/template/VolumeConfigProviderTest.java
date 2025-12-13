package com.sequenceiq.cloudbreak.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.template.model.ConfigProperty;

@ExtendWith(MockitoExtension.class)
class VolumeConfigProviderTest {

    @InjectMocks
    private VolumeConfigProvider underTest;

    @Test
    void testGetValueWhenVolumeParameterPresentedAndGlobalThenShouldReturnTwoSegment() {
        ConfigProperty configProperty = new ConfigProperty("namenode", "testDirectory", "testPrefix");

        String actual = underTest.getValue(false, 2, configProperty, "namenode");

        assertEquals("/hadoopfs/fs1/namenode/testDirectory,/hadoopfs/fs2/namenode/testDirectory", actual);
    }

    @Test
    void testGetValueWhenVolumeParameterIsNullAndNotGlobalThenShouldReturnOneSegment() {
        ConfigProperty configProperty = new ConfigProperty("namenode", "testDirectory", "testPrefix");

        String actual = underTest.getValue(true, null, configProperty, "namenode");

        assertEquals("testPrefix/hadoopfs/fs1/namenode/testDirectory", actual);
    }

}