package org.xuyuji.pattern.singleton;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

public class SerializableTest {

	@Test
	public void testDestroyHungerStaticSingleton() throws Exception {
		HungerStaticSingleton instance = HungerStaticSingleton.getInstance();
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(instance);
			try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
					ObjectInputStream ois = new ObjectInputStream(bis)) {
				HungerStaticSingleton copy = (HungerStaticSingleton) ois.readObject();
				assertNotSame(instance, copy);
			}
		}
	}

	@Test
	public void testDestroySerializableSingleton() throws Exception {
		SerializableSingleton instance = SerializableSingleton.getInstance();
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(instance);
			try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
					ObjectInputStream ois = new ObjectInputStream(bis)) {
				SerializableSingleton copy = (SerializableSingleton) ois.readObject();
				assertEquals(instance, copy);
			}
		}
	}

	@Test
	public void testDestroyEnumSingleton() throws Exception {
		EnumSingleton instance = EnumSingleton.INSTANCE;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(instance);
			try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
					ObjectInputStream ois = new ObjectInputStream(bis)) {
				EnumSingleton copy = (EnumSingleton) ois.readObject();
				assertEquals(instance, copy);
			}
		}
	}
}
