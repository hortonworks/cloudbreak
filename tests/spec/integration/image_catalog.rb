require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Image catalog test cases', :type => :aruba do
  include_context "shared command helpers"
  include_context "mock shared vars"
  
  it "Imagecatalog - Create" do 
    with_environment 'DEBUG' => '1' do
      result = cb.imagecatalog.create.name(@image_catalog_name).url(@image_catalog_url).build(false)  
      expect(result.stderr).to include("create imagecatalog took")    
    end
  end

  it "Imagecatalog - Create - Invalid Url BUG-97072" do
  	skip("BUG-97072")
    with_environment 'DEBUG' => '1' do
      result = cb.imagecatalog.create.name(@image_catalog_name).url("http://www.google.com").build(false)
      expect(result.stderr).to include("create imagecatalog took")    
    end
  end 

  it "Imagecatalog - Create - Invalid json BUG-97072" do
  	skip("BUG-97072")
    with_environment 'DEBUG' => '1' do
      result = cb.imagecatalog.create.name(@image_catalog_name).url(@imagecatalog_invalid_json ).build(false) 
      expect(result.stderr).to include("error")    
    end
  end  

  it "Imagecatalog - List" do
    result = cb.imagecatalog.list.build(false)
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    JSON.parse(result.stdout).each do |s|    
      expect(s).to include_json(
      Name: /.*/,
      Default: /.*/,  
      URL: /.*/    
    )
    end       
  end 

  it "Imagecatalog - Set default" do 
    with_environment 'DEBUG' => '1' do
      result = cb.imagecatalog.set_default.name(@image_catalog_name).build(false)  
      expect(result.stderr).to include("set default imagecatalog took")    
    end
  end                
end  