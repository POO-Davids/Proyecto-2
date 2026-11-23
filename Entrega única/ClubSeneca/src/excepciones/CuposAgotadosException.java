package excepciones;

public class CuposAgotadosException extends TorneoException {

	private static final long serialVersionUID = 1L;

	public CuposAgotadosException(String mensaje) {
		super(mensaje);
	}
}
