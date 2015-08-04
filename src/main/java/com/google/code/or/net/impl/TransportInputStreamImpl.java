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
package com.google.code.or.net.impl;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.or.io.impl.XInputStreamImpl;
import com.google.code.or.net.Packet;
import com.google.code.or.net.TransportInputStream;
import com.google.code.or.net.impl.packet.RawPacket;

/**
 *
 * @author Jingqi Xu
 */
public class TransportInputStreamImpl extends XInputStreamImpl implements TransportInputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(TransportInputStreamImpl.class);
	private static final int MAX_PACKET_SIZE = 0xFFFFFF;
	private int currentPacketSequence;

	/**
	 *
	 */
	public TransportInputStreamImpl(InputStream is) {
		super(is);
		this.readLimit = this.readCount = 0;
	}

	public TransportInputStreamImpl(InputStream is, int size) {
		super(is, size);
		this.readLimit = this.readCount = 0;
	}

	/**
	 *
	 */
	public Packet readPacket() throws IOException {
		final RawPacket r = readPacketHeader();

		final byte[] body = new byte[r.getLength()];

		this.read(body, 0,  r.getLength());
		r.setPacketBody(body);

		return r;
	}

	private RawPacket readPacketHeader() throws IOException {
		final RawPacket r = new RawPacket();

		// read next header
		this.setReadLimit(4);
		int packetLength  = this.readInt(3);
		this.currentPacketSequence = this.readInt(1); // consume packet sequence #

		this.setReadLimit(packetLength);
		r.setLength(packetLength);
		r.setSequence(this.currentPacketSequence);

		return r;
	}

	// TODO: could fall off the edge
	@Override
	public int read() throws IOException {
		if ( this.readCount + 1 > this.readLimit ) {
			readPacketHeader();
		}
		return super.read();
	}

	@Override
	public int read(final byte b[], int off, final int len) throws IOException {
		int left = len;

		// if we're about to read off the end of read-limit, see if this is a response
		// that spans multiple packets.
		while ( (this.readCount + left) > this.readLimit ) {

			// consume from middle of buffer to end of packet.
			int remaining_length = this.readLimit - this.readCount;
			super.read(b, off, remaining_length);

			readPacketHeader();

			left -= remaining_length;
			off += remaining_length;
		}

		// now consume whatever's left
		super.read(b, off, left);

		return len;
	}

	public int currentPacketLength() {
		return this.readLimit;
	}

	public int currentPacketSequence() {
		return this.currentPacketSequence;
	}
}
