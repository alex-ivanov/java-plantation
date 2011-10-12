import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FieldAccessTest {
	private static final int AMOUNT = 100000000;
	private static final int CHECK_TIMES = 30;

	private long field = 0;

	public static long readModifyDirect(long amount, FieldAccessTest object) {
		object.field = 0;
		for (int i = 0; i < amount; i++) {
			object.field = object.field + i;
		}
		return object.field;
	}

	public static long readModifyReflection(Field f, long amount, FieldAccessTest object) throws IllegalAccessException {
		f.setLong(object, 0);
		for (int i = 0; i < amount; i++) {
			f.setLong(object, f.getLong(object) + i);
		}
		return f.getLong(object);
	}

	public static long readModifyUnsafe(Unsafe unsafe, long offset, long amount, FieldAccessTest object) {
		for (int i = 0; i < amount; i++) {
			unsafe.putLong(object, offset, unsafe.getLong(object, offset) + i);
		}
		return unsafe.getLong(object, offset);
	}

	private static long calculateMedian(List<Long> data) {
		Collections.sort(data);
		return data.get(data.size() / 2);
	}

	private static long checkDirect() {
		long startTime = System.currentTimeMillis();
		System.err.println(readModifyDirect(AMOUNT, new FieldAccessTest()));
		return System.currentTimeMillis() - startTime;
	}

	private static long checkReflection() throws NoSuchFieldException, IllegalAccessException {
		Field f = FieldAccessTest.class.getDeclaredField("field");
		f.setAccessible(true);
		long startTime = System.currentTimeMillis();
		System.err.println(readModifyReflection(f, AMOUNT, new FieldAccessTest()));
		return System.currentTimeMillis() - startTime;
	}

	private static long checkUnsafe() throws NoSuchFieldException, IllegalAccessException {
		Field unField = Unsafe.class.getDeclaredField("theUnsafe");
		unField.setAccessible(true);
		Unsafe unsafe = (Unsafe) unField.get(null);
		Field f = FieldAccessTest.class.getDeclaredField("field");
		long offset = unsafe.objectFieldOffset(f);
		long startTime = System.currentTimeMillis();
		System.err.println(readModifyUnsafe(unsafe, offset, AMOUNT, new FieldAccessTest()));
		return System.currentTimeMillis() - startTime;
	}

	public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
		System.out.println("Unsafe warm up");
		checkUnsafe();
		long averageUnsafeTime = 0;
		List<Long> unsafeMeasure = new ArrayList<Long>(CHECK_TIMES);
		for (int i = 0; i < CHECK_TIMES; i++) {
			long value = checkUnsafe();
			averageUnsafeTime = (averageUnsafeTime * i + value) / (i + 1);
			unsafeMeasure.add(value);
		}
		System.out.println("Unsafe modification check is done. Average is " + averageUnsafeTime + " ms.");

		System.out.println("Reflection warm up");
		checkReflection();
		long averageReflectionTime = 0;
		List<Long> reflectionMeasure = new ArrayList<Long>(CHECK_TIMES);
		for (int i = 0; i < CHECK_TIMES; i++) {
			long value = checkReflection();
			averageReflectionTime = (averageReflectionTime * i + value) / (i + 1);
			reflectionMeasure.add(value);
		}
		System.out.println("Reflection modification check is done. Average is " + averageReflectionTime + " ms.");

		System.out.println("Direct warm up");
		checkDirect();
		long averageDirectTime = 0;
		List<Long> directMeasure = new ArrayList<Long>(CHECK_TIMES);
		for (int i = 0; i < CHECK_TIMES; i++) {
			long value = checkDirect();
			averageDirectTime = (averageDirectTime * i + value) / (i + 1);
			directMeasure.add(value);
		}
		System.out.println("Results:");
		System.out.println("Unsafe modification average is " + averageUnsafeTime + " ms. Median: " + calculateMedian(unsafeMeasure) + " ms.");
		System.out.println("Direct modification average is " + averageDirectTime + " ms. Median: " + calculateMedian(directMeasure) + " ms.");
		System.out.println("Reflection modification average is " + averageReflectionTime + " ms. Median: " + calculateMedian(reflectionMeasure) + " ms.");
	}
}
