/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.or.binlog.impl.parser;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.or.binlog.BinlogEventV4Header;
import com.google.code.or.binlog.BinlogParserContext;
import com.google.code.or.binlog.impl.event.FormatDescriptionEvent;
import com.google.code.or.io.XInputStream;

/**
 *
 * @author Jingqi Xu
 */
public class FormatDescriptionEventParser extends AbstractBinlogEventParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(FormatDescriptionEventParser.class);
	/**
	 *
	 */
	public FormatDescriptionEventParser() {
		super(FormatDescriptionEvent.EVENT_TYPE);
	}

	/**
	 *
	 */
	public void parse(XInputStream is, BinlogEventV4Header header, BinlogParserContext context)
	throws IOException {
		final FormatDescriptionEvent event = new FormatDescriptionEvent(header);
		event.setBinlogFilename(context.getBinlogFileName());
		event.setBinlogVersion(is.readInt(2));
		event.setServerVersion(is.readFixedLengthString(50));
		event.setCreateTimestamp(is.readLong(4) * 1000L);
		event.setHeaderLength(is.readInt(1));

		int eventTypeLength = (int) (event.getHeader().getEventLength() - (event.getHeaderLength() + 57));
		byte[] eventTypeBuffer;

		if ( event.checksumPossible() ) {
			eventTypeBuffer = is.readBytes(eventTypeLength - 4);
		} else {
			eventTypeBuffer = is.readBytes(eventTypeLength);
		}

		event.setEventTypes(eventTypeBuffer);

		// for mysql 5.6, there will always be space for a checksum in the FormatDescriptionEvent, even if checksums are off,
		// but our checksumming code will not have been active, so we don't bother to verify the checksum
		// of the formatLogDescription event.

		if ( event.checksumPossible() ) {
			is.readBytes(4);
		}

		context.setChecksumEnabled(event.checksumEnabled());
		context.getEventListener().onEvents(event);
	}
}
