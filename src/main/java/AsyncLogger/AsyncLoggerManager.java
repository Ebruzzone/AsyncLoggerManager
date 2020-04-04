package AsyncLogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncLoggerManager {

	private static org.apache.logging.log4j.core.async.AsyncLogger logger;

	private final ConcurrentLinkedQueue<String> logsTrace;
	private final ConcurrentLinkedQueue<String> logsDebug;
	private final ConcurrentLinkedQueue<String> logsInfo;
	private final ConcurrentLinkedQueue<String> logsWarn;
	private final ConcurrentLinkedQueue<String> logsError;
	private final ConcurrentLinkedQueue<String> logsFatal;

	private final AtomicInteger sevMin;
	private final AtomicBoolean empty;

	private AtomicLong wait;
	private final long waitMax;

	private final AtomicBoolean alive = new AtomicBoolean(true);

	public enum Severity {
		TRACE, DEBUG, INFO, WARN, ERROR, FATAL
	}

	public AsyncLoggerManager(Severity sevMin, long waitMax) {

		this.waitMax = waitMax * 2 / 3;
		this.wait = new AtomicLong(waitMax / 5 + 1);
		this.sevMin = new AtomicInteger();
		this.empty = new AtomicBoolean(true);

		switch (sevMin) {
			case DEBUG:
				this.sevMin.set(1);
				break;
			case INFO:
				this.sevMin.set(2);
				break;
			case WARN:
				this.sevMin.set(3);
				break;
			case ERROR:
				this.sevMin.set(4);
				break;
			case FATAL:
				this.sevMin.set(5);
				break;
			default:
				this.sevMin.set(0);
		}

		logsTrace = new ConcurrentLinkedQueue<>();
		logsDebug = new ConcurrentLinkedQueue<>();
		logsInfo = new ConcurrentLinkedQueue<>();
		logsWarn = new ConcurrentLinkedQueue<>();
		logsError = new ConcurrentLinkedQueue<>();
		logsFatal = new ConcurrentLinkedQueue<>();

		LoggerClassInternal loggerClassInternal = new LoggerClassInternal();
		new Thread(loggerClassInternal).start();

		System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
		logger = (org.apache.logging.log4j.core.async.AsyncLogger) LogManager.getLogger();
	}

	public void kill() {
		alive.set(false);
	}

	//Method to log with the normal asyncLogger
	public void logTrace(String s) {
		if (sevMin.get() == 0) {
			logsTrace.add(s);
		}
	}

	public void logDebug(String s) {
		if (sevMin.get() < 2) {
			logsDebug.add(s);
		}
	}

	public void logInfo(String s) {
		if (sevMin.get() < 3) {
			logsInfo.add(s);
		}
	}

	public void logWarn(String s) {
		if (sevMin.get() < 4) {
			logsWarn.add(s);
		}
	}

	public void logError(String s) {
		if (sevMin.get() < 5) {
			logsError.add(s);
		}
	}

	public void logFatal(String s) {
		logsFatal.add(s);
	}

	private class LoggerClassInternal implements Runnable {

		@Override
		public void run() {

			while (alive.get()) {

				try {
					Thread.sleep(wait.get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				long sizeTemp = 0;

				int size = logsTrace.size();

				for (int i = 0; i < size; i++) {
					logger.trace(logsTrace.poll());
				}

				sizeTemp += size;
				size = logsDebug.size();

				for (int i = 0; i < size; i++) {
					logger.debug(logsDebug.poll());
				}

				sizeTemp += size;
				size = logsInfo.size();

				for (int i = 0; i < size; i++) {
					logger.info(logsInfo.poll());
				}

				sizeTemp += size;
				size = logsWarn.size();

				for (int i = 0; i < size; i++) {
					logger.warn(logsWarn.poll());
				}

				sizeTemp += size;
				size = logsError.size();

				for (int i = 0; i < size; i++) {
					logger.error(logsError.poll());
				}

				sizeTemp += size;
				size = logsFatal.size();

				for (int i = 0; i < size; i++) {
					logger.fatal(logsFatal.poll());
				}

				empty.compareAndSet(true, sizeTemp > 0);

				if (sizeTemp > wait.get() * 10 && wait.get() > 1) {
					wait.decrementAndGet();
				} else if (sizeTemp < wait.get() / 2 && wait.get() < waitMax) {
					wait.incrementAndGet();
				}
			}
		}
	}
}