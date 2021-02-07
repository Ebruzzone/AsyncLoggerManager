package AsyncLogger;

@FunctionalInterface
interface LogWithSeverity {
	void log(org.apache.logging.log4j.core.async.AsyncLogger logger, Log log);
}
