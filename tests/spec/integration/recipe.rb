require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.xdescribe 'Recipe test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"

   it "Recipe - Create from file - All recipe types" do
    with_environment 'DEBUG' => '1' do
      @recipe_types.each  do |type|
        r_name = "cli-" + type
        result = cb.recipe.create.from_file.name(r_name).execution_type(type).file(@recipe_file).build(false)
        expect(result.exit_status).to eql 0
        expect(result.stderr.to_s.downcase).not_to include("failed", "error")
      end
    end 
  end

  it "Recipe - Describe" do
    result = cb.recipe.describe.name("aaaaa").build(false)
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    expect(JSON.parse(result.stdout)).to include_json(
      Name: /.*/,
      Description: /.*/,
      ExecutionType: /.*/
    )
  end

  it "Recipe - List" do
    result = cb.recipe.list.build(false)
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    JSON.parse(result.stdout).each do |s|
      expect(s).to include_json(
        Name: /.*/,
        Description: /.*/,
        ExecutionType: /.*/
      )
    end
  end

  it "Recipe - Delete" do
    with_environment 'DEBUG' => '1' do
      result = cb.recipe.delete.name("aaaaa").build
      expect(result.exit_status).to eql 0
    end
  end
end  