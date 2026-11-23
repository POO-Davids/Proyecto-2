package torneos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import usuarios.Empleado;
import usuarios.Usuario;
import premios.Premio;
import premios.PremioEfectivo;

public class TorneoCompetitivo extends Torneo {

	private float tarifa;
	private float recaudo;
	private float porcentajePremio = 0.70f;

	public TorneoCompetitivo(String idTorneo, String nombre, String deporte, String tipoParticipante, String modalidad,
			String categoria, String nivel, int cupos, int cuposPreferenciales, LocalDate fecha, float tarifa) {
		super(idTorneo, nombre, deporte, tipoParticipante, modalidad, categoria, nivel, cupos, cuposPreferenciales, fecha);
		this.tarifa = tarifa;
		this.recaudo = 0;
	}

	public float getTarifa() {
		return tarifa;
	}

	public float getRecaudo() {
		return recaudo;
	}

	@Override
	public float calcularTarifa(Usuario usuario) {
		return usuario instanceof Empleado ? 0 : tarifa;
	}

	public float calcularRecaudo() {
		float total = 0;
		for (InscripcionTorneo inscripcion : inscripciones) {
			total += inscripcion.getTarifaPagada();
		}
		recaudo = total;
		return recaudo;
	}

	@Override
	public InscripcionTorneo inscribir(ParticipanteTorneo participante, String tipoCupo, float tarifa) {
		InscripcionTorneo inscripcion = super.inscribir(participante, tipoCupo, tarifa);
		calcularRecaudo();
		return inscripcion;
	}

	@Override
	public List<Premio> asignarPremios() {
		List<Premio> nuevos = new ArrayList<>();
		ParticipanteTorneo ganador = posiciones.get(0);
		if (ganador instanceof ParticipanteIndividual individual && individual.getEsEmpleado()) {
			return nuevos;
		}
		nuevos.add(new PremioEfectivo(idTorneo + "-P1", idTorneo, 1, ganador.getNombre(), LocalDate.now(),
				calcularRecaudo() * porcentajePremio));
		return nuevos;
	}

	@Override
	public String toString() {
		return super.toString() + String.format(" | competitivo, tarifa %.0f", tarifa);
	}

	public void restaurarRecaudo(float recaudo) {
		this.recaudo = recaudo;
	}
}
