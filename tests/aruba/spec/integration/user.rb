require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "../common/mock_helper"
require_relative "../common/trace_response_builder"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Users test cases', :type => :aruba, :feature => "Users", :severity => :normal do
  include_context "shared command helpers"
  include_context "mock shared vars"

  before(:each) do
    MockHelper.resetMock()
  end

  it "User - List", :story => "List Users", :severity => :critical, :testId => 5 do
    with_environment 'DEBUG' => '1' do
      responseHash = MockHelper.getResponseHash("../../../responses/users/users.json")

      expectedEndpointResponse = TraceResponseBuilder.getAllUsersResponseFactory(responseHash)
      MockHelper.setupResponse("getAllUsers", responseHash)

      result = cb.user.list.build(false)
      resultHash = MockHelper.getResultHash(result.output, true)

      expect(result.exit_status).to eql 0
      expect(result.stderr.to_s.downcase).not_to include("error")
      expect(MockHelper.getResponseDiff(expectedEndpointResponse, resultHash)).to be_truthy
    end
  end
end