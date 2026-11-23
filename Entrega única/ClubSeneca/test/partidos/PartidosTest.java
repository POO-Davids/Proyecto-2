package partidos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import amonestaciones.AmonestacionFutbol;
import equipos.Equipo;
import excepciones.EquiposIncompatiblesException;
import excepciones.JugadorNoInscritoException;
import excepciones.JugadorSuspendidoException;
import excepciones.MarcadorInvalidoException;
import excepciones.MinimoJugadoresException;
import jugadores.CentralJugadores;
import jugadores.Jugador;
import usuarios.Socio;

class PartidosTest {

	private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 15, 15, 0);
	private CentralJugadores jugadores;
	private Partidos partidos;
	private int socios;

	@BeforeEach
	void setUp() {
		jugadores = new CentralJugadores();
		partidos = new Partidos(jugadores);
	}

	private Equipo equipo(String id, String deporte, String categoria, int n) throws Exception {
		Equipo equipo = new Equipo(id, id, deporte, categoria, 30);
		for (int i = 1; i <= n; i++) {
			Socio socio = new Socio("s" + (++socios), "pw", "S" + socios, categoria.equals("Sub-15") ? "14" : "16");
			Jugador j = jugadores.inscribirJugadorConjunto(socio, deporte, "x", i, equipo);
			equipo.inscribirJugador(j);
			socio.agregarJugador(j.getIdJugador());
		}
		return equipo;
	}

	private static String[] primeros(Equipo equipo, int n) {
		return Arrays.copyOf(equipo.getJugadoresIds(), n);
	}

	@Test
	void minimoDeJugadoresPorDeporte() {
		assertTrue(partidos.verificarMinimoJugadores("Baloncesto", new String[5]));
		assertFalse(partidos.verificarMinimoJugadores("Baloncesto", new String[4]));
		assertTrue(partidos.verificarMinimoJugadores("Voleibol", new String[6]));
		assertFalse(partidos.verificarMinimoJugadores("Voleibol", new String[5]));
		assertFalse(partidos.verificarMinimoJugadores("futbol", new String[6]));
	}

	@Test
	void minimoDeFutbolConTildeComoLoUsaElClub() {
		assertFalse(partidos.verificarMinimoJugadores("Fútbol", new String[6]), "Fútbol (con tilde) debe exigir 7 jugadores");
	}

	@Test
	void programarValidaEquiposYHabilitados() throws Exception {
		Equipo a = equipo("BA", "Baloncesto", "Sub-17", 6);
		Equipo b = equipo("BB", "Baloncesto", "Sub-17", 6);
		Equipo v = equipo("VA", "Voleibol", "Sub-17", 7);
		Equipo c = equipo("BC", "Baloncesto", "Sub-15", 6);
		assertThrows(EquiposIncompatiblesException.class, () -> partidos.programarPartidoInterno(a, v, FECHA, primeros(a, 5), primeros(v, 6)));
		assertThrows(EquiposIncompatiblesException.class, () -> partidos.programarPartidoInterno(a, c, FECHA, primeros(a, 5), primeros(c, 5)));
		assertThrows(EquiposIncompatiblesException.class, () -> partidos.programarPartidoInterno(a, a, FECHA, primeros(a, 5), primeros(a, 5)));
		assertThrows(MinimoJugadoresException.class, () -> partidos.programarPartidoInterno(a, b, FECHA, primeros(a, 4), primeros(b, 5)));
		assertThrows(JugadorNoInscritoException.class, () -> partidos.programarPartidoInterno(a, b, FECHA, primeros(b, 5), primeros(b, 5)));
		PartidoInterno partido = partidos.programarPartidoInterno(a, b, FECHA, primeros(a, 5), primeros(b, 5));
		assertEquals("Baloncesto", partido.getDeporte());
		assertSame(partido, partidos.getPartido(partido.getIdPartido()));
	}

	@Test
	void marcadorSinEmpatesEnBaloncestoNiNegativos() throws Exception {
		Equipo a = equipo("BA", "Baloncesto", "Sub-17", 5);
		Equipo b = equipo("BB", "Baloncesto", "Sub-17", 5);
		PartidoInterno partido = partidos.programarPartidoInterno(a, b, FECHA, primeros(a, 5), primeros(b, 5));
		String id = partido.getIdPartido();
		assertThrows(MarcadorInvalidoException.class, () -> partidos.registrarMarcador(id, 80, 80));
		assertThrows(MarcadorInvalidoException.class, () -> partidos.registrarMarcador(id, -1, 3));
		partidos.registrarMarcador(id, 82, 80);
		assertTrue(partido.getCerrado());
		assertEquals(82, partido.getPuntuacionEquipo1());
		assertThrows(MarcadorInvalidoException.class, () -> partidos.registrarMarcador(id, 90, 80));
	}

	@Test
	void amonestacionesSuspendenYSeCumplenEnElSiguientePartido() throws Exception {
		Equipo a = equipo("VA", "Voleibol", "Sub-17", 7);
		Equipo b = equipo("VB", "Voleibol", "Sub-17", 8);
		String[] hb = primeros(b, 6);
		PartidoInterno p1 = partidos.programarPartidoInterno(a, b, FECHA, primeros(a, 6), hb);
		Jugador carlos = jugadores.getJugadorPorId(hb[0]);
		Jugador mario = jugadores.getJugadorPorId(hb[1]);
		Jugador andres = jugadores.getJugadorPorId(hb[2]);
		partidos.registrarAmonestacion(p1.getIdPartido(), carlos, new AmonestacionFutbol(carlos.getIdJugador(), 2, 0));
		partidos.registrarAmonestacion(p1.getIdPartido(), mario, new AmonestacionFutbol(mario.getIdJugador(), 0, 1));
		partidos.registrarAmonestacion(p1.getIdPartido(), andres, new AmonestacionFutbol(andres.getIdJugador(), 1, 0));
		assertTrue(carlos.estaSuspendido());
		assertTrue(mario.estaSuspendido());
		assertFalse(andres.estaSuspendido());
		partidos.registrarMarcador(p1.getIdPartido(), 3, 1);
		assertTrue(carlos.estaSuspendido(), "La suspensión aplica al siguiente partido, no al de la tarjeta");
		assertThrows(JugadorSuspendidoException.class, () -> partidos.programarPartidoInterno(a, b, FECHA.plusDays(7), primeros(a, 6), hb));
		String[] sinSuspendidos = Arrays.copyOfRange(b.getJugadoresIds(), 2, 8);
		PartidoInterno p2 = partidos.programarPartidoInterno(a, b, FECHA.plusDays(7), primeros(a, 6), sinSuspendidos);
		partidos.registrarMarcador(p2.getIdPartido(), 3, 0);
		assertFalse(carlos.estaSuspendido());
		assertFalse(mario.estaSuspendido());
	}

	@Test
	void reemplazarJugadorYEquipoEnHistorial() throws Exception {
		Equipo a = equipo("BA", "Baloncesto", "Sub-17", 5);
		Equipo b = equipo("BB", "Baloncesto", "Sub-17", 5);
		PartidoInterno partido = partidos.programarPartidoInterno(a, b, FECHA, primeros(a, 5), primeros(b, 5));
		String id = a.getJugadoresIds()[0];
		partidos.reemplazarJugadorEnHistorial(id, jugadores.getJugadorAnonimo());
		assertFalse(partido.estaHabilitado(id));
		assertTrue(partido.estaHabilitado(CentralJugadores.ID_ANONIMO));
		Equipo anonimo = new Equipo("EQ-ANONIMO", "Equipo anónimo", "", "", 0);
		partidos.reemplazarEquipoEnHistorial("BA", anonimo);
		assertSame(anonimo, partido.getEquipo1());
	}
}
