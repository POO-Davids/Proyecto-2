package excepciones;

public class InscripcionTorneoException extends TorneoException {

	private static final long serialVersionUID = 1L;

	public InscripcionTorneoException(String mensaje) {
		super(mensaje);
	}
}
