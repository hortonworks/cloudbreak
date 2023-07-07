package com.sequenceiq.cloudbreak.jvm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NativeMemoryParserTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    private static final String VM_NATIVE_MEMORY = """
            Native Memory Tracking:
            
            (Omitting categories weighting less than 1KB)
            
            Total: reserved=10351844KB, committed=801708KB
                   malloc: 111392KB #969179
                   mmap:   reserved=10240452KB, committed=690316KB
            
            -                 Java Heap (reserved=8388608KB, committed=401408KB)
                                        (mmap: reserved=8388608KB, committed=401408KB)
            
            -                     Class (reserved=1050964KB, committed=18900KB)
                                        (classes #26546)
                                        (  instance classes #24936, array classes #1610)
                                        (malloc=2388KB #56982)
                                        (mmap: reserved=1048576KB, committed=16512KB)
                                        (  Metadata:   )
                                        (    reserved=131072KB, committed=112576KB)
                                        (    used=112276KB)
                                        (    waste=300KB =0.27%)
                                        (  Class space:)
                                        (    reserved=1048576KB, committed=16512KB)
                                        (    used=16156KB)
                                        (    waste=356KB =2.16%)
            
            -                    Thread (reserved=63651KB, committed=63651KB)
                                        (thread #63)
                                        (stack: reserved=63488KB, committed=63488KB)
                                        (malloc=91KB #444)
                                        (arena=72KB #123)
            """;
    // CHECKSTYLE:ON
    // @formatter:on

    @InjectMocks
    private NativeMemoryParser underTest;

    @Test
    void testParseVmNativeMemory() {
        List<MemoryCategory> categories = underTest.parseVmNativeMemory(VM_NATIVE_MEMORY);
        assertThat(categories).hasSize(4);
        assertThat(categories)
                .extracting(MemoryCategory::name, MemoryCategory::reserved, MemoryCategory::committed)
                .containsExactly(
                        tuple("Total", Double.valueOf(10351844), Double.valueOf(801708)),
                        tuple("Java Heap", Double.valueOf(8388608), Double.valueOf(401408)),
                        tuple("Class", Double.valueOf(1050964), Double.valueOf(18900)),
                        tuple("Thread", Double.valueOf(63651), Double.valueOf(63651)));
    }

}