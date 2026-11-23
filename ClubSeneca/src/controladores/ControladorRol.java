package controladores;

import java.util.List;

import excepciones.ElementoNoEncontradoException;
import excepciones.PersistenciaException;
import excepciones.ValorInvalidoException;
import torneos.Torneo;

public abstract class ControladorRol {

	protected CentralClubSeneca central;

	public ControladorRol(CentralClubSeneca central) {
		this.central = central;
	}

	public String consultarTorneos(String deporte, String estado) throws ValorInvalidoException {
		List<Torneo> lista = central.getTorneos().consultarTorneos(deporte, estado);
		if (lista.isEmpty()) {
			return "No hay torneos que coincidan";
		}
		StringBuilder texto = new StringBuilder();
		for (Torneo torneo : lista) {
			texto.append(torneo).append("\n");
		}
		return texto.toString().trim();
	}

	public String consultarDetalleTorneo(String idTorneo) throws ElementoNoEncontradoException {
		return central.getTorneos().getTorneo(idTorneo).getHistorial();
	}

	public abstract String getRol();

	public void cerrarSesion() throws PersistenciaException {
		central.cerrarSesion();
	}
}
