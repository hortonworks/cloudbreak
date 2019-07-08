require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.xdescribe 'Mpack test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"

  it "Mpack - Create" do
    with_environment 'DEBUG' => '1' do 
      result = cb.mpack.create.name(@mpack_name).url(@mpack_url).description("mpack-description").build(false)  
      expect(result.stderr.to_s.downcase).not_to include("error")
    end
  end

  it "Mpack - Create - With purge" do
    with_environment 'DEBUG' => '1' do 
    result = cb.mpack.create.name(@mpack_name + "-purge").url(@mpack_url).purge(" ").purge_list("stack-definitions,mpacks,service-definitions").build(false)  
      expect(result.stderr.to_s.downcase).not_to include("error")
    end
  end    

  it "Mpack - List" do
    result = cb.mpack.list.build(false)
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    
    JSON.parse(result.stdout).each do |s|       
      expect(s).to include_json(
        Name: /.*/,
        Description: /.*/, 
        URL: /.*/,
        Purge: /.*/,
        PurgeList: /.*/,
        Force: /.*/
       )
    end        
  end   

  it "Mpack - Delete" do
  	with_environment 'DEBUG' => '1' do   	
      result = cb.mpack.delete.name(@mpack_name).build(false)  
      expect(result.exit_status).to eql 0              
    end
  end      
end  