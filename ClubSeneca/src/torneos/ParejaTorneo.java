package torneos;

public class ParejaTorneo extends ParticipanteTorneo {

	private String username1;
	private String username2;
	private String nivel;

	public ParejaTorneo(String idParticipante, String username1, String username2, String nivel) {
		super(idParticipante, username1 + " / " + username2);
		this.username1 = username1;
		this.username2 = username2;
		this.nivel = nivel;
	}

	public String getNivel() {
		return nivel;
	}

	@Override
	public String[] getUsernames() {
		return new String[] { username1, username2 };
	}

	@Override
	public int getNumeroIntegrantes() {
		return 2;
	}
}
