package premios;

import java.time.LocalDate;

public class PremioEfectivo extends Premio {

	private float valor;

	public PremioEfectivo(String idPremio, String idTorneoOrigen, int posicion, String beneficiario, LocalDate fecha, float valor) {
		super(idPremio, idTorneoOrigen, posicion, beneficiario, fecha);
		this.valor = valor;
	}

	public float getValor() {
		return valor;
	}

	@Override
	public String getDescripcion() {
		return String.format("%s: premio en efectivo de %.0f para %s (puesto %d, torneo %s)", idPremio, valor, beneficiario, posicion,
				idTorneoOrigen);
	}
}
