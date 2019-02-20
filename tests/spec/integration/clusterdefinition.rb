require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Cluster definition test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"

  it "Cluster definition - Create from url" do 
    with_environment 'DEBUG' => '1' do
      result = cb.clusterdefinition.create.from_url.name("testbp").url(@clusterdefinition_url).build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("cluster definition created")    
    end
  end    

  it "Cluster definition - Create - Url doesn't exist" do
    result = cb.clusterdefinition.create.from_url.name("temp-bp").url("https://something123456789.com").build(false)
    expect(result.exit_status).to eql 1
    expect(result.stderr).to include("error") 
  end

  it "Cluster definition - Create - Invalid url with no protocol " do
    result = cb.clusterdefinition.create.from_url.name("temp-bp").url("something123456789.com").build(false)
    expect(result.exit_status).to eql 1
    expect(result.stderr).to include("error") 
  end

  it "Cluster definition - Create from file" do
    with_environment 'DEBUG' => '1' do
      result = cb.clusterdefinition.create.from_file.name(@clusterdefinition_name_file).file(@clusterdefinition_file).build(false)
      expect(result.exit_status).to eql 0
      expect(result.stderr).to include("cluster definition created")
    end
  end

  it "Cluster definition - Describe a default cluster definition" do
    result = cb.clusterdefinition.describe.name(@default_clusterdefinition_name).build(false)
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    expect(JSON.parse(result.stdout)).to include_json(
      Name: /.*/,
      Description: /.*/,
      StackName: /.*/,
      StackVersion: /.*/,
      HostgroupCount: /.*/,
      Tags: /.*/
    )
  end

  it "Cluster definition - List - All existing" do
    result = cb.clusterdefinition.list.build(false)
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    JSON.parse(result.stdout).each do |s|    
      expect(s).to include_json(
        Name: /.*/,
        Description: /.*/,  
        StackName: /.*/,
        StackVersion: /.*/,
        HostgroupCount: /.*/,
        Tags: /.*/     
    )
    end       
  end              
end  