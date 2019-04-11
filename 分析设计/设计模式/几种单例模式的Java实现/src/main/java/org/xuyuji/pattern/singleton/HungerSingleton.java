package org.xuyuji.pattern.singleton;

public class HungerSingleton {

	private static final HungerSingleton INSTANCE = new HungerSingleton();

	private HungerSingleton() {
	}

	public static HungerSingleton getInstance() {
		return INSTANCE;
	}
}
