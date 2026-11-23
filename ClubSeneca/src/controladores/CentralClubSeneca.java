package controladores;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import competencia.CentralCompetencias;
import entrenamientos.CentralSesionesEntrenamiento;
import equipos.CentralEquipos;
import excepciones.CredencialesInvalidasException;
import excepciones.ElementoDuplicadoException;
import excepciones.PersistenciaException;
import excepciones.ValorInvalidoException;
import instalaciones.CentralInstalaciones;
import jugadores.CentralJugadores;
import partidos.Partidos;
import persistencia.CentralPersistencia;
import reservas.CentralReservas;
import tiendas.Cafeteria;
import tiendas.PagoMensualidad;
import tiendas.Tienda;
import torneos.CentralTorneos;
import usuarios.CentralUsuarios;
import usuarios.EstudianteNoSocio;
import usuarios.PersonaExterna;
import usuarios.Usuario;

public class CentralClubSeneca {

	private List<PagoMensualidad> mensualidades;
	private Boolean[] horarioAtencion;
	private Usuario usuarioActual;
	private Tienda tienda;
	private CentralCompetencias competencias;
	private Partidos partidos;
	private CentralSesionesEntrenamiento sesionesEntrenamiento;
	private CentralInstalaciones instalaciones;
	private CentralReservas reservas;
	private CentralUsuarios usuarios;
	private Cafeteria cafeteria;
	private CentralJugadores jugadores;
	private CentralEquipos equipos;
	private CentralPersistencia persistencia;
	private CentralTorneos torneos;

	public CentralClubSeneca() {
		this("datos");
	}

	public CentralClubSeneca(String directorio) {
		mensualidades = new ArrayList<>();
		horarioAtencion = new Boolean[24];
		for (int h = 0; h < 24; h++) {
			horarioAtencion[h] = h >= 8 && h <= 19;
		}
		tienda = new Tienda();
		cafeteria = new Cafeteria();
		usuarios = new CentralUsuarios();
		jugadores = new CentralJugadores();
		equipos = new CentralEquipos();
		competencias = new CentralCompetencias();
		partidos = new Partidos(jugadores);
		torneos = new CentralTorneos(jugadores, partidos);
		sesionesEntrenamiento = new CentralSesionesEntrenamiento();
		instalaciones = new CentralInstalaciones();
		reservas = new CentralReservas();
		persistencia = new CentralPersistencia(directorio);
	}

	public void cargarInformacion() throws PersistenciaException {
		persistencia.cargarSesion(this);
	}

	public void guardarInformacion() throws PersistenciaException {
		persistencia.guardarSesion(this);
	}

	public ControladorRol iniciarSesion(String username, String password) throws CredencialesInvalidasException {
		usuarioActual = getUsuarios().autenticar(username, password);
		return crearControlador(usuarioActual);
	}

	public void cerrarSesion() throws PersistenciaException {
		usuarioActual = null;
		guardarInformacion();
	}

	public Usuario getUsuarioActual() {
		return usuarioActual;
	}

	public Boolean[] getHorarioAtencion() {
		return horarioAtencion;
	}

	public void setHorarioAtencion(Boolean[] horario) {
		this.horarioAtencion = Arrays.copyOf(horario, 24);
	}

	public void registrarMensualidad(PagoMensualidad pago) {
		mensualidades.add(pago);
	}

	public List<PagoMensualidad> getMensualidades() {
		return mensualidades;
	}

	public CentralUsuarios getUsuarios() {
		return usuarios;
	}

	public CentralJugadores getJugadores() {
		return jugadores;
	}

	public CentralEquipos getEquipos() {
		return equipos;
	}

	public CentralCompetencias getCompetencias() {
		return competencias;
	}

	public Partidos getPartidos() {
		return partidos;
	}

	public CentralSesionesEntrenamiento getSesionesEntrenamiento() {
		return sesionesEntrenamiento;
	}

	public CentralInstalaciones getInstalaciones() {
		return instalaciones;
	}

	public CentralReservas getReservas() {
		return reservas;
	}

	public CentralTorneos getTorneos() {
		return torneos;
	}

	public Tienda getTienda() {
		return tienda;
	}

	public Cafeteria getCafeteria() {
		return cafeteria;
	}

	public void registrarEstudianteNoSocio(String nombre, String login, String password)
			throws ValorInvalidoException, ElementoDuplicadoException {
		verificarRegistro(nombre, login, password);
		usuarios.agregarUsuario(new EstudianteNoSocio(login, password, nombre));
	}

	public void registrarPersonaExterna(String nombre, String login, String password)
			throws ValorInvalidoException, ElementoDuplicadoException {
		verificarRegistro(nombre, login, password);
		usuarios.agregarUsuario(new PersonaExterna(login, password, nombre));
	}

	private void verificarRegistro(String nombre, String login, String password)
			throws ValorInvalidoException, ElementoDuplicadoException {
		if (nombre == null || nombre.isBlank() || login == null || login.isBlank() || password == null || password.isBlank()) {
			throw new ValorInvalidoException("El nombre, el login y el password son obligatorios");
		}
		if (usuarios.existeUsuario(login)) {
			throw new ElementoDuplicadoException("Ya existe un usuario con el login " + login);
		}
	}

	private ControladorRol crearControlador(Usuario usuario) {
		return switch (usuario.getTipo()) {
		case "Administrador" -> new ControladorAdministrador(this);
		case "Socio" -> new ControladorSocio(this, usuario);
		case "Entrenador" -> new ControladorEntrenador(this, usuario);
		case "EstudianteNoSocio", "PersonaExterna" -> new ControladorParticipante(this, usuario);
		default -> new ControladorEmpleado(this, usuario);
		};
	}
}
