package com.sequenceiq.it.verification

import java.util.HashMap

import spark.Request

class Call private constructor() {

    /**
     * Http method
     */
    var method: String? = null
        private set
    /**
     * URI of the call
     */
    var uri: String? = null
        private set
    /**
     * Content type of the call
     */
    var contentType: String? = null
        private set
    /**
     * In case of POST request - returns post body
     */
    var postBody: String? = null
        private set
    var url: String? = null
        private set
    private val headers = HashMap<String, String>()
    /**
     * Map of parameters. All parameters considered as if they were multi-valued.
     */
    var parameters: Map<String, String> = HashMap()
        private set
    /**
     * Returns raw HTTP request
     */
    var request: Request? = null
        private set

    /**
     * Map of headers
     */
    fun getHeaders(): Map<String, String> {
        return headers
    }

    override fun toString(): String {
        return "Call{"
        +"url='" + url + '\''
        +", uri='" + uri + '\''
        +", contentType='" + contentType + '\''
        +", postBody='" + postBody + '\''
        +", method='" + method + '\''
        +", parameters=" + parameters
        +'}'
    }

    companion object {

        /**
         * Factory method
         */
        fun fromRequest(request: Request): Call {
            val call = Call()

            call.request = request
            call.method = request.requestMethod()
            call.uri = request.uri()
            call.contentType = request.contentType()
            call.url = request.url()

            for (s in request.headers()) {
                call.headers.put(s, request.headers(s))
            }

            call.parameters = HashMap(request.params())

            call.postBody = request.body()

            return call
        }
    }
}
