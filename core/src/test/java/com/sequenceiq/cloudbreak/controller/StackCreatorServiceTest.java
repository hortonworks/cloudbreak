package com.sequenceiq.cloudbreak.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;

@RunWith(MockitoJUnitRunner.class)
public class StackCreatorServiceTest {

    @InjectMocks
    private StackCreatorService underTest;

    @Test
    public void testShouldUseBaseCMImageWithNoCmRequest() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request);

        assertFalse(base);
    }

    @Test
    public void testShouldUseBaseCMImageWithCmRepo() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request);

        assertTrue(base);
    }

    @Test
    public void testShouldUseBaseCMImageWithProducts() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerProductV4Request cdpRequest = new ClouderaManagerProductV4Request();
        cdpRequest.setName("CDP");
        cdpRequest.setParcel("parcel");
        cdpRequest.setVersion("version");
        cdpRequest.setCsd(List.of("csd"));
        cmRequest.setProducts(List.of(cdpRequest));
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request);

        assertTrue(base);
    }

    @Test
    public void testShouldUseBaseCMImageWithProductsAndCmRepo() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        ClouderaManagerProductV4Request cdpRequest = new ClouderaManagerProductV4Request();
        cdpRequest.setName("CDP");
        cdpRequest.setParcel("parcel");
        cdpRequest.setVersion("version");
        cdpRequest.setCsd(List.of("csd"));
        cmRequest.setProducts(List.of(cdpRequest));
        ClouderaManagerRepositoryV4Request cmRepoRequest = new ClouderaManagerRepositoryV4Request();
        cmRequest.setRepository(cmRepoRequest);
        clusterV4Request.setCm(cmRequest);

        boolean base = underTest.shouldUseBaseCMImage(clusterV4Request);

        assertTrue(base);
    }
}
