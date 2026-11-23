package reservas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import excepciones.InstalacionNoDisponibleException;
import instalaciones.Instalacion;

class CentralReservasTest {

	private static final LocalDate DIA = LocalDate.of(2026, 10, 17);
	private CentralReservas reservas;
	private Instalacion cancha;

	@BeforeEach
	void setUp() {
		reservas = new CentralReservas();
		cancha = new Instalacion("I1", "tenis", 4, false);
	}

	@Test
	void cruzaConMismaFechaYHora() {
		Reserva reserva = new Reserva("R1", cancha, DIA, new int[] { 10, 11 }, "ana");
		assertTrue(reserva.cruzaCon(DIA, new int[] { 11, 12 }));
		assertFalse(reserva.cruzaCon(DIA, new int[] { 12 }));
		assertFalse(reserva.cruzaCon(DIA.plusDays(1), new int[] { 10 }));
		assertTrue(reserva.estaEnCurso(DIA.atTime(10, 30)));
	}

	@Test
	void crearReservaRechazaCruces() throws Exception {
		Reserva reserva = reservas.crearReserva(cancha, DIA, new int[] { 10 }, "ana");
		assertEquals("ana", reserva.getReservadoPor());
		assertThrows(InstalacionNoDisponibleException.class, () -> reservas.crearReserva(cancha, DIA, new int[] { 10 }, "luis"));
		reservas.cancelarReserva(reserva.getIdReserva());
		assertTrue(cancha.estaDisponible(DIA, new int[] { 10 }));
		assertNotNull(reservas.crearReserva(cancha, DIA, new int[] { 10 }, "luis"));
	}

	@Test
	void cancelarReservasFueraDeHorario() throws Exception {
		Reserva dentro = reservas.crearReserva(cancha, DIA, new int[] { 10 }, "ana");
		Reserva fuera = reservas.crearReserva(cancha, DIA, new int[] { 20 }, "luis");
		Boolean[] horario = new Boolean[24];
		for (int h = 0; h < 24; h++) {
			horario[h] = h >= 7 && h <= 19;
		}
		List<String> canceladas = reservas.cancelarReservasFueraDeHorario("I1", DIA, horario);
		assertEquals(List.of(fuera.getIdReserva()), canceladas);
		assertNotNull(reservas.getReserva(dentro.getIdReserva()));
		assertNull(reservas.getReserva(fuera.getIdReserva()));
	}

	@Test
	void reservasFuturasYEnCurso() throws Exception {
		reservas.crearReserva(cancha, LocalDate.now().plusYears(1), new int[] { 10 }, "ana");
		assertTrue(reservas.tieneReservasFuturas("I1"));
		assertFalse(reservas.tieneReservasFuturas("I2"));
		LocalDateTime ahora = LocalDateTime.now();
		reservas.agregarReserva(new Reserva("R-X", cancha, ahora.toLocalDate(), new int[] { ahora.getHour() }, "luis"));
		assertNotNull(reservas.getReservaEnCurso("luis", ahora));
		assertNull(reservas.getReservaEnCurso("ana", ahora));
	}

	@Test
	void reservasEntreFechasOrdenadas() throws Exception {
		reservas.crearReserva(cancha, DIA.plusDays(2), new int[] { 9 }, "b");
		reservas.crearReserva(cancha, DIA, new int[] { 15 }, "a");
		reservas.crearReserva(cancha, DIA, new int[] { 8 }, "c");
		List<Reserva> lista = reservas.getReservasEntre(DIA, DIA.plusDays(1));
		assertEquals(2, lista.size());
		assertEquals("c", lista.get(0).getReservadoPor());
	}
}
