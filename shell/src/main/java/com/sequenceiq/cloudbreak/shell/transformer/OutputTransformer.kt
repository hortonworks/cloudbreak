package com.sequenceiq.cloudbreak.shell.transformer

import javax.inject.Inject
import javax.ws.rs.NotSupportedException

import org.springframework.stereotype.Component

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.shell.model.OutPutType
import com.sequenceiq.cloudbreak.shell.support.JsonRenderer
import com.sequenceiq.cloudbreak.shell.support.TableRenderer

@Component
class OutputTransformer {

    @Inject
    private val tableRenderer: TableRenderer? = null
    @Inject
    private val jsonRenderer: JsonRenderer? = null

    @Throws(JsonProcessingException::class)
    fun <O : Any> render(outPutType: OutPutType, `object`: O, vararg headers: String): String {
        if (OutPutType.JSON == outPutType) {
            return jsonRenderer!!.render(`object`)
        } else if (OutPutType.RAW == outPutType) {
            if (`object` is Map<Any, Any>) {
                if (!`object`.values.isEmpty()) {
                    if (`object`.values.toArray()[0] is Collection<Any>) {
                        return tableRenderer!!.renderMultiValueMap(`object` as Map<String, List<String>>, true, *headers)
                    } else if (`object`.values.toArray()[0] is String) {
                        return tableRenderer!!.renderSingleMapWithSortedColumn(`object` as Map<Any, String>, *headers)
                    } else {
                        return tableRenderer!!.renderObjectValueMap(`object` as Map<String, Any>, headers[0])
                    }
                } else {
                    return "No available entity"
                }
            } else {
                return "No available entity"
            }
        } else {
            throw NotSupportedException("Output type not supported.")
        }
    }

    @Throws(JsonProcessingException::class)
    fun <O : Any> render(`object`: O, vararg headers: String): String {
        return render(OutPutType.RAW, `object`, *headers)
    }

}
