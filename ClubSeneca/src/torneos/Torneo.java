package torneos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import excepciones.CuposAgotadosException;
import excepciones.ElementoDuplicadoException;
import excepciones.EstadoTorneoException;
import excepciones.InscripcionTorneoException;
import excepciones.ValorInvalidoException;
import partidos.PartidoTorneo;
import usuarios.Usuario;
import premios.Premio;

public abstract class Torneo {

	protected String idTorneo;
	protected String nombre;
	protected String deporte;
	protected String tipoParticipante;
	protected String modalidad;
	protected String categoria;
	protected String nivel;
	protected int cupos;
	protected int cuposPreferenciales;
	protected LocalDate fecha;
	protected String estado;
	protected List<ParticipanteTorneo> posiciones;
	protected List<Premio> premios;
	protected List<InscripcionTorneo> inscripciones;
	protected List<PartidoTorneo> partidos;

	protected Torneo(String idTorneo, String nombre, String deporte, String tipoParticipante, String modalidad, String categoria,
			String nivel, int cupos, int cuposPreferenciales, LocalDate fecha) {
		this.idTorneo = idTorneo;
		this.nombre = nombre;
		this.deporte = deporte;
		this.tipoParticipante = tipoParticipante;
		this.modalidad = modalidad == null ? "" : modalidad;
		this.categoria = categoria == null ? "" : categoria;
		this.nivel = nivel == null ? "" : nivel;
		this.cupos = cupos;
		this.cuposPreferenciales = cuposPreferenciales;
		this.fecha = fecha;
		this.estado = "Inscripciones abiertas";
		this.posiciones = new ArrayList<>();
		this.premios = new ArrayList<>();
		this.inscripciones = new ArrayList<>();
		this.partidos = new ArrayList<>();
	}

	public String getIdTorneo() {
		return idTorneo;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDeporte() {
		return deporte;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public String getEstado() {
		return estado;
	}

	public int getCuposDisponibles() {
		return cupos - inscripciones.size();
	}

	public int getPreferencialesLibres() {
		int ocupados = 0;
		for (InscripcionTorneo inscripcion : inscripciones) {
			if (inscripcion.esPreferencial()) {
				ocupados++;
			}
		}
		return Math.max(0, cuposPreferenciales - ocupados);
	}

	public int getRegularesLibres() {
		int ocupados = 0;
		for (InscripcionTorneo inscripcion : inscripciones) {
			if (!inscripcion.esPreferencial()) {
				ocupados++;
			}
		}
		return Math.max(0, (cupos - cuposPreferenciales) - ocupados);
	}

	public void verificarEstado(String esperado) throws EstadoTorneoException {
		if (!estado.equalsIgnoreCase(esperado)) {
			throw new EstadoTorneoException("El torneo está " + estado);
		}
	}

	public void verificarInscripcion(ParticipanteTorneo participante)
			throws InscripcionTorneoException, ElementoDuplicadoException, CuposAgotadosException {
		boolean deEquipo = tipoParticipante.equalsIgnoreCase("equipo");
		boolean dobles = modalidad.equalsIgnoreCase("dobles");
		if (participante instanceof ParticipanteIndividual && (deEquipo || dobles)) {
			throw new InscripcionTorneoException("El torneo " + idTorneo + " no admite inscripciones individuales");
		}
		if (participante instanceof ParejaTorneo && !dobles) {
			throw new InscripcionTorneoException("El torneo " + idTorneo + " no es de dobles");
		}
		if (participante instanceof EquipoTorneo && !deEquipo) {
			throw new InscripcionTorneoException("El torneo " + idTorneo + " no es de equipos");
		}
		String nivelParticipante = null;
		if (participante instanceof ParticipanteIndividual individual) {
			nivelParticipante = individual.getNivel();
		} else if (participante instanceof ParejaTorneo pareja) {
			nivelParticipante = pareja.getNivel();
		}
		if (nivelParticipante != null && !nivelParticipante.equalsIgnoreCase(nivel)) {
			throw new InscripcionTorneoException("El nivel " + nivelParticipante + " no corresponde al nivel " + nivel + " del torneo");
		}
		for (String username : participante.getUsernames()) {
			if (estaInscrito(username)) {
				throw new ElementoDuplicadoException(username + " ya está inscrito en el torneo " + idTorneo);
			}
		}
		if (participante instanceof EquipoTorneo equipo) {
			for (InscripcionTorneo inscripcion : inscripciones) {
				if (inscripcion.getParticipante() instanceof EquipoTorneo otro
						&& otro.getEquipoClub().getIdEquipo().equals(equipo.getEquipoClub().getIdEquipo())) {
					throw new ElementoDuplicadoException("El equipo " + equipo.getNombre() + " ya está inscrito en el torneo " + idTorneo);
				}
			}
		}
		if (getCuposDisponibles() == 0) {
			throw new CuposAgotadosException("No quedan cupos en el torneo " + idTorneo);
		}
	}

	public InscripcionTorneo inscribir(ParticipanteTorneo participante, String tipoCupo, float tarifa) {
		InscripcionTorneo inscripcion = new InscripcionTorneo("I-" + participante.getIdParticipante(), participante, tipoCupo, tarifa,
				LocalDate.now());
		inscripciones.add(inscripcion);
		return inscripcion;
	}

	public void retirar(String username) throws EstadoTorneoException, InscripcionTorneoException {
		verificarEstado("Inscripciones abiertas");
		if (!estaInscrito(username)) {
			throw new InscripcionTorneoException(username + " no está inscrito en el torneo " + idTorneo);
		}
		inscripciones.removeIf(inscripcion -> inscripcion.getParticipante().contiene(username));
	}

	public Boolean estaInscrito(String username) {
		for (InscripcionTorneo inscripcion : inscripciones) {
			if (inscripcion.getParticipante().contiene(username)) {
				return true;
			}
		}
		return false;
	}

	public ParticipanteTorneo getParticipante(String idParticipante) {
		for (InscripcionTorneo inscripcion : inscripciones) {
			if (inscripcion.getParticipante().getIdParticipante().equals(idParticipante)) {
				return inscripcion.getParticipante();
			}
		}
		return null;
	}

	public void iniciar() throws EstadoTorneoException, InscripcionTorneoException {
		verificarEstado("Inscripciones abiertas");
		if (inscripciones.size() < 2) {
			throw new InscripcionTorneoException("El torneo " + idTorneo + " necesita al menos 2 inscritos para iniciar");
		}
		estado = "En curso";
	}

	public void registrarPartido(PartidoTorneo partido) throws EstadoTorneoException {
		verificarEstado("En curso");
		partidos.add(partido);
	}

	public void registrarPosiciones(List<ParticipanteTorneo> posiciones) throws EstadoTorneoException, ValorInvalidoException {
		verificarEstado("En curso");
		if (posiciones.size() != inscripciones.size()) {
			throw new ValorInvalidoException("Debe registrar la posición de los " + inscripciones.size() + " participantes");
		}
		this.posiciones = new ArrayList<>(posiciones);
	}

	public String getTablaPosiciones() {
		StringBuilder texto = new StringBuilder();
		for (int i = 0; i < posiciones.size(); i++) {
			texto.append(i + 1).append(". ").append(posiciones.get(i).getNombre()).append("\n");
		}
		return texto.toString();
	}

	public List<Premio> finalizar() throws EstadoTorneoException {
		verificarEstado("En curso");
		if (posiciones.isEmpty()) {
			throw new EstadoTorneoException("Faltan las posiciones");
		}
		premios.addAll(asignarPremios());
		estado = "Finalizado";
		return premios;
	}

	public String getHistorial() {
		StringBuilder texto = new StringBuilder(toString()).append("\nParticipantes:\n");
		for (InscripcionTorneo inscripcion : inscripciones) {
			texto.append("- ").append(inscripcion.getParticipante().getIdParticipante()).append(" ")
					.append(inscripcion.getParticipante().getNombre()).append(" | cupo ").append(inscripcion.getTipoCupo())
					.append(String.format(" | tarifa %.0f", inscripcion.getTarifaPagada())).append("\n");
		}
		texto.append("Partidos:\n");
		for (PartidoTorneo partido : partidos) {
			texto.append("- ").append(partido.getResumen()).append("\n");
		}
		texto.append("Posiciones:\n").append(getTablaPosiciones());
		texto.append("Premios:\n");
		for (Premio premio : premios) {
			texto.append("- ").append(premio.getDescripcion()).append("\n");
		}
		return texto.toString();
	}

	@Override
	public String toString() {
		String grupo = tipoParticipante.equalsIgnoreCase("equipo") ? "categoría " + categoria : "nivel " + nivel;
		return String.format("%s | %s | %s | %s%s | %s | %s | %s | cupos libres %d/%d", idTorneo, nombre, deporte, tipoParticipante,
				modalidad.isEmpty() ? "" : " " + modalidad, grupo, fecha, estado, getCuposDisponibles(), cupos);
	}

	public abstract List<Premio> asignarPremios();

	public abstract float calcularTarifa(Usuario usuario);

	public String getTipoParticipante() {
		return tipoParticipante;
	}

	public String getModalidad() {
		return modalidad;
	}

	public String getCategoria() {
		return categoria;
	}

	public String getNivel() {
		return nivel;
	}

	public int getCupos() {
		return cupos;
	}

	public int getCuposPreferenciales() {
		return cuposPreferenciales;
	}

	public List<InscripcionTorneo> getInscripciones() {
		return new ArrayList<>(inscripciones);
	}

	public List<PartidoTorneo> getPartidos() {
		return new ArrayList<>(partidos);
	}

	public List<ParticipanteTorneo> getPosiciones() {
		return new ArrayList<>(posiciones);
	}

	public List<Premio> getPremios() {
		return new ArrayList<>(premios);
	}

	public void restaurarEstado(String estado) {
		this.estado = estado;
	}

	public void agregarInscripcion(InscripcionTorneo inscripcion) {
		inscripciones.add(inscripcion);
	}

	public void agregarPartidoCargado(PartidoTorneo partido) {
		partidos.add(partido);
	}

	public void agregarPremio(Premio premio) {
		premios.add(premio);
	}

	public void setPosiciones(List<ParticipanteTorneo> posiciones) {
		this.posiciones = new ArrayList<>(posiciones);
	}

	public void setCuposPreferenciales(int cuposPreferenciales) {
		this.cuposPreferenciales = cuposPreferenciales;
	}
}
