package AsyncLogger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class AsyncLoggerManager {
	
	private final org.apache.logging.log4j.core.async.AsyncLogger logger;
	
	private final ConcurrentLinkedQueue<String> logsTrace;
	private final ConcurrentLinkedQueue<String> logsDebug;
	private final ConcurrentLinkedQueue<String> logsInfo;
	private final ConcurrentLinkedQueue<String> logsWarn;
	private final ConcurrentLinkedQueue<String> logsError;
	private final ConcurrentLinkedQueue<String> logsFatal;
	
	private final AtomicInteger sevMin;
	private final long waitMax;
	private long wait;
	
	private final AtomicBoolean alive = new AtomicBoolean(true);
	
	public enum Severity {
		TRACE, DEBUG, INFO, WARN, ERROR, FATAL
	}
	
	public AsyncLoggerManager() {
		this(Severity.INFO, 498);
	}
	
	public AsyncLoggerManager(@NotNull Severity sevMin) {
		this(sevMin, 498);
	}
	
	public AsyncLoggerManager(@NotNull Severity sevMin, long waitMax) {
		
		this.waitMax = waitMax * 2 / 3;
		this.wait = waitMax / 5 + 1;
		this.sevMin = new AtomicInteger();
		
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
		
		new LoggerClassInternal().start();
		
		System.setProperty("Log4jContextSelector", AsyncLoggerContextSelector.class.getName());
		logger = (org.apache.logging.log4j.core.async.AsyncLogger) LogManager.getLogger();
	}
	
	public enum LoggerType {
		Console, RandomAccessFile
	}
	
	public static void createLogProperties(@NotNull Severity rootSeverity, String nameLoggerConsole,
	                                       String patternLoggerConsole, String nameLoggerFile,
	                                       String patternLoggerFile, String fileName) throws IOException {
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
	}
	
	public static void createLogProperties(LoggerType type, String name, String pattern, Severity rootSeverity) throws IOException {
		createLogProperties(type, name, pattern, rootSeverity, "logs.log");
	}
	
	public static void createLogProperties(@NotNull LoggerType type, String name, String pattern, @NotNull Severity rootSeverity, String fileName) throws IOException {
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
	}
	
	public void setSeverityMin(@NotNull Severity severityMin) {
		switch (severityMin) {
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
	}
	
	public void kill() {
		alive.set(false);
	}
	
	//Method to log with the normal asyncLogger
	public void logTrace(String s) {
		if (sevMin.get() == 0) {
			logsTrace.offer(s);
		}
	}
	
	public void logDebug(String s) {
		if (sevMin.get() < 2) {
			logsDebug.offer(s);
		}
	}
	
	public void logInfo(String s) {
		if (sevMin.get() < 3) {
			logsInfo.offer(s);
		}
	}
	
	public void logWarn(String s) {
		if (sevMin.get() < 4) {
			logsWarn.offer(s);
		}
	}
	
	public void logError(String s) {
		if (sevMin.get() < 5) {
			logsError.offer(s);
		}
	}
	
	public void logFatal(String s) {
		logsFatal.offer(s);
	}
	
	private class LoggerClassInternal extends Thread {
		
		@Override
		public void run() {
			
			while (alive.get()) {
				
				try {
					Thread.sleep(wait);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				long i = 0;
				
				for (Iterator<String> iter = logsTrace.iterator(); iter.hasNext(); i++) {
					logger.trace(iter.next());
					iter.remove();
				}
				
				for (Iterator<String> iter = logsDebug.iterator(); iter.hasNext(); i++) {
					logger.debug(iter.next());
					iter.remove();
				}
				
				for (Iterator<String> iter = logsInfo.iterator(); iter.hasNext(); i++) {
					logger.info(iter.next());
					iter.remove();
				}
				
				for (Iterator<String> iter = logsWarn.iterator(); iter.hasNext(); i++) {
					logger.warn(iter.next());
					iter.remove();
				}
				
				for (Iterator<String> iter = logsError.iterator(); iter.hasNext(); i++) {
					logger.error(iter.next());
					iter.remove();
				}
				
				for (Iterator<String> iter = logsFatal.iterator(); iter.hasNext(); i++) {
					logger.fatal(iter.next());
					iter.remove();
				}
				
				if (i > (wait << 3) && wait > 1) {
					wait--;
				} else if (i < (wait >> 1) && wait < waitMax) {
					wait++;
				}
			}
		}
	}
}