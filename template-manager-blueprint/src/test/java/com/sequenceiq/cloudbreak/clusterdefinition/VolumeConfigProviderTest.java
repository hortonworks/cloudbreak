package com.sequenceiq.cloudbreak.clusterdefinition;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VolumeConfigProviderTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private VolumeConfigProvider underTest;

    @Test
    public void testGetValueWhenVolumeParameterPresentedAndGlobalThenShouldReturnTwoSegment() {
        ConfigProperty configProperty = new ConfigProperty("namenode", "testDirectory", "testPrefix");

        String actual = underTest.getValue(false, 2, configProperty, "namenode");

        Assert.assertEquals("/hadoopfs/fs1/namenode/testDirectory,/hadoopfs/fs2/namenode/testDirectory", actual);
    }

    @Test
    public void testGetValueWhenVolumeParameterIsNullAndNotGlobalThenShouldReturnOneSegment() {
        ConfigProperty configProperty = new ConfigProperty("namenode", "testDirectory", "testPrefix");

        String actual = underTest.getValue(true, null, configProperty, "namenode");

        Assert.assertEquals("testPrefix/hadoopfs/fs1/namenode/testDirectory", actual);
    }

}