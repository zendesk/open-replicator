package com.google.code.or.io;

import java.io.IOException;

import com.google.code.or.binlog.BinlogEventV4Header;

public class CRCException extends IOException {
	/**
	 *
	 */
	private static final long serialVersionUID = -3079479140853693743L;

	/**
	 *
	 */
	public CRCException(BinlogEventV4Header header) {
		super("CRC Exception processing " + header);
	}

}
