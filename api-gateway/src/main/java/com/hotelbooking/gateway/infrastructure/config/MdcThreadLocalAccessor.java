package com.hotelbooking.gateway.infrastructure.config;

import io.micrometer.context.ThreadLocalAccessor;
import org.slf4j.MDC;

public class MdcThreadLocalAccessor implements ThreadLocalAccessor<String> {
	public static final String TRACE_ID_KEY = "traceId";

	@Override
	public Object key() {
		return TRACE_ID_KEY;
	}

	@Override
	public String getValue() {
		return MDC.get(TRACE_ID_KEY);
	}

	@Override
	public void setValue(String value) {
		MDC.put(TRACE_ID_KEY, value);
	}

	@Override
	public void setValue() {
		MDC.remove(TRACE_ID_KEY);
	}
}
