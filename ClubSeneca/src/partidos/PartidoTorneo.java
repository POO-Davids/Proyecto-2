package partidos;

import java.time.LocalDateTime;

import torneos.ParticipanteTorneo;

public class PartidoTorneo extends Partido {

	private String idTorneo;
	private int marcador1;
	private int marcador2;
	private ParticipanteTorneo participante1;
	private ParticipanteTorneo participante2;

	public PartidoTorneo(String idPartido, String idTorneo, ParticipanteTorneo participante1, ParticipanteTorneo participante2,
			LocalDateTime fecha, String deporte, String categoria) {
		super(idPartido, fecha, deporte, categoria);
		this.idTorneo = idTorneo;
		this.participante1 = participante1;
		this.participante2 = participante2;
	}

	public String getIdTorneo() {
		return idTorneo;
	}

	public ParticipanteTorneo getParticipante1() {
		return participante1;
	}

	public ParticipanteTorneo getParticipante2() {
		return participante2;
	}

	public void registrarMarcador(int marcador1, int marcador2) {
		this.marcador1 = marcador1;
		this.marcador2 = marcador2;
		setCerrado();
	}

	public int getMarcador1() {
		return marcador1;
	}

	public int getMarcador2() {
		return marcador2;
	}

	public ParticipanteTorneo getGanador() {
		if (marcador1 == marcador2) {
			return null;
		}
		return marcador1 > marcador2 ? participante1 : participante2;
	}

	@Override
	public String getResumen() {
		return participante1.getNombre() + " vs " + participante2.getNombre() + ": " + marcador1 + "-" + marcador2 + " (" + fecha + ")";
	}
}
