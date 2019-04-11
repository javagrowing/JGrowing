package org.xuyuji.pattern.singleton;

public class DoubleLockingSingleton {

	private static volatile DoubleLockingSingleton instance;

	private DoubleLockingSingleton() {
	}

	public static DoubleLockingSingleton getInstance() {
		if (instance == null) {
			synchronized (DoubleLockingSingleton.class) {
				if (instance == null) {
					instance = new DoubleLockingSingleton();
				}
			}
		}
		return instance;
	}
}
