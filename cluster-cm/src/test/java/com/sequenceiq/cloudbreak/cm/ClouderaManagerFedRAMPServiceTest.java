package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.LOGIN_BANNER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.model.ApiConfigEnforcement;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerFedRAMPServiceTest {

    @InjectMocks
    private ClouderaManagerFedRAMPService underTest;

    @Test
    public void testGetApiConfigEnforcements() {
        ReflectionTestUtils.setField(underTest, "banner", "banner_text");

        List<ApiConfigEnforcement> result = underTest.getApiConfigEnforcements();

        assertThat(result).hasSize(1);
        assertThat(result).extracting(ApiConfigEnforcement::getLabel)
                .hasSameElementsAs(List.of(LOGIN_BANNER));
        assertThat(result).filteredOn(enforcement -> LOGIN_BANNER.equals(enforcement.getLabel()))
                .extracting(ApiConfigEnforcement::getDefaultValue)
                .containsExactly("banner_text");
    }
}