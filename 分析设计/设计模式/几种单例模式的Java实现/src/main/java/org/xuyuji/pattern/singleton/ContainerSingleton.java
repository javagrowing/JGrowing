package org.xuyuji.pattern.singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerSingleton {

	private static Map<String, Object> container = new ConcurrentHashMap<>();

	private ContainerSingleton() {
	}

	public static Object getInstance(String className) {
		synchronized (container) {
			if (container.containsKey(className)) {
				return container.get(className);
			} else {
				Object obj = null;
				try {
					obj = Class.forName(className).newInstance();
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					e.printStackTrace();
				}
				return obj;
			}
		}
	}
}
