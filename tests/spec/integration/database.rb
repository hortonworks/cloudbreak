require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "../common/response_helpers"
require_relative "spec_helper"


define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.xdescribe 'Database test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"   

  after(:all) do
    MockResponse.reset(ENV['BASE_URL'] + @mock_endpoint_reset)
  end

  it "Database - Create - Mysql" do 
    with_environment 'DEBUG' => '1' do
      result = cb.database.create.mysql.name(@db_name).url(@db_url).db_username(@db_user).db_password(@mock_password).type("Hive").build(false)
      expect(result.stderr.to_s.downcase).not_to include("failed", "error")
    end 
  end

  it "Database - Create - Oracle11" do 
    with_environment 'DEBUG' => '1' do
      result = cb.database.create.oracle11.name(@db_name).url(@db_url).db_username(@db_user).db_password(@mock_password).type("Hive").build(false)
      expect(result.stderr.to_s.downcase).not_to include("failed", "error") 
    end 
  end

   it "Database - Create - Oracle12" do 
    with_environment 'DEBUG' => '1' do
      result = cb.database.create.oracle12.name(@db_name).url(@db_url).db_username(@db_user).db_password(@mock_password).type("Hive").build(false)
      expect(result.stderr.to_s.downcase).not_to include("failed", "error") 
    end 
  end

   it "Database - Create - Postgres" do 
    with_environment 'DEBUG' => '1' do
      result = cb.database.create.postgres.name(@db_name).url(@db_url).db_username(@db_user).db_password(@mock_password).type("Hive").build(false)
      expect(result.stderr.to_s.downcase).not_to include("failed", "error") 
    end 
  end

  it "Database - Delete" do
    with_environment 'DEBUG' => '1' do    
      result = cb.database.delete.name(@db_name).build
      expect(result.exit_status).to eql 0 
    end   
  end

  it "Database - List" do
    result = cb.database.list.build(false)
    expect(result.stdout.empty?).to be_falsy, "Json should't be empty"

    JSON.parse(result.stdout).each do |s|    
        expect(s).to include_json(
          Name: /.*/,
          ConnectionURL: /.*/,  
          DatabaseEngine: /.*/,
          Type: /.*/,
          Driver: /.*/
      )
    end        
  end

end
