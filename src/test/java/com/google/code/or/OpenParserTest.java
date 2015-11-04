package com.google.code.or;

import com.google.code.or.binlog.*;
import com.google.code.or.binlog.impl.FileBasedBinlogParser;
import com.google.code.or.binlog.impl.event.WriteRowsEventV2;
import com.google.code.or.common.util.MySQLConstants;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Test;

public class OpenParserTest {
	static OnetimeServer fiveSixServer = null;

	@BeforeClass
	public static void setupOnetimeServer() throws Exception {
		fiveSixServer = new OnetimeServer("5.6");

		fiveSixServer.boot();
		fiveSixServer.execute("create database foo");
		fiveSixServer.execute("create table foo.bar ( id int(10) primary key )");
		fiveSixServer.execute("insert into foo.bar set id = 1");
		fiveSixServer.execute("insert into foo.bar set id = 2");
	}
	//
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenParserTest.class);

	static int eventCount;

	private OpenParser getOpenParser(OnetimeServer server, String fileName, long offset) {
		final OpenParser op = new OpenParser();

		op.setStartPosition(offset);
		op.setBinlogFileName(fileName);
		op.setBinlogFilePath(server.mysqlPath);
		return op;
	}

	private void TestParserWithChecksums(OpenParser op) throws Exception {
		eventCount = 0;
		op.setBinlogEventListener(new BinlogEventListener() {
			public void onEvents(BinlogEventV4 event) {
				if (event instanceof WriteRowsEventV2) {
					eventCount++;
				}
			}
		});
		op.start();
		Thread.sleep(1000);

		assert(eventCount == 2);
	}

	@Test
	public void TestParserWithChecksumsAtStart() throws Exception {
		OpenParser op = getOpenParser(fiveSixServer, "master.000001", 4L);
		TestParserWithChecksums(op);
	}

	@Test
	public void TestParserWithChecksumsAtOffset() throws Exception {
		OpenParser op = getOpenParser(fiveSixServer, "master.000001", 120L);
		TestParserWithChecksums(op);
	}

	@Test
	public void TestParserWithoutFormatDescriptionListener() throws Exception {
		OpenParser op = getOpenParser(fiveSixServer, "master.000001", 120L);

		FileBasedBinlogParser bp = op.getDefaultBinlogParser();
		bp.unregisterEventParser(MySQLConstants.FORMAT_DESCRIPTION_EVENT);
		op.setBinlogParser(bp);

		TestParserWithChecksums(op);
	}
}
