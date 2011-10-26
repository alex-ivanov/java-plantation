import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class NaNs {
	private static final long NAN_OFFSET = 0x7ff0000000000002L + Integer.MAX_VALUE;

	private static final Unsafe unsafe;
	private static final long fieldOffset;

	static {
		try {
			Field unField = Unsafe.class.getDeclaredField("theUnsafe");
			unField.setAccessible(true);
			unsafe = (Unsafe) unField.get(null);
			Field f = DoubleHolder.class.getField("field");
			fieldOffset = unsafe.objectFieldOffset(f);
		} catch (Exception e) {
			throw new RuntimeException("Can't initialize NaNs helper class.", e);
		}
	}

	private static final ThreadLocal<DoubleHolder> onePerThreadHolder = new ThreadLocal<DoubleHolder>() {
		@Override
		protected DoubleHolder initialValue() {
			return new DoubleHolder();
		}
	};

	private static class DoubleHolder {
		public double field;
	}

	public static double encode(int value) {
		DoubleHolder holder = onePerThreadHolder.get();
		unsafe.putLong(holder, fieldOffset, NAN_OFFSET + value);
		return holder.field;
	}

	public static int decode(double value) {
		if (!Double.isNaN(value)) {
			throw new IllegalArgumentException("Value is not NaN.");
		}
		long d = Double.doubleToRawLongBits(value);
		return (int) (d - NAN_OFFSET);
	}
}
