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
package com.google.code.or.common.glossary.column;

import com.google.code.or.common.glossary.Column;
import com.google.code.or.common.util.MySQLUtils;

/**
 *
 * @author Jingqi Xu
 */
public final class DatetimeColumn implements Column {
	//
	private static final long serialVersionUID = 6444968242222031354L;

	//
	private final java.util.Date value;
	private long longValue;

	/**
	 *
	 */
	private DatetimeColumn(java.util.Date value) {
		this.value = value;
	}

	private DatetimeColumn(long value) {
		this.longValue = value;
		this.value = MySQLUtils.toDatetime(value);
	}

	/**
	 *
	 */
	@Override
	public String toString() {
		return String.valueOf(this.value);
	}

	/**
	 *
	 */
	public java.util.Date getValue() {
		return this.value;
	}

	public long getLongValue() {
		return this.longValue;
	}


	/**
	 *
	 */
	public static final DatetimeColumn valueOf(java.util.Date value) {
		return new DatetimeColumn(value);
	}

	public static final DatetimeColumn valueOf(long value) {
		return new DatetimeColumn(value);
	}
}
