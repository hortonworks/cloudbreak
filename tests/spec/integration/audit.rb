require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "../common/response_helpers"
require_relative "spec_helper"


define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.xdescribe 'Audit test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"   

  after(:all) do
    MockResponse.reset(ENV['BASE_URL'] + @mock_endpoint_reset)
  end

  it "Audit - List events" do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('getAuditEventsInWorkspace', load_json(@audit_json), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)

      result = cb.audit.list.cluster.resource_id('328').build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy  
    end
  end

  it "Audit - Describe audit entry identified by Audit ID " do
    with_environment 'DEBUG' => '1' do
      requestBody = MockResponse.requestBodyCreate('getAuditEventByWorkspace', load_json(@audit_json_single), '200')
      url = ENV['BASE_URL'] + @mock_endpoint_setup
      MockResponse.post(requestBody, url)

      result = cb.audit.describe.audit_id('328').build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy  
    end
  end

  it "Audit - Describe audit entry identified by Audit ID - Invalid format" do
    with_environment 'DEBUG' => '1' do
      result = cb.audit.describe.audit_id('test').build(false)
      expect(result.exit_status).to eql 1
      expect(result.stdout.empty?).to be_truthy  
    end
  end      

  it "Audit - List events - Invalid format of Resource ID" do
    with_environment 'DEBUG' => '1' do
      result = cb.audit.list.cluster.resource_id('test').build(false)
      expect(result.exit_status).to eql 1
      expect(result.stdout.empty?).to be_truthy  
    end
  end      
end  