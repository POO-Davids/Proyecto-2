package torneos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import excepciones.BonoNoDisponibleException;
import excepciones.CuposAgotadosException;
import excepciones.ElementoDuplicadoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.EstadoTorneoException;
import excepciones.InscripcionTorneoException;
import excepciones.ValorInvalidoException;
import jugadores.CentralJugadores;
import partidos.Partidos;
import usuarios.EstudianteNoSocio;
import usuarios.Fisioterapeuta;
import usuarios.Socio;
import usuarios.Usuario;
import premios.Premio;
import premios.PremioEfectivo;
import premios.BonoDescuento;

class CentralTorneosTest {

	private static final LocalDate DIA = LocalDate.of(2026, 12, 5);
	private CentralJugadores jugadores;
	private CentralTorneos torneos;

	@BeforeEach
	void setUp() {
		jugadores = new CentralJugadores();
		torneos = new CentralTorneos(jugadores, new Partidos(jugadores));
	}

	private Socio tenista(String username, String nivel, float elo) throws Exception {
		Socio socio = new Socio(username, "pw", username, "25");
		socio.agregarJugador(jugadores.inscribirJugadorIndividual(socio, "tenis", nivel, elo).getIdJugador());
		return socio;
	}

	private Torneo t1() throws Exception {
		return torneos.crearTorneo("competitivo", "T1", "tenis", "individual", "sencillos", "Intermedio", 10, 20000, DIA);
	}

	@Test
	void cuposPreferencialesSon20PorCientoHaciaArriba() throws Exception {
		assertEquals(2, t1().getCuposPreferenciales());
		assertEquals(3, torneos.crearTorneo("recreativo", "T2", "Fútbol", "equipo", "", "Sub-15", 11, 10, DIA).getCuposPreferenciales());
		assertEquals(1, torneos.crearTorneo("recreativo", "T3", "padel", "individual", "dobles", "Avanzado", 4, 15, DIA)
				.getCuposPreferenciales());
		Torneo t = torneos.getTorneo("T-1");
		assertEquals("Inscripciones abiertas", t.getEstado());
		assertEquals(8, t.getRegularesLibres());
	}

	@Test
	void validacionesAlCrear() {
		assertThrows(ValorInvalidoException.class,
				() -> torneos.crearTorneo("competitivo", "X", "tenis", "individual", "sencillos", "Intermedio", 0, 20000, DIA));
		assertThrows(ValorInvalidoException.class,
				() -> torneos.crearTorneo("competitivo", "X", "tenis", "individual", "sencillos", "Intermedio", 8, 0, DIA));
		assertThrows(ValorInvalidoException.class,
				() -> torneos.crearTorneo("recreativo", "X", "tenis", "individual", "sencillos", "Intermedio", 8, 5000, DIA));
		assertThrows(ValorInvalidoException.class,
				() -> torneos.crearTorneo("recreativo", "X", "tenis", "individual", "sencillos", "Intermedio", 8, 0, DIA));
		assertThrows(ValorInvalidoException.class,
				() -> torneos.crearTorneo("recreativo", "X", "Fútbol", "equipo", "", "Intermedio", 8, 10, DIA));
		assertThrows(ValorInvalidoException.class,
				() -> torneos.crearTorneo("recreativo", "X", "Fútbol", "equipo", "dobles", "Sub-15", 8, 10, DIA));
		assertThrows(ValorInvalidoException.class,
				() -> torneos.crearTorneo("recreativo", "X", "tenis", "individual", "triples", "Intermedio", 8, 10, DIA));
		assertThrows(ValorInvalidoException.class, () -> torneos.crearTorneo("recreativo", "", "tenis", "individual", "sencillos", "Intermedio", 8, 10, DIA));
	}

	@Test
	void elegiblesTomanPreferencialYLuegoLosDemasLosLibres() throws Exception {
		Torneo t = t1();
		Socio sofia = tenista("sofia", "Intermedio", 1100);
		Socio ana = tenista("ana", "Intermedio", 1050);
		Socio[] otros = new Socio[8];
		for (int i = 0; i < 8; i++) {
			otros[i] = tenista("s" + i, "Intermedio", 900 - i);
		}
		assertTrue(torneos.inscribirIndividual("T-1", ana, null).esPreferencial());
		for (int i = 0; i < 8; i++) {
			assertFalse(torneos.inscribirIndividual("T-1", otros[i], null).esPreferencial());
		}
		assertEquals(0, t.getRegularesLibres());
		assertEquals(1, t.getPreferencialesLibres());
		Socio extra = tenista("extra", "Intermedio", 800);
		assertTrue(torneos.inscribirIndividual("T-1", extra, null).esPreferencial(), "Sin regulares, el preferencial libre lo toma cualquiera");
		assertThrows(CuposAgotadosException.class, () -> torneos.inscribirIndividual("T-1", sofia, null));
		assertEquals(10, t.getInscripciones().size());
	}

	@Test
	void inscripcionIndividualValidaNivelDuplicadoYTarifa() throws Exception {
		t1();
		Socio pedro = tenista("pedro", "Intermedio", 1000);
		InscripcionTorneo inscripcion = torneos.inscribirIndividual("T-1", pedro, null);
		assertEquals(20000, inscripcion.getTarifaPagada());
		assertThrows(ElementoDuplicadoException.class, () -> torneos.inscribirIndividual("T-1", pedro, null));
		assertThrows(InscripcionTorneoException.class, () -> torneos.inscribirIndividual("T-1", tenista("p", "Principiante", 1000), null));
		assertThrows(InscripcionTorneoException.class, () -> torneos.inscribirIndividual("T-1", new Socio("sin", "pw", "Sin", "20"), null));
		Usuario esteban = new EstudianteNoSocio("esteban", "pw", "Esteban");
		assertThrows(ValorInvalidoException.class, () -> torneos.inscribirIndividual("T-1", esteban, ""));
		assertNotNull(torneos.inscribirIndividual("T-1", esteban, "Intermedio"));
		Fisioterapeuta laura = new Fisioterapeuta("laura", "pw", "C");
		assertEquals(0, torneos.inscribirIndividual("T-1", laura, "Intermedio").getTarifaPagada());
		assertEquals(40000, ((TorneoCompetitivo) torneos.getTorneo("T-1")).calcularRecaudo());
		assertThrows(ElementoNoEncontradoException.class, () -> torneos.getTorneo("T-999"));
	}

	@Test
	void ciclodeVidaPosicionesYPremioEnEfectivo() throws Exception {
		Torneo t = t1();
		Socio ana = tenista("ana", "Intermedio", 1050);
		Socio pedro = tenista("pedro", "Intermedio", 1000);
		String idAna = torneos.inscribirIndividual("T-1", ana, null).getParticipante().getIdParticipante();
		assertThrows(InscripcionTorneoException.class, () -> torneos.iniciarTorneo("T-1"));
		String idPedro = torneos.inscribirIndividual("T-1", pedro, null).getParticipante().getIdParticipante();
		assertThrows(EstadoTorneoException.class, () -> torneos.registrarPartido("T-1", idAna, idPedro, LocalDateTime.now(), 2, 0));
		torneos.iniciarTorneo("T-1");
		assertThrows(EstadoTorneoException.class, () -> torneos.iniciarTorneo("T-1"));
		assertThrows(EstadoTorneoException.class, () -> torneos.retirar("T-1", "ana"));
		assertThrows(EstadoTorneoException.class, () -> torneos.finalizarTorneo("T-1"));
		torneos.registrarPartido("T-1", idAna, idPedro, DIA.atTime(10, 0), 2, 0);
		assertThrows(InscripcionTorneoException.class, () -> torneos.registrarPartido("T-1", idAna, "P-999", DIA.atTime(10, 0), 2, 0));
		assertThrows(ValorInvalidoException.class, () -> torneos.registrarPosiciones("T-1", new String[] { idAna, idAna }));
		assertThrows(ValorInvalidoException.class, () -> torneos.registrarPosiciones("T-1", new String[] { idAna }));
		torneos.registrarPosiciones("T-1", new String[] { idAna, idPedro });
		List<Premio> premios = torneos.finalizarTorneo("T-1");
		assertEquals("Finalizado", t.getEstado());
		assertEquals(1, premios.size());
		PremioEfectivo premio = (PremioEfectivo) premios.get(0);
		assertEquals(28000, premio.getValor(), 0.01);
		assertTrue(t.getHistorial().contains("Premios"));
	}

	@Test
	void recreativoCreaBonoPorIntegranteYSoloLoGuardaSiPuede() throws Exception {
		Torneo t = torneos.crearTorneo("recreativo", "T4", "tenis", "individual", "sencillos", "Intermedio", 8, 10, DIA);
		Socio pedro = tenista("pedro", "Intermedio", 1000);
		Usuario esteban = new EstudianteNoSocio("esteban", "pw", "Esteban");
		String idPedro = torneos.inscribirIndividual(t.getIdTorneo(), pedro, null).getParticipante().getIdParticipante();
		String idEsteban = torneos.inscribirIndividual(t.getIdTorneo(), esteban, "Intermedio").getParticipante().getIdParticipante();
		torneos.iniciarTorneo(t.getIdTorneo());
		torneos.registrarPosiciones(t.getIdTorneo(), new String[] { idPedro, idEsteban });
		List<Premio> premios = torneos.finalizarTorneo(t.getIdTorneo());
		BonoDescuento bono = (BonoDescuento) premios.get(0);
		assertEquals(10, bono.getPorcentaje());
		assertEquals(List.of(bono), torneos.getBonos("pedro"));
		assertTrue(torneos.getBonos("esteban").isEmpty());
	}

	@Test
	void usarBonoValidaTitularYEstado() throws Exception {
		BonoDescuento bono = new BonoDescuento("B-001", "T-4", 1, "pedro", DIA, 10);
		torneos.agregarBono(bono);
		assertSame(bono, torneos.usarBono("B-001", "pedro"));
		assertThrows(BonoNoDisponibleException.class, () -> torneos.usarBono("B-001", "ana"));
		assertThrows(BonoNoDisponibleException.class, () -> torneos.usarBono("B-404", "pedro"));
		assertEquals(5000, bono.calcularDescuento(50000), 0.01);
		bono.usar();
		assertEquals("usado", bono.getEstado());
		assertThrows(BonoNoDisponibleException.class, bono::usar);
		assertThrows(BonoNoDisponibleException.class, () -> torneos.usarBono("B-001", "pedro"));
	}

	@Test
	void consultarPorDeporteYEstado() throws Exception {
		t1();
		torneos.crearTorneo("recreativo", "T2", "Fútbol", "equipo", "", "Sub-15", 11, 10, DIA);
		assertEquals(1, torneos.consultarTorneos("TENIS", "").size());
		assertEquals(2, torneos.consultarTorneos(null, "inscripciones abiertas").size());
		assertTrue(torneos.consultarTorneos("voleibol", null).isEmpty());
		assertThrows(ValorInvalidoException.class, () -> torneos.consultarTorneos(null, "Cancelado"));
	}

	@Test
	void retirarLiberaCupo() throws Exception {
		Torneo t = t1();
		torneos.inscribirIndividual("T-1", tenista("pedro", "Intermedio", 1000), null);
		assertEquals(9, t.getCuposDisponibles());
		torneos.retirar("T-1", "pedro");
		assertEquals(10, t.getCuposDisponibles());
		assertThrows(InscripcionTorneoException.class, () -> torneos.retirar("T-1", "pedro"));
	}
}
