package premios;

import java.time.LocalDate;

import excepciones.BonoNoDisponibleException;

public class BonoDescuento extends Premio {

	private float porcentaje;
	private String estado;
	private String titularUsername;

	public BonoDescuento(String idPremio, String idTorneoOrigen, int posicion, String username, LocalDate fecha, float porcentaje) {
		super(idPremio, idTorneoOrigen, posicion, username, fecha);
		this.porcentaje = porcentaje;
		this.estado = "disponible";
		this.titularUsername = username;
	}

	public float getPorcentaje() {
		return porcentaje;
	}

	public Boolean estaDisponible() {
		return "disponible".equalsIgnoreCase(estado);
	}

	public float calcularDescuento(float subtotal) {
		return subtotal * porcentaje / 100;
	}

	public void usar() throws BonoNoDisponibleException {
		if (!estaDisponible()) {
			throw new BonoNoDisponibleException("El bono " + idPremio + " ya fue usado");
		}
		estado = "usado";
	}

	@Override
	public String getDescripcion() {
		return String.format("%s: bono de descuento del %.0f%% para %s (puesto %d, torneo %s, %s)", idPremio, porcentaje,
				titularUsername, posicion, idTorneoOrigen, estado);
	}

	public String getEstado() {
		return estado;
	}

	public void restaurarEstado(String estado) {
		this.estado = estado;
	}
}
