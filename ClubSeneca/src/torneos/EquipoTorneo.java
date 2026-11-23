package torneos;

import java.util.Arrays;

import equipos.Equipo;

public class EquipoTorneo extends ParticipanteTorneo {

	private String[] integrantes;
	private Equipo equipoClub;

	public EquipoTorneo(String idParticipante, Equipo equipoClub) {
		super(idParticipante, equipoClub.getNombre());
		this.equipoClub = equipoClub;
		this.integrantes = new String[0];
	}

	public Equipo getEquipoClub() {
		return equipoClub;
	}

	public void agregarIntegrante(String username) {
		integrantes = Arrays.copyOf(integrantes, integrantes.length + 1);
		integrantes[integrantes.length - 1] = username;
	}

	@Override
	public String[] getUsernames() {
		return Arrays.copyOf(integrantes, integrantes.length);
	}

	@Override
	public int getNumeroIntegrantes() {
		return integrantes.length;
	}
}
