package ca.carleton.gcrc.sensorDb.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import java.util.UUID;

import ca.carleton.gcrc.sensorDb.dbapi.Observation;
import junit.framework.TestCase;

public class DbConnectionAndBulkImportTest extends TestCase {

	public void testResolveSocketTimeoutSecondsDefaultsToFiveMinutes() {
		String originalValue = System.getProperty(DbConnection.SOCKET_TIMEOUT_SECONDS_SYSTEM_PROPERTY);
		try {
			System.clearProperty(DbConnection.SOCKET_TIMEOUT_SECONDS_SYSTEM_PROPERTY);
			assertEquals(300, DbConnection.resolveSocketTimeoutSeconds());
		} finally {
			restoreSystemProperty(DbConnection.SOCKET_TIMEOUT_SECONDS_SYSTEM_PROPERTY, originalValue);
		}
	}

	public void testResolveSocketTimeoutSecondsUsesSystemPropertyOverride() {
		String originalValue = System.getProperty(DbConnection.SOCKET_TIMEOUT_SECONDS_SYSTEM_PROPERTY);
		try {
			System.setProperty(DbConnection.SOCKET_TIMEOUT_SECONDS_SYSTEM_PROPERTY, "450");
			assertEquals(450, DbConnection.resolveSocketTimeoutSeconds());
		} finally {
			restoreSystemProperty(DbConnection.SOCKET_TIMEOUT_SECONDS_SYSTEM_PROPERTY, originalValue);
		}
	}

	public void testCreateObservationsIfAbsentReconnectsBeforeCleanupWhenMoveFails() throws Exception {
		UUID importId = UUID.randomUUID();
		TestConnectionHandler failingConnectionHandler = new TestConnectionHandler();
		Connection failingConnection = failingConnectionHandler.createConnection();
		TestConnectionHandler cleanupConnectionHandler = new TestConnectionHandler();
		Connection cleanupConnection = cleanupConnectionHandler.createConnection();
		TestDbConnection dbConnection = new TestDbConnection(failingConnection, cleanupConnection);
		TestDbApiJdbc dbApi = new TestDbApiJdbc(dbConnection, failingConnectionHandler);
		
		List<Observation> observations = new Vector<Observation>();
		observations.add(createObservation(importId.toString(), "import-key-1"));
		
		try {
			dbApi.createObservationsIfAbsent(observations);
			fail("Expected bulk observation insert to fail");
		} catch(Exception e) {
			assertEquals("Error inserting observations into database", e.getMessage());
		}
		
		assertEquals(2, dbConnection.getConnectionCallCount());
		assertEquals(2, dbApi.getDeleteConnections().size());
		assertSame(failingConnection, dbApi.getDeleteConnections().get(0));
		assertSame(cleanupConnection, dbApi.getDeleteConnections().get(1));
		assertEquals(importId, dbApi.getDeleteImportIds().get(1));
		assertEquals(0, failingConnectionHandler.getRollbackCallCount());
		assertEquals(1, cleanupConnectionHandler.getCommitCallCount());
	}

	private Observation createObservation(String importId, String importKey) {
		Observation observation = new Observation();
		observation.setImportId(importId);
		observation.setImportKey(importKey);
		return observation;
	}

	private void restoreSystemProperty(String propertyName, String value) {
		if( null == value ){
			System.clearProperty(propertyName);
		} else {
			System.setProperty(propertyName, value);
		}
	}

	private static class TestDbConnection extends DbConnection {
		private final Connection initialConnection;
		private final Connection cleanupConnection;
		private int getConnectionCallCount = 0;
		
		protected TestDbConnection(Connection initialConnection, Connection cleanupConnection) {
			super(null, "jdbc:test", "user", "password", DbConnection.DEFAULT_SOCKET_TIMEOUT_SECONDS);
			this.initialConnection = initialConnection;
			this.cleanupConnection = cleanupConnection;
		}

		@Override
		public synchronized Connection getConnection() {
			++getConnectionCallCount;
			if( getConnectionCallCount < 2 ){
				return initialConnection;
			}
			return cleanupConnection;
		}

		public int getConnectionCallCount() {
			return getConnectionCallCount;
		}
	}

	private static class TestDbApiJdbc extends DbApiJdbc {
		private final List<Connection> deleteConnections = new Vector<Connection>();
		private final List<UUID> deleteImportIds = new Vector<UUID>();
		private final TestConnectionHandler failingConnectionHandler;

		public TestDbApiJdbc(DbConnection connection, TestConnectionHandler failingConnectionHandler) {
			super(connection);
			this.failingConnectionHandler = failingConnectionHandler;
		}

		@Override
		protected void copyObservationsToStaging(Connection connection, List<Observation> observations) throws Exception {
			// No-op
		}

		@Override
		protected List<String> moveObservationsFromStaging(Connection connection, UUID importId) throws Exception {
			failingConnectionHandler.invalidate();
			throw new SQLException("Read timed out");
		}

		@Override
		protected void deleteStagingObservations(Connection connection, UUID importId) throws Exception {
			deleteConnections.add(connection);
			deleteImportIds.add(importId);
			TestConnectionHandler connectionHandler = (TestConnectionHandler) Proxy.getInvocationHandler(connection);
			if( !connectionHandler.isUsable() ){
				throw new SQLException("This connection has been closed");
			}
		}

		public List<Connection> getDeleteConnections() {
			return deleteConnections;
		}

		public List<UUID> getDeleteImportIds() {
			return deleteImportIds;
		}
	}

	private static class TestConnectionHandler implements InvocationHandler {
		private boolean closed = false;
		private boolean autoCommit = true;
		private int rollbackCallCount = 0;
		private int commitCallCount = 0;
		
		public Connection createConnection() {
			return (Connection) Proxy.newProxyInstance(
				Connection.class.getClassLoader(),
				new Class<?>[]{Connection.class},
				this
			);
		}

		public void invalidate() {
			this.closed = true;
		}

		public boolean isUsable() {
			return !closed;
		}

		public int getRollbackCallCount() {
			return rollbackCallCount;
		}

		public int getCommitCallCount() {
			return commitCallCount;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String methodName = method.getName();
			if( "setAutoCommit".equals(methodName) ){
				if( closed ){
					throw new SQLException("This connection has been closed");
				}
				autoCommit = ((Boolean) args[0]).booleanValue();
				return null;
			} else if( "getAutoCommit".equals(methodName) ){
				if( closed ){
					throw new SQLException("This connection has been closed");
				}
				return Boolean.valueOf(autoCommit);
			} else if( "commit".equals(methodName) ){
				if( closed ){
					throw new SQLException("This connection has been closed");
				}
				++commitCallCount;
				return null;
			} else if( "rollback".equals(methodName) ){
				if( closed ){
					throw new SQLException("This connection has been closed");
				}
				++rollbackCallCount;
				return null;
			} else if( "isValid".equals(methodName) ){
				return Boolean.valueOf(!closed);
			} else if( "isClosed".equals(methodName) ){
				return Boolean.valueOf(closed);
			} else if( "close".equals(methodName) ){
				closed = true;
				return null;
			} else if( "unwrap".equals(methodName) ){
				Class<?> targetClass = (Class<?>) args[0];
				if( targetClass.isInstance(proxy) ){
					return proxy;
				}
				throw new SQLException("Unsupported unwrap target: "+targetClass.getName());
			} else if( "isWrapperFor".equals(methodName) ){
				Class<?> targetClass = (Class<?>) args[0];
				return Boolean.valueOf(targetClass.isInstance(proxy));
			} else if( "toString".equals(methodName) ){
				return "TestConnection[closed="+closed+"]";
			} else if( "hashCode".equals(methodName) ){
				return Integer.valueOf(System.identityHashCode(proxy));
			} else if( "equals".equals(methodName) ){
				return Boolean.valueOf(proxy == args[0]);
			}
			throw new UnsupportedOperationException("Unexpected Connection method: "+methodName);
		}
	}
}
