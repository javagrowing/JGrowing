package org.xuyuji.pattern.singleton;

import java.io.Serializable;

public class SerializableSingleton implements Serializable {

	private static final long serialVersionUID = -3721502058894531866L;

	private static SerializableSingleton instance;

	private SerializableSingleton() {
		if (instance != null) {
			throw new RuntimeException("非法调用");
		}
	}

	public static SerializableSingleton getInstance() {
		if (instance == null) {
			synchronized (SerializableSingleton.class) {
				if (instance == null) {
					instance = new SerializableSingleton();
				}
			}
		}
		return instance;
	}

	private Object readResolve() {
		return instance;
	}
}
