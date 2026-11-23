package interfaz;

import java.util.NoSuchElementException;
import java.util.Scanner;

import controladores.CentralClubSeneca;
import controladores.ControladorAdministrador;
import controladores.ControladorEmpleado;
import controladores.ControladorEntrenador;
import controladores.ControladorParticipante;
import controladores.ControladorRol;
import controladores.ControladorSocio;
import excepciones.ClubSenecaException;

public class CentralInterfaz {

	private InterfazRol interfazActual;
	private CentralClubSeneca central;
	private Scanner entrada;
	private String titulo;
	private String[] rolesPermitidos;
	private boolean permiteRegistro;

	public CentralInterfaz(CentralClubSeneca central, Scanner entrada) {
		this(central, entrada, "Club Deportivo Séneca", true, new String[0]);
	}

	public CentralInterfaz(CentralClubSeneca central, Scanner entrada, String titulo, boolean permiteRegistro,
			String... rolesPermitidos) {
		this.central = central;
		this.entrada = entrada;
		this.titulo = titulo;
		this.permiteRegistro = permiteRegistro;
		this.rolesPermitidos = rolesPermitidos;
	}

	public static void main(String[] args) {
		ejecutar(args, "Club Deportivo Séneca", true);
	}

	public static void ejecutar(String[] args, String titulo, boolean permiteRegistro, String... rolesPermitidos) {
		CentralClubSeneca central = new CentralClubSeneca(args.length > 0 ? args[0] : "datos");
		try {
			central.cargarInformacion();
		} catch (ClubSenecaException e) {
			System.out.println("Error al cargar la información: " + e.getMessage());
			return;
		}
		try {
			new CentralInterfaz(central, new Scanner(System.in), titulo, permiteRegistro, rolesPermitidos).iniciar();
		} catch (NoSuchElementException e) {
			System.out.println("\nFin de la entrada");
		}
		try {
			central.guardarInformacion();
		} catch (ClubSenecaException e) {
			System.out.println("Error al guardar la información: " + e.getMessage());
		}
	}

	public void iniciar() {
		System.out.println("=== " + titulo + " ===");
		while (true) {
			System.out.println(permiteRegistro ? "\n1. Iniciar sesión\n2. Registrarse\n0. Salir" : "\n1. Iniciar sesión\n0. Salir");
			System.out.print("Opción: ");
			String opcion = entrada.nextLine().trim();
			if (opcion.equals("0")) {
				return;
			}
			if (opcion.equals("1")) {
				iniciarSesion();
				cerrarSesion();
			} else if (opcion.equals("2") && permiteRegistro) {
				registrarse();
			} else {
				System.out.println("Opción inválida");
			}
		}
	}

	public void iniciarSesion() {
		System.out.print("Login: ");
		String username = entrada.nextLine().trim();
		System.out.print("Password: ");
		String password = entrada.nextLine();
		try {
			ControladorRol controlador = central.iniciarSesion(username, password);
			if (!rolPermitido(controlador.getRol())) {
				System.out.println("Error: este programa es solo para " + String.join(", ", rolesPermitidos));
				return;
			}
			interfazActual = crearInterfaz(controlador);
			System.out.println("Bienvenido, " + username + " (" + controlador.getRol() + ")");
			interfazActual.mostrarMenu();
		} catch (ClubSenecaException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

	public void registrarse() {
		System.out.print("Tipo (estudiante o externo): ");
		String tipo = entrada.nextLine().trim();
		System.out.print("Nombre: ");
		String nombre = entrada.nextLine().trim();
		System.out.print("Login: ");
		String login = entrada.nextLine().trim();
		System.out.print("Password: ");
		String password = entrada.nextLine();
		try {
			if (tipo.equalsIgnoreCase("estudiante")) {
				central.registrarEstudianteNoSocio(nombre, login, password);
			} else if (tipo.equalsIgnoreCase("externo")) {
				central.registrarPersonaExterna(nombre, login, password);
			} else {
				System.out.println("Error: el tipo debe ser estudiante o externo");
				return;
			}
			System.out.println("Registro exitoso");
		} catch (ClubSenecaException e) {
			System.out.println("Error: " + e.getMessage());
		}
	}

	public void cerrarSesion() {
		interfazActual = null;
	}

	private boolean rolPermitido(String rol) {
		if (rolesPermitidos.length == 0) {
			return true;
		}
		for (String permitido : rolesPermitidos) {
			if (permitido.equals(rol)) {
				return true;
			}
		}
		return false;
	}

	private InterfazRol crearInterfaz(ControladorRol controlador) {
		return switch (controlador.getRol()) {
		case "Administrador" -> new InterfazAdministrador((ControladorAdministrador) controlador, entrada);
		case "Socio" -> new InterfazSocio((ControladorSocio) controlador, entrada);
		case "Entrenador" -> new InterfazEntrenador((ControladorEntrenador) controlador, entrada);
		case "Participante" -> new InterfazParticipante((ControladorParticipante) controlador, entrada);
		default -> new InterfazEmpleado((ControladorEmpleado) controlador, entrada);
		};
	}
}
