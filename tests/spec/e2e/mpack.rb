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
    result = list_with_name_exists(@mpack_name) do
      cb.mpack.list.build
    end
    if (result[0])
      result = cb.mpack.delete.name(@mpack_name).build
      expect(result.exit_status).to eql 0 
    end
  end

  before(:all) do
    result = list_with_name_exists(@mpack_name + "-purge") do
      cb.mpack.list.build
    end
    if (result[0])
      result = cb.mpack.delete.name(@mpack_name + "-purge").build
      expect(result.exit_status).to eql 0 
    end
  end  
  
  it "Mpack - Create" do 
    result = cb.mpack.create.name(@mpack_name).url(@mpack_url).description("mpack-description").build  
    expect(result.exit_status).to eql 0 
  end 


  it "Mpack - List - Checking previosly created mpack" do
    result = list_with_name_exists(@mpack_name) do
      cb.mpack.list.build
    end
    expect(result[0]).to be_truthy

    result[1].each do |s|
      if s["Name"] ==  @mpack_name          
        expect(s).to include_json(
          Name: @mpack_name,
          Description: "mpack-description", 
          URL: @mpack_url,
          Purge: "false",
          PurgeList: /.*/,
          Force: /.*/
        )
      end
    end        
  end

  it "Mpack - Create - With purge" do 
    result = cb.mpack.create.name(@mpack_name + "-purge").url(@mpack_url).purge(" ").purge_list("stack-definitions,mpacks,service-definitions").build  
    expect(result.exit_status).to eql 0 
  end 

   it "Mpack - List - Checking previosly created mpack with purge" do
    result = list_with_name_exists(@mpack_name + "-purge") do
      cb.mpack.list.build
    end
    expect(result[0]).to be_truthy

    result[1].each do |s|
      if s["Name"] ==  @mpack_name + "-purge"          
        expect(s).to include_json(
          Name: @mpack_name + "-purge",
          Description: /.*/, 
          URL: @mpack_url,
          Purge: "true",
          PurgeList: "stack-definitions,mpacks,service-definitions",
          Force: /.*/
        )
      end
    end        
  end   

  it "Mpack - Delete - Previously created mpacks" do
    result = cb.mpack.delete.name(@mpack_name).build
    expect(result.exit_status).to eql 0

    result = cb.mpack.delete.name(@mpack_name + "-purge").build
    expect(result.exit_status).to eql 0           
  end

   it "Mpack - Create - With purge - Invalid purge list" do
    result = cb.mpack.create.name(@mpack_name + "-purge").url(@mpack_url).purge(" ").purge_list("invalid").build(false)  
      expect(result.stderr.to_s.downcase).to include("error")
  end            
end  