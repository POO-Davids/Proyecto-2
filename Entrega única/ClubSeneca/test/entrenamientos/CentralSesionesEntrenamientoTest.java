package entrenamientos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import equipos.Equipo;
import excepciones.ElementoNoEncontradoException;
import excepciones.EstadoAsistenciaInvalidoException;
import excepciones.EvaluacionInvalidaException;
import excepciones.JugadorNoConvocadoException;
import excepciones.SesionIncompletaException;
import instalaciones.Instalacion;
import jugadores.JugadorConjunto;
import reservas.Reserva;
import usuarios.Entrenador;
import usuarios.Socio;

class CentralSesionesEntrenamientoTest {

	private static final LocalDate DIA = LocalDate.of(2026, 10, 12);
	private CentralSesionesEntrenamiento central;
	private SesionEntrenamiento sesion;
	private JugadorConjunto pedro;
	private JugadorConjunto otro;

	@BeforeEach
	void setUp() {
		central = new CentralSesionesEntrenamiento();
		Equipo equipo = new Equipo("EQ-1", "Fútbol Sub-17 A", "Fútbol", "Sub-17", 20);
		pedro = new JugadorConjunto("J1", new Socio("pedro", "pw", "Pedro", "16"), "x", 9, equipo);
		otro = new JugadorConjunto("J2", new Socio("luis", "pw", "Luis", "16"), "x", 9, "EQ-2", "Fútbol");
		Reserva reserva = new Reserva("R1", new Instalacion("I1", "grupal", 30, true), DIA, new int[] { 16, 17 }, "mario");
		sesion = central.crearSesionEquipo(new Entrenador("mario", "pw", "E"), DIA, 16, 90, reserva, "EQ-1");
	}

	@Test
	void asistenciaNormalizaEstadosYRechazaInvalidos() throws Exception {
		central.registrarAsistencia(sesion.getIdSesion(), pedro, "Asistio");
		assertEquals("asistió", sesion.getAsistencia("J1").getAsistio());
		assertEquals(1, pedro.getAsistencias().size());
		central.registrarAsistencia(sesion.getIdSesion(), pedro, "llegó tarde");
		assertEquals("llegó tarde", sesion.getAsistencia("J1").getAsistio());
		assertEquals(1, pedro.getAsistencias().size());
		assertThrows(EstadoAsistenciaInvalidoException.class, () -> central.registrarAsistencia(sesion.getIdSesion(), pedro, "excusado"));
		assertThrows(JugadorNoConvocadoException.class, () -> central.registrarAsistencia(sesion.getIdSesion(), otro, "asistió"));
		assertThrows(ElementoNoEncontradoException.class, () -> central.registrarAsistencia("SES-99", pedro, "asistió"));
	}

	@Test
	void evaluacionSoloParaQuienAsistioYEntre1y5() throws Exception {
		assertThrows(SesionIncompletaException.class, () -> central.registrarEvaluacion(sesion.getIdSesion(), "J1", 4, "x"));
		central.registrarAsistencia(sesion.getIdSesion(), pedro, "faltó");
		assertThrows(SesionIncompletaException.class, () -> central.registrarEvaluacion(sesion.getIdSesion(), "J1", 4, "x"));
		central.registrarAsistencia(sesion.getIdSesion(), pedro, "asistió");
		assertThrows(EvaluacionInvalidaException.class, () -> central.registrarEvaluacion(sesion.getIdSesion(), "J1", 0, "x"));
		assertThrows(EvaluacionInvalidaException.class, () -> central.registrarEvaluacion(sesion.getIdSesion(), "J1", 6, "x"));
		central.registrarEvaluacion(sesion.getIdSesion(), "J1", 4, "Buena definición");
		assertEquals(4, sesion.getAsistencia("J1").getEvaluacion());
		assertEquals("Buena definición", sesion.getAsistencia("J1").getComentario());
	}

	@Test
	void cancelarPorReservaYFiltroPorFechas() {
		assertEquals(1, central.getSesionesEntre(DIA, DIA).size());
		assertEquals(0, central.getSesionesEntre(DIA.plusDays(1), DIA.plusDays(3)).size());
		central.cancelarSesionPorReserva("R1");
		assertNull(central.getSesion(sesion.getIdSesion()));
	}
}
