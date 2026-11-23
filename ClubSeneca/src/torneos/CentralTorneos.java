package torneos;

import java.time.LocalDate;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import equipos.Equipo;
import excepciones.BonoNoDisponibleException;
import excepciones.CuposAgotadosException;
import excepciones.ElementoDuplicadoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.EstadoTorneoException;
import excepciones.InscripcionTorneoException;
import excepciones.ValorInvalidoException;
import jugadores.CentralJugadores;
import jugadores.Jugador;
import jugadores.JugadorIndividual;
import partidos.Partido;
import partidos.PartidoInterno;
import partidos.PartidoTorneo;
import partidos.Partidos;
import usuarios.Empleado;
import usuarios.Socio;
import usuarios.Usuario;
import premios.Premio;
import premios.BonoDescuento;

public class CentralTorneos {

	private static final List<String> ESTADOS = List.of("Inscripciones abiertas", "En curso", "Finalizado");
	private static final List<String> NIVELES = List.of("Principiante", "Intermedio", "Avanzado");

	private LinkedHashMap<String, Torneo> torneos;
	private List<BonoDescuento> bonos;
	private float porcentajePreferencial = 0.20f;
	private int contador;
	private CentralJugadores jugadores;
	private Partidos partidos;

	public CentralTorneos(CentralJugadores jugadores, Partidos partidos) {
		this.torneos = new LinkedHashMap<>();
		this.bonos = new ArrayList<>();
		this.jugadores = jugadores;
		this.partidos = partidos;
	}

	public Torneo crearTorneo(String tipo, String nombre, String deporte, String participante, String modalidad,
			String categoriaONivel, int cupos, float valor, LocalDate fecha) throws ValorInvalidoException {
		if (nombre == null || nombre.isBlank() || fecha == null) {
			throw new ValorInvalidoException("El torneo debe tener nombre y fecha");
		}
		validarDatosTorneo(tipo, deporte, participante, modalidad, categoriaONivel, cupos, valor);
		String idTorneo = "T-" + generarId();
		int cuposPreferenciales = (int) Math.ceil(porcentajePreferencial * cupos);
		boolean deEquipo = participante.equalsIgnoreCase("equipo");
		String categoria = deEquipo ? categoriaONivel : "";
		String nivel = deEquipo ? "" : categoriaONivel;
		String tipoParticipante = participante.toLowerCase();
		String modalidadTorneo = modalidad == null ? "" : modalidad.trim().toLowerCase();
		Torneo torneo = tipo.equalsIgnoreCase("competitivo")
				? new TorneoCompetitivo(idTorneo, nombre, deporte, tipoParticipante, modalidadTorneo, categoria, nivel, cupos,
						cuposPreferenciales, fecha, valor)
				: new TorneoRecreativo(idTorneo, nombre, deporte, tipoParticipante, modalidadTorneo, categoria, nivel, cupos,
						cuposPreferenciales, fecha, valor);
		torneos.put(idTorneo, torneo);
		return torneo;
	}

	private void validarDatosTorneo(String tipo, String deporte, String participante, String modalidad, String categoriaONivel,
			int cupos, float valor) throws ValorInvalidoException {
		if (cupos <= 0) {
			throw new ValorInvalidoException("Los cupos deben ser mayores que cero");
		}
		if (tipo == null || (!tipo.equalsIgnoreCase("competitivo") && !tipo.equalsIgnoreCase("recreativo"))) {
			throw new ValorInvalidoException("El tipo debe ser competitivo o recreativo");
		}
		if (participante == null || (!participante.equalsIgnoreCase("individual") && !participante.equalsIgnoreCase("equipo"))) {
			throw new ValorInvalidoException("El participante debe ser individual o equipo");
		}
		if (deporte == null || deporte.isBlank()) {
			throw new ValorInvalidoException("El torneo debe tener deporte");
		}
		if (categoriaONivel == null || categoriaONivel.isBlank()) {
			throw new ValorInvalidoException("Debe indicar la categoría o el nivel del torneo");
		}
		if (tipo.equalsIgnoreCase("competitivo") && valor <= 0) {
			throw new ValorInvalidoException("Un torneo competitivo debe tener tarifa");
		}
		if (tipo.equalsIgnoreCase("recreativo") && (valor <= 0 || valor > 100)) {
			throw new ValorInvalidoException("Un torneo recreativo debe tener un porcentaje de bono entre 1 y 100, sin tarifa");
		}
		String modo = modalidad == null ? "" : modalidad.trim();
		if (participante.equalsIgnoreCase("equipo")) {
			if (modo.equalsIgnoreCase("dobles")) {
				throw new ValorInvalidoException("Un torneo de equipos no puede ser de dobles");
			}
			for (String nivel : NIVELES) {
				if (nivel.equalsIgnoreCase(categoriaONivel.trim())) {
					throw new ValorInvalidoException("Un torneo de equipos lleva categoría, no nivel");
				}
			}
		} else if (!modo.equalsIgnoreCase("sencillos") && !modo.equalsIgnoreCase("dobles")) {
			throw new ValorInvalidoException("La modalidad debe ser sencillos o dobles");
		}
	}

	public Torneo getTorneo(String idTorneo) throws ElementoNoEncontradoException {
		Torneo torneo = torneos.get(idTorneo);
		if (torneo == null) {
			throw new ElementoNoEncontradoException("No existe el torneo " + idTorneo);
		}
		return torneo;
	}

	public List<Torneo> getTorneos() {
		return new ArrayList<>(torneos.values());
	}

	public List<Torneo> consultarTorneos(String deporte, String estado) throws ValorInvalidoException {
		String filtroDeporte = deporte == null ? "" : deporte.trim();
		String filtroEstado = estado == null ? "" : estado.trim();
		if (!filtroEstado.isEmpty() && ESTADOS.stream().noneMatch(e -> e.equalsIgnoreCase(filtroEstado))) {
			throw new ValorInvalidoException("El estado " + filtroEstado + " no existe");
		}
		List<Torneo> resultado = new ArrayList<>();
		for (Torneo torneo : torneos.values()) {
			if ((filtroDeporte.isEmpty() || torneo.getDeporte().equalsIgnoreCase(filtroDeporte))
					&& (filtroEstado.isEmpty() || torneo.getEstado().equalsIgnoreCase(filtroEstado))) {
				resultado.add(torneo);
			}
		}
		return resultado;
	}

	public InscripcionTorneo inscribirIndividual(String idTorneo, Usuario usuario, String nivelDeclarado)
			throws ElementoNoEncontradoException, EstadoTorneoException, InscripcionTorneoException, ElementoDuplicadoException,
			CuposAgotadosException, ValorInvalidoException {
		Torneo torneo = getTorneo(idTorneo);
		torneo.verificarEstado("Inscripciones abiertas");
		if (usuario instanceof Empleado empleado && tieneTurno(empleado, torneo.getFecha())) {
			throw new InscripcionTorneoException(usuario.getUsername() + " tiene turno el " + torneo.getFecha());
		}
		String nivel = nivelDe(usuario, torneo, nivelDeclarado);
		ParticipanteIndividual participante = new ParticipanteIndividual("P-" + generarId(), usuario.getUsername(), nivel,
				usuario instanceof Empleado);
		torneo.verificarInscripcion(participante);
		String tipoCupo = tipoCupo(torneo, participante);
		float tarifa = torneo.calcularTarifa(usuario);
		return torneo.inscribir(participante, tipoCupo, tarifa);
	}

	public InscripcionTorneo inscribirPareja(String idTorneo, Usuario usuario1, Usuario usuario2, String nivelDeclarado)
			throws ElementoNoEncontradoException, EstadoTorneoException, InscripcionTorneoException, ElementoDuplicadoException,
			CuposAgotadosException, ValorInvalidoException {
		Torneo torneo = getTorneo(idTorneo);
		torneo.verificarEstado("Inscripciones abiertas");
		for (Usuario integrante : new Usuario[] { usuario1, usuario2 }) {
			if (integrante instanceof Empleado empleado && tieneTurno(empleado, torneo.getFecha())) {
				throw new InscripcionTorneoException(integrante.getUsername() + " tiene turno el " + torneo.getFecha());
			}
		}
		String nivel1 = nivelDe(usuario1, torneo, nivelDeclarado);
		String nivel2 = nivelDe(usuario2, torneo, nivelDeclarado);
		if (!nivel1.equalsIgnoreCase(nivel2)) {
			throw new InscripcionTorneoException("Los integrantes de la pareja tienen niveles distintos");
		}
		ParejaTorneo pareja = new ParejaTorneo("P-" + generarId(), usuario1.getUsername(), usuario2.getUsername(), nivel1);
		torneo.verificarInscripcion(pareja);
		String tipoCupo = tipoCupo(torneo, pareja);
		float tarifa = torneo.calcularTarifa(usuario1);
		return torneo.inscribir(pareja, tipoCupo, tarifa);
	}

	public InscripcionTorneo inscribirEquipo(String idTorneo, Equipo equipoClub, Usuario[] integrantes)
			throws ElementoNoEncontradoException, EstadoTorneoException, InscripcionTorneoException, ElementoDuplicadoException,
			CuposAgotadosException {
		Torneo torneo = getTorneo(idTorneo);
		torneo.verificarEstado("Inscripciones abiertas");
		verificarEquipoClub(torneo, equipoClub, integrantes);
		EquipoTorneo equipo = new EquipoTorneo("P-" + generarId(), equipoClub);
		for (Usuario integrante : integrantes) {
			equipo.agregarIntegrante(integrante.getUsername());
		}
		torneo.verificarInscripcion(equipo);
		String tipoCupo = tipoCupo(torneo, equipo);
		float tarifa = torneo.calcularTarifa(integrantes[0]);
		return torneo.inscribir(equipo, tipoCupo, tarifa);
	}

	private String tipoCupo(Torneo torneo, ParticipanteTorneo participante) throws CuposAgotadosException {
		boolean elegible = esElegiblePreferencial(torneo, participante);
		if (elegible && torneo.getPreferencialesLibres() > 0) {
			return "preferencial";
		}
		if (torneo.getRegularesLibres() > 0) {
			return "regular";
		}
		if (torneo.getPreferencialesLibres() > 0) {
			return "preferencial";
		}
		throw new CuposAgotadosException("No quedan cupos en el torneo " + torneo.getIdTorneo());
	}

	public void retirar(String idTorneo, String username)
			throws ElementoNoEncontradoException, EstadoTorneoException, InscripcionTorneoException {
		getTorneo(idTorneo).retirar(username);
	}

	public void iniciarTorneo(String idTorneo) throws ElementoNoEncontradoException, EstadoTorneoException, InscripcionTorneoException {
		getTorneo(idTorneo).iniciar();
	}

	public List<Premio> finalizarTorneo(String idTorneo) throws ElementoNoEncontradoException, EstadoTorneoException {
		Torneo torneo = getTorneo(idTorneo);
		List<Premio> premios = torneo.finalizar();
		ParticipanteTorneo ganador = torneo.posiciones.get(0);
		for (Premio premio : premios) {
			if (premio instanceof BonoDescuento bono) {
				List<Jugador> deUsuario = jugadores.getJugadoresDeUsuario(bono.getBeneficiario());
				boolean guarda;
				if (!deUsuario.isEmpty()) {
					guarda = puedeGuardarBonos(deUsuario.get(0).getUsuario());
				} else {
					guarda = ganador instanceof ParticipanteIndividual individual && individual.getEsEmpleado();
				}
				if (guarda) {
					bonos.add(bono);
				}
			}
		}
		return premios;
	}

	public void registrarPartido(String idTorneo, String idParticipante1, String idParticipante2, LocalDateTime fecha,
			int puntuacion1, int puntuacion2)
			throws ElementoNoEncontradoException, EstadoTorneoException, InscripcionTorneoException, ValorInvalidoException {
		Torneo torneo = getTorneo(idTorneo);
		torneo.verificarEstado("En curso");
		ParticipanteTorneo participante1 = torneo.getParticipante(idParticipante1);
		ParticipanteTorneo participante2 = torneo.getParticipante(idParticipante2);
		if (participante1 == null || participante2 == null) {
			throw new InscripcionTorneoException("Los dos participantes deben estar inscritos en el torneo " + idTorneo);
		}
		if (idParticipante1.equals(idParticipante2) || puntuacion1 < 0 || puntuacion2 < 0) {
			throw new ValorInvalidoException("El partido debe tener dos participantes distintos y puntuaciones no negativas");
		}
		String categoria = torneo.tipoParticipante.equalsIgnoreCase("equipo") ? torneo.categoria : torneo.nivel;
		PartidoTorneo partido = new PartidoTorneo("PT-" + generarId(), idTorneo, participante1, participante2, fecha,
				torneo.getDeporte(), categoria);
		partido.registrarMarcador(puntuacion1, puntuacion2);
		torneo.registrarPartido(partido);
	}

	public void registrarPosiciones(String idTorneo, String[] idsParticipantes) throws ElementoNoEncontradoException,
			ValorInvalidoException, InscripcionTorneoException, EstadoTorneoException {
		Torneo torneo = getTorneo(idTorneo);
		if (new HashSet<>(Arrays.asList(idsParticipantes)).size() != idsParticipantes.length) {
			throw new ValorInvalidoException("Dos participantes no pueden tener la misma posición");
		}
		List<ParticipanteTorneo> lista = new ArrayList<>();
		for (String id : idsParticipantes) {
			ParticipanteTorneo participante = torneo.getParticipante(id);
			if (participante == null) {
				throw new InscripcionTorneoException("El participante " + id + " no está inscrito en el torneo " + idTorneo);
			}
			lista.add(participante);
		}
		torneo.registrarPosiciones(lista);
	}

	public List<BonoDescuento> getBonos(String username) {
		List<BonoDescuento> resultado = new ArrayList<>();
		for (BonoDescuento bono : bonos) {
			if (bono.getBeneficiario().equals(username)) {
				resultado.add(bono);
			}
		}
		return resultado;
	}

	public BonoDescuento usarBono(String idBono, String username) throws BonoNoDisponibleException {
		for (BonoDescuento bono : bonos) {
			if (bono.getIdPremio().equals(idBono)) {
				if (!bono.getBeneficiario().equals(username)) {
					throw new BonoNoDisponibleException("El bono " + idBono + " no es de " + username);
				}
				if (!bono.estaDisponible()) {
					throw new BonoNoDisponibleException("El bono " + idBono + " ya fue usado");
				}
				return bono;
			}
		}
		throw new BonoNoDisponibleException("No existe el bono " + idBono);
	}

	private Boolean esElegiblePreferencial(Torneo torneo, ParticipanteTorneo participante) {
		int n = torneo.cuposPreferenciales;
		if (n <= 0) {
			return false;
		}
		if (participante instanceof EquipoTorneo equipo) {
			HashMap<String, Integer> puntos = calcularPuntosInternos(torneo.getDeporte(), torneo.categoria, torneo.getFecha());
			List<Map.Entry<String, Integer>> orden = new ArrayList<>(puntos.entrySet());
			orden.sort((a, b) -> b.getValue() - a.getValue());
			for (int i = 0; i < Math.min(n, orden.size()); i++) {
				if (orden.get(i).getKey().equals(equipo.getEquipoClub().getIdEquipo())) {
					return true;
				}
			}
			return false;
		}
		Set<String> mejores = new HashSet<>();
		for (JugadorIndividual jugador : getMejoresElo(torneo.getDeporte(), torneo.nivel, n)) {
			mejores.add(jugador.getUsuario().getUsername());
		}
		if (participante instanceof ParejaTorneo) {
			for (String username : participante.getUsernames()) {
				if (!mejores.contains(username)) {
					return false;
				}
			}
			return true;
		}
		for (String username : participante.getUsernames()) {
			if (mejores.contains(username)) {
				return true;
			}
		}
		return false;
	}

	private List<JugadorIndividual> getMejoresElo(String disciplina, String nivel, int n) {
		List<JugadorIndividual> lista = new ArrayList<>();
		for (Jugador jugador : jugadores.getJugadores()) {
			if (jugador instanceof JugadorIndividual individual && individual.getDisciplina().equalsIgnoreCase(disciplina)
					&& individual.getNivel().equalsIgnoreCase(nivel)) {
				lista.add(individual);
			}
		}
		lista.sort((a, b) -> Float.compare(b.getElo(), a.getElo()));
		return new ArrayList<>(lista.subList(0, Math.min(n, lista.size())));
	}

	private HashMap<String, Integer> calcularPuntosInternos(String deporte, String categoria, LocalDate fin) {
		HashMap<String, Integer> puntos = new HashMap<>();
		for (Partido partido : partidos.getPartidosEntre(LocalDate.MIN, fin)) {
			if (partido instanceof PartidoInterno interno && interno.getCerrado() && interno.getDeporte().equalsIgnoreCase(deporte)
					&& interno.getCategoria().equalsIgnoreCase(categoria)) {
				int p1 = interno.getPuntuacionEquipo1();
				int p2 = interno.getPuntuacionEquipo2();
				String id1 = interno.getEquipo1().getIdEquipo();
				String id2 = interno.getEquipo2().getIdEquipo();
				puntos.merge(id1, p1 > p2 ? 3 : p1 == p2 ? 1 : 0, Integer::sum);
				puntos.merge(id2, p2 > p1 ? 3 : p1 == p2 ? 1 : 0, Integer::sum);
			}
		}
		return puntos;
	}

	private String nivelDe(Usuario usuario, Torneo torneo, String nivelDeclarado)
			throws InscripcionTorneoException, ValorInvalidoException {
		if (usuario instanceof Socio) {
			JugadorIndividual jugador = jugadores.getJugadorIndividual(usuario.getUsername(), torneo.getDeporte());
			if (jugador == null) {
				throw new InscripcionTorneoException(usuario.getUsername() + " no es jugador de " + torneo.getDeporte());
			}
			return jugador.getNivel();
		}
		if (nivelDeclarado == null || nivelDeclarado.isBlank()) {
			throw new ValorInvalidoException("Debe declarar su nivel");
		}
		return nivelDeclarado.trim();
	}

	private void verificarEquipoClub(Torneo torneo, Equipo equipoClub, Usuario[] integrantes) throws InscripcionTorneoException {
		if (!torneo.tipoParticipante.equalsIgnoreCase("equipo")) {
			throw new InscripcionTorneoException("El torneo " + torneo.getIdTorneo() + " no es de equipos");
		}
		if (!equipoClub.getDeporte().equalsIgnoreCase(torneo.getDeporte())
				|| !equipoClub.getCategoria().equalsIgnoreCase(torneo.categoria)) {
			throw new InscripcionTorneoException("El equipo " + equipoClub.getNombre() + " no es del deporte o la categoría del torneo");
		}
		String deporteSinTildes = Normalizer.normalize(torneo.getDeporte(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		String[] usernames = Arrays.stream(integrantes).map(Usuario::getUsername).toArray(String[]::new);
		if (!partidos.verificarMinimoJugadores(deporteSinTildes, usernames)) {
			throw new InscripcionTorneoException("El equipo " + equipoClub.getNombre() + " no tiene el mínimo de integrantes para "
					+ torneo.getDeporte() + " (fútbol 7, baloncesto 5, voleibol 6)");
		}
		List<String> idsEquipo = Arrays.asList(equipoClub.getJugadoresIds());
		for (Usuario integrante : integrantes) {
			if (!integrante.getTipo().equals("Socio")) {
				throw new InscripcionTorneoException(integrante.getUsername() + " no es socio del club");
			}
			boolean juega = false;
			for (Jugador jugador : jugadores.getJugadoresDeUsuario(integrante.getUsername())) {
				if (idsEquipo.contains(jugador.getIdJugador())) {
					juega = true;
				}
			}
			if (!juega) {
				throw new InscripcionTorneoException(integrante.getUsername() + " no es jugador de " + equipoClub.getNombre());
			}
		}
	}

	private Boolean tieneTurno(Empleado empleado, LocalDate fecha) {
		for (Boolean hora : empleado.getTurno(fecha)) {
			if (Boolean.TRUE.equals(hora)) {
				return true;
			}
		}
		return false;
	}

	private Boolean puedeGuardarBonos(Usuario usuario) {
		return usuario instanceof Socio || usuario instanceof Empleado;
	}

	private String generarId() {
		return String.valueOf(++contador);
	}

	public void agregarTorneo(Torneo torneo) {
		torneos.put(torneo.getIdTorneo(), torneo);
	}

	public void agregarBono(BonoDescuento bono) {
		bonos.add(bono);
	}

	public List<BonoDescuento> getBonos() {
		return new ArrayList<>(bonos);
	}

	public int getContador() {
		return contador;
	}

	public void setContador(int contador) {
		this.contador = contador;
	}
}
