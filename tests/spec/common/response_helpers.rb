require "integration/spec_helper"

class MockResponse
  def self.post(requestBody, url)
    response = RestClient::Request.execute(
      :headers => {:accept => 'application/json', :content_type => 'application/json'},
      :url => url,
      :method => :post,
      :payload => requestBody.to_json,
      :verify_ssl => false
    )
  end

  def self.reset(url)
    response = RestClient::Request.execute(
      :url => url,
      :method => :post,
      :verify_ssl => false
    )  
  end

  def self.requestBodyCreate(operation_id, response, status_code)
    if response.is_a? String
      response = JSON.parse(response)
    end
    requestBody = {
      :operationid => operation_id,
      :responses => [{:response => response, :statusCode => status_code}]
        }
  end  
end