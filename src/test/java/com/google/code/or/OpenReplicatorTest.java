package com.google.code.or;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.google.code.or.binlog.*;
import com.google.code.or.binlog.impl.ReplicationBasedBinlogParser;
import com.google.code.or.binlog.impl.event.WriteRowsEventV2;
import com.google.code.or.common.util.MySQLConstants;
import com.google.code.or.net.Transport;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenReplicatorTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenReplicatorTest.class);

	static OnetimeServer fiveSixServer = null;

	@BeforeClass
	public static void setupOnetimeServer() throws Exception {
		fiveSixServer = new OnetimeServer("5.6");

		fiveSixServer.boot();
		fiveSixServer.execute("GRANT REPLICATION CLIENT, REPLICATION SLAVE on *.* to 'ortest'@'localhost' IDENTIFIED BY 'ortest'");
		fiveSixServer.execute("create database foo");
		fiveSixServer.execute("create table foo.bar ( id int(10) primary key )");
		fiveSixServer.execute("insert into foo.bar set id = 1");
		fiveSixServer.execute("insert into foo.bar set id = 2");
	}
	//

	static int eventCount;
	private OpenReplicator getOpenReplicator(OnetimeServer server, String fileName, long offset) {
		final OpenReplicator or = new OpenReplicator();

		or.setHost("127.0.0.1");
		or.setPort(server.getPort());
		or.setUser("ortest");
		or.setPassword("ortest");

		or.setBinlogPosition(offset);
		or.setBinlogFileName(fileName);
		return or;
	}

	private void TestReplicator(OpenReplicator or) throws Exception {
		eventCount = 0;
		or.setBinlogEventListener(new BinlogEventListener() {
			public void onEvents(BinlogEventV4 event) {
				if (event instanceof WriteRowsEventV2) {
					eventCount++;
				}
			}
		});
		or.start();
		Thread.sleep(1000);

		assert(eventCount == 2);
	}

	@Test
	public void TestNormalReplicator() throws Exception {
		TestReplicator(getOpenReplicator(fiveSixServer, "master.000001", 4L));
	}

	@Test
	public void TestParserWithoutFormatDescriptionListener() throws Exception {
		OpenReplicator or = getOpenReplicator(fiveSixServer, "master.000001", 120L);

		Transport transport = or.getDefaultTransport();
		transport.connect("localhost", fiveSixServer.getPort());
		or.setTransport(transport);

		ReplicationBasedBinlogParser bp = or.getDefaultBinlogParser();
		bp.setTransport(transport);

		bp.setEventFilter(new BinlogEventFilter() {
			public boolean accepts(BinlogEventV4Header header, BinlogParserContext context) {
				return header.getEventType() != MySQLConstants.FORMAT_DESCRIPTION_EVENT;
			}
		});

		or.setBinlogParser(bp);

		TestReplicator(or);
	}

	@Test
	public void TestHeartbeatReplicator() throws Exception {
		OpenReplicator or = getOpenReplicator(fiveSixServer, "master.000001", 4L);
		or.setHeartbeatPeriod(0.1f);
		TestReplicator(or);

		assert(or.getHeartbeatCount() > 5);  // depends on timing, really
		assert(or.millisSinceLastHeartbeat() != null);
		assert(or.millisSinceLastHeartbeat() > 0);
	}
}
