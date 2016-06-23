package com.sequenceiq.cloudbreak.converter

import java.util.ArrayList

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterRegistry

abstract class AbstractConversionServiceAwareConverter<S, T> : Converter<S, T> {
    @Inject
    @Qualifier("conversionService")
    val conversionService: ConversionService? = null

    @PostConstruct
    private fun register() {
        if (conversionService is ConverterRegistry) {
            conversionService.addConverter(this)
        } else {
            throw IllegalStateException("Can't register Converter to ConverterRegistry")
        }
    }

    override fun convert(source: S): T {
        return convert(source)
    }

    fun convert(sources: Collection<S>?): List<T> {
        val targets = ArrayList<T>()
        if (sources != null) {
            for (source in sources) {
                targets.add(convert(source))
            }
        }
        return targets
    }
}
