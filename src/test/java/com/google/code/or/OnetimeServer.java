package com.google.code.or;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class OnetimeServer {
	public final long SERVER_ID = 123456L;
	private final String mysqlVersion;
	private int port;
	private int serverPid;

	public String mysqlPath;

	private Connection connection;

	static final Logger LOGGER = LoggerFactory.getLogger(OnetimeServer.class);

	public OnetimeServer(String mysqlVersion) {
		this.mysqlVersion = mysqlVersion;
	}

	public int getPort() {
		return port;
	}

	public void boot() throws IOException, InterruptedException {
		final String dir = System.getProperty("user.dir");

		ProcessBuilder pb = new ProcessBuilder(dir + "/src/test/onetimeserver", "--mysql-version=" + mysqlVersion,
				"--log-bin=master", "--binlog_format=row", "--innodb_flush_log_at_trx_commit=1", "--server_id=" + SERVER_ID);
		Process p = pb.start();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

		p.waitFor();

		final BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));

		new Thread() {
			@Override
			public void run() {
				while (true) {
					String l = null;
					try {
						l = errReader.readLine();
					} catch ( IOException e) {};

					if (l == null)
						break;
					System.err.println(l);
				}
			}
		}.start();

		String json = reader.readLine();
		String outputFile = null;
		try {
			JSONObject output = new JSONObject(json);
			this.port = output.getInt("port");
			this.serverPid = output.getInt("server_pid");
			this.mysqlPath = output.getString("mysql_path");
		} catch ( JSONException e ) {
			LOGGER.error("got exception while parsing " + json, e);
			throw(e);
		}

		LOGGER.debug("booted at port " + this.port + ", outputting to file " + outputFile);
	}

	public Connection getConnection() throws SQLException {
		if ( this.connection == null ) {
			this.connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:" + port + "/mysql", "root", "");
		}
		return this.connection;
	}

	public void shutDown() {
		try {
			Runtime.getRuntime().exec("kill " + this.serverPid);
		} catch ( IOException e ) {}
	}

	public void execute(String sql) {
		try {
			getConnection().createStatement().execute(sql);
		} catch ( SQLException e ) {
			LOGGER.error("got exception while executing '{}': {}", sql, e);
		}

	}
}
