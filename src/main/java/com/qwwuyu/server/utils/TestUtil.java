package com.qwwuyu.server.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

public class TestUtil {
	public static void sys(HttpServletRequest request) {
		Iterator<Entry<String, String[]>> it = request.getParameterMap().entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String[]> next = it.next();
			System.out.println(next.getKey() + ":" + Arrays.toString(next.getValue()));
		}
	}
}