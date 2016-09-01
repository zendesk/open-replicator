package com.google.code.or;

import java.sql.Time;
import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

import com.google.code.or.binlog.*;
import com.google.code.or.binlog.impl.ReplicationBasedBinlogParser;
import com.google.code.or.binlog.impl.event.WriteRowsEventV2;
import com.google.code.or.common.glossary.Column;
import com.google.code.or.common.glossary.Row;
import com.google.code.or.common.glossary.column.Datetime2Column;
import com.google.code.or.common.glossary.column.Time2Column;
import com.google.code.or.common.glossary.column.Timestamp2Column;
import com.google.code.or.common.util.MySQLConstants;
import com.google.code.or.net.Transport;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

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
	}

	private void createData() throws Exception {
		fiveSixServer.execute("RESET MASTER");
		fiveSixServer.execute("TRUNCATE TABLE foo.bar");
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

	private WriteRowsEventV2 getFirstInsert(OpenReplicator or) throws Exception {
		final AtomicReference<WriteRowsEventV2> ref = new AtomicReference<WriteRowsEventV2>();
		or.setBinlogEventListener(new BinlogEventListener() {
			public void onEvents(BinlogEventV4 event) {
				if (event instanceof WriteRowsEventV2) {
					ref.set((WriteRowsEventV2) event);
				}
			}
		});
		or.start();
		Thread.sleep(2000);
		return ref.get();
	}

	@Test
	public void TestNormalReplicator() throws Exception {
		createData();
		TestReplicator(getOpenReplicator(fiveSixServer, "master.000001", 4L));
	}

	@Test
	public void TestParserWithoutFormatDescriptionListener() throws Exception {
		createData();
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
		createData();
		OpenReplicator or = getOpenReplicator(fiveSixServer, "master.000001", 4L);
		or.setHeartbeatPeriod(0.1f);
		TestReplicator(or);

		assert(or.getHeartbeatCount() > 5);  // depends on timing, really
		assert(or.millisSinceLastEvent() != null);
		assert(or.millisSinceLastEvent() <= 150);
	}

	private void testPrecision(String type, String table, String... times) throws Exception {
		fiveSixServer.execute("RESET MASTER");
		fiveSixServer.execute(
			String.format(
				"create table foo.%s ( t1 %s(1), t2 %s(2), t3 %s(3), t4 %s(4), t5 %s(5), t6 %s(6))",
				table,
				type, type, type, type, type, type
			)
		);
		String insertSQL = String.format("insert into foo.%s values(?, ?, ?, ?, ?, ?)", table);
		PreparedStatement s	= fiveSixServer.getConnection().prepareStatement(insertSQL);

		for ( int i = 0; i < 6; i++ ) {
			s.setString(i + 1, times[i]);
		}

		s.execute();

		OpenReplicator or = getOpenReplicator(fiveSixServer, "master.000001", 4L);
		WriteRowsEventV2 event = getFirstInsert(or);
		Row row = event.getRows().get(0);
		assertThat(row, is(not(nullValue())));
		List<Column> columns = row.getColumns();
		for ( int i = 0; i < 6; i++ ) {
			Column c = columns.get(i);
			if ( c instanceof Timestamp2Column ) {
				assertEquals(times[i], c.toString());
	 		} else if ( c instanceof Datetime2Column ) {
	 			Timestamp ts = Timestamp.valueOf(times[i]);
				assertEquals(ts.getTime(), ((Date) c.getValue()).getTime());
			} else if ( c instanceof Time2Column ) {
				Timestamp t = Timestamp.valueOf("1970-01-01 " + times[i]);
				Time t2 = (Time) c.getValue();
				assertEquals(t.getTime(), t2.getTime());
			}
		}
	}

	@Test
	public void testTimestampPrecision() throws Exception {
		testPrecision(
			"timestamp",
			"ts",
			"2001-01-01 00:00:00.9",
			"2001-01-01 00:00:00.99",
			"2001-01-01 00:00:00.999",
			"2001-01-01 00:00:00.9999",
			"2001-01-01 00:00:00.99999",
			"2001-01-01 00:00:00.999999"
		);
	}

	@Test
	public void testDatetimePrecision() throws Exception {
		testPrecision(
			"datetime",
			"dt",
			"2001-01-01 00:00:00.9",
			"2001-01-01 00:00:00.99",
			"2001-01-01 00:00:00.999",
			"2001-01-01 00:00:00.9999",
			"2001-01-01 00:00:00.99999",
			"2001-01-01 00:00:00.999999"
		);
	}

	@Test
	public void testTimePrecision() throws Exception {
		testPrecision(
			"time",
			"tm",
			"00:00:00.9",
			"00:00:00.99",
			"00:00:00.999",
			"00:00:00.9999",
			"00:00:00.99999",
			"00:00:00.999999"
		);
	}
}
