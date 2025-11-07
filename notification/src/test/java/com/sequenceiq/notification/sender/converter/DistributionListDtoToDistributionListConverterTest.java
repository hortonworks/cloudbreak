package com.sequenceiq.notification.sender.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.notification.client.dto.DistributionListDto;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.DistributionListManagementType;

public class DistributionListDtoToDistributionListConverterTest {

    private final DistributionListDtoToDistributionListConverter underTest = new DistributionListDtoToDistributionListConverter();

    @Test
    @DisplayName("convert null dto -> null (default type path)")
    void testConvertNullDtoDefaultPath() {
        assertNull(underTest.convert((DistributionListDto) null));
    }

    @Test
    @DisplayName("convert dto -> domain with default USER_MANAGED type")
    void testConvertDefaultType() {
        DistributionListDto dto = new DistributionListDto("dl-123", "crn:resource:1");
        DistributionList domain = underTest.convert(dto);
        assertNotNull(domain);
        assertEquals("crn:resource:1", domain.getResourceCrn());
        assertEquals("dl-123", domain.getExternalDistributionListId());
        assertEquals(DistributionListManagementType.USER_MANAGED, domain.getType());
    }

    @Test
    @DisplayName("convert(dto,null type) -> domain with default USER_MANAGED")
    void testConvertNullTypeUsesDefault() {
        DistributionListDto dto = new DistributionListDto("dl-456", "crn:resource:2");
        DistributionList domain = underTest.convert(dto, null);
        assertNotNull(domain);
        assertEquals("crn:resource:2", domain.getResourceCrn());
        assertEquals("dl-456", domain.getExternalDistributionListId());
        assertEquals(DistributionListManagementType.USER_MANAGED, domain.getType());
    }

    @Test
    @DisplayName("convert(dto,SYSTEM_MANAGED) -> domain with provided type")
    void testConvertExplicitType() {
        DistributionListDto dto = new DistributionListDto("dl-789", "crn:resource:3");
        DistributionList domain = underTest.convert(dto, DistributionListManagementType.SYSTEM_MANAGED);
        assertNotNull(domain);
        assertEquals("crn:resource:3", domain.getResourceCrn());
        assertEquals("dl-789", domain.getExternalDistributionListId());
        assertEquals(DistributionListManagementType.SYSTEM_MANAGED, domain.getType());
    }

    @Test
    @DisplayName("convert(dto,type) null dto -> null")
    void testConvertNullDtoExplicitPath() {
        assertNull(underTest.convert(null, DistributionListManagementType.SYSTEM_MANAGED));
    }
}

