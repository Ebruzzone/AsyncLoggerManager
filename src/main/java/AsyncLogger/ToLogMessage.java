package AsyncLogger;

@FunctionalInterface
public interface ToLogMessage<E> {
	String action(E object);
}
