package AsyncLogger;

@SuppressWarnings("unused")
public enum Severity {
	TRACE, DEBUG, INFO, WARN, ERROR, FATAL, NO_LOG;
	private static Severity[] severities;
	
	static {
		severities = new Severity[NO_LOG.ordinal()];
		severities[TRACE.ordinal()] = TRACE;
		severities[DEBUG.ordinal()] = DEBUG;
		severities[INFO.ordinal()] = INFO;
		severities[WARN.ordinal()] = WARN;
		severities[ERROR.ordinal()] = ERROR;
		severities[FATAL.ordinal()] = FATAL;
	}
	
	public static Severity getNewSeverity(int sev) {
		if (sev < 0 || sev >= AsyncLoggerManager.noLog) {
			throw new IllegalArgumentException("Severity must be >= 0 and < " + AsyncLoggerManager.noLog);
		}
		return severities[sev];
	}
}
