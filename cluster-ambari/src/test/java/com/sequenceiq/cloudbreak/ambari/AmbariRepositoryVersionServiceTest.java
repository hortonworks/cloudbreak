package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariRepositoryVersionService.AMBARI_VERSION_2_6_0_0;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AmbariRepositoryVersionServiceTest {

    @InjectMocks
    private final AmbariRepositoryVersionService underTest = new AmbariRepositoryVersionService();

    @Test
    public void testIsNewerOrEqualAmbariApi() {
        boolean actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", () -> "2.6.0.0");
        Assert.assertTrue(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", () -> "2.5.0.0");
        Assert.assertTrue(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", () -> "2.5.11.0");
        Assert.assertTrue(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", () -> "2.5.11");
        Assert.assertTrue(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0", () -> "2.5.11.0");
        Assert.assertTrue(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", () -> "3.0.0.0");
        Assert.assertFalse(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.11.0", () -> "3.0.0.0");
        Assert.assertFalse(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", () -> "3.12.0.0");
        Assert.assertFalse(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", () -> "2.6.5.0");
        Assert.assertFalse(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0", () -> "2.6.5.0");
        Assert.assertFalse(actual);

        actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", () -> "2.6.5");
        Assert.assertFalse(actual);
    }

    @Test
    public void testIsNewerOrEqualAmbariApiWhenCurrentVersionIsNewerThanLimitedApiVersion() {
        boolean actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.7.0.0", AMBARI_VERSION_2_6_0_0);
        Assert.assertTrue(actual);
    }

    @Test
    public void testIsNewerOrEqualAmbariApiWhenCurrentVersionIsOlderThanLimitedApiVersion() {
        boolean actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.5.0.0", AMBARI_VERSION_2_6_0_0);
        Assert.assertFalse(actual);
    }

    @Test
    public void testIsNewerOrEqualAmbariApiWhenCurrentVersionEqualsWithTheLimitedApiVersion() {
        boolean actual = underTest.isVersionNewerOrEqualThanLimited(() -> "2.6.0.0", AMBARI_VERSION_2_6_0_0);
        Assert.assertTrue(actual);
    }
}
