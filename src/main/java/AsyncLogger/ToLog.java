package AsyncLogger;

@FunctionalInterface
public interface ToLog<E> {
	Log action(E object);
}
