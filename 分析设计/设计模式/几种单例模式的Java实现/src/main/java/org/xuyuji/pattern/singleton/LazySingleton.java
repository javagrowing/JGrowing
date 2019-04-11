package org.xuyuji.pattern.singleton;

import java.lang.reflect.Field;

public class LazySingleton {

	private static LazySingleton INSTANCE;

	private LazySingleton() {
	}

	public static LazySingleton getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new LazySingleton();
		}
		return INSTANCE;
	}

	public static void main(String[] args)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		LazySingleton.getInstance();

		Class<LazySingleton> clazz = LazySingleton.class;
		Field f = clazz.getDeclaredField("INSTANCE");
		f.setAccessible(true);
		f.set(LazySingleton.getInstance(), null);

		LazySingleton.getInstance();
	}
}
