package premios;

import java.time.LocalDate;

public abstract class Premio {

	protected String idPremio;
	protected String idTorneoOrigen;
	protected int posicion;
	protected String beneficiario;
	protected LocalDate fecha;

	protected Premio(String idPremio, String idTorneoOrigen, int posicion, String beneficiario, LocalDate fecha) {
		this.idPremio = idPremio;
		this.idTorneoOrigen = idTorneoOrigen;
		this.posicion = posicion;
		this.beneficiario = beneficiario;
		this.fecha = fecha;
	}

	public String getIdPremio() {
		return idPremio;
	}

	public String getTorneoOrigen() {
		return idTorneoOrigen;
	}

	public String getBeneficiario() {
		return beneficiario;
	}

	public int getPosicion() {
		return posicion;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public abstract String getDescripcion();
}
