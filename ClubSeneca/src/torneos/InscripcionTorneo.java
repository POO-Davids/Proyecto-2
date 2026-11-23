package torneos;

import java.time.LocalDate;

public class InscripcionTorneo {

	private String idInscripcion;
	private String tipoCupo;
	private float tarifaPagada;
	private LocalDate fecha;
	private ParticipanteTorneo participante;

	public InscripcionTorneo(String idInscripcion, ParticipanteTorneo participante, String tipoCupo, float tarifaPagada,
			LocalDate fecha) {
		this.idInscripcion = idInscripcion;
		this.participante = participante;
		this.tipoCupo = tipoCupo;
		this.tarifaPagada = tarifaPagada;
		this.fecha = fecha;
	}

	public String getIdInscripcion() {
		return idInscripcion;
	}

	public ParticipanteTorneo getParticipante() {
		return participante;
	}

	public String getTipoCupo() {
		return tipoCupo;
	}

	public float getTarifaPagada() {
		return tarifaPagada;
	}

	public Boolean esPreferencial() {
		return "preferencial".equalsIgnoreCase(tipoCupo);
	}

	public LocalDate getFecha() {
		return fecha;
	}
}
