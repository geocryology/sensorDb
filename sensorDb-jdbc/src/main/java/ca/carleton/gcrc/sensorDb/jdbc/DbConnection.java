package ca.carleton.gcrc.sensorDb.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException; 
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.carleton.gcrc.sensorDb.dbapi.DbAPI;

public class DbConnection {

	final static protected Logger logger = LoggerFactory.getLogger(DbConnection.class);
	static final String SOCKET_TIMEOUT_SECONDS_SYSTEM_PROPERTY = "sensorDb.jdbc.socketTimeoutSeconds";
	static final String SOCKET_TIMEOUT_SECONDS_ENVIRONMENT_VARIABLE = "SENSORDB_JDBC_SOCKET_TIMEOUT_SECONDS";
	static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 300;

	private final String connectionString;
	private final String user;
	private final String password;
	private final int socketTimeoutSeconds;
	
	private Connection connection;

	static public DbConnection fromParameters(
			String connectionString,
			String user,
			String password
			) throws Exception {
		return fromParameters(connectionString, user, password, resolveSocketTimeoutSeconds());
	}

	static public DbConnection fromParameters(
			String connectionString,
			String user,
			String password,
			int socketTimeoutSeconds
			) throws Exception {
		
		Connection con = createNewSqlConnection(connectionString, user, password, socketTimeoutSeconds);
		return new DbConnection(con, connectionString, user, password, socketTimeoutSeconds);
	}

	protected DbConnection(Connection connection, String connectionString, String user, String password, int socketTimeoutSeconds) {
		this.connection = connection;
		this.connectionString = connectionString;
		this.user = user;
		this.password = password;
		this.socketTimeoutSeconds = socketTimeoutSeconds;
	}
        public synchronized Connection getConnection() {
            final int MAX_RETRIES = 8;  // Increased from 5
            final long INITIAL_BACKOFF_MS = 2000;  // Start at 2 seconds instead of 1
            final long MAX_BACKOFF_MS = 120000;    // 2 minutes max wait between retries
            
            int attempt = 0;
            
            while (attempt <= MAX_RETRIES) {
                try {
                    // Check if the connection is dead, using a 2-second timeout.
                    if (this.connection == null || !this.connection.isValid(2)) {
                        logger.warn("Database connection was stale or closed. Reconnecting...");
                        this.connection = createNewSqlConnection(this.connectionString, this.user, this.password, this.socketTimeoutSeconds);
                    }
                    return this.connection; // Success - return the connection
                    
                } catch (Exception e) {
                    attempt++;
                    logger.error("Database connection validation failed (attempt " + attempt + " of " + MAX_RETRIES + "). Reconnecting...", e);
                    
                    // If isValid() throws an error, the connection is definitely dead.
                    try {
                        this.connection = createNewSqlConnection(this.connectionString, this.user, this.password, this.socketTimeoutSeconds);
                        return this.connection; // Success after reconnect
                    } catch (Exception newConnectException) { 
                        logger.error("Failed to reconnect to the database (attempt " + attempt + " of " + MAX_RETRIES + ")", newConnectException);
                        this.connection = null;
                        
                        // Calculate exponential backoff with jitter
                        if (attempt < MAX_RETRIES) {
                            long backoffMs = Math.min(INITIAL_BACKOFF_MS * (long) Math.pow(2, attempt - 1), MAX_BACKOFF_MS);
                            // Add jitter (random between 50-150% of backoff time)
                            backoffMs = (long) (backoffMs * (0.5 + Math.random()));
                            
                            logger.warn("Retrying connection in " + (backoffMs/1000) + " seconds...");
                            try {
                                Thread.sleep(backoffMs);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                logger.error("Connection retry interrupted", ie);
                                break;
                            }
                        }
                    }
                }
            }
            
            // If we get here, all retries failed
            logger.error("All connection attempts failed. Returning null connection.");
            return null;
        }

	static int resolveSocketTimeoutSeconds() {
		String configuredValue = System.getProperty(SOCKET_TIMEOUT_SECONDS_SYSTEM_PROPERTY);
		if( null == configuredValue || configuredValue.trim().length() < 1 ){
			configuredValue = System.getenv(SOCKET_TIMEOUT_SECONDS_ENVIRONMENT_VARIABLE);
		}
		if( null != configuredValue && configuredValue.trim().length() > 0 ){
			try {
				int socketTimeoutSeconds = Integer.parseInt(configuredValue.trim());
				if( socketTimeoutSeconds >= 0 ){
					return socketTimeoutSeconds;
				}
				logger.warn("Ignoring negative JDBC socket timeout value: "+configuredValue);
			} catch(Exception e) {
				logger.warn("Ignoring invalid JDBC socket timeout value: "+configuredValue, e);
			}
		}
		return DEFAULT_SOCKET_TIMEOUT_SECONDS;
	}

	static Properties createConnectionProperties(String user, String password, int socketTimeoutSeconds) {
		Properties properties = new Properties();
		if( null != user ){
			properties.setProperty("user", user);
		}
		if( null != password ){
			properties.setProperty("password", password);
		}
		properties.setProperty("socketTimeout", String.valueOf(socketTimeoutSeconds));
		return properties;
	}

	private static Connection createNewSqlConnection(String connectionString, String user, String password, int socketTimeoutSeconds) throws Exception {
		try {
		    Class.forName("org.postgresql.Driver"); //load the driver
		    Properties connectionProperties = createConnectionProperties(user, password, socketTimeoutSeconds);
			Connection con = DriverManager.getConnection(
					"jdbc:postgresql:"+connectionString,
					connectionProperties
				); //connect to the db
		    DatabaseMetaData dbmd = con.getMetaData(); //get MetaData to confirm connection
		    logger.info("Connection to "+dbmd.getDatabaseProductName()+" "+
		                       dbmd.getDatabaseProductVersion()+" successful.\n");
			return con;
		} catch (Exception e) {
			throw new Exception("Couldn't get db connection: "+connectionString,e);
		}
	}

	public DbAPI getAPI()  {
		return new DbApiJdbc(this);
	}
}
