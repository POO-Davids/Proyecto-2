package jugadores;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import estadisticas.EstadisticaTenisPadel;
import usuarios.Entrenador;
import usuarios.Socio;

class JugadorIndividualTest {

	private JugadorIndividual jugador(String id, String nivel, String disciplina) {
		return new JugadorIndividual(id, new Socio(id, "pw", id, "20"), disciplina, nivel, 1000);
	}

	@Test
	void nivelesIgualesOAdyacentesSonCompatibles() {
		assertTrue(jugador("a", "Intermedio", "tenis").esNivelCompatible(jugador("b", "Avanzado", "tenis")));
		assertTrue(jugador("a", "Principiante", "tenis").esNivelCompatible(jugador("b", "Principiante", "tenis")));
		assertFalse(jugador("a", "Avanzado", "tenis").esNivelCompatible(jugador("b", "Principiante", "tenis")));
		assertFalse(jugador("a", "Intermedio", "tenis").esNivelCompatible(jugador("b", "Intermedio", "padel")));
		assertFalse(jugador("a", "Intermedio", "tenis").esNivelCompatible(null));
	}

	@Test
	void entrenadorEsExcepcionDeNivel() {
		JugadorIndividual entrenador = new JugadorIndividual("e", new Entrenador("e", "pw", "C"), "tenis", "Principiante", 1000);
		assertTrue(entrenador.getEsEntrenador());
		assertTrue(jugador("a", "Avanzado", "tenis").esNivelCompatible(entrenador));
	}

	@Test
	void acumulaEstadisticasYSuspensiones() {
		JugadorIndividual j = jugador("a", "Intermedio", "tenis");
		j.acumularEstadistica(new EstadisticaTenisPadel("a", 2, 12, 3, 5));
		j.acumularEstadistica(new EstadisticaTenisPadel("a", 1, 8, 1, 2));
		EstadisticaTenisPadel acumulada = (EstadisticaTenisPadel) j.getEstadisticaAcumulada();
		assertEquals(3, acumulada.getSetGanados());
		assertEquals(20, acumulada.getGamesGanados());
		assertEquals(4, acumulada.getAces());
		assertFalse(j.estaSuspendido());
		j.suspender(1);
		assertTrue(j.estaSuspendido());
		j.cumplirFechaSuspension();
		assertFalse(j.estaSuspendido());
		assertThrows(IllegalArgumentException.class, () -> j.suspender(0));
	}
}
