package com.sequenceiq.cloudbreak.service.stack

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.MockitoAnnotations
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class StackServiceTest {

    @InjectMocks
    private var stackService: StackService? = null

    @Before
    fun before() {
        stackService = StackService()
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testLogger() {
        val t = IllegalStateException("mamamama")
        LOGGER.error("test. Ex: {}", 12, t)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StackServiceTest::class.java)
    }

}
