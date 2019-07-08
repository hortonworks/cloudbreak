require 'rest-client'
require 'json'
require_relative "../integration/spec_helper"

class MockHelper
    @@cb_base_url = ENV['CB_BASE_URL']
    @@dl_base_url = ENV['DL_BASE_URL']
    @@env_base_url = ENV['ENV_BASE_URL']
    @@cb_base = "cb"
    @@env_base = "environmentservice"
    @@dl_base = "dl"
    @@cb_api_base = "#{@@cb_base}/api"
    @@dl_api_base = "#{@@dl_base}/api"
    @@env_api_base = "#{@@env_base}/api"
    @@cb_endpoint = "#{@@cb_base_url}/#{@@cb_api_base}"
    @@dl_endpoint = "#{@@dl_base_url}/#{@@dl_api_base}"
    @@env_endpoint = "#{@@env_base_url}/#{@@env_api_base}"
    @@reset_cb_trace_endpoint = "#{@@cb_endpoint}/resettrace"
    @@reset_dl_trace_endpoint = "#{@@dl_endpoint}/resettrace"
    @@reset_env_trace_endpoint = "#{@@env_endpoint}/resettrace"
    @@reset_cb_endpoint = "#{@@cb_endpoint}/reset"
    @@reset_dl_endpoint = "#{@@dl_endpoint}/reset"
    @@reset_env_endpoint = "#{@@env_endpoint}/reset"
    @@setup_cb_endpoint = "#{@@cb_endpoint}/setup"
    @@setup_dl_endpoint = "#{@@dl_endpoint}/setup"
    @@setup_env_endpoint = "#{@@env_endpoint}/setup"
    @@get_cb_trace_endpoint = "#{@@cb_endpoint}/trace"
    @@get_dl_trace_endpoint = "#{@@dl_endpoint}/trace"
    @@get_env_trace_endpoint = "#{@@env_endpoint}/trace"

    def self.resetTrace(service)
        case service
        when "cb"
            puts "Reset CB trace: #{@@reset_cb_trace_endpoint}"

            RestClient::Request.execute(
                :url => @@reset_cb_trace_endpoint,
                :method => :post,
                :verify_ssl => false
            )
        when "dl"
            puts "Reset DATALAKE trace: #{@@reset_dl_trace_endpoint}"

            RestClient::Request.execute(
                :url => @@reset_dl_trace_endpoint,
                :method => :post,
                :verify_ssl => false
            )
        when "env"
            puts "Reset ENVIRONMENT trace: #{@@reset_env_trace_endpoint}"

            RestClient::Request.execute(
                :url => @@reset_env_trace_endpoint,
                :method => :post,
                :verify_ssl => false
            )
        else
          "#{service} - Is not recognized as a service"
        end
    end

    def self.resetMock(service)
        case service
        when "cb"
            puts "Reset CB endpoint: #{@@reset_cb_endpoint}"

            resetTrace(service)
            RestClient::Request.execute(
                :url => @@reset_cb_endpoint,
                :method => :post,
                :verify_ssl => false
            )
        when "dl"
            puts "Reset DATALAKE endpoint: #{@@reset_dl_endpoint}"

            resetTrace(service)
            RestClient::Request.execute(
                :url => @@reset_dl_endpoint,
                :method => :post,
                :verify_ssl => false
            )
        when "env"
            puts "Reset ENVIRONMENT endpoint: #{@@reset_env_endpoint}"

            resetTrace(service)
            RestClient::Request.execute(
                :url => @@reset_env_endpoint,
                :method => :post,
                :verify_ssl => false
            )
        else
          "#{service} - Is not recognized as a service"
        end
    end

    def self.getTrace(service)
        case service
        when "cb"
            puts "Get CB trace: #{@@get_cb_trace_endpoint}"

            response = RestClient::Request.execute(
                :headers => {:accept => 'application/json', :content_type => 'application/json'},
                :url => @@get_cb_trace_endpoint,
                :method => :get,
                :verify_ssl => false
            )

            JSON.parse(response.body)
        when "dl"
            puts "Get DATALAKE trace: #{@@get_dl_trace_endpoint}"

            response = RestClient::Request.execute(
                :headers => {:accept => 'application/json', :content_type => 'application/json'},
                :url => @@get_dl_trace_endpoint,
                :method => :get,
                :verify_ssl => false
            )

            JSON.parse(response.body)
        when "env"
            puts "Get ENVIRONMENT trace: #{@@get_env_trace_endpoint}"

            response = RestClient::Request.execute(
                :headers => {:accept => 'application/json', :content_type => 'application/json'},
                :url => @@get_env_trace_endpoint,
                :method => :get,
                :verify_ssl => false
            )

            JSON.parse(response.body)
        else
          "#{service} - Is not recognized as a service"
        end
    end

    def self.setupResponse(service, operationId, response, statusCode = 200)
        case service
        when "cb"
            puts "Setup CB endpoint: #{@@setup_cb_endpoint}"

            requestBody = {
                :operationid => operationId,
                :responses => [{:response => response, :statusCode => statusCode}]
            }

            RestClient::Request.execute(
                :headers => {:accept => 'application/json', :content_type => 'application/json'},
                :url => @@setup_cb_endpoint,
                :method => :post,
                :payload => requestBody.to_json,
                :verify_ssl => false
            )
        when "dl"
            puts "Setup DATALAKE endpoint: #{@@setup_dl_endpoint}"

            requestBody = {
                :operationid => operationId,
                :responses => [{:response => response, :statusCode => statusCode}]
            }

            RestClient::Request.execute(
                :headers => {:accept => 'application/json', :content_type => 'application/json'},
                :url => @@setup_dl_endpoint,
                :method => :post,
                :payload => requestBody.to_json,
                :verify_ssl => false
            )
        when "env"
            puts "Setup ENVIRONMENT endpoint: #{@@setup_env_endpoint}"

            requestBody = {
                :operationid => operationId,
                :responses => [{:response => response, :statusCode => statusCode}]
            }

            RestClient::Request.execute(
                :headers => {:accept => 'application/json', :content_type => 'application/json'},
                :url => @@setup_env_endpoint,
                :method => :post,
                :payload => requestBody.to_json,
                :verify_ssl => false
            )
        else
          "#{service} - Is not recognized as a service"
        end
    end

    def self.getRequestDiff(expectedResponse)
        actualTrace = getTrace()
        prettyJsonActualTrace = JSON.pretty_generate(actualTrace)
        prettyJsonExpectedResponse = JSON.pretty_generate(expectedResponse)
        hashActualTraceValue = JSON.parse(prettyJsonActualTrace)[-1]["params"]["body"]["value"]
        hashActualTraceURL = JSON.parse(prettyJsonActualTrace)[-1]["url"]
        hashExpectedResponseValue = JSON.parse(prettyJsonExpectedResponse)["sentValue"]
        hashExpectedResponseURL = JSON.parse(prettyJsonExpectedResponse)["calledEndpoint"]

        # For debugging purposes
        puts "Pretty JSON Actual Trace: #{prettyJsonActualTrace}"
        #puts "Pretty JSON Expected Response: #{prettyJsonExpectedResponse}"

        isEndpointCorrect = hashActualTraceURL.include? hashExpectedResponseURL
        if !isEndpointCorrect
            puts "Actual Trace and Expected URL are not equal!"
            puts "Hash Actual Trace URL: #{hashActualTraceURL}"
            puts "Hash Expected URL: #{hashExpectedResponseURL}"
        end

        isTraceCorrect = (hashActualTraceValue.values - hashExpectedResponseValue.values).empty?
        if !isTraceCorrect
            puts "Actual Trace and Expected Values are not equal!"
            puts "Hash Actual Trace Value: #{hashActualTraceValue.sort_by { |_, trace| -trace }}"
            puts "Hash Expected Response Value: #{hashExpectedResponseValue.sort_by { |_, response| -response }}"
        end
        return isEndpointCorrect && isTraceCorrect
    end

    def self.getResponseDiff(expectedResponse, actualResponse)
        prettyJsonActualResponse = JSON.pretty_generate(actualResponse)
        prettyJsonExpectedResponse = JSON.pretty_generate(expectedResponse)

        hashActualResponse = JSON.parse(prettyJsonActualResponse)
        hashExpectedResponse = JSON.parse(prettyJsonExpectedResponse)["receivedValue"]
        if !hashExpectedResponse.respond_to?(:values)
            hashExpectedResponse = JSON.parse(prettyJsonExpectedResponse)["receivedValue"][0]
        end
        if !hashActualResponse.respond_to?(:values)
            hashActualResponse = JSON.parse(prettyJsonActualResponse)[0]
        end

        # For debugging purposes
        puts "Pretty JSON Actual Response: #{prettyJsonActualResponse}"
        #puts "Pretty JSON Expected Response: #{prettyJsonExpectedResponse}"

        isResponseCorrect = (hashActualResponse.values - hashExpectedResponse.values).empty?
        if !isResponseCorrect
            puts "Actual and Expected Values are not equal!"
            puts "Hash Actual Response: #{hashActualResponse.sort_by { |_, actual| -actual }}"
            puts "Hash Expected Response: #{hashExpectedResponse.sort_by { |_, expected| -expected }}"
        end
        return isResponseCorrect
    end

    def self.getResponseHash(relativePath)
        responseJson = File.read(File.expand_path(relativePath, __dir__))

        JSON.parse(responseJson)
    end

    def self.getResultHash(resultOutput, isArray = false)
        resultJson = "#{resultOutput}".match( /{.+}/ )[0]

        if isArray
            JSON.parse("[#{resultJson}]")
        else
            JSON.parse(resultJson)
        end
    end
end
