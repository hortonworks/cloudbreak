package com.sequenceiq.cloudbreak.service.cluster.flow;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AmbariRepositoryVersionServiceTest {

    @InjectMocks
    private AmbariRepositoryVersionService underTest = new AmbariRepositoryVersionService();

    @Test
    public void testIsNewerOrEqualAmbariApi() {
        boolean actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0.0", () -> "2.6.0.0");
        Assert.assertTrue(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0.0", () -> "2.5.0.0");
        Assert.assertTrue(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0.0", () -> "2.5.11.0");
        Assert.assertTrue(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0.0", () -> "2.5.11");
        Assert.assertTrue(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0", () -> "2.5.11.0");
        Assert.assertTrue(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0.0", () -> "3.0.0.0");
        Assert.assertFalse(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.11.0", () -> "3.0.0.0");
        Assert.assertFalse(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0.0", () -> "3.12.0.0");
        Assert.assertFalse(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0.0", () -> "2.6.5.0");
        Assert.assertFalse(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0", () -> "2.6.5.0");
        Assert.assertFalse(actual);

        actual = underTest.isNewerOrEqualAmbariApi(() -> "2.6.0.0", () -> "2.6.5");
        Assert.assertFalse(actual);
    }

}
