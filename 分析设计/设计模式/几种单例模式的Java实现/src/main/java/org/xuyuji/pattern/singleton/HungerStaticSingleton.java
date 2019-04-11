package org.xuyuji.pattern.singleton;

import java.io.Serializable;

public class HungerStaticSingleton implements Serializable {

	private static final long serialVersionUID = 5213046951929348684L;

	private static final HungerStaticSingleton INSTANCE;

	static {
		INSTANCE = new HungerStaticSingleton();
	}

	private HungerStaticSingleton() {
		if (INSTANCE != null) {
			throw new RuntimeException("非法操作");
		}
	}

	public static HungerStaticSingleton getInstance() {
		return INSTANCE;
	}
}
