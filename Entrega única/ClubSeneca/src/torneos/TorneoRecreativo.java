package torneos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import usuarios.Usuario;
import premios.Premio;
import premios.BonoDescuento;

public class TorneoRecreativo extends Torneo {

	private float porcentajeBono;

	public TorneoRecreativo(String idTorneo, String nombre, String deporte, String tipoParticipante, String modalidad,
			String categoria, String nivel, int cupos, int cuposPreferenciales, LocalDate fecha, float porcentajeBono) {
		super(idTorneo, nombre, deporte, tipoParticipante, modalidad, categoria, nivel, cupos, cuposPreferenciales, fecha);
		this.porcentajeBono = porcentajeBono;
	}

	public float getPorcentajeBono() {
		return porcentajeBono;
	}

	@Override
	public float calcularTarifa(Usuario usuario) {
		return 0;
	}

	@Override
	public List<Premio> asignarPremios() {
		List<Premio> nuevos = new ArrayList<>();
		int n = 1;
		for (String username : posiciones.get(0).getUsernames()) {
			nuevos.add(new BonoDescuento(idTorneo + "-B" + n, idTorneo, 1, username, LocalDate.now(), porcentajeBono));
			n++;
		}
		return nuevos;
	}

	@Override
	public String toString() {
		return super.toString() + String.format(" | recreativo, bono %.0f%%", porcentajeBono);
	}
}
