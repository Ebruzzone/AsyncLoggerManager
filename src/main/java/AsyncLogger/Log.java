package AsyncLogger;

import org.apache.logging.log4j.Marker;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class Log {
	Marker marker;
	String message;
	int severity;
	Object[] objects;
	
	Log(String message, int severity) {
		this.message = message;
		this.severity = severity;
		this.marker = null;
		this.objects = null;
	}
	
	Log(String message, Marker marker) {
		this.message = message;
		this.marker = marker;
		this.objects = null;
	}
	
	Log(String message, int severity, Object... objects) {
		this.message = message;
		this.severity = severity;
		this.marker = null;
		this.objects = objects;
	}
	
	Log(Marker marker, String message, int severity) {
		this.marker = marker;
		this.message = message;
		this.severity = severity;
		this.objects = null;
	}
	
	Log(String message, Marker marker, Object[] objects) {
		this.message = message;
		this.marker = marker;
		this.objects = objects;
	}
	
	Log(Marker marker, String message, int severity, Object[] objects) {
		this.marker = marker;
		this.message = message;
		this.severity = severity;
		this.objects = objects;
	}
	
	Log addSeverity(int severity) {
		this.severity = severity;
		return this;
	}
	
	@NotNull
	@Contract("_, _ -> new")
	public static Log newLog(Marker marker, String message) {
		return new Log(message, marker);
	}
	
	@NotNull
	@Contract("_, _, _ -> new")
	public static Log newLog(Marker marker, String message, Object... params) {
		return new Log(message, marker, params);
	}
}
