package controladores;

import excepciones.CuposAgotadosException;
import excepciones.ElementoDuplicadoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.EstadoTorneoException;
import excepciones.InscripcionTorneoException;
import excepciones.ValorInvalidoException;
import usuarios.Usuario;

public class ControladorParticipante extends ControladorRol {

	protected Usuario usuario;

	public ControladorParticipante(CentralClubSeneca central, Usuario usuario) {
		super(central);
		this.usuario = usuario;
	}

	public void inscribirseIndividual(String idTorneo, String nivelDeclarado) throws ElementoNoEncontradoException,
			EstadoTorneoException, InscripcionTorneoException, ElementoDuplicadoException, CuposAgotadosException,
			ValorInvalidoException {
		central.getTorneos().inscribirIndividual(idTorneo, usuario, nivelDeclarado);
	}

	public void inscribirPareja(String idTorneo, String usernameCompanero, String nivelDeclarado) throws ElementoNoEncontradoException,
			EstadoTorneoException, InscripcionTorneoException, ElementoDuplicadoException, CuposAgotadosException,
			ValorInvalidoException {
		Usuario companero = central.getUsuarios().getUsuario(usernameCompanero);
		if (companero == null || companero.getUsername().equals(usuario.getUsername())) {
			throw new InscripcionTorneoException("No se puede inscribir una pareja con un solo jugador");
		}
		central.getTorneos().inscribirPareja(idTorneo, usuario, companero, nivelDeclarado);
	}

	public void retirarse(String idTorneo) throws ElementoNoEncontradoException, EstadoTorneoException, InscripcionTorneoException {
		central.getTorneos().retirar(idTorneo, usuario.getUsername());
	}

	@Override
	public String getRol() {
		return "Participante";
	}
}
