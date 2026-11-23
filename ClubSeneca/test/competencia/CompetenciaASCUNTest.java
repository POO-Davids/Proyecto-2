package competencia;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import equipos.Equipo;
import estadisticas.EstadisticaFutbol;
import excepciones.ElementoDuplicadoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.MarcadorInvalidoException;
import excepciones.RequisitosInscripcionException;
import jugadores.Jugador;
import jugadores.JugadorConjunto;
import partidos.PartidoCompetencia;
import usuarios.Socio;

class CompetenciaASCUNTest {

	private CompetenciaASCUN ascun;
	private Equipo equipo;
	private String[] nomina;
	private int partidos;

	@BeforeEach
	void setUp() throws Exception {
		ascun = new CompetenciaASCUN("C1", "ASCUN Fútbol 2026", "ASCUN", "2026-2", "Fútbol", "Mayores", "Grupos", 12, 2);
		equipo = new Equipo("EQ-1", "Fútbol Mayores", "Fútbol", "Mayores", 20);
		for (int i = 1; i <= 15; i++) {
			Jugador j = new JugadorConjunto("J" + i, new Socio("s" + i, "pw", "S" + i, "20"), "x", i, equipo);
			equipo.inscribirJugador(j);
		}
		nomina = Arrays.copyOf(equipo.getJugadoresIds(), 14);
	}

	@Test
	void requisitosDeInscripcion() throws Exception {
		assertFalse(ascun.verificarRequisitosInscripcion(equipo, Arrays.copyOf(nomina, 10)));
		assertFalse(ascun.verificarRequisitosInscripcion(new Equipo("EQ-2", "Sub", "Fútbol", "Sub-17", 20), new String[0]));
		assertFalse(ascun.verificarRequisitosInscripcion(equipo, new String[] { "J1", "J2", "J3", "J4", "J5", "J6", "J7", "J8", "J9",
				"J10", "J11", "NO-ES" }));
		assertThrows(RequisitosInscripcionException.class, () -> ascun.inscribirEquipo(equipo, Arrays.copyOf(nomina, 10)));
		ascun.inscribirEquipo(equipo, nomina);
		EquipoPropioCompetencia propio = (EquipoPropioCompetencia) ascun.getEquipoParticipante("EQ-1");
		assertEquals(14, propio.getNumInscritos());
		assertEquals(0, propio.getPuntos());
		assertThrows(ElementoDuplicadoException.class, () -> ascun.inscribirEquipo(equipo, nomina));
		assertTrue(ascun.jugadorPuedeJugar("J1"));
		assertFalse(ascun.jugadorPuedeJugar("J15"));
	}

	private PartidoCompetencia partido(String rival) {
		PartidoCompetencia p = new PartidoCompetencia("P" + (++partidos), "C1", LocalDateTime.of(2026, 10, 20, 15, 0), "Fútbol", "Mayores",
				(EquipoPropioCompetencia) ascun.getEquipoParticipante("EQ-1"), (EquipoAfueraCompetencia) ascun.getEquipoParticipante(rival),
				"Estadio", Arrays.copyOf(nomina, 11), 2);
		ascun.agregarPartido(p);
		return p;
	}

	@Test
	void puntosTablaDeGrupoYClasificacion() throws Exception {
		ascun.inscribirEquipo(equipo, nomina);
		for (String rival : List.of("Nacional", "Javeriana", "Rosario")) {
			ascun.agregarEquipoAfuera(rival);
		}
		ascun.crearGrupo("A");
		for (String id : List.of("EQ-1", "Nacional", "Javeriana", "Rosario")) {
			ascun.inscribirEquipoAGrupo("A", id);
		}
		assertThrows(ElementoNoEncontradoException.class, () -> ascun.inscribirEquipoAGrupo("A", "NoExiste"));
		ascun.registrarResultado(partido("Nacional").getIdPartido(), 3, 1);
		ascun.registrarResultado(partido("Javeriana").getIdPartido(), 1, 1);
		ascun.registrarResultado(partido("Rosario").getIdPartido(), 0, 1);
		assertEquals(4, ascun.getEquipoParticipante("EQ-1").getPuntos());
		assertEquals(3, ascun.getEquipoParticipante("Rosario").getPuntos());
		assertEquals(1, ascun.getEquipoParticipante("Javeriana").getPuntos());
		List<EquipoCompetencia> tabla = ascun.getTablaPosiciones("A");
		assertEquals("EQ-1", tabla.get(0).getEquipoCompetenciaId());
		ascun.terminarFaseGrupos();
		assertEquals(List.of("EQ-1", "Rosario"), Arrays.asList(ascun.getFaseEliminacion().getEquiposRestantes()));
	}

	@Test
	void sinEmpatesEnBaloncesto() throws Exception {
		CompetenciaCerros cerros = new CompetenciaCerros("C2", "Cerros", "Cerros", "2026", "Baloncesto", "Mayores", "Liga", 1, 2);
		Equipo basket = new Equipo("EQ-B", "Basket", "Baloncesto", "Mayores", 10);
		basket.inscribirJugador(new JugadorConjunto("B1", new Socio("b", "pw", "B", "20"), "x", 1, basket));
		cerros.inscribirEquipo(basket, new String[] { "B1" });
		cerros.agregarEquipoAfuera("Rival");
		PartidoCompetencia p = new PartidoCompetencia("PB", "C2", LocalDateTime.now(), "Baloncesto", "Mayores",
				(EquipoPropioCompetencia) cerros.getEquipoParticipante("EQ-B"), (EquipoAfueraCompetencia) cerros.getEquipoParticipante("Rival"),
				"Coliseo", new String[] { "B1" }, 2);
		cerros.agregarPartido(p);
		assertThrows(MarcadorInvalidoException.class, () -> cerros.registrarResultado("PB", 70, 70));
		cerros.registrarResultado("PB", 70, 65);
		assertEquals(3, cerros.getEquipoParticipante("EQ-B").getPuntos());
		assertThrows(ElementoNoEncontradoException.class, () -> cerros.registrarResultado("NO", 1, 0));
	}

	@Test
	void goleadoresOrdenadosYDestacado() throws Exception {
		ascun.inscribirEquipo(equipo, nomina);
		ascun.acumularEstadistica(new EstadisticaFutbol("J1", 2, 0));
		ascun.acumularEstadistica(new EstadisticaFutbol("J2", 1, 1));
		ascun.acumularEstadistica(new EstadisticaFutbol("J1", 1, 0));
		assertEquals("J1", ascun.getGoleadores().get(0).getUsuarioUsername());
		assertEquals(3, ascun.getGoleadores().get(0).getValorPrincipal());
		assertEquals("J1", ascun.getJugadorDestacado());
	}

	@Test
	void faseGruposMaximoYDuplicados() throws Exception {
		FaseGrupos fase = new FaseGrupos(2);
		fase.crearGrupo("A");
		fase.crearGrupo("B");
		assertThrows(ElementoDuplicadoException.class, () -> fase.crearGrupo("A"));
		fase.agregarEquipoAGrupo("A", "x");
		fase.agregarEquipoAGrupo("A", "y");
		assertThrows(RequisitosInscripcionException.class, () -> fase.agregarEquipoAGrupo("A", "z"));
		assertThrows(ElementoDuplicadoException.class, () -> fase.agregarEquipoAGrupo("B", "x"));
		assertThrows(ElementoNoEncontradoException.class, () -> fase.agregarEquipoAGrupo("C", "z"));
	}
}
