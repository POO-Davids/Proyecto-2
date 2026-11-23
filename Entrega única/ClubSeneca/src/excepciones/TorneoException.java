package excepciones;

public abstract class TorneoException extends ClubSenecaException {

	private static final long serialVersionUID = 1L;

	public TorneoException(String mensaje) {
		super(mensaje);
	}
}
