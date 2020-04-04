package Main;

import AsyncLogger.AsyncLoggerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;

public class Main {

	public static void main(String[] args) {

		System.out.println("Start");

		System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
		org.apache.logging.log4j.core.async.AsyncLogger logger =
				(org.apache.logging.log4j.core.async.AsyncLogger) LogManager.getLogger();

		long len = 100000L, t = 0, t1 = 0, t2 = 0, t3 = 0, t4, t5, t6, t7;

		AsyncLoggerManager asyncLoggerManager = new AsyncLoggerManager(AsyncLoggerManager.Severity.INFO, 1000L);

		String str = "CiaociaociaociaociaociaociaociaoCiaociaociaociaociaociaociaociao" +
				"CiaociaociaociaociaociaociaociaoCiaociaociaociaociaociaociaociao" +
				"CiaociaociaociaociaociaociaociaoCiaociaociaociaociaociaociaociao" +
				"CiaociaociaociaociaociaociaociaoCiaociaociaociaociaociaociaociao";

		for (int n = 0; n < 30; n++) {
			t4 = System.currentTimeMillis();

			for (int i = 0; i < len; i++) {

				logger.info(str);
			}

			t4 = System.currentTimeMillis() - t4;

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			t5 = System.currentTimeMillis();

			for (int i = 0; i < len; i++) {

				asyncLoggerManager.logInfo(str);
			}

			t5 = System.currentTimeMillis() - t5;

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			t6 = System.currentTimeMillis();

			for (int i = 0; i < len * 20; i++) {

				logger.trace(str);
			}

			t6 = System.currentTimeMillis() - t6;

			t7 = System.currentTimeMillis();

			for (int i = 0; i < len * 20; i++) {

				asyncLoggerManager.logTrace(str);
			}

			t7 = System.currentTimeMillis() - t7;

			t += t4;
			t1 += t5;
			t2 += t6;
			t3 += t7;
		}

		System.out.println("Time logger info: " + t);
		System.out.println("Time my logger info: " + t1);
		System.out.println("Time logger trace: " + t2);
		System.out.println("Time my logger trace: " + t3);

		asyncLoggerManager.kill();
	}
}
