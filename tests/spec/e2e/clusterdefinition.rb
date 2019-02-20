require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Clusterdefinition test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "e2e shared vars"

  before(:all) do
    result = list_with_name_exists(@clusterdefinition_name_url) do
      cb.clusterdefinition.list.build
    end
    if (result[0])
      result = cb.clusterdefinition.delete.name(@clusterdefinition_name_url).build
      expect(result.exit_status).to eql 0
    end
  end

  before(:all) do
    result = list_with_name_exists(@clusterdefinition_name_file) do
      cb.clusterdefinition.list.build
    end
    if (result[0])
      result = cb.clusterdefinition.delete.name(@clusterdefinition_name_file).build
      expect(result.exit_status).to eql 0
    end
  end  

  it "Clusterdefinition - Create from url - Describe - List - Delete " do
    bp_create_describe_delete(cb, @clusterdefinition_name_url) do
      cb.clusterdefinition.create.from_url.name(@clusterdefinition_name_url).url(@clusterdefinition_url).build
    end 
  end    

  it "Clusterdefinition - Create - Url doesn't exist" do
    result = cb.clusterdefinition.create.from_url.name("temp-bp").url("https://something123456789.com").build
    expect(result.exit_status).to eql 1
    expect(result.stderr).to include("error") 
  end

  it "Clusterdefinition - Create - Invalid url with no protocol " do
    result = cb.clusterdefinition.create.from_url.name("temp-bp").url("something123456789.com").build
    expect(result.exit_status).to eql 1
    expect(result.stderr).to include("error") 
  end

  it "Clusterdefinition - Create from file - Describe List - Delete " do
    bp_create_describe_delete(cb, @clusterdefinition_name_file) do
      cb.clusterdefinition.create.from_file.name(@clusterdefinition_name_file).file(@clusterdefinition_file).build
    end 
  end 

  it "Clusterdefinition - Describe a default clusterdefinition" do
    result = cb.clusterdefinition.describe.name(@default_clusterdefinition_name).build
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy 
    json = JSON.parse(result.stdout)
    expect(json).to include_json(
      Name: /.*/,
      Description: /.*/,
      StackName: /.*/,  
      StackVersion: /.*/,
      HostgroupCount: /.*/,
      Tags: /.*/,
      ClusterDefinitionTextAsBase64: /.*/,
      ID: /.*/
    )       
  end

  it "Clusterdefinition - List - All existing" do
    result = cb.clusterdefinition.list.build
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    json = JSON.parse(result.stdout)
    json.each do |s|    
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