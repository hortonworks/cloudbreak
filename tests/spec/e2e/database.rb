require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Database test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "e2e shared vars"

  before(:all) do
    result = list_with_name_exists(@db_name) do
      cb.database.list.build
    end
    if (result[0])
      result = cb.database.delete.name(@db_name).build
      expect(result.exit_status).to eql 0 
    end
  end
  
  it "Database - Create - Mysql" do 
    result = cb.database.create.mysql.name(@db_name).url(ENV['INTEGRATIONTEST_RDSCONFIG_RDSCONNECTIONURL'])
    .db_username(ENV['INTEGRATIONTEST_RDSCONFIG_RDSUSER']).db_password(ENV['INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD']).type("Hive").builds  
    expect(result.exit_status).to eql 0 
  end 

  it "Database - List - Checking previosly created db" do
    result = list_with_name_exists(@db_name) do
      cb.database.list.build
    end
    expect(result[0]).to be_truthy

    result[1].each do |s|    
      expect(s).to include_json(
        Name: /.*/,
        ConnectionURL: /.*/,  
        DatabaseEngine: /.*/,
        Type: /.*/,
        Driver: /.*/
      )
    end        
  end 

  xit "Database - Test - By name - Previously created db" do
    with_environment 'DEBUG' => '1' do       
      result = cb.database.test.by_name.name(@db_name).builds
      expect(result.exit_status).to eql 0 
      expect(result.stderr.to_s.downcase).to include("connected")      
      expect(result.stderr.to_s.downcase).not_to include("failed")
    end 
  end  

  xit "Database - Test - By parameters" do
    with_environment 'DEBUG' => '1' do   
      result = cb.database.test.by_params.url(ENV['INTEGRATIONTEST_RDSCONFIG_RDSCONNECTIONURL'])
      .db_username(ENV['INTEGRATIONTEST_RDSCONFIG_RDSUSER']).db_password(ENV['INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD']).type("Hive").builds  
      expect(result.exit_status).to eql 0 
      expect(result.stderr.to_s.downcase).to include("connected")      
      expect(result.stderr.to_s.downcase).not_to include("failed")     
    end 
  end

  it "Database - Delete - Previously created db" do
    result = cb.database.delete.name(@db_name).build
    expect(result.exit_status).to eql 0    
  end 

  it "Database - Create - Invalid url - No jdbc protocol" do 
      result = cb.database.create.mysql.name(@db_name).url("postgresql://test:99/mock").db_username(ENV['INTEGRATIONTEST_RDSCONFIG_RDSUSER']).db_password(ENV['INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD']).type("Hive").build       
      expect(result.exit_status).to eql 1 
      expect(result.stderr.to_s.downcase).to include("error")
  end

  it "Database - Create - Invalid url - Invalid db type" do 
      result = cb.database.create.mysql.name(@db_name).url("jdbc:mock://test:99/mock").db_username(ENV['INTEGRATIONTEST_RDSCONFIG_RDSUSER']).db_password(ENV['INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD']).type("Hive").build      
      expect(result.exit_status).to eql 1 
      expect(result.stderr.to_s.downcase).to include("error")
  end

   it "Database - Create - Invalid url - No port" do 
      result = cb.database.create.mysql.name(@db_name).url("jdbc:postgresql://test/mock").db_username(ENV['INTEGRATIONTEST_RDSCONFIG_RDSUSER']).db_password(ENV['INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD']).type("Hive").build    
      expect(result.exit_status).to eql 1 
      expect(result.stderr.to_s.downcase).to include("error")
  end

    it "Database - Create - Invalid url - No db" do 
      result = cb.database.create.mysql.name(@db_name).url("jdbc:postgresql://test:99/").db_username(ENV['INTEGRATIONTEST_RDSCONFIG_RDSUSER']).db_password(ENV['INTEGRATIONTEST_RDSCONFIG_RDSPASSWORD']).type("Hive").build       
      expect(result.exit_status).to eql 1 
      expect(result.stderr.to_s.downcase).to include("error")
  end           
end  