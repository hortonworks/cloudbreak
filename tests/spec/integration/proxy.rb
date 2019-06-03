require_relative "../common/mock_vars"
require_relative "../common/command_helpers"
require_relative "spec_helper"

define_method(:cb) do
  cb = CommandBuilder.new
  CommandBuilder.cmd = "dp "
  return cb
end

RSpec.describe 'Proxy test cases', :type => :aruba do
  include_context "shared command helpers"    
  include_context "mock shared vars"

  skip "Proxy - Create - Without additional params" do
    with_environment 'DEBUG' => '1' do 
      result = cb.proxy.create.name(@proxy_name).proxy_host(@proxy_server).proxy_port(@proxy_port).build(false)  
      expect(result.stderr.to_s.downcase).not_to include("failed", "error")
    end
  end 

  skip "Proxy - List" do
    result = cb.proxy.list.build(false)
    expect(result.exit_status).to eql 0
    expect(result.stdout.empty?).to be_falsy
    
    JSON.parse(result.stdout).each do |s|       
      expect(s).to include_json(
        Name: /.*/,
        Host: /.*/,  
        Port: /.*/,
        Protocol: /.*/
       )
    end        
  end

  skip "Proxy - Create - With additional params" do 
    with_environment 'DEBUG' => '1' do   	
      result = cb.proxy.create.name(@proxy_name).proxy_host(@proxy_server).proxy_port(@proxy_port).proxy_user(@proxy_user)
      .proxy_password(@proxy_password).build(false)  
      expect(result.stderr.to_s.downcase).not_to include("failed", "error")
    end 
  end


  skip "Proxy - Create - With additional params - Https protocol" do 
    with_environment 'DEBUG' => '1' do   	  	
      result = cb.proxy.create.name(@proxy_name).proxy_host(@proxy_server).proxy_port(@proxy_port).proxy_user(@proxy_user)
      .proxy_password(@proxy_password).proxy_protocol("https").build(false)  
      expect(result.stderr.to_s.downcase).not_to include("failed", "error")
    end
  end

  skip "Proxy - Create - With additional params - Invalid Https protocol" do 
    with_environment 'DEBUG' => '1' do   	  	
      result = cb.proxy.create.name(@proxy_name).proxy_host(@proxy_server).proxy_port(@proxy_port).proxy_user(@proxy_user)
      .proxy_password(@proxy_password).proxy_protocol("mock").build(false)  
      expect(result.stderr.to_s.downcase).to include("error")
    end
  end         

  skip "Proxy - Delete" do
  	with_environment 'DEBUG' => '1' do   	
      result = cb.proxy.delete.name(@proxy_name).build(false)  
      expect(result.exit_status).to eql 0              
    end
  end      
end  