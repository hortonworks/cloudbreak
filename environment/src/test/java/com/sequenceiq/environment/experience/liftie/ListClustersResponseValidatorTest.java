package com.sequenceiq.environment.experience.liftie;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.experience.liftie.responses.LiftieClusterView;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;
import com.sequenceiq.environment.experience.liftie.responses.PageStats;

class ListClustersResponseValidatorTest {

    private ListClustersResponseValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new ListClustersResponseValidator();
    }

    @Test
    void testIsListClustersResponseEmptyWhenResponseIsNullThenTrueShouldReturn() {
        assertTrue(underTest.isListClustersResponseEmpty(null));
    }

    @Test
    void testIsListClustersResponseEmptyWhenResponseClusterMapIsNullThenTrueShouldReturn() {
        ListClustersResponse response = createValidListClustersResponse();
        response.setClusters(null);

        assertTrue(underTest.isListClustersResponseEmpty(response));
    }

    @Test
    void testIsListClustersResponseEmptyWhenResponseClusterMapIsEmptyThenTrueShouldReturn() {
        ListClustersResponse response = createValidListClustersResponse();
        response.setClusters(new LinkedHashMap<>());

        assertTrue(underTest.isListClustersResponseEmpty(response));
    }

    @Test
    void testIsListClustersResponseEmptyWhenClusterMapIsNotEmptyButPageIsNullThenTrueShouldReturn() {
        ListClustersResponse response = createValidListClustersResponse();
        response.setPage(null);

        assertTrue(underTest.isListClustersResponseEmpty(response));
    }

    @Test
    void testIsListClustersResponseEmptyWhenClusterMapAndPageIsNotEmptyButTotalPageIsNullThenTrueShouldReturn() {
        ListClustersResponse response = createValidListClustersResponse();
        response.getPage().setTotalPages(null);

        assertTrue(underTest.isListClustersResponseEmpty(response));
    }

    @Test
    void testIsListClustersResponseEmptyWhenClusterMapIsNotEmptyButTotalPageIsNullThenTrueShouldReturn() {
        ListClustersResponse response = createValidListClustersResponse();
        response.getPage().setTotalPages(0);

        assertTrue(underTest.isListClustersResponseEmpty(response));
    }

    @Test
    void testIsListClustersResponseEmptyWhenClusterMapAndPageIsNotEmptyAndMoreThanOneTotalPageExistsThenFalseShouldReturn() {
        assertFalse(underTest.isListClustersResponseEmpty(createValidListClustersResponse()));
    }

    private ListClustersResponse createValidListClustersResponse() {
        ListClustersResponse response = new ListClustersResponse();
        response.setClusters(Map.of("someCluster", new LiftieClusterView()));
        PageStats ps = new PageStats();
        ps.setTotalPages(Integer.MAX_VALUE);
        response.setPage(ps);
        return response;
    }

}
