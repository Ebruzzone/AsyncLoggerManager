package AsyncLogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AsyncLoggerManager {

	private static org.apache.logging.log4j.core.async.AsyncLogger logger;

	final ConcurrentLinkedQueue<String> logsTrace;
	final ConcurrentLinkedQueue<String> logsDebug;
	final ConcurrentLinkedQueue<String> logsInfo;
	final ConcurrentLinkedQueue<String> logsWarn;
	final ConcurrentLinkedQueue<String> logsError;
	final ConcurrentLinkedQueue<String> logsFatal;

	private LinkedList<String> trace = new LinkedList<>(), debug = new LinkedList<>(), info = new LinkedList<>(),
			warn = new LinkedList<>(), error = new LinkedList<>(), fatal = new LinkedList<>();

	private final LoggerClassInternal loggerClassInternal;

	private final AtomicInteger sevMin;
	private final AtomicBoolean empty;

	private AtomicLong wait;
	private final long waitMax;

	private final AtomicBoolean alive = new AtomicBoolean(true);

	public enum Severity {
		TRACE, DEBUG, INFO, WARN, ERROR, FATAL
	}

	public AsyncLoggerManager(Severity sevMin, long waitMax) {

		this.waitMax = waitMax / 2;
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

		loggerClassInternal = new LoggerClassInternal();
		new Thread(loggerClassInternal).start();

		LoggerInternal loggerInternal = new LoggerInternal();
		new Thread(loggerInternal).start();

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

	private class LoggerInternal implements Runnable {

		@Override
		public void run() {

			while (alive.get()) {

				try {
					Thread.sleep(wait.get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (!empty.get()) {
					synchronized (loggerClassInternal) {

						for (String l : trace) {
							logger.trace(l);
						}

						for (String l : debug) {
							logger.debug(l);
						}

						for (String l : info) {
							logger.info(l);
						}

						for (String l : warn) {
							logger.warn(l);
						}

						for (String l : error) {
							logger.error(l);
						}

						for (String l : fatal) {
							logger.fatal(l);
						}

						trace.clear();
						debug.clear();
						info.clear();
						warn.clear();
						error.clear();
						fatal.clear();
					}
				}
			}
		}
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

				synchronized (this) {

					int size = logsTrace.size();

					for (int i = 0; i < size; i++) {
						trace.addLast(logsTrace.poll());
					}

					sizeTemp += size;
					size = logsDebug.size();

					for (int i = 0; i < size; i++) {
						debug.addLast(logsDebug.poll());
					}

					sizeTemp += size;
					size = logsInfo.size();

					for (int i = 0; i < size; i++) {
						info.addLast(logsInfo.poll());
					}

					sizeTemp += size;
					size = logsWarn.size();

					for (int i = 0; i < size; i++) {
						warn.addLast(logsWarn.poll());
					}

					sizeTemp += size;
					size = logsError.size();

					for (int i = 0; i < size; i++) {
						error.addLast(logsError.poll());
					}

					sizeTemp += size;
					size = logsFatal.size();

					for (int i = 0; i < size; i++) {
						fatal.addLast(logsFatal.poll());
					}
				}

				empty.compareAndSet(true, sizeTemp > 0);

				if (sizeTemp > wait.get() * 10 && wait.get() > 1) {
					wait.decrementAndGet();
				} else if (sizeTemp < wait.get() / 5 && wait.get() < waitMax) {
					wait.incrementAndGet();
				}
			}
		}
	}
}