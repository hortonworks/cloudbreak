class CommandBuilder
  include Aruba::Api
  alias_method :list, :method_missing
  alias_method :type, :method_missing

	@@cmd = ""
  def method_missing(m, *args, &block)
    tmp = CommandBuilder.new
    if (args.size > 0)
      m = m.to_s.gsub("_", "-")
      CommandBuilder.cmd += "--" + m.to_s + " " + args.join("") + " "
    else
          CommandBuilder.cmd += m.to_s + " "
          CommandBuilder.cmd = CommandBuilder.cmd.gsub("_", "-")
    end
    return tmp
  end

  def build(with_print=true)
    return builder(with_print, CommandBuilder.cmd, CommandBuilder.cmd)
  end

  def builds(with_print=true)
    str_to_print = ""
    flag=false
    command_to_print = CommandBuilder.cmd.split(" ")
    command_to_print.each do |s|
      if flag
        str_to_print = str_to_print + "***** "
        flag=false
      else 
        str_to_print = str_to_print + s + " "
        if s.to_s.include? "--" and s.to_s != "--name"
          flag=true
         end
      end
    end

    builder(with_print, CommandBuilder.cmd, str_to_print)
  end

 def builder(with_print, command_to_run, command_to_print)
    result = run(command_to_run)
    result.stop
    html_print do
      puts command_to_print
      if with_print
        puts result.stdout
        puts result.stderr   
      else 
        if (result.stderr.to_s.downcase.include? "error") 
          puts result.stdout
          puts result.stderr 
        end 
      end  
    end  
    CommandBuilder.cmd = "cb "     
    return result  
 end
  
  class << self
        attr_accessor :cmd
  end
end