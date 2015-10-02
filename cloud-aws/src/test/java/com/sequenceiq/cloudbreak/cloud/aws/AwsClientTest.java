package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.auth.BasicSessionCredentials;
import com.sequenceiq.cloudbreak.cloud.aws.cache.CachingConfig;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestContext.class)
public class AwsClientTest {
    public static final long CREDENTIAL_ID = 1L;

    @Inject
    private CacheManager cm;

    @Inject
    private TestContext.AwsClientWrapper w;

    @Test
    public void testRetrieveSessionCredentialsCacheTest() throws Exception {
        AwsCredentialView c = mock(AwsCredentialView.class);
        when(c.getId()).thenReturn(CREDENTIAL_ID);

        w.retrieveCachedSessionCredentials(c);
        w.retrieveCachedSessionCredentials(c);
        w.retrieveCachedSessionCredentials(c);

        BasicSessionCredentials fromCache = (BasicSessionCredentials) getCache().get(CREDENTIAL_ID).get();
        assertEquals("awsAccessKey", fromCache.getAWSAccessKeyId());
        assertEquals(1, TestContext.AwsClientWrapper.getCounter());
    }

    private Cache getCache() {
        return cm.getCache(CachingConfig.TEMPORARY_AWS_CREDENTIAL_CACHE);
    }
}