require_relative "../common/e2e_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"
require_relative "../common/helpers"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "cb "
  return cb
end

RSpec.describe 'Image catalog test cases', :type => :aruba do
  include_context "shared helpers"
  include_context "shared command helpers"    
  include_context "shared vars"
  
  before(:all) do
    @ic_exist = imagecat_list_with_check(@image_catalog_name) 
    if (@ic_exist)
      result = cb.imagecatalog.delete.name(@image_catalog_name).build
      expect(result.exit_status).to eql 0 
    end
  end   
  after(:all) do 
    result = cb.imagecatalog.set_default.name(@image_catalog_name_default).build
    expect(result.exit_status).to eql 0    
  end 

  it "Imagecatalog - Create - List - Set default - Delete" do 
    result = cb.imagecatalog.create.name(@image_catalog_name).url(@image_catalog_url).build
    expect(result.exit_status).to eql 0
    expect(result.stderr).to be_empty

    result = cb.imagecatalog.list.build
    expect(result.exit_status).to eql 0
    
    expect(result.stdout.empty?).to be_falsy
    json = JSON.parse(result.stdout)
    json.each do |s| 
      expect(s).to include_json(
       Name: /.*/,
       Default: /.*/,
       URL: /.*/    
      ) 
    end    

    result = cb.imagecatalog.set_default.name(@image_catalog_name).build
    expect(result.exit_status).to eql 0    

    result = cb.imagecatalog.delete.name(@image_catalog_name).build
    expect(result.exit_status).to eql 0                    
  end
end  