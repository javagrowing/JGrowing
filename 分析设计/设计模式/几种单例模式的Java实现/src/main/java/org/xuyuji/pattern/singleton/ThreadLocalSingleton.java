package org.xuyuji.pattern.singleton;

public class ThreadLocalSingleton {

	private static final ThreadLocal<ThreadLocalSingleton> threadLocalSingleton = new ThreadLocal<ThreadLocalSingleton>() {
		@Override
		protected ThreadLocalSingleton initialValue() {
			return new ThreadLocalSingleton();
		}
	};

	private ThreadLocalSingleton() {
	}

	public static ThreadLocalSingleton getInstance() {
		return threadLocalSingleton.get();
	}
}
