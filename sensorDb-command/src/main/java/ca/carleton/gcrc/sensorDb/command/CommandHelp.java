package ca.carleton.gcrc.sensorDb.command;

import java.io.PrintStream;
import java.util.Stack;

public class CommandHelp implements Command {


	static public void reportGlobalSettingServerDir(PrintStream ps){
		ps.println("     --server-dir <dir>  Indicates the location of the server directory.");
		ps.println("                         If this option is not specified, the current");
		ps.println("                         directory is assumed to be the server directory.");
	}
	
	@Override
	public String getCommandString() {
		return "help";
	}

	@Override
	public boolean matchesKeyword(String keyword) {
		if( getCommandString().equalsIgnoreCase(keyword) ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isDeprecated() {
		return false;
	}

	@Override
	public boolean requiresServerDir() {
		return false;
	}

	@Override
	public void reportHelp(PrintStream ps) {
		final Properties properties = new Properties();
		properties.load(this.getClassLoader().getResourceAsStream("version.properties"));
		final String version = properties.getProperty("version");

		ps.println("SensorDb - Help Command");
		ps.println("Version: " + version);
		ps.println();
		ps.println("Command Syntax:");
		ps.println("  sensorDb help [<command-name>]");
		ps.println();
		ps.println("  sensorDb help");
		ps.println("    Provides general help.");
		ps.println();
		ps.println("  sensorDb help <command-name>");
		ps.println("    Provides specific information about a command.");
	}

	@Override
	public void runCommand(
		GlobalSettings gs
		,Stack<String> argumentStack
		) throws Exception {
		
		if( argumentStack.size() > 0 ) {
			String commandName = argumentStack.pop();
			for(Command command : Main.getCommands()){
				if( command.matchesKeyword(commandName) ){
					reportCommandSpecificHelp(gs, command);
					return;
				}
			}
			
			throw new Exception("Unrecognized command: "+commandName);
		}
		
		reportGeneralHelp(gs);
	}

	private void reportGeneralHelp(GlobalSettings gs){
		gs.getOutStream().println("SensorDb");
		gs.getOutStream().println();
		gs.getOutStream().println("Command Syntax:");
		gs.getOutStream().println("  sensorDb [<global-options>] <command> [<command-specific-options>]");
		gs.getOutStream().println();
		gs.getOutStream().println("For more information about a command:");
		gs.getOutStream().println("  sensorDb help <command-name>");
		gs.getOutStream().println();
		gs.getOutStream().println("Possible commands:");
		for(Command command : Main.getCommands()){
			if( false == command.isDeprecated() ) {
				gs.getOutStream().println("\t"+command.getCommandString());
			}
		}
	}

	private void reportCommandSpecificHelp(GlobalSettings gs, Command command){
		gs.getOutStream().println();
		command.reportHelp( gs.getOutStream() );
		gs.getOutStream().println();
	}
}
