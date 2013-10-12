package zcu.xutil.sql;


import java.lang.Thread;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import javax.sql.ConnectionPoolDataSource;

import zcu.xutil.Logger;
import zcu.xutil.sql.MiniDataSource;


public class TestMiniDataSource {

	private static final int maxConnections = 8; // number of connections
	private static final int noOfThreads = 50; // number of worker threads
	private static final int processingTime = 30; // total processing time of
													// the test program in
													// seconds
	private static final int threadPauseTime1 = 100; // max. thread pause time
														// in microseconds,
														// without a connection
	private static final int threadPauseTime2 = 100; // max. thread pause time
														// in microseconds, with
														// a connection

	private static MiniDataSource poolMgr;
	private static WorkerThread[] threads;
	private static boolean shutdownFlag;
	private static Object shutdownObj = new Object();
	private static Random random = new Random();

	private static class WorkerThread extends Thread {
		public int threadNo;

		public void run() {
			threadMain(threadNo);
		}
	};

	private static ConnectionPoolDataSource createDataSource() throws Exception {

		// Version for H2:
		org.h2.jdbcx.JdbcDataSource dataSource = new org.h2.jdbcx.JdbcDataSource();
		dataSource.setURL("jdbc:h2:file:d:/temp/h2db/temp_TestMiniConnectionPoolManagerDB;DB_CLOSE_DELAY=-1");

		// Version for Apache Derby:

		// org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource dataSource =
		// new org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource();
		// dataSource.setDatabaseName
		// ("d:/temp/temp_TestMiniConnectionPoolManagerDB");
		// dataSource.setCreateDatabase ("create");
		// dataSource.setLogWriter (new PrintWriter(System.out));

		// Versioo for JTDS:
		/*
		 * net.sourceforge.jtds.jdbcx.JtdsDataSource dataSource = new
		 * net.sourceforge.jtds.jdbcx.JtdsDataSource(); dataSource.setAppName
		 * ("TestMiniConnectionPoolManager"); dataSource.setDatabaseName
		 * ("Northwind"); dataSource.setServerName ("localhost");
		 * dataSource.setUser ("sa"); dataSource.setPassword
		 * (System.getProperty("saPassword"));
		 */

		// Version for the Microsoft SQL Server driver (sqljdbc.jar):
		/*
		 * // The sqljdbc 1.1 documentation, chapter "Using Connection Pooling",
		 * recommends to use // SQLServerXADataSource instead of
		 * SQLServerConnectionPoolDataSource, even when no // distributed
		 * transactions are used.
		 * com.microsoft.sqlserver.jdbc.SQLServerXADataSource dataSource = new
		 * com.microsoft.sqlserver.jdbc.SQLServerXADataSource();
		 * dataSource.setApplicationName ("TestMiniConnectionPoolManager");
		 * dataSource.setDatabaseName ("Northwind"); dataSource.setServerName
		 * ("localhost"); dataSource.setUser ("sa"); dataSource.setPassword
		 * (System.getProperty("saPassword")); dataSource.setLogWriter (new
		 * PrintWriter(System.out));
		 */

		return dataSource;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("Program started.");
		ConnectionPoolDataSource dataSource = createDataSource();
		poolMgr = new MiniDataSource(dataSource,4, maxConnections);
		poolMgr.setTestQuery("");
		Thread.sleep(10000);
		initDb();
		System.out.println("inited.");
		startWorkerThreads();
		pause(processingTime * 1000000);
		System.out.println("\nStopping threads.");
		stopWorkerThreads();
		System.out.println("\nAll threads stopped.");
		for(int i=0;i<60;i++){
			Logger.LOG.info("IdleNumber={},ActiveNumber={}",poolMgr.getIdleNumber(),poolMgr.getActiveNumber());
			Connection conn = poolMgr.getConnection();
			Thread.sleep(1000);
			conn.close();
		}

//		Thread.sleep(30000);
//		Connection conn =poolMgr.getConnection();
//		conn.close();
		poolMgr.destroy();
		System.out.println("Program completed.");
	}

	private static void startWorkerThreads() {
		threads = new WorkerThread[noOfThreads];
		for (int threadNo = 0; threadNo < noOfThreads; threadNo++) {
			WorkerThread thread = new WorkerThread();
			threads[threadNo] = thread;
			thread.threadNo = threadNo;
			thread.start();
		}
	}

	private static void stopWorkerThreads() throws Exception {
		setShutdownFlag();
		for (int threadNo = 0; threadNo < noOfThreads; threadNo++) {
			threads[threadNo].join();
		}
	}

	private static void setShutdownFlag() {
		synchronized (shutdownObj) {
			shutdownFlag = true;
			shutdownObj.notifyAll();
		}
	}

	private static void threadMain(int threadNo) {
		try {
			threadMain2(threadNo);
		} catch (Throwable e) {
			System.out.println("\nException in thread " + threadNo + ": " + e);
			e.printStackTrace(System.out);
			setShutdownFlag();
		}
	}

	private static void threadMain2(int threadNo) throws Exception {
		// System.out.println ("Thread "+threadNo+" started.");
		while (true) {
			if (!pauseRandom(threadPauseTime1))
				return;
			threadTask(threadNo);
		}
	}
	static int lineWith=0;
	static StringBuilder buf =new StringBuilder();
	synchronized static void print(int threadNo){
		if(lineWith++>50){
			lineWith=0;
			System.out.println(buf.toString());
			buf.setLength(0);
			buf.append(threadNo).append(' ');
		}else
			buf.append(threadNo).append(' ');
	}

	private static void threadTask(int threadNo) throws Exception {
		Connection conn = null;
		try {
			conn = poolMgr.getConnection();
			if (shutdownFlag)
				return;
			print(threadNo);
//			if(lineWith++>50){
//				lineWith=0;
//				System.out.println(threadNo + " ");
//			}else
//				System.out.print(threadNo + " ");
			incrementThreadCounter(conn, threadNo);
			pauseRandom(threadPauseTime2);
		} finally {
			if (conn != null)
				conn.close();
		}
	}

	private static boolean pauseRandom(int maxPauseTime) throws Exception {
		return pause(random.nextInt(maxPauseTime));
	}

	private static boolean pause(int pauseTime) throws Exception {
		synchronized (shutdownObj) {
			if (shutdownFlag)
				return false;
			if (pauseTime <= 0)
				return true;
			int ms = pauseTime / 1000;
			int ns = (pauseTime % 1000) * 1000;
			shutdownObj.wait(ms, ns);
		}
		return true;
	}

	private static void initDb() throws SQLException {
		Connection conn = null;
		try {
			conn = poolMgr.getConnection();
			System.out.println("initDb connected");
			initDb2(conn);
		} finally {
			if (conn != null)
				conn.close();
		}
		System.out.println("initDb done");
	}

	private static void initDb2(Connection conn) throws SQLException {
		execSqlNoErr(conn, "drop table temp");
		execSql(conn, "create table temp (threadNo integer, ctr integer)");
		for (int i = 0; i < noOfThreads; i++)
			execSql(conn, "insert into temp values(" + i + ",0)");
	}

	private static void incrementThreadCounter(Connection conn, int threadNo) throws SQLException {
		execSql(conn, "update temp set ctr = ctr + 1 where threadNo=" + threadNo);
	}

	private static void execSqlNoErr(Connection conn, String sql) {
		try {
			execSql(conn, sql);
		} catch (SQLException e) {
		}
	}

	private static void execSql(Connection conn, String sql) throws SQLException {
		Statement st = null;
		try {
			st = conn.createStatement();
			st.executeUpdate(sql);
		} finally {
			if (st != null)
				st.close();
		}
	}

} // end class TestMiniConnectionPoolManager
