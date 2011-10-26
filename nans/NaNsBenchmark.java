import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class NaNsBenchmark {
	public static long testNans() {
		long start = System.currentTimeMillis();
		long acc = Long.MIN_VALUE;
		for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE; i++) {
			int decode = NaNs.decode(NaNs.encode(i));
			if (i != decode) {
				throw new RuntimeException("Something is wrong for " + i);
			}
			acc += decode;
		}
		if (acc == Long.MAX_VALUE) {
			throw new RuntimeException("Oh, really?");
		}
		return System.currentTimeMillis() - start;
	}

	public static long testDirect() {
		long NAN_OFFSET = 0x7ff0000000000002L + Integer.MAX_VALUE;
		long start = System.currentTimeMillis();
		long acc = Long.MIN_VALUE;
		for (int i = Integer.MIN_VALUE; i < Integer.MAX_VALUE; i++) {
			double a = Double.longBitsToDouble(NAN_OFFSET + i);
			long l = Double.doubleToRawLongBits(a);
			if (!Double.isNaN(a) || l == 0) {
				throw new RuntimeException("Something is wrong for " + i);
			}
			acc += l;
		}
		if (acc == Long.MAX_VALUE) {
			throw new RuntimeException("Oh, really?");
		}
		return System.currentTimeMillis() - start;
	}

	public static void checkAndPrintStatistics(Callable<Long> test, int amountOfTests) throws Exception {
		if (amountOfTests == 0) return;
		long[] times = new long[amountOfTests];
		double average = 0;
		for (int i = 0; i < amountOfTests; i++) {
			times[i] = test.call();
			average = (average * i + times[i]) / (i + 1);
			System.out.print(".");
		}
		System.out.println();
		Arrays.sort(times);
		long median = times[times.length / 2];

		double oneOperationCost = TimeUnit.NANOSECONDS.convert(median,
				TimeUnit.MILLISECONDS) * 1.0d / (Math.abs((long) Integer.MIN_VALUE) + Integer.MAX_VALUE + 1);

		System.out.println("Average: " + average + "ms, median: " + median +
				"ms, one operation cost: " + oneOperationCost + "ns");
	}

	public static void main(String[] args) throws Exception {
		checkAndPrintStatistics(new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				return testNans();
			}
		}, 10);
		checkAndPrintStatistics(new Callable<Long>() {
			@Override
			public Long call() throws Exception {
				return testDirect();
			}
		}, 10);
	}
}
