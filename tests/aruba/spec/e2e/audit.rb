require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Cloud test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "e2e shared vars"
  class_variable_set(:@@audit_event, "")

  before(:all) do
  	with_environment 'DEBUG' => '1' do
    result = list_with_name_exists("recipe-audit") do
      cb.recipe.list.build
    end
    if !(result[0])
      result = cb.recipe.create.from_file.name("recipe-audit").execution_type("pre-ambari-start").file(@recipe_file).build(false) 
      expect(result.exit_status).to eql 0 
    end
      result = cb.recipe.describe.name("recipe-audit").build(false)
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy     
      @recipe_id = JSON.parse(parse_debug_json(result.stderr))["id"]
  end
  end

  after(:all) do 
    result = cb.recipe.delete.name("recipe-audit").build(false)
    expect(result).to be_successfully_executed    
  end 

  it "Audit - List Recipe events" do
      result = cb.audit.list.resource_type('recipes').resource_id(@recipe_id).build()
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy

      json = JSON.parse(result.stdout)
      json.each do |s|
        @@audit_event = s["Audit"]
        break 
      end            
  end

  it "Audit - Describe Recipe audit entry identified by Audit ID " do
      result = cb.audit.describe.audit_id(@@audit_event["auditId"]).build()
      expect(result.exit_status).to eql 0
      expect(result.stdout.empty?).to be_falsy
      expect(JSON.parse(result.stdout)["Audit"]["operation"]["resourceType"]).to eql "recipes"
      expect(JSON.parse(result.stdout)["Audit"]["operation"]["resourceId"]).to eql @@audit_event["operation"]["resourceId"]       
  end 
end  