package torneos;

public class ParticipanteIndividual extends ParticipanteTorneo {

	private String username;
	private String nivel;
	private Boolean esEmpleado;

	public ParticipanteIndividual(String idParticipante, String username, String nivel, Boolean esEmpleado) {
		super(idParticipante, username);
		this.username = username;
		this.nivel = nivel;
		this.esEmpleado = esEmpleado;
	}

	public String getUsername() {
		return username;
	}

	public String getNivel() {
		return nivel;
	}

	public Boolean getEsEmpleado() {
		return esEmpleado;
	}

	@Override
	public String[] getUsernames() {
		return new String[] { username };
	}

	@Override
	public int getNumeroIntegrantes() {
		return 1;
	}
}
