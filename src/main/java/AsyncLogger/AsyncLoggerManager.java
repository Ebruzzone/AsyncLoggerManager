package AsyncLogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class AsyncLoggerManager {
	// Apache AsyncLogger log4j
	private final org.apache.logging.log4j.core.async.AsyncLogger logger;
	// Queue that contains all the logs that are ready to log
	private final ConcurrentLinkedQueue<Log> queue;
	// int of all the severities
	static final int trace = Severity.TRACE.ordinal();
	static final int debug = Severity.DEBUG.ordinal();
	static final int info = Severity.INFO.ordinal();
	static final int warn = Severity.WARN.ordinal();
	static final int error = Severity.ERROR.ordinal();
	static final int fatal = Severity.FATAL.ordinal();
	static final int noLog = Severity.NO_LOG.ordinal();
	// static array of functional interface lambdas for logging
	private static final LogWithSeverity[] toLog = new LogWithSeverity[noLog];
	
	// static filling of the array of lambdas
	static {
		toLog[trace] = (logger, log) -> logger.trace(log.marker, log.message, log.objects);
		toLog[debug] = (logger, log) -> logger.debug(log.marker, log.message, log.objects);
		toLog[info] = (logger, log) -> logger.info(log.marker, log.message, log.objects);
		toLog[warn] = (logger, log) -> logger.warn(log.marker, log.message, log.objects);
		toLog[error] = (logger, log) -> logger.error(log.marker, log.message, log.objects);
		toLog[fatal] = (logger, log) -> logger.fatal(log.marker, log.message, log.objects);
	}
	
	// minimum severity of log to be processed
	private final AtomicInteger sevMin;
	// last severity of log set
	private final AtomicInteger lastSet;
	// maximum milliseconds that a log can wait in the queue
	private final long waitMax;
	// if logging is not critical, you can choose cpuSaving true
	private final boolean cpuSaving;
	// 'enum' if the AsyncLogger is active, paused or killed
	private final AtomicInteger status = new AtomicInteger(2);
	
	public AsyncLoggerManager() {
		this(Severity.INFO, 500, false);
	}
	
	public AsyncLoggerManager(@NotNull Severity sevMin) {
		this(sevMin, 500, false);
	}
	
	public AsyncLoggerManager(@NotNull Severity sevMin, boolean cpuSaving) {
		this(sevMin, 500, cpuSaving);
	}
	
	public AsyncLoggerManager(@NotNull Severity sevMin, long waitMax, boolean cpuSaving) {
		if (waitMax < 1) {
			throw new IllegalArgumentException("Waiting milliseconds must be > 0");
		}
		// waitMax is calculated with a certain margin
		this.waitMax = waitMax - ((waitMax + 118) >> 7);
		// save cpu
		this.cpuSaving = cpuSaving;
		// minimum severity
		this.sevMin = new AtomicInteger(sevMin.ordinal());
		this.lastSet = new AtomicInteger(sevMin.ordinal());
		// new queue
		this.queue = new ConcurrentLinkedQueue<>();
		// thread that logs
		new AsyncLoggerManager.Logger().start();
		// new Apache AsyncLogger
		System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
		this.logger = (org.apache.logging.log4j.core.async.AsyncLogger) LogManager.getLogger();
	}
	
	// if the log are on console or on file
	public enum LoggerType {
		Console, RandomAccessFile
	}
	
	// create the logging properties to log both console and file and a AsyncLogger
	@NotNull
	@Contract("_, _, _, _, _, _, _, _ -> new")
	public static AsyncLoggerManager createLogProperties(@NotNull Severity rootSeverity, String nameLoggerConsole,
	                                                     String patternLoggerConsole, String nameLoggerFile,
	                                                     String patternLoggerFile, @NotNull String fileName,
	                                                     long waitMax, boolean cpuSaving) throws IOException {
		String t1 = "Console", t2 = "RandomAccessFile";
		String file = "rootLogger.level = " + rootSeverity.toString() + "\n" + "rootLogger.appenderRefs = " +
				t1 + "," + t2 + "\nrootLogger.appenderRef." + t1 + ".ref = " + nameLoggerConsole +
				"\nrootLogger.appenderRef." + t2 + ".ref = " + nameLoggerFile + "\n\nappenders = " + t1 + "," + t2 +
				"\n\nappender." + t1 + ".type = " + t1 + "\nappender." + t1 + ".name = " + nameLoggerConsole +
				"\nappender." + t1 + ".layout.type = PatternLayout\nappender." + t1 + ".layout.pattern = " +
				patternLoggerConsole + "\n\nappender." + t2 + ".type = " + t2 + "\nappender." + t2 + ".name = " +
				nameLoggerFile + "\nappender." + t2 + ".layout.type = PatternLayout\nappender." + t2 +
				".layout.pattern = " + patternLoggerFile + "\nappender." + t2 + ".fileName = " + fileName;
		String path = Objects.requireNonNull(AsyncLoggerManager.class.getClassLoader().getResource("")).getPath();
		path = path.startsWith("/C:/") ? path.substring(1) : path;
		Files.write(Paths.get(path + "log4j2.properties"), file.getBytes());
		
		return new AsyncLoggerManager(rootSeverity, waitMax, cpuSaving);
	}
	
	// create the logging properties to log both console or file and a AsyncLogger
	@NotNull
	public static AsyncLoggerManager createLogProperties(LoggerType type, String name, String pattern,
	                                                     Severity rootSeverity, long waitMax) throws IOException {
		return createLogProperties(type, name, pattern, rootSeverity, "logs.log", waitMax, false);
	}
	
	// create the logging properties to log both console or file and a AsyncLogger
	@NotNull
	@Contract("_, _, _, _, _, _, _ -> new")
	public static AsyncLoggerManager createLogProperties(@NotNull LoggerType type, String name, String pattern,
	                                                     @NotNull Severity rootSeverity, @NotNull String fileName,
	                                                     long waitMax, boolean cpuSaving) throws IOException {
		String t = type.toString(), rowFile;
		if (type == LoggerType.RandomAccessFile) {
			rowFile = "\nappender." + t + ".fileName = " + fileName;
		} else {
			rowFile = "\n";
		}
		String file = "rootLogger.level = " + rootSeverity.toString() + "\n" + "rootLogger.appenderRefs = " +
				t + "\nrootLogger.appenderRef." + t + ".ref = " + name + "\n\nappenders = " + t + "\n\nappender." + t +
				".type = " + t + "\nappender." + t + ".name = " + name + "\nappender." + t +
				".layout.type = PatternLayout\nappender." + t + ".layout.pattern = " + pattern + rowFile;
		String path = Objects.requireNonNull(AsyncLoggerManager.class.getClassLoader().getResource("")).getPath();
		path = path.startsWith("/C:/") ? path.substring(1) : path;
		Files.write(Paths.get(path + "log4j2.properties"), file.getBytes());
		return new AsyncLoggerManager(rootSeverity, waitMax, cpuSaving);
	}
	
	// set the minimum severity
	public void setSeverityMin(@NotNull Severity severityMin) {
		int last = this.lastSet.getAndSet(severityMin.ordinal());
		if (this.sevMin.get() < this.lastSet.get()) {
			this.sevMin.set(this.lastSet.get());
		} else {
			this.sevMin.compareAndSet(last, this.lastSet.get());
		}
	}
	
	// log the last logs and kill the AsyncLogger
	public void kill() {
		synchronized (status) {
			status.set(0);
			sevMin.set(noLog);
			lastSet.set(noLog);
			status.notify();
		}
	}
	
	// pause logging, to use when you want save CPU
	public void pause() {
		if (status.get() < 1) {
			throw new IllegalStateException("Logger is already killed");
		}
		sevMin.set(noLog);
		status.set(1);
	}
	
	// restart logging
	public void go() {
		if (status.get() < 1) {
			throw new IllegalStateException("Logger is already killed");
		}
		synchronized (status) {
			sevMin.compareAndSet(noLog, lastSet.get());
			status.compareAndSet(1, 2);
			status.notify();
		}
	}
	
	// restart thread and logging, after killed too.
	// WARNING: it is not efficient if the thread is already killed
	public void restart(@NotNull Severity newSeverity) {
		synchronized (status) {
			sevMin.compareAndSet(noLog, newSeverity.ordinal());
			lastSet.set(newSeverity.ordinal());
			
			if (status.get() < 1) {
				if (status.get() == 0) {
					try {
						status.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				new AsyncLoggerManager.Logger().start();
			}
			
			if (status.get() < 2) {
				status.set(2);
				status.notify();
			}
		}
	}
	
	// add a log of trace severity
	public void logTrace(String s) {
		if (sevMin.get() <= trace) {
			queue.offer(new Log(s, trace));
		}
	}
	
	// add a log of debug severity
	public void logDebug(String s) {
		if (sevMin.get() <= debug) {
			queue.offer(new Log(s, debug));
		}
	}
	
	// add a log of info severity
	public void logInfo(String s) {
		if (sevMin.get() <= info) {
			queue.offer(new Log(s, info));
		}
	}
	
	// add a log of warning severity
	public void logWarn(String s) {
		if (sevMin.get() <= warn) {
			queue.offer(new Log(s, warn));
		}
	}
	
	// add a log of error severity
	public void logError(String s) {
		if (sevMin.get() <= error) {
			queue.offer(new Log(s, error));
		}
	}
	
	// add a log of fatal severity
	public void logFatal(String s) {
		if (sevMin.get() <= fatal) {
			queue.offer(new Log(s, fatal));
		}
	}
	
	public void logTrace(String s, Object... params) {
		if (sevMin.get() <= trace) {
			queue.offer(new Log(s, trace, params));
		}
	}
	
	public void logDebug(String s, Object... params) {
		if (sevMin.get() <= debug) {
			queue.offer(new Log(s, debug, params));
		}
	}
	
	public void logInfo(String s, Object... params) {
		if (sevMin.get() <= info) {
			queue.offer(new Log(s, info, params));
		}
	}
	
	public void logWarn(String s, Object... params) {
		if (sevMin.get() <= warn) {
			queue.offer(new Log(s, warn, params));
		}
	}
	
	public void logError(String s, Object... params) {
		if (sevMin.get() <= error) {
			queue.offer(new Log(s, error, params));
		}
	}
	
	public void logFatal(String s, Object... params) {
		if (sevMin.get() <= fatal) {
			queue.offer(new Log(s, fatal, params));
		}
	}
	
	public void logTrace(Marker marker, String s) {
		if (sevMin.get() <= trace) {
			queue.offer(new Log(marker, s, trace));
		}
	}
	
	public void logDebug(Marker marker, String s) {
		if (sevMin.get() <= debug) {
			queue.offer(new Log(marker, s, debug));
		}
	}
	
	public void logInfo(Marker marker, String s) {
		if (sevMin.get() <= info) {
			queue.offer(new Log(marker, s, info));
		}
	}
	
	public void logWarn(Marker marker, String s) {
		if (sevMin.get() <= warn) {
			queue.offer(new Log(marker, s, warn));
		}
	}
	
	public void logError(Marker marker, String s) {
		if (sevMin.get() <= error) {
			queue.offer(new Log(marker, s, error));
		}
	}
	
	public void logFatal(Marker marker, String s) {
		if (sevMin.get() <= fatal) {
			queue.offer(new Log(marker, s, fatal));
		}
	}
	
	public void logTrace(Marker marker, String s, Object... params) {
		if (sevMin.get() <= trace) {
			queue.offer(new Log(marker, s, trace, params));
		}
	}
	
	public void logDebug(Marker marker, String s, Object... params) {
		if (sevMin.get() <= debug) {
			queue.offer(new Log(marker, s, debug, params));
		}
	}
	
	public void logInfo(Marker marker, String s, Object... params) {
		if (sevMin.get() <= info) {
			queue.offer(new Log(marker, s, info, params));
		}
	}
	
	public void logWarn(Marker marker, String s, Object... params) {
		if (sevMin.get() <= warn) {
			queue.offer(new Log(marker, s, warn, params));
		}
	}
	
	public void logError(Marker marker, String s, Object... params) {
		if (sevMin.get() <= error) {
			queue.offer(new Log(marker, s, error, params));
		}
	}
	
	public void logFatal(Marker marker, String s, Object... params) {
		if (sevMin.get() <= fatal) {
			queue.offer(new Log(marker, s, fatal, params));
		}
	}
	
	// add a trace log after computing a lambda expression to have the message
	// to notice that if the minimum severity is more than trace, the lambda is not processed
	public <O> void logTrace(@NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= trace) {
			queue.offer(new Log(toLogMessage.action(object), trace));
		}
	}
	
	// add a debug log after computing a lambda expression to have the message
	// to notice that if the minimum severity is more than debug, the lambda is not processed
	public <O> void logDebug(@NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= debug) {
			queue.offer(new Log(toLogMessage.action(object), debug));
		}
	}
	
	// add an info log after computing a lambda expression to have the message
	// to notice that if the minimum severity is more than info, the lambda is not processed
	public <O> void logInfo(@NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= info) {
			queue.offer(new Log(toLogMessage.action(object), info));
		}
	}
	
	// add a warning log after computing a lambda expression to have the message
	// to notice that if the minimum severity is more than warning, the lambda is not processed
	public <O> void logWarn(@NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= warn) {
			queue.offer(new Log(toLogMessage.action(object), warn));
		}
	}
	
	// add an error log after computing a lambda expression to have the message
	// to notice that if the minimum severity is more than error, the lambda is not processed
	public <O> void logError(@NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= error) {
			queue.offer(new Log(toLogMessage.action(object), error));
		}
	}
	
	// add a fatal log after computing a lambda expression to have the message
	// to notice that if the minimum severity is more than fatal, the lambda is not processed
	public <O> void logFatal(@NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= fatal) {
			queue.offer(new Log(toLogMessage.action(object), fatal));
		}
	}
	
	public <O> void logTrace(Marker marker, @NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= trace) {
			queue.offer(new Log(marker, toLogMessage.action(object), trace));
		}
	}
	
	public <O> void logDebug(Marker marker, @NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= debug) {
			queue.offer(new Log(marker, toLogMessage.action(object), debug));
		}
	}
	
	public <O> void logInfo(Marker marker, @NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= info) {
			queue.offer(new Log(marker, toLogMessage.action(object), info));
		}
	}
	
	public <O> void logWarn(Marker marker, @NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= warn) {
			queue.offer(new Log(marker, toLogMessage.action(object), warn));
		}
	}
	
	public <O> void logError(Marker marker, @NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= error) {
			queue.offer(new Log(marker, toLogMessage.action(object), error));
		}
	}
	
	public <O> void logFatal(Marker marker, @NotNull ToLogMessage<O> toLogMessage, O object) {
		if (sevMin.get() <= fatal) {
			queue.offer(new Log(marker, toLogMessage.action(object), fatal));
		}
	}
	
	// log a log with a specified severity
	public void log(String log, @NotNull Severity severity) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(new Log(log, severity.ordinal()));
		}
	}
	
	// log a log with a specified severity
	public void log(String log, @NotNull Severity severity, Object... params) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(new Log(log, severity.ordinal(), params));
		}
	}
	
	// log a log with a specified severity
	public void log(Marker marker, String log, @NotNull Severity severity) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(new Log(marker, log, severity.ordinal()));
		}
	}
	
	// log a log with a specified severity
	public void log(Marker marker, String log, @NotNull Severity severity, Object... params) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(new Log(marker, log, severity.ordinal(), params));
		}
	}
	
	// process an object and log a log with a specified severity
	public <O> void log(@NotNull ToLogMessage<O> toLogMessage, O object, @NotNull Severity severity) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(new Log(toLogMessage.action(object), severity.ordinal()));
		}
	}
	
	// process an object and log a log with a specified severity
	public <O> void log(@NotNull ToLogMessage<O> toLogMessage, O object, @NotNull Severity severity,
	                    Object... params) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(new Log(toLogMessage.action(object), severity.ordinal(), params));
		}
	}
	
	// process an object and log a log with a specified severity
	public <O> void log(Marker marker, @NotNull ToLogMessage<O> toLogMessage, O object, @NotNull Severity severity) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(new Log(marker, toLogMessage.action(object), severity.ordinal()));
		}
	}
	
	// process an object and log a log with a specified severity
	public <O> void log(Marker marker, @NotNull ToLogMessage<O> toLogMessage, O object, @NotNull Severity severity,
	                    Object... params) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(new Log(marker, toLogMessage.action(object), severity.ordinal(), params));
		}
	}
	
	// process an object and log a log with a specified severity
	public <O> void log(@NotNull ToLog<O> toLog, O object, @NotNull Severity severity) {
		if (sevMin.get() <= severity.ordinal()) {
			if (severity.ordinal() >= noLog) {
				throw new IllegalArgumentException("Severity must be less than NO_LOG");
			}
			queue.offer(toLog.action(object).addSeverity(severity.ordinal()));
		}
	}
	
	// Thread with waiting time management
	private class Logger extends Thread {
		
		private static final int LOG_TIMES = 1024;
		private static final String warning = "There are too logs to log them all";
		
		@Override
		public void run() {
			long time;
			int times;
			while (status.get() > 0) {
				time = System.currentTimeMillis();
				while (status.get() == 2) {
					int temp1 = sevMin.get();
					
					try {
						Thread.sleep(waitMax);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					log();
					long temp;
					if ((temp = waitMax + time) + waitMax < (time = System.currentTimeMillis())) {
						if (cpuSaving) {
							AsyncLoggerManager.this.logWarn(warning);
							sevMin.compareAndSet(temp1, sevMin.get() + 1);
						} else {
							status.compareAndSet(2, 3);
						}
					} else if (time - temp < 2 && lastSet.get() < (temp1 = sevMin.get())) {
						sevMin.compareAndSet(temp1, sevMin.get() - 1);
					}
				}
				
				while (status.get() == 3) {
					times = 0;
					for (int i = 0; i < LOG_TIMES; i++) {
						times += logSize();
					}
					if (times > LOG_TIMES) {
						AsyncLoggerManager.this.logWarn(warning);
						sevMin.set(sevMin.get() + 1);
					} else if (times < 2) {
						if (lastSet.get() >= sevMin.get()) {
							status.compareAndSet(3, 2);
						} else {
							sevMin.set(Math.max(sevMin.get() - 1, lastSet.get()));
						}
					}
				}
				
				if (status.get() == 1) {
					synchronized (status) {
						try {
							status.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			log();
			
			synchronized (status) {
				status.set(-1);
				status.notifyAll();
			}
		}
		
		private void log() {
			final Iterator<Log> iter = queue.iterator();
			while (iter.hasNext()) {
				final Log log = iter.next();
				toLog[log.severity].log(logger, log);
				iter.remove();
			}
		}
		
		private int logSize() {
			int size = 0;
			final Iterator<Log> iter = queue.iterator();
			while (iter.hasNext()) {
				final Log log = iter.next();
				toLog[log.severity].log(logger, log);
				iter.remove();
				size++;
			}
			return size;
		}
	}
}