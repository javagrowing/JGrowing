package org.xuyuji.pattern.singleton;

import static org.junit.Assert.assertNotSame;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class ReflectionTest {

	@Test
	public void testDestroyHungerSingleton() throws Exception {
		Class<HungerSingleton> clazz = HungerSingleton.class;
		Constructor<HungerSingleton> c = clazz.getDeclaredConstructor();
		c.setAccessible(true);
		assertNotSame(HungerSingleton.getInstance(), c.newInstance());
	}

	@Test(expected = InvocationTargetException.class)
	public void testDestroyHungerStaticSingleton() throws Exception {
		Class<HungerStaticSingleton> clazz = HungerStaticSingleton.class;
		Constructor<HungerStaticSingleton> c = clazz.getDeclaredConstructor();
		c.setAccessible(true);
		assertNotSame(HungerStaticSingleton.getInstance(), c.newInstance());
	}

	@Test(expected = NoSuchMethodException.class)
	public void testDestroyEnumSingleton() throws Exception {
		Class<EnumSingleton> clazz = EnumSingleton.class;
		Constructor<EnumSingleton> c = clazz.getDeclaredConstructor();
		c.setAccessible(true);
		assertNotSame(EnumSingleton.INSTANCE, c.newInstance());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDestroyEnumSingleton2() throws Exception {
		Class<EnumSingleton> clazz = EnumSingleton.class;
		Constructor<EnumSingleton> c = clazz.getDeclaredConstructor(String.class, int.class);
		c.setAccessible(true);
		assertNotSame(EnumSingleton.INSTANCE, c.newInstance("test", 1));
	}
}
