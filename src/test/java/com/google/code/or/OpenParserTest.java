package com.google.code.or;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.google.code.or.binlog.impl.event.WriteRowsEvent;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.or.binlog.BinlogEventListener;
import com.google.code.or.binlog.BinlogEventV4;
import com.google.code.or.binlog.impl.event.XidEvent;

public class OpenParserTest {
	//
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenParserTest.class);
	private static final OnetimeServer onetimeServer = new OnetimeServer("5.5");

	@BeforeClass
	public static void BootOnetimeServer() throws Exception {
		onetimeServer.boot();
	}

	static int eventCount;
	@Test
	public void TestParserWithChecksums() throws Exception {
		OnetimeServer fiveSixServer = new OnetimeServer("5.6");

		fiveSixServer.boot();
		fiveSixServer.execute("create database foo");
		fiveSixServer.execute("create table foo.bar ( id int(10) primary key )");
		fiveSixServer.execute("insert into foo.bar set id = 1");
		fiveSixServer.execute("insert into foo.bar set id = 2");

		final OpenParser op = new OpenParser();

		op.setStartPosition(4);
		op.setBinlogFileName("master.000001");
		op.setBinlogFilePath(fiveSixServer.mysqlPath);

		eventCount = 0;
		op.setBinlogEventListener(new BinlogEventListener() {
			public void onEvents(BinlogEventV4 event) {
				LOGGER.info("received event: {}", event);
				if ( event instanceof WriteRowsEvent ) {
					eventCount++;
				}
			}
		});
		op.start();
		Thread.sleep(1000);

		assert(eventCount == 2);
	}
}
