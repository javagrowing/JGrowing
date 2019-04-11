package org.xuyuji.pattern.singleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.junit.BeforeClass;
import org.junit.Test;

public class ConcurrentTest {

	private static final int LOOP_TIMES = 100;

	private static ExecutorService es;

	@BeforeClass
	public static void init() {
		es = Executors.newFixedThreadPool(2);
	}

	@Test(expected = Exception.class)
	public void testDestroyLazySingleton() throws Exception {
		for (int i = 0; i < LOOP_TIMES; i++) {
			Future<LazySingleton> f1 = getLazySingleton();
			Future<LazySingleton> f2 = getLazySingleton();
			LazySingleton s1 = f1.get();
			LazySingleton s2 = f2.get();
			LazySingleton s3 = LazySingleton.getInstance();
			if (s1 != s2 || s2 != s3) {
				System.out.println("s1:" + s1);
				System.out.println("s2:" + s2);
				System.out.println("s3:" + s3);
				throw new Exception("单例异常");
			}
			resetLazySingleton();
		}
	}

	private Future<LazySingleton> getLazySingleton() {
		FutureTask<LazySingleton> task = new FutureTask<LazySingleton>(new Callable<LazySingleton>() {

			@Override
			public LazySingleton call() throws Exception {
				return LazySingleton.getInstance();
			}

		});
		es.execute(task);
		return task;
	}

	private void resetLazySingleton() {
		try {
			Class<LazySingleton> clazz = LazySingleton.class;
			Field f = clazz.getDeclaredField("INSTANCE");
			f.setAccessible(true);
			f.set(LazySingleton.getInstance(), null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testDestroyDoubleLockingSingleton() throws Exception {
		for (int i = 0; i < LOOP_TIMES; i++) {
			Future<DoubleLockingSingleton> f1 = getDoubleLockingSingleton();
			Future<DoubleLockingSingleton> f2 = getDoubleLockingSingleton();
			DoubleLockingSingleton s1 = f1.get();
			DoubleLockingSingleton s2 = f2.get();
			DoubleLockingSingleton s3 = DoubleLockingSingleton.getInstance();
			assertEquals(s1, s2);
			assertEquals(s2, s3);
		}

	}

	private Future<DoubleLockingSingleton> getDoubleLockingSingleton() {
		FutureTask<DoubleLockingSingleton> task = new FutureTask<DoubleLockingSingleton>(
				new Callable<DoubleLockingSingleton>() {

					@Override
					public DoubleLockingSingleton call() throws Exception {
						return DoubleLockingSingleton.getInstance();
					}

				});
		es.execute(task);
		return task;
	}

	@Test
	public void testDestroyInnerClassSingleton() throws Exception {
		for (int i = 0; i < LOOP_TIMES; i++) {
			Future<InnerClassSingleton> f1 = getInnerClassSingleton();
			Future<InnerClassSingleton> f2 = getInnerClassSingleton();
			InnerClassSingleton s1 = f1.get();
			InnerClassSingleton s2 = f2.get();
			InnerClassSingleton s3 = InnerClassSingleton.getInstance();
			assertEquals(s1, s2);
			assertEquals(s2, s3);
		}

	}

	private Future<InnerClassSingleton> getInnerClassSingleton() {
		FutureTask<InnerClassSingleton> task = new FutureTask<InnerClassSingleton>(new Callable<InnerClassSingleton>() {

			@Override
			public InnerClassSingleton call() throws Exception {
				return InnerClassSingleton.getInstance();
			}

		});
		es.execute(task);
		return task;
	}

	@Test
	public void testThreadLocalSingleton() throws Exception {
		assertEquals(ThreadLocalSingleton.getInstance(), ThreadLocalSingleton.getInstance());

		Future<ThreadLocalSingleton> f1 = getThreadLocalSingleton();
		Future<ThreadLocalSingleton> f2 = getThreadLocalSingleton();
		ThreadLocalSingleton s1 = f1.get();
		ThreadLocalSingleton s2 = f2.get();
		ThreadLocalSingleton s3 = ThreadLocalSingleton.getInstance();

		assertNotSame(s1, s2);
		assertNotSame(s2, s3);
		assertNotSame(s1, s3);
	}

	private Future<ThreadLocalSingleton> getThreadLocalSingleton() {
		FutureTask<ThreadLocalSingleton> task = new FutureTask<ThreadLocalSingleton>(
				new Callable<ThreadLocalSingleton>() {

					@Override
					public ThreadLocalSingleton call() throws Exception {
						return ThreadLocalSingleton.getInstance();
					}

				});
		es.execute(task);
		return task;
	}
}
