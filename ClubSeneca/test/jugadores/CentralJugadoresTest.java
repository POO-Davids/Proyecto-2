package jugadores;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import equipos.Equipo;
import excepciones.CamisetaRepetidaException;
import excepciones.CategoriaEdadException;
import excepciones.CupoEquipoExcedidoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.EquipoPorDeporteException;
import excepciones.ValorInvalidoException;
import usuarios.Entrenador;
import usuarios.Socio;

class CentralJugadoresTest {

	private CentralJugadores jugadores;
	private Socio ana;
	private Socio sofia;

	@BeforeEach
	void setUp() {
		jugadores = new CentralJugadores();
		ana = new Socio("ana", "pw", "Ana", "20");
		sofia = new Socio("sofia", "pw", "Sofía", "20");
	}

	@Test
	void eloConK32YResultadoEsperadoIgual() throws Exception {
		Jugador a = jugadores.inscribirJugadorIndividual(ana, "tenis", "Intermedio", 1000);
		Jugador s = jugadores.inscribirJugadorIndividual(sofia, "tenis", "Intermedio", 1000);
		jugadores.actualizarElo(new String[] { a.getIdJugador() }, new String[] { s.getIdJugador() });
		assertEquals(1016, ((JugadorIndividual) a).getElo(), 0.001);
		assertEquals(984, ((JugadorIndividual) s).getElo(), 0.001);
	}

	@Test
	void eloFavoritoGanaMenosPuntos() throws Exception {
		JugadorIndividual a = (JugadorIndividual) jugadores.inscribirJugadorIndividual(ana, "tenis", "Intermedio", 1200);
		JugadorIndividual s = (JugadorIndividual) jugadores.inscribirJugadorIndividual(sofia, "tenis", "Intermedio", 1000);
		jugadores.actualizarElo(new String[] { a.getIdJugador() }, new String[] { s.getIdJugador() });
		float ganancia = a.getElo() - 1200;
		assertTrue(ganancia > 0 && ganancia < 16);
		assertEquals(2200, a.getElo() + s.getElo(), 0.01);
	}

	@Test
	void jugadorIndividualUnoPorDisciplinaYNivelValido() throws Exception {
		jugadores.inscribirJugadorIndividual(ana, "tenis", "Intermedio", 1000);
		assertThrows(EquipoPorDeporteException.class, () -> jugadores.inscribirJugadorIndividual(ana, "TENIS", "Avanzado", 1000));
		assertThrows(ValorInvalidoException.class, () -> jugadores.inscribirJugadorIndividual(ana, "padel", "Experto", 1000));
		assertNotNull(jugadores.inscribirJugadorIndividual(ana, "padel", "Avanzado", 1100));
		assertEquals(2, jugadores.getJugadoresDeUsuario("ana").size());
	}

	@Test
	void reglasDeJugadorConjunto() throws Exception {
		Equipo mayores = new Equipo("EQ-1", "Fútbol Femenino", "Fútbol", "Mayores", 2);
		Jugador j = jugadores.inscribirJugadorConjunto(ana, "Fútbol", "Delantera", 9, mayores);
		mayores.inscribirJugador(j);
		ana.agregarJugador(j.getIdJugador());
		assertThrows(EquipoPorDeporteException.class, () -> jugadores.inscribirJugadorConjunto(ana, "Fútbol", "Defensa", 4, mayores));
		assertThrows(CamisetaRepetidaException.class, () -> jugadores.inscribirJugadorConjunto(sofia, "Fútbol", "Defensa", 9, mayores));
		assertThrows(CategoriaEdadException.class,
				() -> jugadores.inscribirJugadorConjunto(sofia, "Fútbol", "Defensa", 4, new Equipo("EQ-2", "Sub", "Fútbol", "Sub-17", 20)));
		assertThrows(EquipoPorDeporteException.class, () -> jugadores.inscribirJugadorConjunto(sofia, "Baloncesto", "Base", 4, mayores));
		Jugador s = jugadores.inscribirJugadorConjunto(sofia, "Fútbol", "Defensa", 4, mayores);
		mayores.inscribirJugador(s);
		Socio otra = new Socio("eva", "pw", "Eva", "25");
		assertThrows(CupoEquipoExcedidoException.class, () -> jugadores.inscribirJugadorConjunto(otra, "Fútbol", "Arquera", 1, mayores));
	}

	@Test
	void menorDeEdadEntraEnSuCategoria() throws Exception {
		Socio nino = new Socio("nino", "pw", "Niño", "14");
		assertNotNull(jugadores.inscribirJugadorConjunto(nino, "Fútbol", "Volante", 8, new Equipo("EQ-3", "Sub-15", "Fútbol", "Sub-15", 20)));
		Socio joven = new Socio("joven", "pw", "Joven", "16");
		assertThrows(CategoriaEdadException.class,
				() -> jugadores.inscribirJugadorConjunto(joven, "Fútbol", "Volante", 8, new Equipo("EQ-4", "M", "Fútbol", "Mayores", 20)));
	}

	@Test
	void darDeBajaYAnonimo() throws Exception {
		Jugador a = jugadores.inscribirJugadorIndividual(ana, "tenis", "Intermedio", 1000);
		assertSame(a, jugadores.darDeBajaJugador(a.getIdJugador()));
		assertNull(jugadores.getJugadorPorId(a.getIdJugador()));
		assertThrows(ElementoNoEncontradoException.class, () -> jugadores.darDeBajaJugador(a.getIdJugador()));
		assertSame(jugadores.getJugadorAnonimo(), jugadores.getJugadorPorId(CentralJugadores.ID_ANONIMO));
	}

	@Test
	void nivelesCompatiblesEntreEquipos() throws Exception {
		Jugador a = jugadores.inscribirJugadorIndividual(ana, "tenis", "Avanzado", 1000);
		Jugador s = jugadores.inscribirJugadorIndividual(sofia, "tenis", "Principiante", 1000);
		Jugador e = jugadores.inscribirJugadorIndividual(new Entrenador("mario", "pw", "E"), "tenis", "Principiante", 1000);
		assertFalse(jugadores.verificarNivelesEquipos(new String[] { a.getIdJugador() }, new String[] { s.getIdJugador() }));
		assertTrue(jugadores.verificarNivelesEquipos(new String[] { a.getIdJugador() }, new String[] { e.getIdJugador() }));
		assertThrows(ElementoNoEncontradoException.class, () -> jugadores.verificarNivelesCompatibles("JUG-99", a.getIdJugador()));
	}
}
