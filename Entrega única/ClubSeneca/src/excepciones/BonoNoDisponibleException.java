package excepciones;

public class BonoNoDisponibleException extends TiendaException {

	private static final long serialVersionUID = 1L;

	public BonoNoDisponibleException(String mensaje) {
		super(mensaje);
	}
}
