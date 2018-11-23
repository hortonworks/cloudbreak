require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "../common/mock_helper"
require_relative "../common/trace_response_builder"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Users test cases', :type => :aruba, :feature => "Users", :severity => :normal do
  include_context "shared command helpers"
  include_context "mock shared vars"

  before(:each) do
    MockHelper.resetMock()
  end

  before(:step) do |step|
    puts "Before step #{step.current_step}"
  end

  it "User - List", :story => "List Users", :severity => :critical, :testId => 5 do |test|
    with_environment 'DEBUG' => '1' do
      test.step "step1" do
        @responseHash = MockHelper.getResponseHash("../../../responses/users/users.json")

        @expectedEndpointResponse = TraceResponseBuilder.getAllUsersResponseFactory(@responseHash)
        MockHelper.setupResponse("getAllUsers", @responseHash)
      end

      test.step "step2" do
        @result = cb.user.list.build(false)
        @resultHash = MockHelper.getResultHash(@result.output, true)
      end

      test.step "step3" do
        expect(@result.exit_status).to eql(0), "expected 0 exit status, got #{@result.exit_status}"
        expect(@result.stderr.to_s.downcase).not_to include("error"), "expected no ERROR in result, got #{@result.stderr.to_s.downcase}"
        expect(MockHelper.getResponseDiff(@expectedEndpointResponse, @resultHash)).to be_truthy, "expected response should be, got #{JSON.pretty_generate(@resultHash)}"
      end
    end
  end
end