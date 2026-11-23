package integracion;

import java.nio.file.Path;
import java.util.List;

import controladores.CentralClubSeneca;
import controladores.ControladorAdministrador;
import controladores.ControladorRol;
import equipos.Equipo;
import jugadores.Jugador;
import jugadores.JugadorConjunto;
import jugadores.JugadorIndividual;
import usuarios.Administrador;
import usuarios.Socio;
import usuarios.Usuario;

/** Arma un club en memoria para las pruebas de integración; la persistencia apunta a un directorio temporal. */
final class Club {

	static final String PW = "pw";

	final CentralClubSeneca central;
	final ControladorAdministrador adm;

	Club(Path dir) throws Exception {
		central = new CentralClubSeneca(dir.toString());
		central.getUsuarios().agregarUsuario(new Administrador("admin", PW));
		adm = (ControladorAdministrador) central.iniciarSesion("admin", PW);
	}

	@SuppressWarnings("unchecked")
	<T extends ControladorRol> T login(String username) throws Exception {
		return (T) central.iniciarSesion(username, PW);
	}

	<T extends Usuario> T usuario(T usuario) throws Exception {
		adm.registrarUsuario(usuario);
		return usuario;
	}

	Socio socio(String username, String edad) throws Exception {
		return usuario(new Socio(username, PW, Character.toUpperCase(username.charAt(0)) + username.substring(1), edad));
	}

	String individual(String username, String disciplina, String nivel, float elo) throws Exception {
		adm.crearJugador(username, new JugadorIndividual("tmp", null, disciplina, nivel, elo));
		return central.getJugadores().getJugadorIndividual(username, disciplina).getIdJugador();
	}

	Equipo equipo(String nombre, String deporte, String categoria, int cupo) throws Exception {
		adm.crearEquipo(nombre, deporte, categoria, cupo);
		return central.getEquipos().buscarPorNombre(nombre);
	}

	String conjunto(String username, Equipo equipo, int camiseta) throws Exception {
		adm.crearJugador(username, new JugadorConjunto("tmp", null, "campo", camiseta, equipo.getIdEquipo(), equipo.getDeporte()));
		for (Jugador jugador : central.getJugadores().getJugadoresDeUsuario(username)) {
			if (jugador instanceof JugadorConjunto c && c.getEquipoId().equals(equipo.getIdEquipo())) {
				return jugador.getIdJugador();
			}
		}
		throw new IllegalStateException("No se creó el jugador");
	}

	/** Crea n socios (prefijo1..prefijoN) con la edad dada y los inscribe en el equipo con camisetas 1..n. */
	String[] llenar(Equipo equipo, String prefijo, String edad, int n) throws Exception {
		String[] ids = new String[n];
		for (int i = 1; i <= n; i++) {
			socio(prefijo + i, edad);
			ids[i - 1] = conjunto(prefijo + i, equipo, i);
		}
		return ids;
	}

	Jugador jugador(String id) {
		return central.getJugadores().getJugadorPorId(id);
	}

	static Boolean[] horas(int desde, int hasta) {
		Boolean[] turno = new Boolean[24];
		for (int h = 0; h < 24; h++) {
			turno[h] = h >= desde && h <= hasta;
		}
		return turno;
	}

	static String[] ids(List<String> lista) {
		return lista.toArray(new String[0]);
	}
}
