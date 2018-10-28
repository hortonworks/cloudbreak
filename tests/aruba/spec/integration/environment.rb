require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "../common/response_helpers"
require_relative "spec_helper"


define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Environment test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"   

  after(:all) do
    MockResponse.reset(ENV['BASE_URL'] + @mock_endpoint_reset)
  end

  it "Environment - List" do
    with_environment 'DEBUG' => '1' do
      result = cb.environment.list.build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy  
    end
  end
  
  it "Environment - Create" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('create', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.environment.create.name("test-env").credential("test-cred").regions("test").build(false)
      expect(result.exit_status).to eql 0
    end
  end

  it "Environment - Describe" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('get', load_json(@env_single_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)
      result = cb.environment.create.name("test-env").build(false)
      expect(result.exit_status).to eql 0
    end
  end      
end  