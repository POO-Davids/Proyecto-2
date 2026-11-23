package instalaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import excepciones.InstalacionNoDisponibleException;
import excepciones.ValorInvalidoException;

class InstalacionTest {

	private static final LocalDate DIA = LocalDate.of(2026, 10, 17);

	@Test
	void capacidadSegunTipoYModalidad() {
		Instalacion padel = new Instalacion("I1", "padel", 4, true);
		assertTrue(padel.admiteJugadores(4, "dobles"));
		assertFalse(padel.admiteJugadores(2, "sencillos"));
		Instalacion tenis = new Instalacion("I2", "tenis", 4, false);
		assertTrue(tenis.admiteJugadores(2, "sencillos"));
		assertTrue(tenis.admiteJugadores(4, "dobles"));
		assertFalse(tenis.admiteJugadores(4, "sencillos"));
		assertFalse(tenis.admiteJugadores(3, "dobles"));
		Instalacion mesa = new Instalacion("I3", "mesa", 4, false);
		assertTrue(mesa.admiteJugadores(2, null));
		assertTrue(mesa.admiteJugadores(4, null));
		Instalacion grupal = new Instalacion("I4", "grupal", 30, true);
		assertTrue(grupal.admiteJugadores(30, null));
		assertFalse(grupal.admiteJugadores(31, null));
	}

	@Test
	void horarioPorDefectoYReservas() throws Exception {
		Instalacion tenis = new Instalacion("I1", "tenis", 4, false);
		assertTrue(tenis.estaDisponible(DIA, new int[] { 6, 21 }));
		assertFalse(tenis.estaDisponible(DIA, new int[] { 22 }));
		assertFalse(tenis.estaDisponible(DIA, new int[0]));
		tenis.agregarReserva(DIA, new int[] { 10 });
		assertTrue(tenis.estaReservada(DIA, new int[] { 10 }));
		assertThrows(InstalacionNoDisponibleException.class, () -> tenis.agregarReserva(DIA, new int[] { 10 }));
		tenis.liberarHoras(DIA, new int[] { 10 });
		assertTrue(tenis.estaDisponible(DIA, new int[] { 10 }));
	}

	@Test
	void cambiarHorarioConservaHorasReservadas() throws Exception {
		Instalacion padel = new Instalacion("I1", "padel", 4, true);
		padel.agregarReserva(DIA, new int[] { 10 });
		Boolean[] nuevo = new Boolean[24];
		for (int h = 0; h < 24; h++) {
			nuevo[h] = h >= 7 && h <= 19;
		}
		padel.setHorario(DIA, nuevo);
		assertTrue(Arrays.equals(nuevo, padel.getHorario().get(DIA)));
		assertFalse(padel.estaDisponible(DIA, new int[] { 10 }));
		assertFalse(padel.estaDisponible(DIA, new int[] { 21 }));
		assertTrue(padel.estaDisponible(DIA, new int[] { 7 }));
	}

	@Test
	void centralControlaMaximoSimultaneas() throws Exception {
		CentralInstalaciones central = new CentralInstalaciones();
		for (int i = 0; i < 3; i++) {
			central.agregarInstalacion("padel", 4, true);
		}
		central.setMaximoSimultaneas("padel", 2);
		int[] diez = { 10 };
		assertTrue(central.hayCupoSimultaneo("padel", DIA, diez));
		central.getInstalacion("INS-1").agregarReserva(DIA, diez);
		central.getInstalacion("INS-2").agregarReserva(DIA, diez);
		assertFalse(central.hayCupoSimultaneo("padel", DIA, diez));
		assertTrue(central.hayCupoSimultaneo("padel", DIA, new int[] { 11 }));
		assertTrue(central.hayCupoSimultaneo("tenis", DIA, diez));
		assertThrows(ValorInvalidoException.class, () -> central.agregarInstalacion("mesa", 0, true));
	}
}
