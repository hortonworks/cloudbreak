require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Blueprint test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "e2e shared vars"

  before(:all) do
    result = list_with_name_exists(@blueprint_name_url) do
      cb.blueprint.list.build
    end
    if (result[0])
      result = cb.blueprint.delete.name(@blueprint_name_url).build
      expect(result.exit_status).to eql 0
    end
  end

  before(:all) do
    result = list_with_name_exists(@blueprint_name_file) do
      cb.blueprint.list.build
    end
    if (result[0])
      result = cb.blueprint.delete.name(@blueprint_name_file).build
      expect(result.exit_status).to eql 0
    end
  end  

  it "Blueprint - Create from url - Describe - List - Delete " do
    bp_create_describe_delete(cb, @blueprint_name_url) do
      cb.blueprint.create.from_url.name(@blueprint_name_url).url(@blueprint_url).build
    end 
  end    

  it "Blueprint - Create - Url doesn't exist" do
    result = cb.blueprint.create.from_url.name("temp-bp").url("https://something123456789.com").build
    expect(result.exit_status).to eql 1
    expect(result.stderr).to include("error") 
  end

  it "Blueprint - Create - Invalid url with no protocol " do
    result = cb.blueprint.create.from_url.name("temp-bp").url("something123456789.com").build
    expect(result.exit_status).to eql 1
    expect(result.stderr).to include("error") 
  end

  it "Blueprint - Create from file - Describe List - Delete " do
    bp_create_describe_delete(cb, @blueprint_name_file) do
      cb.blueprint.create.from_file.name(@blueprint_name_file).file(@blueprint_file).build
    end 
  end 

  it "Blueprint - Describe a default blueprint" do
    result = cb.blueprint.describe.name(@default_blueprint_name).build
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
      BlueprintTextAsBase64: /.*/,
      ID: /.*/
    )       
  end

  it "Blueprint - List - All existing" do
    result = cb.blueprint.list.build
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