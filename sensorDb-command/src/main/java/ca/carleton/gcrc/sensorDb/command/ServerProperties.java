package ca.carleton.gcrc.sensorDb.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import ca.carleton.gcrc.sensorDb.command.impl.PropertiesWriter;

public class ServerProperties {

	static public ServerProperties fromServerDir(File serverDir) throws Exception {
		Properties props = new Properties();
		readProperties(serverDir, props);
		
		return fromProperties(props);
	}

	static public ServerProperties fromProperties(Properties props) throws Exception {
		ServerProperties serverProps = new ServerProperties();
		
		// Server port
		try {
			String portString = props.getProperty("servlet.url.port");
			int port = Integer.parseInt(portString);
			if( 0 == port ) {
				throw new Exception("Invalid servlet port: "+portString);
			}
			serverProps.setServerPort(port);
		} catch(Exception e) {
			throw new Exception("Unable to interpret servlet port",e);
		}

		// Server Key
		try {
			String serverKeyString = props.getProperty("server.key",null);
			if( null != serverKeyString ){
				byte[] serverKey = Base64.decodeBase64(serverKeyString);
				serverProps.setServerKey(serverKey);
			}
		} catch(Exception e) {
			throw new Exception("Unable to interpret server key",e);
		}

		// DB Connection
		{
			String dbConn = props.getProperty("db.connection",null);
			if( null != dbConn ){
				serverProps.setDbConnection(dbConn);
			}
		}

		// DB User
		{
			String dbUser = props.getProperty("db.user",null);
			if( null != dbUser ){
				serverProps.setDbUser(dbUser);
			}
		}

		// DB Password
		{
			String dbPassword = props.getProperty("db.password",null);
			if( null != dbPassword ){
				serverProps.setDbPassword(dbPassword);
			}
		}

		// DB Socket Timeout
		{
			String dbSocketTimeout = props.getProperty("db.socketTimeout",null);
			if( null != dbSocketTimeout ){
				try {
					int socketTimeoutSeconds = Integer.parseInt(dbSocketTimeout);
					if( socketTimeoutSeconds < 0 ){
						throw new Exception("Invalid db socket timeout: "+dbSocketTimeout);
					}
					serverProps.setDbSocketTimeoutSeconds(Integer.valueOf(socketTimeoutSeconds));
				} catch(Exception e) {
					throw new Exception("Unable to interpret db socket timeout", e);
				}
			}
		}
		
		return serverProps;
	}

	static public void readProperties(File serverDir, Properties props) throws Exception {
		// install.properties
		{
			File installPropFile = new File(serverDir,"config/install.properties");
			if( installPropFile.exists() && installPropFile.isFile() ){
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(installPropFile);
					InputStreamReader reader = new InputStreamReader(fis,"UTF-8");
					props.load(reader);
					
				} catch(Exception e) {
					throw new Exception("Unable to read config properties from: "+installPropFile.getAbsolutePath(), e);
					
				} finally {
					if( null != fis ){
						try{
							fis.close();
						} catch(Exception e) {
							// Ignore
						}
					}
				}
			}
		}

		// sensitive.properties
		{
			File sensitivePropFile = new File(serverDir,"config/sensitive.properties");
			if( sensitivePropFile.exists() && sensitivePropFile.isFile() ){
				FileInputStream fis = null;
				try {
					fis = new FileInputStream(sensitivePropFile);
					InputStreamReader reader = new InputStreamReader(fis,"UTF-8");
					props.load(reader);
					
				} catch(Exception e) {
					throw new Exception("Unable to read config properties from: "+sensitivePropFile.getAbsolutePath(), e);
					
				} finally {
					if( null != fis ){
						try{
							fis.close();
						} catch(Exception e) {
							// Ignore
						}
					}
				}
			}
		}
	}
	
	static public void writeProperties(File serverDir, Properties props) throws Exception {
		// Create config directory, if needed
		File configDir = new File(serverDir,"config");
		try {
			if( false == configDir.exists() ){
				if( false == configDir.mkdir() ) {
					throw new Exception("Error creating directory: "+configDir.getAbsolutePath());
				}
			}
		} catch(Exception e) {
			throw new Exception("Unable to create config directory",e);
		}
		
		// Figure out which properties are saved in the sensitive file
		Set<String> sensitivePropertyNames = new HashSet<String>();
		{
			sensitivePropertyNames.add("server.key");
			sensitivePropertyNames.add("db.password");
			
			File sensitivePropFile = new File(serverDir,"config/sensitive.properties");
			if( sensitivePropFile.exists() && sensitivePropFile.isFile() ){
				FileInputStream fis = null;
				try {
					Properties sensitivePropsCopy = new Properties();

					fis = new FileInputStream(sensitivePropFile);
					InputStreamReader reader = new InputStreamReader(fis,"UTF-8");
					sensitivePropsCopy.load(reader);
					
					Enumeration<?> keyEnum = sensitivePropsCopy.propertyNames();
					while( keyEnum.hasMoreElements() ){
						Object keyObj = keyEnum.nextElement();
						if( keyObj instanceof String ){
							String key = (String)keyObj;
							sensitivePropertyNames.add(key);
						}
					}
					
				} catch(Exception e) {
					// Just ignore
					
				} finally {
					if( null != fis ){
						try{
							fis.close();
						} catch(Exception e) {
							// Ignore
						}
					}
				}
			}
		}
		
		// Divide public and sensitive properties
		Properties publicProps = new Properties();
		Properties sensitiveProps = new Properties();
		
		Enumeration<?> namesEnum = props.propertyNames();
		while( namesEnum.hasMoreElements() ){
			Object keyObj = namesEnum.nextElement();
			if( keyObj instanceof String ) {
				String key = (String)keyObj;
				String value = props.getProperty(key);
				if( sensitivePropertyNames.contains(key) ) {
					sensitiveProps.put(key, value);
				} else {
					publicProps.put(key, value);
				}
			}
		}
		
		// Write public file
		{
			File installPropFile = new File(configDir,"install.properties");
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(installPropFile);
				OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
				PropertiesWriter propWriter = new PropertiesWriter(osw);
				propWriter.write(publicProps);
				
				osw.flush();
				
			} catch(Exception e) {
				throw new Exception("Unable to write config properties to: "+installPropFile.getAbsolutePath(), e);
				
			} finally {
				if( null != fos ){
					try{
						fos.close();
					} catch(Exception e) {
						// Ignore
					}
				}
			}
		}
		
		// Write sensitive file
		{
			File sensitivePropFile = new File(configDir,"sensitive.properties");
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(sensitivePropFile);
				OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
				PropertiesWriter propWriter = new PropertiesWriter(osw);
				propWriter.write(sensitiveProps);
				
				osw.flush();
				
			} catch(Exception e) {
				throw new Exception("Unable to write config properties to: "+sensitivePropFile.getAbsolutePath(), e);
				
			} finally {
				if( null != fos ){
					try{
						fos.close();
					} catch(Exception e) {
						// Ignore
					}
				}
			}
		}
	}
	
	private int serverPort = 8080;
	private byte[] serverKey = null;
	private String dbConnection = null;
	private String dbUser = null;
	private String dbPassword = null;
	private Integer dbSocketTimeoutSeconds = null;

	public int getServerPort() {
		return serverPort;
	}
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public byte[] getServerKey() {
		return serverKey;
	}
	public void setServerKey(byte[] serverKey) {
		this.serverKey = serverKey;
	}

	public String getDbConnection() {
		return dbConnection;
	}
	public void setDbConnection(String conn) {
		this.dbConnection = conn;
	}

	public String getDbPassword() {
		return dbPassword;
	}
	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public String getDbUser() {
		return dbUser;
	}
	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public Integer getDbSocketTimeoutSeconds() {
		return dbSocketTimeoutSeconds;
	}
	public void setDbSocketTimeoutSeconds(Integer dbSocketTimeoutSeconds) {
		this.dbSocketTimeoutSeconds = dbSocketTimeoutSeconds;
	}
}
