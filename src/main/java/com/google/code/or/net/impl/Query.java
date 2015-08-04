package com.google.code.or.net.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.or.common.glossary.column.StringColumn;
import com.google.code.or.net.Packet;
import com.google.code.or.net.Transport;
import com.google.code.or.net.TransportException;
import com.google.code.or.net.impl.packet.EOFPacket;
import com.google.code.or.net.impl.packet.ErrorPacket;
import com.google.code.or.net.impl.packet.ResultSetHeaderPacket;
import com.google.code.or.net.impl.packet.ResultSetRowPacket;
import com.google.code.or.net.impl.packet.command.ComQuery;

public class Query {
	private static final Logger LOGGER = LoggerFactory.getLogger(Query.class);
	private final Transport transport;

	public Query(Transport transport) {
		this.transport = transport;
	}

	public List<String> getFirst(String sql) throws IOException, TransportException {
		List<String> result = null;

		final ComQuery command = new ComQuery();
		command.setSql(StringColumn.valueOf(sql.getBytes()));
		transport.getOutputStream().writePacket(command);
		transport.getOutputStream().flush();

		//
		Packet packet = transport.getInputStream().readPacket();
		if(packet.getPacketBody()[0] == ErrorPacket.PACKET_MARKER) {
			throw new TransportException(ErrorPacket.valueOf(packet));
		}

		ResultSetHeaderPacket header = ResultSetHeaderPacket.valueOf(packet);
		if ( header.getFieldCount().longValue() == 0 ) {
			return null;
		}

		while(true) {
			packet = transport.getInputStream().readPacket();
			if(packet.getPacketBody()[0] == EOFPacket.PACKET_MARKER) {
				break;
			}
		}

		while(true) {
			packet = transport.getInputStream().readPacket();
			if(packet.getPacketBody()[0] == EOFPacket.PACKET_MARKER) {
				break;
			} else {
				ResultSetRowPacket row = ResultSetRowPacket.valueOf(packet);
				if ( result == null ) {
					result = new ArrayList<String>();

					for ( StringColumn c : row.getColumns() ) {
						result.add(c.toString() );
					}
				}
			}
		}
		return result;
	}
}
