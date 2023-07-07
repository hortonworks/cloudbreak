package com.sequenceiq.cloudbreak.jvm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VmFlagsParserTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    private static final String VM_FLAGS = """
            [Global flags]
                  int ActiveProcessorCount                     = -1                                        {product} {default}
                uintx AdaptiveSizeDecrementScaleFactor         = 4                                         {product} {default}
                uintx AdaptiveSizeMajorGCDecayTimeScale        = 10                                        {product} {default}
            """;
    // CHECKSTYLE:ON
    // @formatter:on

    @InjectMocks
    private VmFlagsParser underTest;

    @Test
    void testParseVmFlags() {
        List<String> vmFlags = underTest.parseVmFlags(VM_FLAGS);
        assertThat(vmFlags).containsExactly("[Globalflags]", "intActiveProcessorCount=-1", "uintxAdaptiveSizeDecrementScaleFactor=4",
                "uintxAdaptiveSizeMajorGCDecayTimeScale=10");
    }

}