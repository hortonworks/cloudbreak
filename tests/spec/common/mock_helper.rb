require 'rest-client'
require 'json'
require_relative "../integration/spec_helper"

class MockHelper
    @@base_url = ENV['BASE_URL']
    @@cloudbreak_base = "cb"
    @@api_base = "#{@@cloudbreak_base}/api"
    @@reset_trace_endpoint = "#{@@base_url}/#{@@api_base}/resettrace"
    @@reset_mock_endpoint = "#{@@base_url}/#{@@api_base}/reset"
    @@setup_endpoint = "#{@@base_url}/#{@@api_base}/setup"
    @@get_trace_endpoint = "#{@@base_url}/#{@@api_base}/trace"

    def self.resetTrace()
        RestClient::Request.execute(
            :url => @@reset_trace_endpoint,
            :method => :post,
            :verify_ssl => false
        )
    end

    def self.resetMock()
        resetTrace()
        RestClient::Request.execute(
            :url => @@reset_mock_endpoint,
            :method => :post,
            :verify_ssl => false
        )
    end

    def self.getTrace()
        response = RestClient::Request.execute(
            :headers => {:accept => 'application/json', :content_type => 'application/json'},
            :url => @@get_trace_endpoint,
            :method => :get,
            :verify_ssl => false
        )

        JSON.parse(response.body)
    end

    def self.setupResponse(operationId, response, statusCode = 200)
        requestBody = {
            :operationid => operationId,
            :responses => [{:response => response, :statusCode => statusCode}]
        }

        RestClient::Request.execute(
            :headers => {:accept => 'application/json', :content_type => 'application/json'},
            :url => @@setup_endpoint,
            :method => :post,
            :payload => requestBody.to_json,
            :verify_ssl => false
        )
    end

    def self.getRequestDiff(expectedResponse)
        actualTrace = getTrace()
        prettyJsonActualTrace = JSON.pretty_generate(actualTrace)
        prettyJsonExpectedResponse = JSON.pretty_generate(expectedResponse)
        hashActualTraceValue = JSON.parse(prettyJsonActualTrace)[1]["params"]["body"]["value"]
        hashActualTraceURL = JSON.parse(prettyJsonActualTrace)[1]["url"]
        hashExpectedResponseValue = JSON.parse(prettyJsonExpectedResponse)["sentValue"]
        hashExpectedResponseURL = JSON.parse(prettyJsonExpectedResponse)["calledEndpoint"]

        # For debugging purposes
        #puts "Pretty JSON Actual Trace: #{prettyJsonActualTrace}"
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
        #puts "Pretty JSON Actual Response: #{prettyJsonActualResponse}"
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
