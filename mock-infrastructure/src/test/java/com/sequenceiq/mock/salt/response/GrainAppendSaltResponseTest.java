package com.sequenceiq.mock.salt.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.mock.salt.SaltStoreService;

@ExtendWith(MockitoExtension.class)
class GrainAppendSaltResponseTest {

    private static final String MOCK_UUID = "uuid";

    private static final String HOST = "host-1";

    private static final String KEY = "roles";

    private static final String VALUE = "ipa_member";

    @Mock
    private SaltStoreService saltStoreService;

    @InjectMocks
    private GrainAppendSaltResponse underTest;

    private Map<String, Multimap<String, String>> grains;

    @BeforeEach
    void setUp() {
        grains = new ConcurrentHashMap<>();
        Multimap<String, String> grainsForHost = ArrayListMultimap.create();
        grainsForHost.put(KEY, VALUE);
        grains.put(HOST, grainsForHost);
        when(saltStoreService.getGrains(MOCK_UUID)).thenReturn(grains);
        ReflectionTestUtils.setField(underTest, "objectMapper", new ObjectMapper());
    }

    @Test
    void appendingAnAlreadyPresentValueDoesNotCreateDuplicate() throws Exception {
        Map<String, List<String>> params = Map.of(
                "tgt", List.of(HOST),
                "arg", List.of(KEY, VALUE));

        underTest.run(MOCK_UUID, params);
        underTest.run(MOCK_UUID, params);
        underTest.run(MOCK_UUID, params);

        assertThat(grains.get(HOST).get(KEY)).containsExactly(VALUE);
    }

    @Test
    void appendingAppendsDistinctValues() throws Exception {
        underTest.run(MOCK_UUID, Map.of("tgt", List.of(HOST), "arg", List.of(KEY, "gateway")));

        assertThat(grains.get(HOST).get(KEY)).containsExactlyInAnyOrder(VALUE, "gateway");
    }
}
