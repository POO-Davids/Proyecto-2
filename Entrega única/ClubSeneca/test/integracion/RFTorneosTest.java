package integracion;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controladores.ControladorAdministrador;
import controladores.ControladorParticipante;
import controladores.ControladorSocio;
import controladores.ControladorUsuario;
import equipos.Equipo;
import excepciones.BonoNoDisponibleException;
import excepciones.CuposAgotadosException;
import excepciones.ElementoDuplicadoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.EstadoTorneoException;
import excepciones.InscripcionTorneoException;
import excepciones.PromocionNoAcumulableException;
import excepciones.ValorInvalidoException;
import partidos.PartidoInterno;
import tiendas.ArticuloTienda;
import tiendas.Bebida;
import tiendas.VentaCafeteria;
import tiendas.VentaTienda;
import premios.BonoDescuento;
import torneos.EquipoTorneo;
import torneos.InscripcionTorneo;
import torneos.ParejaTorneo;
import premios.Premio;
import premios.PremioEfectivo;
import torneos.Torneo;
import torneos.TorneoCompetitivo;
import torneos.TorneoRecreativo;
import usuarios.Entrenador;
import usuarios.EstudianteNoSocio;
import usuarios.Fisioterapeuta;
import usuarios.PersonaExterna;
import usuarios.Socio;

/** Casos de prueba de RF-TOR-01 a RF-TOR-16 (sección 4 del análisis), con los datos base de torneos. */
class RFTorneosTest {

	private static final LocalDate DIA_T1 = LocalDate.of(2026, 12, 5);
	private static final List<String> TENISTAS = List.of("sofia", "ana", "pedro", "luis", "camila", "juan", "valentina", "tomas");
	private static final String[] OCHO = TENISTAS.toArray(new String[0]);

	@TempDir
	Path dir;
	Club club;
	ControladorAdministrador adm;
	Equipo a, b, sub17;

	@BeforeEach
	void datosBase() throws Exception {
		club = new Club(dir);
		adm = club.adm;
		float[] elos = { 1100, 1050, 1000, 990, 980, 970, 960, 950 };
		for (int i = 0; i < TENISTAS.size(); i++) {
			club.socio(TENISTAS.get(i), "25");
			club.individual(TENISTAS.get(i), "tenis", "Intermedio", elos[i]);
		}
		club.individual("pedro", "tenis de mesa", "Principiante", 1000);
		club.central.registrarEstudianteNoSocio("Esteban", "esteban", Club.PW);
		club.central.registrarPersonaExterna("Marta", "marta", Club.PW);
		a = club.equipo("Fútbol Sub-15 A", "Fútbol", "Sub-15", 20);
		b = club.equipo("Fútbol Sub-15 B", "Fútbol", "Sub-15", 20);
		Equipo c = club.equipo("Fútbol Sub-15 C", "Fútbol", "Sub-15", 20);
		sub17 = club.equipo("Fútbol Sub-17 A", "Fútbol", "Sub-17", 20);
		club.llenar(a, "a", "14", 12);
		club.llenar(b, "b", "14", 10);
		club.llenar(c, "c", "14", 5);
		club.llenar(sub17, "d", "16", 9);
		interno("PI-1", a, b, 9, 2, 0);
		interno("PI-2", a, b, 10, 1, 0);
		interno("PI-3", a, b, 11, 3, 1);
		interno("PI-4", b, c, 11, 2, 1);
		club.usuario(new Entrenador("mario", Club.PW, "EMP-MARIO"));
		adm.asignarTurno("mario", DIA_T1, Club.horas(8, 11));
		club.usuario(new Fisioterapeuta("laura", Club.PW, "EMP-LAURA"));

		adm.crearTorneo("competitivo", "Tenis Intermedio Diciembre", "tenis", "individual", "sencillos", "Intermedio", 10, 20000, DIA_T1);
		adm.crearTorneo("recreativo", "Mini Fútbol Sub-15", "Fútbol", "equipo", "", "Sub-15", 11, 10, LocalDate.of(2026, 12, 12));
		adm.crearTorneo("recreativo", "Pádel Dobles Avanzado", "padel", "individual", "dobles", "Avanzado", 4, 15, LocalDate.of(2026, 12, 13));
		adm.crearTorneo("recreativo", "Tenis de mesa Principiante", "tenis de mesa", "individual", "sencillos", "Principiante", 8, 10,
				LocalDate.of(2026, 12, 6));
	}

	private void interno(String id, Equipo e1, Equipo e2, int mes, int p1, int p2) {
		PartidoInterno partido = new PartidoInterno(id, e1, e2, LocalDateTime.of(2026, mes, 3, 10, 0), new String[0], new String[0]);
		partido.registrarMarcador(p1, p2);
		partido.setCerrado();
		club.central.getPartidos().agregarPartido(partido);
	}

	private void padelistas() throws Exception {
		String[][] padel = { { "carlos", "Avanzado" }, { "diana", "Avanzado" }, { "felipe", "Avanzado" }, { "elena", "Intermedio" } };
		for (String[] p : padel) {
			club.socio(p[0], "30");
			club.individual(p[0], "padel", p[1], 1100);
		}
	}

	private void socioTenis(String username, String nivel, float elo) throws Exception {
		club.socio(username, "25");
		club.individual(username, "tenis", nivel, elo);
	}

	private Torneo t(String id) throws Exception {
		return club.central.getTorneos().getTorneo(id);
	}

	private ControladorParticipante part(String username) throws Exception {
		return club.login(username);
	}

	private void inscribir(String idTorneo, String nivel, String... usernames) throws Exception {
		for (String username : usernames) {
			part(username).inscribirseIndividual(idTorneo, nivel);
		}
	}

	private InscripcionTorneo inscripcionDe(String idTorneo, String username) throws Exception {
		for (InscripcionTorneo inscripcion : t(idTorneo).getInscripciones()) {
			if (inscripcion.getParticipante().contiene(username)) {
				return inscripcion;
			}
		}
		return null;
	}

	private String idDe(String idTorneo, String username) throws Exception {
		InscripcionTorneo inscripcion = inscripcionDe(idTorneo, username);
		return inscripcion == null ? "P-999" : inscripcion.getParticipante().getIdParticipante();
	}

	private String[] posiciones(String idTorneo, String... primeros) throws Exception {
		List<String> ids = new ArrayList<>();
		for (String username : primeros) {
			ids.add(idDe(idTorneo, username));
		}
		for (InscripcionTorneo inscripcion : t(idTorneo).getInscripciones()) {
			if (!ids.contains(inscripcion.getParticipante().getIdParticipante())) {
				ids.add(inscripcion.getParticipante().getIdParticipante());
			}
		}
		return ids.toArray(new String[0]);
	}

	private List<Premio> jugarYFinalizar(String idTorneo, String... primeros) throws Exception {
		adm.iniciarTorneo(idTorneo);
		adm.registrarPosicionesTorneo(idTorneo, posiciones(idTorneo, primeros));
		adm.finalizarTorneo(idTorneo);
		return t(idTorneo).getPremios();
	}

	private static String[] integrantes(String prefijo, int n) {
		String[] lista = new String[n];
		for (int i = 0; i < n; i++) {
			lista[i] = prefijo + (i + 1);
		}
		return lista;
	}

	private void t1ConSietePagantesYLaura() throws Exception {
		inscribir("T-1", null, "sofia", "ana", "pedro", "luis", "camila", "juan", "valentina");
		inscribir("T-1", "Intermedio", "laura");
	}

	private void t4ConPedroYEsteban() throws Exception {
		inscribir("T-4", null, "pedro");
		inscribir("T-4", "Principiante", "esteban");
	}

	private void t2ConAYB() throws Exception {
		adm.inscribirEquipo("T-2", a.getIdEquipo(), integrantes("a", 9));
		adm.inscribirEquipo("T-2", b.getIdEquipo(), integrantes("b", 7));
	}

	private static final LocalDate F = LocalDate.of(2026, 12, 20);

	private void t1EnCurso() throws Exception {
		inscribir("T-1", null, OCHO);
		adm.iniciarTorneo("T-1");
	}

	private Club clubSinRegistros() throws Exception {
		return new Club(dir.resolve("vacio"));
	}

	private void pedroEnT1ConSeisCupos() throws Exception {
		inscribir("T-1", null, "pedro", "ana", "luis", "camila");
		assertEquals(6, t("T-1").getCuposDisponibles());
	}

	private void anaYOchoRegulares() throws Exception {
		inscribir("T-1", null, "ana", "pedro", "luis", "camila", "juan", "valentina", "tomas");
		inscribir("T-1", "Intermedio", "esteban", "marta");
	}

	private String bonoDePedro() throws Exception {
		adm.agregarArticulo("Tienda", new ArticuloTienda("Raqueta", 50000, "Tenis"));
		adm.abastecer("Tienda", "Raqueta", 5);
		t4ConPedroYEsteban();
		jugarYFinalizar("T-4", "pedro");
		assertEquals(0, ((Socio) club.central.getUsuarios().getUsuario("pedro")).getPuntosFidelidad());
		return club.central.getTorneos().getBonos("pedro").get(0).getIdPremio();
	}

	private VentaTienda pedroCompraConBono() throws Exception {
		String idBono = bonoDePedro();
		ControladorSocio pedro = club.login("pedro");
		pedro.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 0, idBono);
		return club.central.getTienda().getVentas().get(0);
	}

	private String bonoDeA2() throws Exception {
		adm.agregarArticulo("Cafeteria", new Bebida("Limonada", 4000, false));
		adm.abastecer("Cafeteria", "Limonada", 5);
		t2ConAYB();
		jugarYFinalizar("T-2", "a1");
		return club.central.getTorneos().getBonos("a2").get(0).getIdPremio();
	}

	private ControladorParticipante tresEstados() throws Exception {
		inscribir("T-1", null, "ana", "pedro");
		adm.iniciarTorneo("T-1");
		adm.registrarPartidoTorneo("T-1", idDe("T-1", "ana"), idDe("T-1", "pedro"), DIA_T1.atTime(10, 0), 2, 0);
		adm.registrarPosicionesTorneo("T-1", posiciones("T-1", "ana"));
		adm.finalizarTorneo("T-1");
		t2ConAYB();
		adm.iniciarTorneo("T-2");
		adm.registrarPartidoTorneo("T-2", idDe("T-2", "a1"), idDe("T-2", "b1"), LocalDateTime.of(2026, 12, 12, 9, 0), 3, 1);
		return part("marta");
	}

	private void parejaNoSocios() throws Exception {
		part("esteban").inscribirPareja("T-3", "marta", "Avanzado");
	}

	private String bonoDeLaura() throws Exception {
		adm.agregarArticulo("Tienda", new ArticuloTienda("Raqueta", 50000, "Tenis"));
		adm.abastecer("Tienda", "Raqueta", 5);
		inscribir("T-4", "Principiante", "laura", "esteban");
		jugarYFinalizar("T-4", "laura");
		return club.central.getTorneos().getBonos("laura").get(0).getIdPremio();
	}

	@Nested
	@DisplayName("RF-TOR-01 Crear torneo")
	class RfTor01 {
		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 1: T1 recibe ID, estado \"Inscripciones abiertas\", 2 preferenciales y 8 regulares")
		void rfTor01_caso1_t1() throws Exception {
			Torneo t1 = t("T-1");
			assertInstanceOf(TorneoCompetitivo.class, t1);
			assertEquals("T-1", t1.getIdTorneo());
			assertEquals("Inscripciones abiertas", t1.getEstado());
			assertEquals(2, t1.getCuposPreferenciales());
			assertEquals(8, t1.getRegularesLibres());
			assertEquals(20000, ((TorneoCompetitivo) t1).getTarifa());
		}

		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 2: T2 tiene 3 preferenciales y 8 regulares, sin tarifa")
		void rfTor01_caso2_t2() throws Exception {
			Torneo t2 = t("T-2");
			assertInstanceOf(TorneoRecreativo.class, t2);
			assertEquals(3, t2.getCuposPreferenciales());
			assertEquals(8, t2.getRegularesLibres());
			assertEquals(0, t2.calcularTarifa(club.central.getUsuarios().getUsuario("pedro")));
		}

		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 3: Los cuatro torneos existen al mismo tiempo")
		void rfTor01_caso3_cuatroTorneos() throws Exception {
			assertEquals(List.of("T-1", "T-2", "T-3", "T-4"), club.central.getTorneos().getTorneos().stream().map(Torneo::getIdTorneo).toList());
		}

		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 4: Se rechaza cupos=0")
		void rfTor01_caso4_cuposCero() {
			assertThrows(ValorInvalidoException.class,
					() -> adm.crearTorneo("competitivo", "X", "tenis", "individual", "sencillos", "Intermedio", 0, 20000, F));
		}

		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 4: Se rechaza un recreativo con tarifa=5000")
		void rfTor01_caso4_recreativoConTarifa() {
			assertThrows(ValorInvalidoException.class,
					() -> adm.crearTorneo("recreativo", "X", "tenis", "individual", "sencillos", "Intermedio", 8, 5000, F));
		}

		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 4: Se rechaza un recreativo sin porcentaje de bono")
		void rfTor01_caso4_recreativoSinBono() {
			assertThrows(ValorInvalidoException.class,
					() -> adm.crearTorneo("recreativo", "X", "tenis", "individual", "sencillos", "Intermedio", 8, 0, F));
		}

		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 4: Se rechaza un competitivo sin tarifa")
		void rfTor01_caso4_competitivoSinTarifa() {
			assertThrows(ValorInvalidoException.class,
					() -> adm.crearTorneo("competitivo", "X", "tenis", "individual", "sencillos", "Intermedio", 8, 0, F));
		}

		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 4: Se rechaza un torneo de fútbol con nivel en lugar de categoría")
		void rfTor01_caso4_futbolConNivel() {
			assertThrows(ValorInvalidoException.class, () -> adm.crearTorneo("recreativo", "X", "Fútbol", "equipo", "", "Intermedio", 8, 10, F));
		}

		@Test
		@DisplayName("RF-TOR-01 / HU-ADM-09 – caso 4: Se rechaza un torneo de fútbol en modalidad dobles")
		void rfTor01_caso4_futbolDobles() {
			assertThrows(ValorInvalidoException.class, () -> adm.crearTorneo("recreativo", "X", "Fútbol", "equipo", "dobles", "Sub-15", 8, 10, F));
		}
	}

	@Nested
	@DisplayName("RF-TOR-02 Iniciar y finalizar torneos")
	class RfTor02 {
		@Test
		@DisplayName("RF-TOR-02 / HU-ADM-10 – caso 2: Al iniciar T1 queda \"En curso\" y nadie puede inscribirse ni retirarse")
		void rfTor02_caso2_iniciar() throws Exception {
			inscribir("T-1", null, OCHO);
			adm.iniciarTorneo("T-1");
			assertEquals("En curso", t("T-1").getEstado());
			assertThrows(EstadoTorneoException.class, () -> part("esteban").inscribirseIndividual("T-1", "Intermedio"));
			assertThrows(EstadoTorneoException.class, () -> part("pedro").retirarse("T-1"));
		}

		@Test
		@DisplayName("RF-TOR-02 / HU-ADM-10 – caso 3: Se rechaza iniciar T1 otra vez")
		void rfTor02_caso3_iniciarDosVeces() throws Exception {
			inscribir("T-1", null, OCHO);
			adm.iniciarTorneo("T-1");
			assertThrows(EstadoTorneoException.class, () -> adm.iniciarTorneo("T-1"));
		}

		@Test
		@DisplayName("RF-TOR-02 / HU-ADM-10 – caso 4: Se rechaza finalizar T1 sin posiciones registradas")
		void rfTor02_caso4_finalizarSinPosiciones() throws Exception {
			inscribir("T-1", null, OCHO);
			adm.iniciarTorneo("T-1");
			assertThrows(EstadoTorneoException.class, () -> adm.finalizarTorneo("T-1"));
			assertEquals("En curso", t("T-1").getEstado());
		}

		@Test
		@DisplayName("RF-TOR-02 / HU-ADM-10 – caso 5: Con las posiciones registradas, T1 queda \"Finalizado\" y se asignan premios")
		void rfTor02_caso5_finalizar() throws Exception {
			inscribir("T-1", null, OCHO);
			jugarYFinalizar("T-1", "ana", "sofia", "pedro");
			assertEquals("Finalizado", t("T-1").getEstado());
			assertFalse(t("T-1").getPremios().isEmpty());
		}

		@Test
		@DisplayName("RF-TOR-02 / HU-ADM-10 – caso 6: Se rechaza iniciar un torneo con 1 solo inscrito")
		void rfTor02_caso6_unSoloInscrito() throws Exception {
			inscribir("T-4", null, "pedro");
			assertThrows(InscripcionTorneoException.class, () -> adm.iniciarTorneo("T-4"));
		}

		@Test
		@DisplayName("RF-TOR-02 / HU-ADM-10 – caso 6: Se rechaza finalizar un torneo en \"Inscripciones abiertas\"")
		void rfTor02_caso6_finalizarAbierto() {
			assertThrows(EstadoTorneoException.class, () -> adm.finalizarTorneo("T-3"));
		}

		@Test
		@DisplayName("RF-TOR-02 / HU-ADM-10 – caso 6: Se rechaza operar sobre el ID T-999")
		void rfTor02_caso6_inexistente() {
			assertThrows(ElementoNoEncontradoException.class, () -> adm.iniciarTorneo("T-999"));
			assertThrows(ElementoNoEncontradoException.class, () -> adm.finalizarTorneo("T-999"));
		}
	}

	@Nested
	@DisplayName("RF-TOR-03 Registrar partidos y posiciones")
	class RfTor03 {
		@Test
		@DisplayName("RF-TOR-03 / HU-ADM-11 – caso 2: El partido Ana vs Pedro (2-0) queda en los partidos de T1")
		void rfTor03_caso2_partido() throws Exception {
			t1EnCurso();
			adm.registrarPartidoTorneo("T-1", idDe("T-1", "ana"), idDe("T-1", "pedro"), DIA_T1.atTime(10, 0), 2, 0);
			assertEquals(1, t("T-1").getPartidos().size());
			assertEquals(2, t("T-1").getPartidos().get(0).getMarcador1());
			assertEquals(0, t("T-1").getPartidos().get(0).getMarcador2());
			assertEquals(idDe("T-1", "ana"), t("T-1").getPartidos().get(0).getGanador().getIdParticipante());
		}

		@Test
		@DisplayName("RF-TOR-03 / HU-ADM-11 – casos 3 y 4: La tabla de posiciones queda completa y ordenada: 1.º Ana, 2.º Sofía, 3.º Pedro")
		void rfTor03_caso4_tabla() throws Exception {
			t1EnCurso();
			adm.registrarPosicionesTorneo("T-1", posiciones("T-1", "ana", "sofia", "pedro"));
			String tabla = t("T-1").getTablaPosiciones();
			assertEquals(8, tabla.lines().count());
			assertTrue(tabla.startsWith("1. ana\n2. sofia\n3. pedro\n"), tabla);
		}

		@Test
		@DisplayName("RF-TOR-03 / HU-ADM-11 – caso 5: Se rechaza un partido con un participante no inscrito")
		void rfTor03_caso5_noInscrito() throws Exception {
			t1EnCurso();
			assertThrows(InscripcionTorneoException.class,
					() -> adm.registrarPartidoTorneo("T-1", idDe("T-1", "ana"), "P-999", DIA_T1.atTime(12, 0), 2, 0));
		}

		@Test
		@DisplayName("RF-TOR-03 / HU-ADM-11 – caso 5: Se rechazan dos participantes con la misma posición")
		void rfTor03_caso5_mismaPosicion() throws Exception {
			t1EnCurso();
			String[] pos = posiciones("T-1", "ana", "sofia");
			pos[1] = pos[0];
			assertThrows(ValorInvalidoException.class, () -> adm.registrarPosicionesTorneo("T-1", pos));
		}

		@Test
		@DisplayName("RF-TOR-03 / HU-ADM-11 – caso 5: Se rechaza un partido en un torneo en \"Inscripciones abiertas\"")
		void rfTor03_caso5_torneoAbierto() throws Exception {
			t4ConPedroYEsteban();
			assertThrows(EstadoTorneoException.class,
					() -> adm.registrarPartidoTorneo("T-4", idDe("T-4", "pedro"), idDe("T-4", "esteban"), LocalDateTime.of(2026, 12, 6, 9, 0), 2, 0));
		}

		@Test
		@DisplayName("RF-TOR-03 / HU-ADM-11 – caso 5: Se rechaza un partido en un torneo \"Finalizado\"")
		void rfTor03_caso5_torneoFinalizado() throws Exception {
			inscribir("T-1", null, OCHO);
			jugarYFinalizar("T-1", "ana");
			assertThrows(EstadoTorneoException.class,
					() -> adm.registrarPartidoTorneo("T-1", idDe("T-1", "ana"), idDe("T-1", "luis"), DIA_T1.atTime(16, 0), 2, 0));
		}
	}

	@Nested
	@DisplayName("RF-TOR-04 Asignar premios")
	class RfTor04 {
		@Test
		@DisplayName("RF-TOR-04 / HU-ADM-11 – casos 1 y 2: Recaudo de 140000 y PremioEfectivo de 98000 para Ana asociado a T1")
		void rfTor04_caso2_premioEfectivo() throws Exception {
			t1ConSietePagantesYLaura();
			assertEquals(140000, ((TorneoCompetitivo) t("T-1")).calcularRecaudo());
			List<Premio> premios = jugarYFinalizar("T-1", "ana");
			PremioEfectivo premio = (PremioEfectivo) premios.get(0);
			assertEquals(98000, premio.getValor(), 0.01);
			assertEquals("T-1", premio.getTorneoOrigen());
			assertEquals("ana", premio.getBeneficiario());
		}

		@Test
		@DisplayName("RF-TOR-04 / HU-ADM-11, HU-EMP-04 – caso 3: Si gana Laura (empleada), no recibe PremioEfectivo")
		void rfTor04_caso3_empleadaSinPremio() throws Exception {
			t1ConSietePagantesYLaura();
			assertTrue(jugarYFinalizar("T-1", "laura").stream().noneMatch(p -> p instanceof PremioEfectivo));
		}

		@Test
		@DisplayName("RF-TOR-04 / HU-ADM-11 – caso 4: Pedro gana T4 y tiene un BonoDescuento de 10 % de T4, disponible")
		void rfTor04_caso4_bonoPedro() throws Exception {
			t4ConPedroYEsteban();
			jugarYFinalizar("T-4", "pedro");
			BonoDescuento bono = club.central.getTorneos().getBonos("pedro").get(0);
			assertEquals(10, bono.getPorcentaje());
			assertEquals("T-4", bono.getTorneoOrigen());
			assertEquals("disponible", bono.getEstado());
		}

		@Test
		@DisplayName("RF-TOR-04 / HU-ADM-11 – caso 5: \"Fútbol Sub-15 A\" gana T2 con 9 jugadores: 9 BonoDescuento, uno por integrante")
		void rfTor04_caso5_bonosEquipo() throws Exception {
			t2ConAYB();
			assertEquals(9, jugarYFinalizar("T-2", "a1").size());
			for (String integrante : integrantes("a", 9)) {
				assertEquals(1, club.central.getTorneos().getBonos(integrante).size(), integrante);
			}
		}

		@Test
		@DisplayName("RF-TOR-04 / HU-ADM-11, HU-PAR-01 – caso 6: Esteban gana T4: el premio queda en el historial y no se crea bono en su cuenta")
		void rfTor04_caso6_estudianteSinBono() throws Exception {
			t4ConPedroYEsteban();
			List<Premio> premios = jugarYFinalizar("T-4", "esteban");
			assertTrue(t("T-4").getHistorial().contains(premios.get(0).getDescripcion()));
			assertTrue(club.central.getTorneos().getBonos("esteban").isEmpty());
		}

		@Test
		@DisplayName("RF-TOR-04 / HU-ADM-11 – caso 7: El historial de T1, T2 y T4 guarda posiciones y premios")
		void rfTor04_caso7_historial() throws Exception {
			t1ConSietePagantesYLaura();
			jugarYFinalizar("T-1", "ana");
			t2ConAYB();
			jugarYFinalizar("T-2", "a1");
			t4ConPedroYEsteban();
			jugarYFinalizar("T-4", "pedro");
			for (String id : List.of("T-1", "T-2", "T-4")) {
				String historial = t(id).getHistorial();
				assertTrue(historial.contains("Posiciones:\n1. ") && historial.contains("Premios:\n- "), id);
			}
		}
	}

	@Nested
	@DisplayName("RF-TOR-05 Registrar estudiante no socio")
	class RfTor05 {
		@Test
		@DisplayName("RF-TOR-05 / HU-USR-02 – casos 1 y 2: Esteban queda como EstudianteNoSocio y al iniciar sesión solo ve opciones de torneos")
		void rfTor05_caso2_registro() throws Exception {
			Club otro = clubSinRegistros();
			otro.central.registrarEstudianteNoSocio("Esteban", "esteban", Club.PW);
			assertInstanceOf(EstudianteNoSocio.class, otro.central.getUsuarios().getUsuario("esteban"));
			assertEquals(ControladorParticipante.class, otro.central.iniciarSesion("esteban", Club.PW).getClass());
		}

		@Test
		@DisplayName("RF-TOR-05 / HU-USR-02 – caso 3: Se rechaza otro usuario con el login \"esteban\"")
		void rfTor05_caso3_loginRepetido() throws Exception {
			Club otro = clubSinRegistros();
			otro.central.registrarEstudianteNoSocio("Esteban", "esteban", Club.PW);
			assertThrows(ElementoDuplicadoException.class, () -> otro.central.registrarEstudianteNoSocio("Otro", "esteban", Club.PW));
		}

		@Test
		@DisplayName("RF-TOR-05 / HU-USR-02 – caso 3: Se rechaza un registro con el nombre vacío")
		void rfTor05_caso3_nombreVacio() throws Exception {
			Club otro = clubSinRegistros();
			assertThrows(ValorInvalidoException.class, () -> otro.central.registrarEstudianteNoSocio("", "esteban", Club.PW));
		}
	}

	@Nested
	@DisplayName("RF-TOR-06 Registrar persona externa")
	class RfTor06 {
		@Test
		@DisplayName("RF-TOR-06 / HU-USR-03 – casos 1 y 2: Marta queda como PersonaExterna y al iniciar sesión solo ve opciones de torneos")
		void rfTor06_caso2_registro() throws Exception {
			Club otro = clubSinRegistros();
			otro.central.registrarPersonaExterna("Marta", "marta", Club.PW);
			assertInstanceOf(PersonaExterna.class, otro.central.getUsuarios().getUsuario("marta"));
			assertEquals(ControladorParticipante.class, otro.central.iniciarSesion("marta", Club.PW).getClass());
		}

		@Test
		@DisplayName("RF-TOR-06 / HU-USR-03 – caso 3: Se rechaza otra persona con el login \"marta\"")
		void rfTor06_caso3_loginRepetido() throws Exception {
			Club otro = clubSinRegistros();
			otro.central.registrarPersonaExterna("Marta", "marta", Club.PW);
			assertThrows(ElementoDuplicadoException.class, () -> otro.central.registrarPersonaExterna("Marta", "marta", Club.PW));
		}
	}

	@Nested
	@DisplayName("RF-TOR-07 Inscripción en torneos individuales")
	class RfTor07 {
		@Test
		@DisplayName("RF-TOR-07 / HU-SOC-06 – caso 1: Pedro se inscribe en T1: cupos de 10 a 9 y tarifa de 20000")
		void rfTor07_caso1_pedro() throws Exception {
			assertEquals(10, t("T-1").getCuposDisponibles());
			part("pedro").inscribirseIndividual("T-1", null);
			assertEquals(9, t("T-1").getCuposDisponibles());
			assertEquals(20000, inscripcionDe("T-1", "pedro").getTarifaPagada());
			assertTrue(t("T-1").toString().contains("tarifa 20000"));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-PAR-01 – caso 2: Esteban y Marta se inscriben en T1 declarando nivel Intermedio")
		void rfTor07_caso2_estudianteYExterna() throws Exception {
			inscribir("T-1", "Intermedio", "esteban", "marta");
			assertTrue(t("T-1").estaInscrito("esteban"));
			assertTrue(t("T-1").estaInscrito("marta"));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-SOC-06 – caso 3: Se rechaza que Pedro se inscriba otra vez en T1")
		void rfTor07_caso3_duplicado() throws Exception {
			inscribir("T-1", null, "pedro");
			assertThrows(ElementoDuplicadoException.class, () -> part("pedro").inscribirseIndividual("T-1", null));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-SOC-06 – caso 3: Se rechaza a un socio con nivel Principiante en tenis")
		void rfTor07_caso3_principiante() throws Exception {
			socioTenis("paco", "Principiante", 1000);
			assertThrows(InscripcionTorneoException.class, () -> part("paco").inscribirseIndividual("T-1", null));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-SOC-06 – caso 3: Se rechaza a Pedro en T2 (torneo de equipos)")
		void rfTor07_caso3_torneoDeEquipos() {
			assertThrows(InscripcionTorneoException.class, () -> part("pedro").inscribirseIndividual("T-2", null));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-SOC-06 – caso 3: Se rechaza a Pedro en T3 sin pareja")
		void rfTor07_caso3_dobles() {
			assertThrows(InscripcionTorneoException.class, () -> part("pedro").inscribirseIndividual("T-3", null));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-SOC-06 – caso 3: Se rechaza inscribirse en T1 cuando ya está \"En curso\"")
		void rfTor07_caso3_enCurso() throws Exception {
			inscribir("T-1", null, "ana", "pedro");
			adm.iniciarTorneo("T-1");
			assertThrows(EstadoTorneoException.class, () -> part("luis").inscribirseIndividual("T-1", null));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-SOC-06 – caso 3: Se rechaza inscribirse en T1 con los 10 cupos ocupados")
		void rfTor07_caso3_lleno() throws Exception {
			inscribir("T-1", null, OCHO);
			inscribir("T-1", "Intermedio", "esteban", "marta");
			socioTenis("nuevo", "Intermedio", 900);
			assertThrows(CuposAgotadosException.class, () -> part("nuevo").inscribirseIndividual("T-1", null));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-PAR-01 – criterio de la HU: Dado un torneo de conjunto, cuando el participante intenta inscribirse, entonces se rechaza")
		void huPar01_torneoDeConjunto() {
			assertThrows(InscripcionTorneoException.class, () -> part("esteban").inscribirseIndividual("T-2", "Sub-15"));
		}

		@Test
		@DisplayName("RF-TOR-07 / HU-PAR-01 – criterio de la HU: Dado un nivel declarado distinto o una inscripción previa, cuando el participante intenta inscribirse, entonces se rechaza")
		void huPar01_nivelODuplicado() throws Exception {
			assertThrows(InscripcionTorneoException.class, () -> part("marta").inscribirseIndividual("T-1", "Avanzado"));
			inscribir("T-1", "Intermedio", "marta");
			assertThrows(ElementoDuplicadoException.class, () -> part("marta").inscribirseIndividual("T-1", "Intermedio"));
		}
	}

	@Nested
	@DisplayName("RF-TOR-08 Inscripción de parejas")
	class RfTor08 {
		@Test
		@DisplayName("RF-TOR-08 / HU-SOC-07 – caso 1: Una pareja de pádel Avanzado se inscribe en T3 y ocupa 1 de los 4 cupos")
		void rfTor08_caso1_pareja() throws Exception {
			padelistas();
			part("carlos").inscribirPareja("T-3", "diana", null);
			ParejaTorneo pareja = (ParejaTorneo) inscripcionDe("T-3", "carlos").getParticipante();
			assertEquals(List.of("carlos", "diana"), Arrays.asList(pareja.getUsernames()));
			assertEquals(3, t("T-3").getCuposDisponibles());
		}

		@Test
		@DisplayName("RF-TOR-08 / HU-SOC-07 – caso 2: Se rechaza una pareja con un solo jugador")
		void rfTor08_caso2_unSoloJugador() throws Exception {
			padelistas();
			assertThrows(InscripcionTorneoException.class, () -> part("felipe").inscribirPareja("T-3", "felipe", null));
		}

		@Test
		@DisplayName("RF-TOR-08 / HU-SOC-07 – caso 2: Se rechaza una pareja en T1 (sencillos)")
		void rfTor08_caso2_sencillos() {
			assertThrows(InscripcionTorneoException.class, () -> part("sofia").inscribirPareja("T-1", "ana", null));
		}

		@Test
		@DisplayName("RF-TOR-08 / HU-SOC-07 – caso 2: Se rechaza una pareja con un integrante ya inscrito en T3")
		void rfTor08_caso2_integranteInscrito() throws Exception {
			padelistas();
			part("carlos").inscribirPareja("T-3", "diana", null);
			assertThrows(ElementoDuplicadoException.class, () -> part("felipe").inscribirPareja("T-3", "carlos", null));
		}

		@Test
		@DisplayName("RF-TOR-08 / HU-SOC-07 – caso 2: Se rechaza una pareja con un integrante Intermedio en pádel")
		void rfTor08_caso2_nivel() throws Exception {
			padelistas();
			assertThrows(InscripcionTorneoException.class, () -> part("felipe").inscribirPareja("T-3", "elena", null));
		}

		@Test
		@DisplayName("RF-TOR-08 / HU-PAR-01 – criterio de la HU: Dado un torneo de dobles, cuando Esteban se inscribe con Marta declarando el nivel, entonces se registra la pareja ocupando un cupo")
		void huPar01_dobles() throws Exception {
			parejaNoSocios();
			ParejaTorneo pareja = (ParejaTorneo) inscripcionDe("T-3", "esteban").getParticipante();
			assertEquals(List.of("esteban", "marta"), Arrays.asList(pareja.getUsernames()));
			assertEquals("Avanzado", pareja.getNivel());
			assertEquals(3, t("T-3").getCuposDisponibles());
			assertFalse(inscripcionDe("T-3", "esteban").esPreferencial(), "Sin ranking no es elegible para cupo preferencial");
		}
	}

	@Nested
	@DisplayName("RF-TOR-09 Inscripción de equipos")
	class RfTor09 {
		@Test
		@DisplayName("RF-TOR-09 / HU-ADM-12 – caso 1: \"Fútbol Sub-15 A\" se inscribe en T2 con 9 jugadores; cupos de 11 a 10")
		void rfTor09_caso1_equipo() throws Exception {
			adm.inscribirEquipo("T-2", a.getIdEquipo(), integrantes("a", 9));
			EquipoTorneo equipo = (EquipoTorneo) inscripcionDe("T-2", "a1").getParticipante();
			assertSame(a, equipo.getEquipoClub());
			assertEquals(9, equipo.getNumeroIntegrantes());
			assertEquals(10, t("T-2").getCuposDisponibles());
		}

		@Test
		@DisplayName("RF-TOR-09 / HU-ADM-12 – caso 2: Se rechaza \"Fútbol Sub-17 A\" en T2 (categoría distinta)")
		void rfTor09_caso2_categoria() {
			assertThrows(InscripcionTorneoException.class, () -> adm.inscribirEquipo("T-2", sub17.getIdEquipo(), integrantes("d", 9)));
		}

		@Test
		@DisplayName("RF-TOR-09 / HU-ADM-12 – caso 2: Se rechaza un equipo que no existe en el club")
		void rfTor09_caso2_inexistente() {
			assertThrows(ElementoNoEncontradoException.class, () -> adm.inscribirEquipo("T-2", "EQ-999", integrantes("b", 9)));
		}

		@Test
		@DisplayName("RF-TOR-09 / HU-ADM-12 – caso 2: Se rechaza \"Fútbol Sub-15 B\" con Esteban como integrante")
		void rfTor09_caso2_integranteAjeno() {
			String[] lista = integrantes("b", 8);
			lista[7] = "esteban";
			assertThrows(InscripcionTorneoException.class, () -> adm.inscribirEquipo("T-2", b.getIdEquipo(), lista));
		}

		@Test
		@DisplayName("RF-TOR-09 / HU-ADM-12 – caso 2: Se rechaza inscribir otra vez \"Fútbol Sub-15 A\" en T2")
		void rfTor09_caso2_duplicado() throws Exception {
			adm.inscribirEquipo("T-2", a.getIdEquipo(), integrantes("a", 9));
			assertThrows(ElementoDuplicadoException.class, () -> adm.inscribirEquipo("T-2", a.getIdEquipo(), integrantes("a", 9)));
		}

		@Test
		@DisplayName("RF-TOR-09 / HU-ADM-12 – caso 2: Se rechaza inscribir un equipo en T1 (torneo individual)")
		void rfTor09_caso2_torneoIndividual() {
			assertThrows(InscripcionTorneoException.class, () -> adm.inscribirEquipo("T-1", b.getIdEquipo(), integrantes("b", 9)));
		}

		@Test
		@DisplayName("RF-TOR-09 / HU-ADM-12 – caso 2: Se rechaza \"Fútbol Sub-15 B\" con solo 5 integrantes")
		void rfTor09_caso2_minimo() {
			assertThrows(InscripcionTorneoException.class, () -> adm.inscribirEquipo("T-2", b.getIdEquipo(), integrantes("b", 5)));
		}

		@Test
		@DisplayName("RF-TOR-09 / HU-ADM-12 – caso 2: Un participante que no es administrador no tiene la opción de inscribir equipos")
		void rfTor09_caso2_noAdministrador() {
			assertTrue(Arrays.stream(ControladorSocio.class.getMethods()).noneMatch(m -> m.getName().equals("inscribirEquipo")));
			assertTrue(Arrays.stream(ControladorParticipante.class.getMethods()).noneMatch(m -> m.getName().equals("inscribirEquipo")));
		}
	}

	@Nested
	@DisplayName("RF-TOR-10 Retirarse de un torneo")
	class RfTor10 {
		@Test
		@DisplayName("RF-TOR-10 / HU-USR-05 – caso 2: Pedro se retira: su inscripción se cancela y los cupos pasan a 7")
		void rfTor10_caso2_retiro() throws Exception {
			pedroEnT1ConSeisCupos();
			part("pedro").retirarse("T-1");
			assertFalse(t("T-1").estaInscrito("pedro"));
			assertEquals(7, t("T-1").getCuposDisponibles());
		}

		@Test
		@DisplayName("RF-TOR-10 / HU-USR-05 – caso 3: Se rechaza retirar a alguien que no está inscrito")
		void rfTor10_caso3_noInscrito() throws Exception {
			pedroEnT1ConSeisCupos();
			assertThrows(InscripcionTorneoException.class, () -> part("tomas").retirarse("T-1"));
		}

		@Test
		@DisplayName("RF-TOR-10 / HU-USR-05 – caso 3: Se rechaza retirarse cuando T1 está \"En curso\"")
		void rfTor10_caso3_enCurso() throws Exception {
			pedroEnT1ConSeisCupos();
			adm.iniciarTorneo("T-1");
			assertThrows(EstadoTorneoException.class, () -> part("pedro").retirarse("T-1"));
		}

		@Test
		@DisplayName("RF-TOR-10 / HU-USR-05 – criterio de la HU: Dado que Marta está inscrita en pareja con Esteban, cuando se retira antes del inicio, entonces se cancela la inscripción y se libera el cupo")
		void huUsr05_retiroDeParejaNoSocia() throws Exception {
			parejaNoSocios();
			part("marta").retirarse("T-3");
			assertFalse(t("T-3").estaInscrito("marta"));
			assertFalse(t("T-3").estaInscrito("esteban"));
			assertEquals(4, t("T-3").getCuposDisponibles());
		}

		@Test
		@DisplayName("RF-TOR-10 / HU-USR-05 – criterio de la HU: Dado que el torneo está \"En curso\", cuando Esteban solicita retirarse, entonces se rechaza")
		void huUsr05_noSocioEnCurso() throws Exception {
			inscribir("T-1", "Intermedio", "esteban", "marta");
			adm.iniciarTorneo("T-1");
			assertThrows(EstadoTorneoException.class, () -> part("esteban").retirarse("T-1"));
		}
	}

	@Nested
	@DisplayName("RF-TOR-11 Cupos preferenciales")
	class RfTor11 {
		@Test
		@DisplayName("RF-TOR-11 / HU-ADM-09, HU-ADM-12, HU-SOC-06 – caso 1: T1 tiene 2 preferenciales, T2 tiene 3 y T3 tiene 1")
		void rfTor11_caso1_cantidad() throws Exception {
			assertEquals(2, t("T-1").getCuposPreferenciales());
			assertEquals(3, t("T-2").getCuposPreferenciales());
			assertEquals(1, t("T-3").getCuposPreferenciales());
		}

		@Test
		@DisplayName("RF-TOR-11 / HU-SOC-06, HU-PAR-01 – caso 2: Son elegibles Sofía (1100) y Ana (1050); Esteban y Marta no")
		void rfTor11_caso2_elegibles() throws Exception {
			inscribir("T-1", "Intermedio", "esteban", "marta");
			inscribir("T-1", null, "pedro");
			assertFalse(inscripcionDe("T-1", "esteban").esPreferencial());
			assertFalse(inscripcionDe("T-1", "marta").esPreferencial());
			assertFalse(inscripcionDe("T-1", "pedro").esPreferencial());
			inscribir("T-1", null, "sofia", "ana");
			assertTrue(inscripcionDe("T-1", "sofia").esPreferencial());
			assertTrue(inscripcionDe("T-1", "ana").esPreferencial());
		}

		@Test
		@DisplayName("RF-TOR-11 / HU-SOC-06, HU-ADM-12 – caso 3: Ana ocupa un cupo preferencial (1/2)")
		void rfTor11_caso3_ana() throws Exception {
			inscribir("T-1", null, "ana");
			assertTrue(inscripcionDe("T-1", "ana").esPreferencial());
			assertEquals(1, t("T-1").getPreferencialesLibres());
		}

		@Test
		@DisplayName("RF-TOR-11 / HU-SOC-06, HU-ADM-12 – caso 4: Pedro, Luis, Camila, Juan, Valentina, Tomás, Esteban y Marta ocupan los 8 regulares")
		void rfTor11_caso4_regulares() throws Exception {
			anaYOchoRegulares();
			for (String username : List.of("pedro", "luis", "camila", "juan", "valentina", "tomas", "esteban", "marta")) {
				assertFalse(inscripcionDe("T-1", username).esPreferencial(), username);
			}
			assertEquals(0, t("T-1").getRegularesLibres());
		}

		@Test
		@DisplayName("RF-TOR-11 / HU-SOC-06, HU-ADM-12 – caso 5: Sofía ocupa el segundo preferencial (2/2)")
		void rfTor11_caso5_sofia() throws Exception {
			anaYOchoRegulares();
			inscribir("T-1", null, "sofia");
			assertTrue(inscripcionDe("T-1", "sofia").esPreferencial());
			assertEquals(0, t("T-1").getPreferencialesLibres());
		}

		@Test
		@DisplayName("RF-TOR-11 / HU-SOC-06, HU-ADM-12 – casos 6 y 7: Un nuevo socio es rechazado porque T1 está lleno; T1 nunca supera 10 cupos")
		void rfTor11_caso6_lleno() throws Exception {
			anaYOchoRegulares();
			inscribir("T-1", null, "sofia");
			socioTenis("nuevo", "Intermedio", 900);
			assertThrows(CuposAgotadosException.class, () -> part("nuevo").inscribirseIndividual("T-1", null));
			assertEquals(10, t("T-1").getInscripciones().size());
		}

		@Test
		@DisplayName("RF-TOR-11 / HU-ADM-12 – caso 8: En T2, \"Fútbol Sub-15 A\" (9) y \"Fútbol Sub-15 B\" (3) ocupan cupos preferenciales")
		void rfTor11_caso8_equipos() throws Exception {
			adm.inscribirEquipo("T-2", a.getIdEquipo(), integrantes("a", 9));
			adm.inscribirEquipo("T-2", b.getIdEquipo(), integrantes("b", 9));
			assertTrue(inscripcionDe("T-2", "a1").esPreferencial());
			assertTrue(inscripcionDe("T-2", "b1").esPreferencial());
		}
	}

	@Nested
	@DisplayName("RF-TOR-12 Liberación de cupos preferenciales")
	class RfTor12 {
		@Test
		@DisplayName("RF-TOR-12 / HU-SOC-06, HU-PAR-01, HU-EMP-04, HU-ADM-12 – casos 1 y 2: Con los regulares ocupados, un socio no elegible toma el preferencial libre")
		void rfTor12_caso2_tomaPreferencial() throws Exception {
			anaYOchoRegulares();
			assertEquals(1, t("T-1").getPreferencialesLibres());
			socioTenis("nuevo", "Intermedio", 900);
			inscribir("T-1", null, "nuevo");
			assertTrue(inscripcionDe("T-1", "nuevo").esPreferencial());
		}

		@Test
		@DisplayName("RF-TOR-12 / HU-SOC-06, HU-ADM-12 – caso 3: T1 sigue con 10 cupos totales y 2 preferenciales")
		void rfTor12_caso3_totales() throws Exception {
			anaYOchoRegulares();
			socioTenis("nuevo", "Intermedio", 900);
			inscribir("T-1", null, "nuevo");
			assertEquals(10, t("T-1").getCupos());
			assertEquals(2, t("T-1").getCuposPreferenciales());
		}

		@Test
		@DisplayName("RF-TOR-12 / HU-ADM-10, HU-SOC-06 – caso 4: Al iniciar T1, los cupos no cambian")
		void rfTor12_caso4_iniciar() throws Exception {
			anaYOchoRegulares();
			socioTenis("nuevo", "Intermedio", 900);
			inscribir("T-1", null, "nuevo");
			adm.iniciarTorneo("T-1");
			assertEquals(10, t("T-1").getCupos());
			assertEquals(2, t("T-1").getCuposPreferenciales());
			assertEquals(0, t("T-1").getCuposDisponibles());
		}
	}

	@Nested
	@DisplayName("RF-TOR-13 Inscripción de empleados")
	class RfTor13 {
		@Test
		@DisplayName("RF-TOR-13 / HU-EMP-04 – caso 1: Laura se inscribe en T1 con tarifa=0")
		void rfTor13_caso1_laura() throws Exception {
			part("laura").inscribirseIndividual("T-1", "Intermedio");
			assertEquals(0, inscripcionDe("T-1", "laura").getTarifaPagada());
		}

		@Test
		@DisplayName("RF-TOR-13 / HU-EMP-04 – caso 2: Mario (con turno el 2026-12-05) es rechazado en T1")
		void rfTor13_caso2_mario() {
			assertThrows(InscripcionTorneoException.class, () -> part("mario").inscribirseIndividual("T-1", "Intermedio"));
		}

		@Test
		@DisplayName("RF-TOR-13 / HU-EMP-04 – caso 3: Laura es rechazada en T2 (torneo de equipos)")
		void rfTor13_caso3_lauraEquipos() {
			assertThrows(InscripcionTorneoException.class, () -> part("laura").inscribirseIndividual("T-2", "Intermedio"));
		}
	}

	@Nested
	@DisplayName("RF-TOR-14 Guardar y consultar bonos")
	class RfTor14 {
		@Test
		@DisplayName("RF-TOR-14 / HU-SOC-08, HU-ADM-11 – caso 1: Pedro consulta sus bonos: id, torneo T4, porcentaje 10 y estado disponible")
		void rfTor14_caso1_consulta() throws Exception {
			t4ConPedroYEsteban();
			jugarYFinalizar("T-4", "pedro");
			String idBono = club.central.getTorneos().getBonos("pedro").get(0).getIdPremio();
			String bonos = ((ControladorUsuario) club.login("pedro")).consultarBonos();
			assertEquals(idBono + " | torneo T-4 | 10% | disponible", bonos);
		}

		@Test
		@DisplayName("RF-TOR-14 / HU-SOC-08, HU-ADM-11 – caso 2: No hay bono de T4 antes de que finalice")
		void rfTor14_caso2_antesDeFinalizar() throws Exception {
			t4ConPedroYEsteban();
			adm.iniciarTorneo("T-4");
			assertTrue(club.central.getTorneos().getBonos("pedro").isEmpty());
			assertEquals("No tiene bonos", ((ControladorUsuario) club.login("pedro")).consultarBonos());
		}

		@Test
		@DisplayName("RF-TOR-14 / HU-SOC-08, HU-ADM-11 – caso 2: Un socio que no fue ganador no recibe bono de T4")
		void rfTor14_caso2_noGanador() throws Exception {
			t4ConPedroYEsteban();
			jugarYFinalizar("T-4", "pedro");
			assertEquals("No tiene bonos", ((ControladorUsuario) club.login("luis")).consultarBonos());
		}

		@Test
		@DisplayName("RF-TOR-14 / HU-EMP-05 – criterio de la HU: Dado que Laura ganó T4 y ya se finalizó, cuando consulta sus bonos, entonces ve el bono de T4 con 10 % y estado disponible")
		void huEmp05_consultarBono() throws Exception {
			String idBono = bonoDeLaura();
			String bonos = ((ControladorUsuario) club.login("laura")).consultarBonos();
			assertEquals(idBono + " | torneo T-4 | 10% | disponible", bonos);
		}
	}

	@Nested
	@DisplayName("RF-TOR-15 Usar bono de descuento")
	class RfTor15 {
		@Test
		@DisplayName("RF-TOR-15 / HU-SOC-09 – caso 2: Descuento 5000, subtotal 45000, IVA 8550 y total 53550")
		void rfTor15_caso2_valores() throws Exception {
			VentaTienda venta = pedroCompraConBono();
			assertEquals(5000, venta.getDescuento());
			assertEquals(45000, venta.calcularSubtotal() - venta.getDescuento());
			assertEquals(8550, venta.getIva());
			assertEquals(53550, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-SOC-09 – caso 2: Pedro gana 900 puntos")
		void rfTor15_caso2_puntos() throws Exception {
			pedroCompraConBono();
			assertEquals(900, ((Socio) club.central.getUsuarios().getUsuario("pedro")).getPuntosFidelidad(), 0.01);
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-SOC-09 – caso 2: El bono queda con estado \"usado\"")
		void rfTor15_caso2_usado() throws Exception {
			pedroCompraConBono();
			assertEquals("usado", club.central.getTorneos().getBonos("pedro").get(0).getEstado());
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-SOC-09, HU-SOC-04 – caso 3: Bono de T2 en la cafetería: descuento 400, subtotal 3600, impuesto 288, propina 360, total 4248")
		void rfTor15_caso3_cafeteria() throws Exception {
			String idBono = bonoDeA2();
			ControladorSocio a2 = club.login("a2");
			assertEquals("Subtotal: 3600 | Impuesto al consumo: 288 | Propina sugerida: 360 | Total: 4248",
					a2.cotizarCompraCafeteria(new String[] { "Limonada" }, new int[] { 1 }, idBono));
			a2.comprar("Cafeteria", new String[] { "Limonada" }, new int[] { 1 }, null, 360, 0, idBono);
			VentaCafeteria venta = club.central.getCafeteria().getVentas().get(0);
			assertEquals(400, venta.getDescuento());
			assertEquals(288, venta.getIva());
			assertEquals(360, venta.getPropina());
			assertEquals(4248, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-SOC-09 – caso 4: Se rechaza usar el bono otra vez")
		void rfTor15_caso4_otraVez() throws Exception {
			String idBono = bonoDePedro();
			ControladorSocio pedro = club.login("pedro");
			pedro.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 0, idBono);
			assertThrows(BonoNoDisponibleException.class,
					() -> pedro.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 0, idBono));
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-SOC-09 – caso 4: Se rechaza usar el bono de otro socio")
		void rfTor15_caso4_bonoAjeno() throws Exception {
			String idBono = bonoDePedro();
			ControladorSocio luis = club.login("luis");
			assertThrows(BonoNoDisponibleException.class,
					() -> luis.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 0, idBono));
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-SOC-09 – caso 4: Se rechaza usar un bono junto con el código de un empleado")
		void rfTor15_caso4_conCodigo() throws Exception {
			String idBono = bonoDePedro();
			ControladorSocio pedro = club.login("pedro");
			assertThrows(PromocionNoAcumulableException.class,
					() -> pedro.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, "EMP-MARIO", 0, 0, idBono));
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-SOC-09 – caso 4: Se rechaza usar un bono junto con puntos de fidelidad")
		void rfTor15_caso4_conPuntos() throws Exception {
			String idBono = bonoDePedro();
			((Socio) club.central.getUsuarios().getUsuario("pedro")).setPuntosFidelidad(500);
			ControladorSocio pedro = club.login("pedro");
			assertThrows(PromocionNoAcumulableException.class,
					() -> pedro.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 100, idBono));
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-EMP-05 – criterio de la HU: Dado un bono disponible y una compra sin otro descuento, cuando Laura lo aplica, entonces se descuenta antes de impuestos y queda usado")
		void huEmp05_usarBono() throws Exception {
			String idBono = bonoDeLaura();
			((ControladorUsuario) club.login("laura")).comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 0, idBono);
			VentaTienda venta = club.central.getTienda().getVentas().get(0);
			assertEquals(5000, venta.getDescuento());
			assertEquals(8550, venta.getIva());
			assertEquals(53550, venta.calcularTotal());
			assertEquals("usado", club.central.getTorneos().getBonos("laura").get(0).getEstado());
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-EMP-05 – criterio de la HU: Dado que la compra ya tiene el código de Laura, cuando intenta usar también el bono, entonces se rechaza la acumulación")
		void huEmp05_bonoConCodigo() throws Exception {
			String idBono = bonoDeLaura();
			ControladorUsuario laura = club.login("laura");
			assertThrows(PromocionNoAcumulableException.class,
					() -> laura.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, "EMP-LAURA", 0, 0, idBono));
		}

		@Test
		@DisplayName("RF-TOR-15 / HU-EMP-05 – criterio de la HU: Dado que el bono no es de Laura o ya fue usado, cuando intenta usarlo, entonces se rechaza")
		void huEmp05_bonoAjenoOUsado() throws Exception {
			String idBono = bonoDeLaura();
			ControladorUsuario mario = club.login("mario");
			assertThrows(BonoNoDisponibleException.class,
					() -> mario.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 0, idBono));
			ControladorUsuario laura = club.login("laura");
			laura.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 0, idBono);
			assertThrows(BonoNoDisponibleException.class,
					() -> laura.comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, null, 0, 0, idBono));
		}
	}

	@Nested
	@DisplayName("RF-TOR-16 Consultar estado e historial de torneos")
	class RfTor16 {
		@Test
		@DisplayName("RF-TOR-16 / HU-USR-04 – caso 2: Marta filtra por estado \"En curso\" y ve solo T2; por deporte \"tenis\" y ve solo T1")
		void rfTor16_caso2_filtros() throws Exception {
			ControladorParticipante marta = tresEstados();
			String enCurso = marta.consultarTorneos(null, "En curso");
			assertTrue(enCurso.startsWith("T-2 ") && !enCurso.contains("\n"), enCurso);
			String tenis = marta.consultarTorneos("tenis", null);
			assertTrue(tenis.startsWith("T-1 ") && !tenis.contains("\n"), tenis);
		}

		@Test
		@DisplayName("RF-TOR-16 / HU-USR-04 – caso 3: El detalle de T1 muestra participantes, partidos, posiciones y premios")
		void rfTor16_caso3_detalleT1() throws Exception {
			String detalle = tresEstados().consultarDetalleTorneo("T-1");
			for (String seccion : List.of("Participantes:\n- ", "Partidos:\n- ", "Posiciones:\n1. ana", "Premios:\n- ")) {
				assertTrue(detalle.contains(seccion), seccion);
			}
		}

		@Test
		@DisplayName("RF-TOR-16 / HU-USR-04 – caso 4: El detalle de T2 muestra los partidos jugados hasta la fecha")
		void rfTor16_caso4_detalleT2() throws Exception {
			String detalle = tresEstados().consultarDetalleTorneo("T-2");
			assertTrue(detalle.contains("Partidos:\n- ") && detalle.contains("Fútbol Sub-15 A") && detalle.contains("Fútbol Sub-15 B"));
		}

		@Test
		@DisplayName("RF-TOR-16 / HU-USR-04 – caso 5: Filtrar por \"voleibol\" muestra \"No hay torneos que coincidan\"")
		void rfTor16_caso5_sinCoincidencias() throws Exception {
			assertEquals("No hay torneos que coincidan", tresEstados().consultarTorneos("voleibol", null));
		}

		@Test
		@DisplayName("RF-TOR-16 / HU-USR-04 – caso 6: Se rechaza consultar el ID T-999")
		void rfTor16_caso6_inexistente() throws Exception {
			ControladorParticipante marta = tresEstados();
			assertThrows(ElementoNoEncontradoException.class, () -> marta.consultarDetalleTorneo("T-999"));
		}

		@Test
		@DisplayName("RF-TOR-16 / HU-USR-04 – caso 6: Se rechaza consultar el estado \"Cancelado\"")
		void rfTor16_caso6_estadoInvalido() throws Exception {
			ControladorParticipante marta = tresEstados();
			assertThrows(ValorInvalidoException.class, () -> marta.consultarTorneos(null, "Cancelado"));
		}

		@Test
		@DisplayName("RF-TOR-16 / HU-USR-04 – caso 7: La consulta no modifica ningún dato")
		void rfTor16_caso7_noModifica() throws Exception {
			ControladorParticipante marta = tresEstados();
			String antes = t("T-1").getHistorial() + t("T-2").getHistorial() + t("T-3").getHistorial() + t("T-4").getHistorial();
			marta.consultarTorneos(null, "En curso");
			marta.consultarTorneos("tenis", null);
			marta.consultarTorneos("voleibol", null);
			marta.consultarDetalleTorneo("T-1");
			marta.consultarDetalleTorneo("T-2");
			assertEquals(antes, t("T-1").getHistorial() + t("T-2").getHistorial() + t("T-3").getHistorial() + t("T-4").getHistorial());
			assertEquals(4, club.central.getTorneos().getTorneos().size());
		}
	}
}
