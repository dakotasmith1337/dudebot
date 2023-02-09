package dakota.dude.handler.database;

/**
 * A simple, named exception class.
 *
 */
public class DudeDatabaseException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public DudeDatabaseException(String message) {
		super(message);
	}

	public DudeDatabaseException(Throwable error) {
		super(error);
	}
}
