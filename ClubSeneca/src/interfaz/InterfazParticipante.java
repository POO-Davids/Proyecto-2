package interfaz;

import java.util.Scanner;

import controladores.ControladorParticipante;
import excepciones.ClubSenecaException;

public class InterfazParticipante extends InterfazRol {

	public InterfazParticipante(ControladorParticipante controlador, Scanner entrada) {
		super(controlador, entrada);
	}

	private ControladorParticipante participante() {
		return (ControladorParticipante) controlador;
	}

	@Override
	protected String[] getOpciones() {
		return new String[] { "Consultar torneos", "Consultar detalle de torneo", "Inscribirme en un torneo", "Retirarme de un torneo" };
	}

	public void consultarTorneos() throws ClubSenecaException {
		String deporte = leerTexto("Deporte (vacío = todos)");
		String estado = leerTexto("Estado: Inscripciones abiertas, En curso o Finalizado (vacío = todos)");
		mostrarMensaje(controlador.consultarTorneos(deporte, estado));
	}

	public void consultarDetalleTorneo() throws ClubSenecaException {
		mostrarMensaje(controlador.consultarDetalleTorneo(leerTexto("Id del torneo")));
	}

	public void inscribirse() throws ClubSenecaException {
		String idTorneo = leerTexto("Id del torneo");
		String forma = leerTexto("individual o pareja");
		if (forma.equalsIgnoreCase("pareja")) {
			String companero = leerTexto("Username del compañero");
			participante().inscribirPareja(idTorneo, companero, leerTexto("Nivel declarado (vacío si es socio)"));
		} else {
			participante().inscribirseIndividual(idTorneo, leerTexto("Nivel declarado (vacío si es socio)"));
			mostrarMensaje("Inscripción exitosa");
			mostrarMensaje(controlador.consultarDetalleTorneo(idTorneo));
			return;
		}
		mostrarMensaje("Inscripción exitosa");
	}

	public void retirarse() throws ClubSenecaException {
		participante().retirarse(leerTexto("Id del torneo"));
		mostrarMensaje("Retiro exitoso");
	}

	@Override
	public void ejecutarOpcion(int opcion) {
		try {
			switch (opcion) {
			case 1 -> consultarTorneos();
			case 2 -> consultarDetalleTorneo();
			case 3 -> inscribirse();
			case 4 -> retirarse();
			default -> super.ejecutarOpcion(opcion);
			}
		} catch (ClubSenecaException e) {
			mostrarError(e);
		}
	}

	public static void main(String[] args) {
		CentralInterfaz.ejecutar(args, "Club Deportivo Séneca - Participantes de torneos", true, "Participante");
	}
}
