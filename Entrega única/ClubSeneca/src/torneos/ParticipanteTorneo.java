package torneos;

public abstract class ParticipanteTorneo {

	protected String idParticipante;
	protected String nombre;

	protected ParticipanteTorneo(String idParticipante, String nombre) {
		this.idParticipante = idParticipante;
		this.nombre = nombre;
	}

	public String getIdParticipante() {
		return idParticipante;
	}

	public String getNombre() {
		return nombre;
	}

	public Boolean contiene(String username) {
		for (String integrante : getUsernames()) {
			if (integrante.equals(username)) {
				return true;
			}
		}
		return false;
	}

	public abstract String[] getUsernames();

	public abstract int getNumeroIntegrantes();
}
