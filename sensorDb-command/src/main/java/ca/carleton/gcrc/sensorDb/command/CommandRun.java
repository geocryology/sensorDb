package ca.carleton.gcrc.sensorDb.command;

import java.io.File;
import java.io.PrintStream;
import java.util.Stack;

import javax.servlet.http.HttpServlet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.rolling.RollingFileAppender;
import org.apache.log4j.rolling.TimeBasedRollingPolicy;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.bridge.SLF4JBridgeHandler;

import ca.carleton.gcrc.sensorDb.html.HtmlServletFactory;
import ca.carleton.gcrc.sensorDb.jdbc.DbConnection;
import ca.carleton.gcrc.sensorDb.servlet.db.DbServlet;
import ca.carleton.gcrc.sensorDb.upload.observations.SensorFileUploaded;
import ca.carleton.gcrc.upload.OnUploadedListener;
import ca.carleton.gcrc.upload.UploadServlet;


public class CommandRun implements Command {

	@Override
	public String getCommandString() {
		return "run";
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
		return true;
	}

	@Override
	public void reportHelp(PrintStream ps) {
		ps.println("SensorDb - Run Command");
		ps.println();
		ps.println("The run command starts the server.");
		ps.println();
		ps.println("Once the server is started, it can be stopped by pressing CTRL-C.");
		ps.println();
		ps.println("Command Syntax:");
		ps.println("  sensorDb [<global-options>] run");
		ps.println();
		ps.println("Global Options");
		CommandHelp.reportGlobalSettingServerDir(ps);
	}

	@Override
	public void runCommand(
		GlobalSettings gs
		,Stack<String> argumentStack
		) throws Exception {
		
		File serverDir = gs.getServerDir();

		// Load properties for server
		ServerProperties serverProperties = ServerProperties.fromServerDir(serverDir);
		
		// Run command log4j configuration
		{
			Logger rootLogger = Logger.getRootLogger();
			
			rootLogger.setLevel(Level.INFO);

			TimeBasedRollingPolicy rollingPolicy = new TimeBasedRollingPolicy();
			File logDir = new File(gs.getServerDir(), "logs");
			rollingPolicy.setFileNamePattern(logDir.getAbsolutePath()+"/sensorDb.%d.gz");
			rollingPolicy.activateOptions();
			
			RollingFileAppender fileAppender = new RollingFileAppender();
			fileAppender.setRollingPolicy(rollingPolicy);
			fileAppender.setTriggeringPolicy(rollingPolicy);
			fileAppender.setLayout(new PatternLayout("%d{ISO8601}[%-5p]: %m%n"));
			fileAppender.activateOptions();
			
			rootLogger.addAppender(fileAppender);
		}

		// Capture java.util.Logger
		{
			 // Optionally remove existing handlers attached to j.u.l root logger
			 SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

			 // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
			 // the initialization phase of your application
			 SLF4JBridgeHandler.install();
		}
		
		// Test connection to database
		String connectionString = serverProperties.getDbConnection();
		String dbUser = serverProperties.getDbUser();
		String dbPassword = serverProperties.getDbPassword();
		Integer dbSocketTimeoutSeconds = serverProperties.getDbSocketTimeoutSeconds();
		DbConnection dbConnection = null;
		if( null != dbSocketTimeoutSeconds ){
			dbConnection = DbConnection.fromParameters(connectionString, dbUser, dbPassword, dbSocketTimeoutSeconds.intValue());
		} else {
			dbConnection = DbConnection.fromParameters(connectionString, dbUser, dbPassword);
		}
		
		// Figure out media directory
		File mediaDir = new File(serverDir, "media");

		// Create server
		Server server = new Server(serverProperties.getServerPort());
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
		server.setHandler(context);


        // Proxy to server
//        {
//        	ServletHolder servletHolder = new ServletHolder(new TransparentProxyFixedEscaped());
//        	servletHolder.setInitParameter("proxyTo", serverUrl.toExternalForm());
//        	servletHolder.setInitParameter("prefix", "/server");
//        	context.addServlet(servletHolder,"/server/*");
//        }

        // Proxy to media
        {
        	ServletHolder servletHolder = new ServletHolder(new DefaultServlet());
        	servletHolder.setInitParameter("dirAllowed", "false");
        	servletHolder.setInitParameter("gzip", "true");
        	servletHolder.setInitParameter("pathInfoOnly", "true");
        	servletHolder.setInitParameter("resourceBase", mediaDir.getAbsolutePath());
        	context.addServlet(servletHolder,"/media/*");
        }

        // Proxy to html pages
        {
        	HtmlServletFactory factory = new HtmlServletFactory();
        	HttpServlet htmlServlet = factory.create("/");
        	ServletHolder servletHolder = new ServletHolder(htmlServlet);
        	context.addServlet(servletHolder,"/*");
        }

        // Servlet for db
        {
        	ServletHolder servletHolder = new ServletHolder(new DbServlet(dbConnection, mediaDir));
        	servletHolder.setInitOrder(1);
        	context.addServlet(servletHolder,"/db/*");
        }

        // Servlet for uploading observations
        {
        	OnUploadedListener onUploadedListener = new SensorFileUploaded(dbConnection);
        	
        	UploadServlet uploadServlet = new UploadServlet();
        	uploadServlet.setOnUploadedListener(onUploadedListener);
        	uploadServlet.setRepositoryDir(mediaDir);
        	
        	ServletHolder servletHolder = new ServletHolder(uploadServlet);
        	servletHolder.setInitOrder(1);
        	context.addServlet(servletHolder,"/uploadObservations/*");
        }

		// Start server
		server.start();
		server.join();
	}

}
