package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_ID;
import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.model.AccountAwareResource;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;

@ExtendWith(MockitoExtension.class)
public class DataCollectorComponentTest {

    @InjectMocks
    private RepositoryBasedDataCollector underTest;

    private Map<String, AccountAwareResourceRepository<?, ?>> pathRepositoryMap = new HashMap<>();

    private final String userCrn = CrnTestUtil.getUserCrnBuilder()
            .setResource("res")
            .setAccountId("acc")
            .build()
            .toString();

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "pathRepositoryMap", pathRepositoryMap);
    }

    @Test
    public void testFetchDataFromDbIfNeedWhenNameAndCrnAreNull() {
        AccountAwareResourceRepository<?, ?> repo = mock(AccountAwareResourceRepository.class);
        pathRepositoryMap.put("key", repo);
        Map<String, String> params = new HashMap<>();
        params.put(RESOURCE_CRN, null);
        params.put(RESOURCE_NAME, null);
        ThreadBasedUserCrnProvider.doAs(userCrn, () -> underTest.fetchDataFromDbIfNeed(params));
        assertNull(params.get(RESOURCE_NAME));
        assertNull(params.get(RESOURCE_CRN));
        assertNull(params.get(RESOURCE_ID));
    }

    @Test
    public void testFetchDataFromDbIfNeedWhenNameNotNullAndCrnIsNullButNotFound() {
        AccountAwareResourceRepository<?, ?> repo = mock(AccountAwareResourceRepository.class);
        pathRepositoryMap.put("key", repo);
        Map<String, String> params = new HashMap<>();
        params.put(RESOURCE_CRN, null);
        params.put(RESOURCE_NAME, "name");
        when(repo.findResourceBasicViewByNameAndAccountId("name", "acc")).thenReturn(Optional.empty());
        ThreadBasedUserCrnProvider.doAs(userCrn, () -> underTest.fetchDataFromDbIfNeed(params));
        assertEquals("name", params.get(RESOURCE_NAME));
        assertNull(params.get(RESOURCE_CRN));
        assertNull(params.get(RESOURCE_ID));

        verify(repo).findResourceBasicViewByNameAndAccountId("name", "acc");
    }

    @Test
    public void testFetchDataFromDbIfNeedWhenNameNotNullAndCrnIsNullAndFound() {
        AccountAwareResourceRepository<AccountAwareResource, Long> repo = mock(AccountAwareResourceRepository.class);
        ResourceBasicView resource = mock(ResourceBasicView.class);
        pathRepositoryMap.put("key", repo);
        Map<String, String> params = new HashMap<>();
        params.put(RESOURCE_CRN, null);
        params.put(RESOURCE_NAME, "name");
        Optional<ResourceBasicView> entityOpt = Optional.of(resource);

        when(resource.getResourceCrn()).thenReturn("crn-ret");
        when(resource.getId()).thenReturn(342L);
        when(repo.findResourceBasicViewByNameAndAccountId("name", "acc")).thenReturn(entityOpt);

        ThreadBasedUserCrnProvider.doAs(userCrn, () -> underTest.fetchDataFromDbIfNeed(params));
        assertEquals("name", params.get(RESOURCE_NAME));
        assertEquals("crn-ret", params.get(RESOURCE_CRN));
        assertEquals("342", params.get(RESOURCE_ID));

        verify(repo).findResourceBasicViewByNameAndAccountId("name", "acc");
    }

    @Test
    public void testFetchDataFromDbIfNeedWhenCrnNotNullAndNameIsNullButNotFound() {
        AccountAwareResourceRepository<?, ?> repo = mock(AccountAwareResourceRepository.class);
        pathRepositoryMap.put("key", repo);
        Map<String, String> params = new HashMap<>();
        params.put(RESOURCE_CRN, "crn");
        params.put(RESOURCE_NAME, null);
        when(repo.findResourceBasicViewByResourceCrn("crn")).thenReturn(Optional.empty());
        ThreadBasedUserCrnProvider.doAs(userCrn, () -> underTest.fetchDataFromDbIfNeed(params));
        assertNull(params.get(RESOURCE_NAME));
        assertEquals("crn", params.get(RESOURCE_CRN));
        assertNull(params.get(RESOURCE_ID));

        verify(repo).findResourceBasicViewByResourceCrn("crn");
    }

    @Test
    public void testFetchDataFromDbIfNeedWhenCrnNotNullAndNameIsNullAndFound() {
        AccountAwareResourceRepository<AccountAwareResource, Long> repo = mock(AccountAwareResourceRepository.class);
        ResourceBasicView resource = mock(ResourceBasicView.class);
        Optional<ResourceBasicView> entityOpt = Optional.of(resource);
        pathRepositoryMap.put("key", repo);

        Map<String, String> params = new HashMap<>();
        params.put(RESOURCE_CRN, "crn");
        params.put(RESOURCE_NAME, null);

        when(resource.getName()).thenReturn("name-ret");
        when(resource.getId()).thenReturn(342L);
        when(repo.findResourceBasicViewByResourceCrn("crn")).thenReturn(entityOpt);
        ThreadBasedUserCrnProvider.doAs(userCrn, () -> underTest.fetchDataFromDbIfNeed(params));

        assertEquals("name-ret", params.get(RESOURCE_NAME));
        assertEquals("crn", params.get(RESOURCE_CRN));
        assertEquals("342", params.get(RESOURCE_ID));

        verify(repo).findResourceBasicViewByResourceCrn("crn");
    }
}
