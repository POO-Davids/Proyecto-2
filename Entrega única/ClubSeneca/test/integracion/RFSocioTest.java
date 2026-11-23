package integracion;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controladores.ControladorAdministrador;
import controladores.ControladorEmpleado;
import controladores.ControladorEntrenador;
import controladores.ControladorParticipante;
import controladores.ControladorRol;
import controladores.ControladorSocio;
import estadisticas.EstadisticaTenisMesa;
import estadisticas.EstadisticaTenisPadel;
import excepciones.BebidaCalienteException;
import excepciones.CapacidadInstalacionException;
import excepciones.CredencialesInvalidasException;
import excepciones.InstalacionNoDisponibleException;
import excepciones.InventarioInsuficienteException;
import excepciones.MaximoSimultaneasException;
import excepciones.NivelIncompatibleException;
import excepciones.PuntosInsuficientesException;
import excepciones.ValorInvalidoException;
import instalaciones.Instalacion;
import jugadores.JugadorIndividual;
import partidos.Partido;
import partidos.PartidoPractica;
import reservas.Reserva;
import tiendas.ArticuloTienda;
import tiendas.Bebida;
import tiendas.Snack;
import tiendas.VentaCafeteria;
import tiendas.VentaTienda;
import usuarios.Entrenador;
import usuarios.EstudianteNoSocio;
import usuarios.Fisioterapeuta;
import usuarios.PersonaExterna;
import usuarios.Socio;

/** Casos de prueba de RF-S01 a RF-S06 y RF-T (sección 4 del análisis). */
class RFSocioTest {

	private static final LocalDate SABADO = LocalDate.of(2026, 10, 17);

	@TempDir
	Path dir;
	Club club;

	@BeforeEach
	void setUp() throws Exception {
		club = new Club(dir);
	}

	private String jugador(String username, String disciplina, String nivel, float elo) throws Exception {
		if (club.central.getUsuarios().getUsuario(username) == null) {
			club.socio(username, "25");
		}
		return club.individual(username, disciplina, nivel, elo);
	}

	private List<PartidoPractica> practicas() {
		return club.central.getPartidos().getPartidos().stream().filter(p -> p instanceof PartidoPractica).map(p -> (PartidoPractica) p)
				.toList();
	}

	private String[] dobles;
	private ControladorSocio ana;

	private void anaReservaDobles() throws Exception {
		prepararDobles();
		ana.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "dobles", new String[] { dobles[0], dobles[1] },
				new String[] { dobles[2], dobles[3] });
	}

	private void prepararDobles() throws Exception {
		club.adm.agregarInstalacion("tenis", 4, false);
		dobles = new String[] { jugador("ana", "tenis", "Intermedio", 1000), jugador("bea", "tenis", "Intermedio", 1000),
				jugador("carla", "tenis", "Intermedio", 1000), jugador("dora", "tenis", "Intermedio", 1000) };
		ana = club.login("ana");
	}

	private Socio socioCompraPelotas() throws Exception {
		Socio socio = club.socio("ana", "20");
		club.adm.agregarArticulo("Tienda", new ArticuloTienda("Pelotas Tenis", 20000, "Tenis"));
		club.adm.abastecer("Tienda", "Pelotas Tenis", 6);
		((ControladorSocio) club.login("ana")).comprar("Tienda", new String[] { "Pelotas Tenis" }, new int[] { 2 }, null, 0, 0);
		return socio;
	}

	private ControladorSocio socioRedime() throws Exception {
		club.socio("ana", "20").setPuntosFidelidad(500);
		club.adm.agregarArticulo("Tienda", new ArticuloTienda("Gorra", 30000, "Ropa"));
		club.adm.abastecer("Tienda", "Gorra", 3);
		ControladorSocio socio = club.login("ana");
		socio.comprar("Tienda", new String[] { "Gorra" }, new int[] { 1 }, null, 0, 300);
		return socio;
	}

	private String anaTenis, sofiaTenis, anaPadel;
	private Partido partidoTenis;

	private void partidoAnaSofia() throws Exception {
		club.adm.agregarInstalacion("tenis", 4, false);
		anaTenis = jugador("ana", "tenis", "Intermedio", 1000);
		sofiaTenis = jugador("sofia", "tenis", "Intermedio", 1000);
		anaPadel = club.individual("ana", "padel", "Intermedio", 1100);
		ControladorSocio socio = club.login("ana");
		socio.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "sencillos", new String[] { anaTenis }, new String[] { sofiaTenis });
		partidoTenis = practicas().get(0);
		socio.registrarResultadoPartidoPractica(partidoTenis.getIdPartido(),
				List.of(new EstadisticaTenisPadel(anaTenis, 2, 12, 3, 5), new EstadisticaTenisPadel(sofiaTenis, 0, 7, 1, 9)));
	}

	private Socio socioCafeteria;
	private ControladorSocio cafeteriaSocio;

	private void cafeteria() throws Exception {
		club.adm.agregarArticulo("Cafeteria", new Snack("Barra de granola", 6000, new String[] { "maní", "gluten" }));
		club.adm.agregarArticulo("Cafeteria", new Bebida("Café", 5000, true));
		club.adm.agregarArticulo("Cafeteria", new Bebida("Limonada", 4000, false));
		club.adm.abastecer("Cafeteria", "Barra de granola", 10);
		club.adm.abastecer("Cafeteria", "Café", 10);
		club.adm.abastecer("Cafeteria", "Limonada", 10);
		socioCafeteria = club.socio("ana", "20");
		cafeteriaSocio = club.login("ana");
	}

	private VentaCafeteria compraConPropina500() throws Exception {
		cafeteria();
		cafeteriaSocio.comprar("Cafeteria", new String[] { "Barra de granola", "Café" }, new int[] { 1, 1 }, null, 500, 0);
		return club.central.getCafeteria().getVentas().get(0);
	}

	private ControladorSocio socioConReservaEnCurso(String username, String tipo, boolean techada) throws Exception {
		club.adm.agregarInstalacion(tipo, 4, techada);
		List<Instalacion> lista = club.central.getInstalaciones().getInstalaciones();
		Instalacion instalacion = lista.get(lista.size() - 1);
		club.socio(username, "30");
		LocalDateTime ahora = LocalDateTime.now();
		club.central.getReservas().agregarReserva(new Reserva("R-" + username, instalacion, ahora.toLocalDate(), new int[] { ahora.getHour() }, username));
		return club.login(username);
	}

	private void seisUsuarios() throws Exception {
		club.usuario(new Entrenador("mario", Club.PW, "C-mario"));
		club.usuario(new Fisioterapeuta("laura", Club.PW, "C-laura"));
		club.socio("ana", "20");
		club.central.registrarEstudianteNoSocio("Esteban", "esteban", Club.PW);
		club.central.registrarPersonaExterna("Marta", "marta", Club.PW);
	}

	@Nested
	@DisplayName("RF-S01 Reservar instalaciones")
	class RfS01 {
		@Test
		@DisplayName("RF-S01 / HU-SOC-01 – caso 2: Se crea la Reserva (2026-10-17, hora=[10]) y un PartidoPracticaRaqueta con 2 jugadores por equipo")
		void rfS01_caso2_reservaYPartido() throws Exception {
			anaReservaDobles();
			Reserva reserva = club.central.getReservas().getReservas().get(0);
			assertEquals(SABADO, reserva.getFecha());
			assertArrayEquals(new int[] { 10 }, reserva.getHoras());
			assertEquals("INS-1", reserva.getInstalacion().getInstalacionId());
			PartidoPractica partido = practicas().get(0);
			assertTrue(List.of("ana", dobles[0]).contains(partido.getReservador()), "reservadoPor identifica a Ana");
			assertSame(reserva, partido.getReserva());
			assertEquals(2, partido.getEquipo1().length);
			assertEquals(2, partido.getEquipo2().length);
			for (String id : dobles) {
				assertInstanceOf(JugadorIndividual.class, club.jugador(id));
			}
		}

		@Test
		@DisplayName("RF-S01 / HU-SOC-01 – caso 3: Si la instalación no tenía Horario para esa fecha, se crea uno nuevo")
		void rfS01_caso3_horarioNuevo() throws Exception {
			prepararDobles();
			Instalacion cancha = club.central.getInstalaciones().getInstalacion("INS-1");
			assertFalse(cancha.getHorario().containsKey(SABADO));
			ana.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "dobles", new String[] { dobles[0], dobles[1] },
					new String[] { dobles[2], dobles[3] });
			assertTrue(cancha.getHorario().containsKey(SABADO));
		}

		@Test
		@DisplayName("RF-S01 / HU-SOC-01 – caso 4: Otro socio no puede reservar la misma cancha en el mismo bloque")
		void rfS01_caso4_mismoBloque() throws Exception {
			anaReservaDobles();
			ControladorSocio bea = club.login("bea");
			assertThrows(InstalacionNoDisponibleException.class, () -> bea.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "dobles",
					new String[] { dobles[0], dobles[1] }, new String[] { dobles[2], dobles[3] }));
		}

		@Test
		@DisplayName("RF-S01 / HU-SOC-01 – caso 5: Se rechaza reservar con 3 jugadores")
		void rfS01_caso5_tresJugadores() throws Exception {
			anaReservaDobles();
			assertThrows(CapacidadInstalacionException.class, () -> ana.reservarInstalacion("INS-1", SABADO, new int[] { 11 }, "dobles",
					new String[] { dobles[0], dobles[1] }, new String[] { dobles[2] }));
		}

		@Test
		@DisplayName("RF-S01 / HU-SOC-01 – caso 5: Se rechaza reservar en sencillos con 4 jugadores")
		void rfS01_caso5_sencillosConCuatro() throws Exception {
			anaReservaDobles();
			assertThrows(CapacidadInstalacionException.class, () -> ana.reservarInstalacion("INS-1", SABADO, new int[] { 11 }, "sencillos",
					new String[] { dobles[0], dobles[1] }, new String[] { dobles[2], dobles[3] }));
		}

		@Test
		@DisplayName("RF-S01 / HU-SOC-01 – caso 6: Con maxSimultaneas(padel)=2 y 3 canchas, la tercera a las 10:00 se rechaza y a las 11:00 se acepta")
		void rfS01_caso6_maximoSimultaneas() throws Exception {
			for (int i = 0; i < 3; i++) {
				club.adm.agregarInstalacion("padel", 4, true);
			}
			club.adm.setMaximoInstalacionesSimultaneas("padel", 2);
			String[] e1 = { jugador("ana", "padel", "Avanzado", 1100), jugador("bea", "padel", "Avanzado", 1100) };
			String[] e2 = { jugador("carla", "padel", "Avanzado", 1100), jugador("dora", "padel", "Avanzado", 1100) };
			ControladorSocio socio = club.login("ana");
			socio.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "dobles", e1, e2);
			socio.reservarInstalacion("INS-2", SABADO, new int[] { 10 }, "dobles", e1, e2);
			assertThrows(MaximoSimultaneasException.class, () -> socio.reservarInstalacion("INS-3", SABADO, new int[] { 10 }, "dobles", e1, e2));
			socio.reservarInstalacion("INS-3", SABADO, new int[] { 11 }, "dobles", e1, e2);
			assertEquals(3, club.central.getReservas().getReservas().size());
		}

		@Test
		@DisplayName("RF-S01 / HU-SOC-01 – caso 7: El Administrador no puede reservar")
		void rfS01_caso7_administrador() throws Exception {
			ControladorRol admin = club.login("admin");
			assertFalse(admin instanceof ControladorSocio);
		}
	}

	@Nested
	@DisplayName("RF-S02 Programar partidos de práctica")
	class RfS02 {
		@Test
		@DisplayName("RF-S02 / HU-SOC-02 – caso 1: Intermedio contra Avanzado (niveles adyacentes) se acepta")
		void rfS02_caso1_adyacentes() throws Exception {
			club.adm.agregarInstalacion("tenis", 4, false);
			String propio = jugador("ana", "tenis", "Intermedio", 1000);
			String rival = jugador("beto", "tenis", "Avanzado", 1000);
			((ControladorSocio) club.login("ana")).reservarInstalacion("INS-1", SABADO, new int[] { 9 }, "sencillos", new String[] { propio },
					new String[] { rival });
			assertEquals(1, practicas().size());
		}

		@Test
		@DisplayName("RF-S02 / HU-SOC-02 – caso 2: Avanzado contra Principiante (no adyacentes) se rechaza")
		void rfS02_caso2_noAdyacentes() throws Exception {
			club.adm.agregarInstalacion("padel", 4, true);
			String[] propio = { jugador("ana", "padel", "Avanzado", 1000), jugador("bea", "padel", "Avanzado", 1000) };
			String[] rival = { jugador("caro", "padel", "Principiante", 1000), jugador("dani", "padel", "Principiante", 1000) };
			ControladorSocio socio = club.login("ana");
			assertThrows(NivelIncompatibleException.class,
					() -> socio.reservarInstalacion("INS-1", SABADO, new int[] { 9 }, "dobles", propio, rival));
			assertTrue(practicas().isEmpty());
		}

		@Test
		@DisplayName("RF-S02 / HU-SOC-02 – caso 3: Con un rival que es entrenador (esEntrenador=true) se acepta por la excepción")
		void rfS02_caso3_entrenador() throws Exception {
			club.adm.agregarInstalacion("tenis", 4, false);
			String propio = jugador("ana", "tenis", "Avanzado", 1000);
			club.usuario(new Entrenador("coach", Club.PW, "C-coach"));
			String rival = club.individual("coach", "tenis", "Principiante", 1000);
			assertTrue(club.jugador(rival).getEsEntrenador());
			((ControladorSocio) club.login("ana")).reservarInstalacion("INS-1", SABADO, new int[] { 9 }, "sencillos", new String[] { propio },
					new String[] { rival });
			assertEquals(1, practicas().size());
		}

		@Test
		@DisplayName("RF-S02 / HU-SOC-02 – caso 4: El partido aceptado tiene una Reserva con instalación asignada que no se cruza con otra")
		void rfS02_caso4_reserva() throws Exception {
			club.adm.agregarInstalacion("tenis", 4, false);
			String propio = jugador("ana", "tenis", "Intermedio", 1000);
			String rival = jugador("beto", "tenis", "Avanzado", 1000);
			ControladorSocio socio = club.login("ana");
			socio.reservarInstalacion("INS-1", SABADO, new int[] { 9 }, "sencillos", new String[] { propio }, new String[] { rival });
			Reserva reserva = practicas().get(0).getReserva();
			assertEquals("INS-1", reserva.getInstalacion().getInstalacionId());
			assertEquals(1, club.central.getReservas().getReservas().stream()
					.filter(r -> r.getInstalacion() == reserva.getInstalacion() && r.cruzaCon(SABADO, new int[] { 9 })).count());
			assertThrows(InstalacionNoDisponibleException.class,
					() -> socio.reservarInstalacion("INS-1", SABADO, new int[] { 9 }, "sencillos", new String[] { propio }, new String[] { rival }));
		}
	}

	@Nested
	@DisplayName("RF-S03 Comprar artículos deportivos")
	class RfS03 {
		@Test
		@DisplayName("RF-S03 / HU-SOC-03 – caso 2: Total 47600, disponibilidad 4 y la VentaTienda queda con el socio como comprador")
		void rfS03_caso2_compra() throws Exception {
			Socio socio = socioCompraPelotas();
			VentaTienda venta = club.central.getTienda().getVentas().get(0);
			assertEquals(47600, venta.calcularTotal());
			assertEquals(4, club.central.getTienda().getArticulo("Pelotas Tenis").getDisponibilidad());
			assertSame(socio, venta.getComprador());
		}

		@Test
		@DisplayName("RF-S03 / HU-SOC-03 – caso 3: Se generan 800 puntos de fidelidad")
		void rfS03_caso3_puntos() throws Exception {
			assertEquals(800, socioCompraPelotas().getPuntosFidelidad(), 0.01);
		}

		@Test
		@DisplayName("RF-S03 / HU-SOC-03 – caso 4: Se rechaza comprar 7 unidades cuando hay 4, sin modificar el inventario")
		void rfS03_caso4_insuficiente() throws Exception {
			socioCompraPelotas();
			ControladorSocio socio = club.login("ana");
			assertThrows(InventarioInsuficienteException.class,
					() -> socio.comprar("Tienda", new String[] { "Pelotas Tenis" }, new int[] { 7 }, null, 0, 0));
			assertEquals(4, club.central.getTienda().getArticulo("Pelotas Tenis").getDisponibilidad());
		}
	}

	@Nested
	@DisplayName("RF-S04 Redimir puntos de fidelidad")
	class RfS04 {
		@Test
		@DisplayName("RF-S04 / HU-SOC-05 – caso 3: puntosRedimidos=300 antes del IVA: valor 29700, IVA 5643, total 35343")
		void rfS04_caso3_venta() throws Exception {
			socioRedime();
			VentaTienda venta = club.central.getTienda().getVentas().get(0);
			assertEquals(300, venta.getPuntosRedimidos());
			assertEquals(29700, venta.calcularSubtotal() - venta.getPuntosRedimidos());
			assertEquals(5643, venta.getIva());
			assertEquals(35343, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-S04 / HU-SOC-05 – caso 4: La compra genera 594 puntos nuevos")
		void rfS04_caso4_puntosGenerados() throws Exception {
			socioRedime();
			assertEquals(594, club.central.getTienda().getVentas().get(0).calcularPuntosGenerados(), 0.01);
		}

		@Test
		@DisplayName("RF-S04 / HU-SOC-05 – caso 5: El saldo final es 794 puntos")
		void rfS04_caso5_saldo() throws Exception {
			assertEquals(794, socioRedime().consultarPuntosFidelidad(), 0.01);
		}

		@Test
		@DisplayName("RF-S04 / HU-SOC-05 – caso 6: Se rechaza redimir 1000 puntos teniendo 794")
		void rfS04_caso6_insuficientes() throws Exception {
			ControladorSocio socio = socioRedime();
			assertThrows(PuntosInsuficientesException.class, () -> socio.comprar("Tienda", new String[] { "Gorra" }, new int[] { 1 }, null, 0, 1000));
			assertEquals(794, socio.consultarPuntosFidelidad(), 0.01);
		}
	}

	@Nested
	@DisplayName("RF-S05 Registrar estadísticas y actualizar ranking de partidos de raqueta")
	class RfS05 {
		@Test
		@DisplayName("RF-S05 / HU-SOC-02 – caso 3: Las 2 estadísticas quedan en el partido y se suman a la estadística acumulada de cada jugadora")
		void rfS05_caso3_estadisticas() throws Exception {
			partidoAnaSofia();
			assertEquals(2, partidoTenis.getEstadisticas().size());
			EstadisticaTenisPadel ana = (EstadisticaTenisPadel) club.jugador(anaTenis).getEstadisticaAcumulada();
			EstadisticaTenisPadel sofia = (EstadisticaTenisPadel) club.jugador(sofiaTenis).getEstadisticaAcumulada();
			assertEquals(List.of(2, 12, 3, 5), List.of(ana.getSetGanados(), ana.getGamesGanados(), ana.getAces(), ana.getErroresNoForzados()));
			assertEquals(List.of(0, 7, 1, 9),
					List.of(sofia.getSetGanados(), sofia.getGamesGanados(), sofia.getAces(), sofia.getErroresNoForzados()));
		}

		@Test
		@DisplayName("RF-S05 / HU-SOC-02 – caso 4: ELO de tenis: Ana 1016 y Sofía 984; el ELO de pádel de Ana sigue en 1100")
		void rfS05_caso4_elo() throws Exception {
			partidoAnaSofia();
			assertEquals(1016, ((JugadorIndividual) club.jugador(anaTenis)).getElo(), 0.001);
			assertEquals(984, ((JugadorIndividual) club.jugador(sofiaTenis)).getElo(), 0.001);
			assertEquals(1100, ((JugadorIndividual) club.jugador(anaPadel)).getElo(), 0.001);
		}

		@Test
		@DisplayName("RF-S05 / HU-SOC-02 – caso 5: En tenis de mesa se registra EstadisticaTenisMesa y se actualiza el ELO de esa disciplina")
		void rfS05_caso5_mesa() throws Exception {
			club.adm.agregarInstalacion("mesa", 4, false);
			String ana = jugador("ana", "tenis de mesa", "Intermedio", 1000);
			String sofia = jugador("sofia", "tenis de mesa", "Intermedio", 1000);
			ControladorSocio socio = club.login("ana");
			socio.reservarInstalacion("INS-1", SABADO, new int[] { 11 }, "sencillos", new String[] { ana }, new String[] { sofia });
			Partido mesa = practicas().get(0);
			socio.registrarResultadoPartidoPractica(mesa.getIdPartido(), List.of(new EstadisticaTenisMesa(ana, 3, 44), new EstadisticaTenisMesa(sofia, 1, 40)));
			assertEquals(2, mesa.getEstadisticas().size());
			assertEquals(1016, ((JugadorIndividual) club.jugador(ana)).getElo(), 0.001);
			assertEquals(984, ((JugadorIndividual) club.jugador(sofia)).getElo(), 0.001);
		}
	}

	@Nested
	@DisplayName("RF-S06 Comprar en la cafetería")
	class RfS06 {
		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 2: Antes de confirmar se muestran los alérgenos \"maní, gluten\"")
		void rfS06_caso2_alergenos() throws Exception {
			cafeteria();
			assertEquals("maní, gluten", String.join(", ", cafeteriaSocio.consultarAlergenos(new String[] { "Barra de granola" })));
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 3: Subtotal 11000, impuesto al consumo 880, propina sugerida 1100 y total 12980")
		void rfS06_caso3_cotizacion() throws Exception {
			cafeteria();
			assertEquals("Subtotal: 11000 | Impuesto al consumo: 880 | Propina sugerida: 1100 | Total: 12980",
					cafeteriaSocio.cotizarCompraCafeteria(new String[] { "Barra de granola", "Café" }, new int[] { 1, 1 }));
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 4: Con propina 500 el total queda en 12380")
		void rfS06_caso4_total() throws Exception {
			assertEquals(12380, compraConPropina500().calcularTotal());
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 4: La VentaCafeteria queda en ventas con propina=500, impuestoConsumo=880 y el socio como comprador")
		void rfS06_caso4_venta() throws Exception {
			VentaCafeteria venta = compraConPropina500();
			assertEquals(500, venta.getPropina());
			assertEquals(880, venta.getIva());
			assertSame(socioCafeteria, venta.getComprador());
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 4: La disponibilidad de cada artículo baja en 1")
		void rfS06_caso4_disponibilidad() throws Exception {
			compraConPropina500();
			assertEquals(9, club.central.getCafeteria().getArticulo("Barra de granola").getDisponibilidad());
			assertEquals(9, club.central.getCafeteria().getArticulo("Café").getDisponibilidad());
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 4: El café queda en las bebidasSinConsumir del socio")
		void rfS06_caso4_bebidaSinConsumir() throws Exception {
			compraConPropina500();
			assertEquals(List.of("Café"), socioCafeteria.getBebidasSinConsumir().stream().map(Bebida::getNombre).toList());
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 4: El socio gana 220 puntos de fidelidad")
		void rfS06_caso4_puntos() throws Exception {
			compraConPropina500();
			assertEquals(220, socioCafeteria.getPuntosFidelidad(), 0.01);
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 5: Con una reserva en curso en pádel, el café se rechaza y la limonada se acepta")
		void rfS06_caso5_padel() throws Exception {
			cafeteria();
			ControladorSocio socio = socioConReservaEnCurso("pablo", "padel", true);
			assertThrows(BebidaCalienteException.class, () -> socio.comprar("Cafeteria", new String[] { "Café" }, new int[] { 1 }, null, 0, 0));
			socio.comprar("Cafeteria", new String[] { "Limonada" }, new int[] { 1 }, null, 0, 0);
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 5: Con una reserva en curso en una mesa techada, el café se rechaza y la limonada se acepta")
		void rfS06_caso5_mesaTechada() throws Exception {
			cafeteria();
			ControladorSocio socio = socioConReservaEnCurso("marcos", "mesa", true);
			assertThrows(BebidaCalienteException.class, () -> socio.comprar("Cafeteria", new String[] { "Café" }, new int[] { 1 }, null, 0, 0));
			socio.comprar("Cafeteria", new String[] { "Limonada" }, new int[] { 1 }, null, 0, 0);
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 5: Con una reserva en curso en una mesa no techada, el café se acepta")
		void rfS06_caso5_mesaNoTechada() throws Exception {
			cafeteria();
			ControladorSocio socio = socioConReservaEnCurso("nora", "mesa", false);
			socio.comprar("Cafeteria", new String[] { "Café" }, new int[] { 1 }, null, 0, 0);
			assertEquals(1, club.central.getCafeteria().getVentas().size());
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 6: Con el café sin consumir, el socio no puede usar una cancha de pádel")
		void rfS06_caso6_padel() throws Exception {
			compraConPropina500();
			club.adm.agregarInstalacion("padel", 4, true);
			String[] e1 = { club.individual("ana", "padel", "Avanzado", 1000), jugador("bea", "padel", "Avanzado", 1000) };
			String[] e2 = { jugador("carla", "padel", "Avanzado", 1000), jugador("dora", "padel", "Avanzado", 1000) };
			assertThrows(BebidaCalienteException.class, () -> cafeteriaSocio.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "dobles", e1, e2));
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 6: Con el café sin consumir, el socio no puede usar una mesa de tenis de mesa techada")
		void rfS06_caso6_mesaTechada() throws Exception {
			compraConPropina500();
			club.adm.agregarInstalacion("mesa", 4, true);
			String ana = club.individual("ana", "tenis de mesa", "Avanzado", 1000);
			String bea = jugador("bea", "tenis de mesa", "Avanzado", 1000);
			assertThrows(BebidaCalienteException.class,
					() -> cafeteriaSocio.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "sencillos", new String[] { ana }, new String[] { bea }));
		}

		@Test
		@DisplayName("RF-S06 / HU-SOC-04 – caso 7: Se rechaza una propina negativa")
		void rfS06_caso7_propinaNegativa() throws Exception {
			cafeteria();
			assertThrows(ValorInvalidoException.class,
					() -> cafeteriaSocio.comprar("Cafeteria", new String[] { "Limonada" }, new int[] { 1 }, null, -500, 0));
		}
	}

	@Nested
	@DisplayName("RF-T")
	class RfT {
		@Test
		@DisplayName("RF-T / HU-USR-01 – caso 2: Cada usuario inicia sesión y el sistema lo identifica con su tipo y solo sus opciones")
		void rfT_caso2_cadaTipo() throws Exception {
			seisUsuarios();
			assertInstanceOf(ControladorAdministrador.class, club.login("admin"));
			assertEquals("Administrador", club.central.getUsuarioActual().getTipo());
			ControladorRol entrenador = club.login("mario");
			assertInstanceOf(ControladorEntrenador.class, entrenador);
			assertEquals("Entrenador", club.central.getUsuarioActual().getTipo());
			ControladorRol fisio = club.login("laura");
			assertInstanceOf(ControladorEmpleado.class, fisio);
			assertFalse(fisio instanceof ControladorEntrenador);
			assertEquals("Fisioterapeuta", fisio.getRol());
			ControladorRol socio = club.login("ana");
			assertInstanceOf(ControladorSocio.class, socio);
			assertFalse(socio instanceof ControladorAdministrador, "El socio no ve \"Crear Equipos\"");
			assertEquals(ControladorParticipante.class, club.login("esteban").getClass());
			assertInstanceOf(EstudianteNoSocio.class, club.central.getUsuarioActual());
			assertEquals(ControladorParticipante.class, club.login("marta").getClass());
			assertInstanceOf(PersonaExterna.class, club.central.getUsuarioActual());
		}

		@Test
		@DisplayName("RF-T / HU-USR-01 – caso 3: Se rechaza una contraseña incorrecta")
		void rfT_caso3_passwordIncorrecto() throws Exception {
			seisUsuarios();
			assertThrows(CredencialesInvalidasException.class, () -> club.central.iniciarSesion("ana", "otra"));
		}

		@Test
		@DisplayName("RF-T / HU-USR-01 – caso 3: Se rechaza un login que no existe")
		void rfT_caso3_loginInexistente() throws Exception {
			seisUsuarios();
			assertThrows(CredencialesInvalidasException.class, () -> club.central.iniciarSesion("nadie", Club.PW));
		}
	}
}
