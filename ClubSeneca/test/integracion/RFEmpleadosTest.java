package integracion;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controladores.ControladorEmpleado;
import controladores.ControladorEntrenador;
import controladores.ControladorRol;
import controladores.ControladorSocio;
import entrenamientos.AsistenciaJugador;
import entrenamientos.SesionEntrenamiento;
import equipos.Equipo;
import excepciones.AccionNoPermitidaException;
import excepciones.ClubSenecaException;
import excepciones.CodigoDescuentoInvalidoException;
import excepciones.EstadoAsistenciaInvalidoException;
import excepciones.EvaluacionInvalidaException;
import excepciones.FueraDeTurnoException;
import excepciones.InstalacionNoDisponibleException;
import excepciones.InventarioInsuficienteException;
import excepciones.JugadorNoConvocadoException;
import excepciones.PromocionNoAcumulableException;
import excepciones.SesionIncompletaException;
import excepciones.ValorInvalidoException;
import tiendas.Articulo;
import tiendas.ArticuloTienda;
import tiendas.Snack;
import tiendas.VentaTienda;
import premios.BonoDescuento;
import usuarios.Entrenador;
import usuarios.Fisioterapeuta;
import usuarios.Socio;
import usuarios.SolicitudCambioTurno;

/** Casos de prueba de RF-E01 a RF-E05, RF-F01, RF-F02, RF-EMP-01 y RF-EMP-02 (sección 4 del análisis). */
class RFEmpleadosTest {

	private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);

	@TempDir
	Path dir;
	Club club;

	@BeforeEach
	void setUp() throws Exception {
		club = new Club(dir);
	}

	private VentaTienda ultimaVenta() {
		List<VentaTienda> ventas = club.central.getTienda().getVentas();
		return ventas.get(ventas.size() - 1);
	}

	private ControladorEntrenador entrenador;
	private Equipo sub17;
	private String ana, sofia;

	private void entrenadorConTurno() throws Exception {
		club.usuario(new Entrenador("mario", Club.PW, "C-mario"));
		club.adm.asignarTurno("mario", LUNES, Club.horas(16, 19));
		club.adm.agregarInstalacion("grupal", 30, true);
		club.adm.agregarInstalacion("tenis", 4, false);
		sub17 = club.equipo("Fútbol Sub-17 A", "Fútbol", "Sub-17", 20);
		club.socio("ana", "20");
		club.socio("sofia", "20");
		ana = club.individual("ana", "tenis", "Intermedio", 1000);
		sofia = club.individual("sofia", "tenis", "Intermedio", 1000);
		entrenador = club.login("mario");
	}

	private SesionEntrenamiento sesionEquipo() throws Exception {
		entrenador.crearSesionEntrenamiento("equipo", LUNES, 16, 90, "INS-1", new String[] { sub17.getIdEquipo() });
		return club.central.getSesionesEntrenamiento().getSesiones().get(0);
	}

	private String[] pedroLuisCarlos;
	private String idSesion;

	private void sesionConTresJugadores() throws Exception {
		entrenadorConTurno();
		pedroLuisCarlos = new String[3];
		String[] nombres = { "pedro", "luis", "carlos" };
		for (int i = 0; i < 3; i++) {
			club.socio(nombres[i], "16");
			pedroLuisCarlos[i] = club.conjunto(nombres[i], sub17, i + 1);
		}
		idSesion = sesionEquipo().getIdSesion();
	}

	private void pedroAsistioCarlosFalto() throws Exception {
		sesionConTresJugadores();
		entrenador.registrarAsistencia(idSesion, pedroLuisCarlos[0], "asistió");
		entrenador.registrarAsistencia(idSesion, pedroLuisCarlos[2], "faltó");
	}

	private Entrenador marioE04;
	private Socio anaE04;

	private void entrenadorConCodigo() throws Exception {
		marioE04 = club.usuario(new Entrenador("mario", Club.PW, "ENT-0042"));
		anaE04 = club.socio("ana", "20");
		club.adm.agregarArticulo("Tienda", new ArticuloTienda("Raqueta Tenis", 150000, "Tenis"));
		club.adm.abastecer("Tienda", "Raqueta Tenis", 5);
	}

	private void marioCompartyConAna() throws Exception {
		entrenadorConCodigo();
		((ControladorEmpleado) club.login("mario")).compartirCodigoDescuento("ana");
	}

	private ControladorEntrenador entrenadorCompraBalon() throws Exception {
		club.usuario(new Entrenador("mario", Club.PW, "ENT-0042"));
		club.adm.agregarArticulo("Tienda", new ArticuloTienda("Balón Básquet", 90000, "Baloncesto"));
		club.adm.abastecer("Tienda", "Balón Básquet", 4);
		ControladorEntrenador mario = club.login("mario");
		mario.comprar("Tienda", new String[] { "Balón Básquet" }, new int[] { 1 }, "ENT-0042", 0, 0);
		return mario;
	}

	private ControladorEmpleado fisioterapeutaCompraCuerdas() throws Exception {
		club.usuario(new Fisioterapeuta("laura", Club.PW, "FIS-0007"));
		club.adm.agregarArticulo("Tienda", new ArticuloTienda("Cuerda de fuerza", 25000, "Gimnasio"));
		club.adm.abastecer("Tienda", "Cuerda de fuerza", 10);
		ControladorEmpleado laura = club.login("laura");
		laura.comprar("Tienda", new String[] { "Cuerda de fuerza" }, new int[] { 2 }, null, 0, 0);
		return laura;
	}

	private Fisioterapeuta lauraF02;
	private Socio luisF02;

	private void lauraCompartyConLuis() throws Exception {
		lauraF02 = club.usuario(new Fisioterapeuta("laura", Club.PW, "FIS-0007"));
		luisF02 = club.socio("luis", "25");
		club.adm.agregarArticulo("Tienda", new ArticuloTienda("Tobillera", 40000, "Protección"));
		club.adm.abastecer("Tienda", "Tobillera", 5);
		assertTrue(Arrays.asList(club.central.getTienda().getCodigosDescuento()).contains("FIS-0007"));
		((ControladorEmpleado) club.login("laura")).compartirCodigoDescuento("luis");
	}

	private ControladorEmpleado marioSugiereTobillera() throws Exception {
		club.usuario(new Entrenador("mario", Club.PW, "C-mario"));
		club.adm.agregarArticulo("Tienda", new ArticuloTienda("Balón Fútbol", 80000, "Fútbol"));
		ControladorEmpleado mario = club.login("mario");
		mario.sugerirArticulo("Tienda", new ArticuloTienda("Tobillera", 40000, "Protección"));
		return mario;
	}

	private Fisioterapeuta lauraEmp02;

	private ControladorEmpleado lauraSolicitaCambio() throws Exception {
		lauraEmp02 = club.usuario(new Fisioterapeuta("laura", Club.PW, "C-laura"));
		club.adm.asignarTurno("laura", LUNES, Club.horas(8, 11));
		ControladorEmpleado laura = club.login("laura");
		laura.solicitarCambioTurno(LUNES, Club.horas(12, 15));
		return laura;
	}

	@Nested
	@DisplayName("RF-E01 Crear Sesiones De Entrenamiento")
	class RfE01 {
		@Test
		@DisplayName("RF-E01 / HU-ENT-01 – caso 1: Inicia sesión un Entrenador cuyo turno del 2026-10-12 tiene h16 a h19 en true")
		void rfE01_caso1_inicioSesion() throws Exception {
			entrenadorConTurno();
			assertEquals("Entrenador", entrenador.getRol());
			assertArrayEquals(Club.horas(16, 19), ((Entrenador) club.central.getUsuarioActual()).getTurno(LUNES));
		}

		@Test
		@DisplayName("RF-E01 / HU-ENT-01 – casos 2 y 3: La sesión de equipo queda con el entrenador y una Reserva (2026-10-12, hora=[16,17]) en la instalación grupal")
		void rfE01_caso3_sesionEquipo() throws Exception {
			entrenadorConTurno();
			SesionEntrenamiento sesion = sesionEquipo();
			assertEquals("mario", sesion.getEntrenadorUsername());
			assertEquals(LUNES, sesion.getReserva().getFecha());
			assertArrayEquals(new int[] { 16, 17 }, sesion.getReserva().getHoras());
			assertEquals("grupal", sesion.getReserva().getInstalacion().getTipo());
			assertTrue(club.central.getReservas().getReservas().contains(sesion.getReserva()));
		}

		@Test
		@DisplayName("RF-E01 / HU-ENT-01 – caso 4: La sesión individual a las 18:00 con Ana y Sofía tiene reserva hora=[18]")
		void rfE01_caso4_sesionIndividual() throws Exception {
			entrenadorConTurno();
			entrenador.crearSesionEntrenamiento("raqueta", LUNES, 18, 60, "INS-2", new String[] { ana, sofia });
			SesionEntrenamiento sesion = club.central.getSesionesEntrenamiento().getSesiones().get(0);
			assertArrayEquals(new int[] { 18 }, sesion.getReserva().getHoras());
			assertEquals("tenis", sesion.getReserva().getInstalacion().getTipo());
			assertTrue(sesion.esConvocado(ana) && sesion.esConvocado(sofia));
		}

		@Test
		@DisplayName("RF-E01 / HU-ENT-01 – caso 5: Se rechaza otra sesión en la misma instalación y hora")
		void rfE01_caso5_mismaInstalacionYHora() throws Exception {
			entrenadorConTurno();
			sesionEquipo();
			assertThrows(InstalacionNoDisponibleException.class,
					() -> entrenador.crearSesionEntrenamiento("equipo", LUNES, 16, 60, "INS-1", new String[] { sub17.getIdEquipo() }));
		}

		@Test
		@DisplayName("RF-E01 / HU-ENT-01 – caso 5: Se rechaza una sesión fuera del turno del entrenador")
		void rfE01_caso5_fueraDeTurno() throws Exception {
			entrenadorConTurno();
			assertThrows(FueraDeTurnoException.class,
					() -> entrenador.crearSesionEntrenamiento("equipo", LUNES, 20, 60, "INS-1", new String[] { sub17.getIdEquipo() }));
		}

		@Test
		@DisplayName("RF-E01 / HU-ENT-01 – caso 5: Se rechaza una sesión sin convocados")
		void rfE01_caso5_sinConvocados() throws Exception {
			entrenadorConTurno();
			assertThrows(ClubSenecaException.class, () -> entrenador.crearSesionEntrenamiento("raqueta", LUNES, 18, 60, "INS-2", new String[0]));
		}

		@Test
		@DisplayName("RF-E01 / HU-ENT-01 – caso 5: Se rechaza una sesión con duracion=0")
		void rfE01_caso5_duracionCero() throws Exception {
			entrenadorConTurno();
			assertThrows(ValorInvalidoException.class,
					() -> entrenador.crearSesionEntrenamiento("equipo", LUNES, 16, 0, "INS-1", new String[] { sub17.getIdEquipo() }));
		}

		@Test
		@DisplayName("RF-E01 / HU-ENT-01 – caso 5: Se rechaza convocar a una sesión de tenis a un socio sin JugadorIndividual de tenis")
		void rfE01_caso5_sinJugadorDeTenis() throws Exception {
			entrenadorConTurno();
			club.socio("eva", "20");
			String evaPadel = club.individual("eva", "padel", "Intermedio", 1000);
			assertThrows(ClubSenecaException.class,
					() -> entrenador.crearSesionEntrenamiento("raqueta", LUNES, 18, 60, "INS-2", new String[] { ana, evaPadel }));
		}
	}

	@Nested
	@DisplayName("RF-E02 Registrar asistencia")
	class RfE02 {
		@Test
		@DisplayName("RF-E02 / HU-ENT-02 – casos 2 y 3: Cada AsistenciaJugador guarda asistió / llegó tarde / faltó con su sesión y aparece en las del jugador")
		void rfE02_caso3_asistencias() throws Exception {
			sesionConTresJugadores();
			String[] valores = { "asistió", "llegó tarde", "faltó" };
			for (int i = 0; i < 3; i++) {
				entrenador.registrarAsistencia(idSesion, pedroLuisCarlos[i], valores[i]);
			}
			SesionEntrenamiento sesion = club.central.getSesionesEntrenamiento().getSesion(idSesion);
			for (int i = 0; i < 3; i++) {
				AsistenciaJugador asistencia = sesion.getAsistencia(pedroLuisCarlos[i]);
				assertEquals(valores[i], asistencia.getAsistio());
				assertSame(sesion, asistencia.getSesion());
				assertEquals(pedroLuisCarlos[i], asistencia.getJugadorId());
				assertTrue(club.jugador(pedroLuisCarlos[i]).getAsistencias().contains(asistencia));
			}
		}

		@Test
		@DisplayName("RF-E02 / HU-ENT-02 – caso 4: Se rechaza el valor \"excusado\"")
		void rfE02_caso4_excusado() throws Exception {
			sesionConTresJugadores();
			assertThrows(EstadoAsistenciaInvalidoException.class, () -> entrenador.registrarAsistencia(idSesion, pedroLuisCarlos[0], "excusado"));
		}

		@Test
		@DisplayName("RF-E02 / HU-ENT-02 – caso 4: Se rechaza la asistencia de un jugador que no fue convocado")
		void rfE02_caso4_noConvocado() throws Exception {
			sesionConTresJugadores();
			Equipo otro = club.equipo("Fútbol Sub-17 B", "Fútbol", "Sub-17", 20);
			club.socio("eva", "16");
			String eva = club.conjunto("eva", otro, 1);
			assertThrows(JugadorNoConvocadoException.class, () -> entrenador.registrarAsistencia(idSesion, eva, "asistió"));
		}

		@Test
		@DisplayName("RF-E02 / HU-ENT-02 – caso 4: Se rechaza registrar la asistencia de una sesión de otro entrenador")
		void rfE02_caso4_otroEntrenador() throws Exception {
			sesionConTresJugadores();
			club.usuario(new Entrenador("jose", Club.PW, "C-jose"));
			ControladorEntrenador jose = club.login("jose");
			assertThrows(AccionNoPermitidaException.class, () -> jose.registrarAsistencia(idSesion, pedroLuisCarlos[0], "asistió"));
		}
	}

	@Nested
	@DisplayName("RF-E03 Registrar Evaluación De Desempeño")
	class RfE03 {
		@Test
		@DisplayName("RF-E03 / HU-ENT-03 – casos 2 y 3: La AsistenciaJugador de Pedro guarda evaluacion=4 y comentario=\"Buena definición\"")
		void rfE03_caso3_evaluacion() throws Exception {
			pedroAsistioCarlosFalto();
			entrenador.registrarEvaluacion(idSesion, pedroLuisCarlos[0], 4, "Buena definición");
			AsistenciaJugador pedro = club.central.getSesionesEntrenamiento().getSesion(idSesion).getAsistencia(pedroLuisCarlos[0]);
			assertEquals(4, pedro.getEvaluacion());
			assertEquals("Buena definición", pedro.getComentario());
		}

		@Test
		@DisplayName("RF-E03 / HU-ENT-03 – caso 4: Se rechaza evaluar a un jugador con asistio=\"faltó\"")
		void rfE03_caso4_falto() throws Exception {
			pedroAsistioCarlosFalto();
			assertThrows(SesionIncompletaException.class, () -> entrenador.registrarEvaluacion(idSesion, pedroLuisCarlos[2], 4, "x"));
		}

		@Test
		@DisplayName("RF-E03 / HU-ENT-03 – caso 4: Se rechaza una evaluación de 0")
		void rfE03_caso4_cero() throws Exception {
			pedroAsistioCarlosFalto();
			assertThrows(EvaluacionInvalidaException.class, () -> entrenador.registrarEvaluacion(idSesion, pedroLuisCarlos[0], 0, "x"));
		}

		@Test
		@DisplayName("RF-E03 / HU-ENT-03 – caso 4: Se rechaza una evaluación de 6")
		void rfE03_caso4_seis() throws Exception {
			pedroAsistioCarlosFalto();
			assertThrows(EvaluacionInvalidaException.class, () -> entrenador.registrarEvaluacion(idSesion, pedroLuisCarlos[0], 6, "x"));
		}
	}

	@Nested
	@DisplayName("RF-E04 Compartir Codigo De Descuento A Un Socio")
	class RfE04 {
		@Test
		@DisplayName("RF-E04 / HU-EMP-01 – caso 1: El codigoDescuentoTienda \"ENT-0042\" está en Tienda.codigosDescuento")
		void rfE04_caso1_codigo() throws Exception {
			entrenadorConCodigo();
			assertEquals("ENT-0042", marioE04.getCodigoDescuentoTienda());
			assertTrue(Arrays.asList(club.central.getTienda().getCodigosDescuento()).contains("ENT-0042"));
		}

		@Test
		@DisplayName("RF-E04 / HU-EMP-01 – caso 2: Ana queda en las solicitudesCodigo del entrenador con codigoDescuentoDado=\"ENT-0042\"")
		void rfE04_caso2_compartir() throws Exception {
			marioCompartyConAna();
			assertTrue(Arrays.asList(marioE04.getSociosConCodigo()).contains("ana"));
			assertEquals("ENT-0042", anaE04.getCodigoDescuentoDado());
		}

		@Test
		@DisplayName("RF-E04 / HU-EMP-01 – caso 3: Ana compra una \"Raqueta Tenis\" con 8 %: descuento 12000, subtotal 138000, IVA 26220, total 164220")
		void rfE04_caso3_compra() throws Exception {
			marioCompartyConAna();
			((ControladorSocio) club.login("ana")).comprar("Tienda", new String[] { "Raqueta Tenis" }, new int[] { 1 }, "ENT-0042", 0, 0);
			VentaTienda venta = ultimaVenta();
			assertEquals(12000, venta.getDescuento());
			assertEquals(138000, venta.calcularSubtotal() - venta.getDescuento());
			assertEquals(26220, venta.getIva());
			assertEquals(164220, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-E04 / HU-EMP-01 – caso 4: Se rechaza usar el código junto con un bono de torneo en la misma compra")
		void rfE04_caso4_conBono() throws Exception {
			marioCompartyConAna();
			club.central.getTorneos().agregarBono(new BonoDescuento("T-4-B1", "T-4", 1, "ana", LocalDate.of(2026, 12, 6), 10));
			ControladorSocio socio = club.login("ana");
			assertThrows(PromocionNoAcumulableException.class,
					() -> socio.comprar("Tienda", new String[] { "Raqueta Tenis" }, new int[] { 1 }, "ENT-0042", 0, 0, "T-4-B1"));
			assertTrue(club.central.getTienda().getVentas().isEmpty());
		}
	}

	@Nested
	@DisplayName("RF-E05 Comprar en la tienda y cafetería")
	class RfE05 {
		@Test
		@DisplayName("RF-E05 / HU-EMP-01 – caso 3: disponibilidad=3, 15 % (descuento 13500, subtotal 76500, IVA 14535, total 91035) y venta registrada")
		void rfE05_caso3_compra() throws Exception {
			entrenadorCompraBalon();
			assertEquals(3, club.central.getTienda().getArticulo("Balón Básquet").getDisponibilidad());
			VentaTienda venta = ultimaVenta();
			assertEquals(13500, venta.getDescuento());
			assertEquals(76500, venta.calcularSubtotal() - venta.getDescuento());
			assertEquals(14535, venta.getIva());
			assertEquals(91035, venta.calcularTotal());
			assertEquals("mario", venta.getComprador().getUsername());
		}

		@Test
		@DisplayName("RF-E05 / HU-EMP-01 – caso 4: Se rechaza comprar 5 unidades de un artículo con disponibilidad=3")
		void rfE05_caso4_insuficiente() throws Exception {
			ControladorEntrenador mario = entrenadorCompraBalon();
			assertThrows(InventarioInsuficienteException.class,
					() -> mario.comprar("Tienda", new String[] { "Balón Básquet" }, new int[] { 5 }, null, 0, 0));
			assertEquals(3, club.central.getTienda().getArticulo("Balón Básquet").getDisponibilidad());
		}
	}

	@Nested
	@DisplayName("RF-F01 Comprar en la tienda y cafetería")
	class RfF01 {
		@Test
		@DisplayName("RF-F01 / HU-EMP-01 – caso 3: disponibilidad=8 y la venta queda con subtotal 50000, IVA 9500 y total 59500")
		void rfF01_caso3_compra() throws Exception {
			fisioterapeutaCompraCuerdas();
			assertEquals(8, club.central.getTienda().getArticulo("Cuerda de fuerza").getDisponibilidad());
			VentaTienda venta = ultimaVenta();
			assertEquals(50000, venta.calcularSubtotal());
			assertEquals(9500, venta.getIva());
			assertEquals(59500, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-F01 / HU-EMP-01 – caso 4: Se rechaza comprar un artículo con disponibilidad=0")
		void rfF01_caso4_agotado() throws Exception {
			ControladorEmpleado laura = fisioterapeutaCompraCuerdas();
			club.adm.agregarArticulo("Tienda", new ArticuloTienda("Mancuerna", 60000, "Gimnasio"));
			assertThrows(InventarioInsuficienteException.class,
					() -> laura.comprar("Tienda", new String[] { "Mancuerna" }, new int[] { 1 }, null, 0, 0));
		}
	}

	@Nested
	@DisplayName("RF-F02 Compartir Codigo De Descuento A Un Socio")
	class RfF02 {
		@Test
		@DisplayName("RF-F02 / HU-EMP-01 – casos 1 y 2: Luis queda en las solicitudesCodigo de la fisioterapeuta con codigoDescuentoDado=\"FIS-0007\"")
		void rfF02_caso2_compartir() throws Exception {
			lauraCompartyConLuis();
			assertTrue(Arrays.asList(lauraF02.getSociosConCodigo()).contains("luis"));
			assertEquals("FIS-0007", luisF02.getCodigoDescuentoDado());
		}

		@Test
		@DisplayName("RF-F02 / HU-EMP-01 – caso 3: Luis compra una \"Tobillera\" con 8 %: subtotal 36800, IVA 6992, total 43792")
		void rfF02_caso3_compra() throws Exception {
			lauraCompartyConLuis();
			((ControladorSocio) club.login("luis")).comprar("Tienda", new String[] { "Tobillera" }, new int[] { 1 }, "FIS-0007", 0, 0);
			VentaTienda venta = ultimaVenta();
			assertEquals(36800, venta.calcularSubtotal() - venta.getDescuento());
			assertEquals(6992, venta.getIva());
			assertEquals(43792, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-F02 / HU-EMP-01 – caso 4: Se rechaza un código que no está en Tienda.codigosDescuento")
		void rfF02_caso4_codigoInexistente() throws Exception {
			lauraCompartyConLuis();
			ControladorSocio luis = club.login("luis");
			assertThrows(CodigoDescuentoInvalidoException.class,
					() -> luis.comprar("Tienda", new String[] { "Tobillera" }, new int[] { 1 }, "FIS-9999", 0, 0));
		}
	}

	@Nested
	@DisplayName("RF-EMP-01 Sugerir artículos o snacks")
	class RfEmp01 {
		@Test
		@DisplayName("RF-EMP-01 / HU-EMP-02 – caso 1: \"Tobillera\" queda en Tienda.sugerencias con sugeridoPor=Mario")
		void rfEmp01_caso1_tobillera() throws Exception {
			marioSugiereTobillera();
			List<ArticuloTienda> sugerencias = club.central.getTienda().getSugerencias();
			assertEquals(1, sugerencias.size());
			ArticuloTienda tobillera = sugerencias.get(0);
			assertEquals("Tobillera", tobillera.getNombre());
			assertEquals("Protección", tobillera.getCategoria());
			assertEquals(40000, tobillera.getPrecio());
			Method sugeridoPor = Arrays.stream(Articulo.class.getMethods()).filter(m -> m.getName().equals("getSugeridoPor")).findFirst()
					.orElse(null);
			assertNotNull(sugeridoPor, "El artículo sugerido no guarda sugeridoPor");
			assertEquals("mario", String.valueOf(sugeridoPor.invoke(tobillera)));
		}

		@Test
		@DisplayName("RF-EMP-01 / HU-EMP-02 – caso 2: La fisioterapeuta Laura sugiere el Snack \"Maní salado\" y queda en Cafeteria.sugerencias")
		void rfEmp01_caso2_snack() throws Exception {
			club.usuario(new Fisioterapeuta("laura", Club.PW, "C-laura"));
			ControladorEmpleado laura = club.login("laura");
			laura.sugerirArticulo("Cafeteria", new Snack("Maní salado", 3000, new String[] { "maní" }));
			assertEquals("Maní salado", club.central.getCafeteria().getSugerencias().get(0).getNombre());
			assertArrayEquals(new String[] { "maní" }, club.central.getCafeteria().getSugerencias().get(0).getAlergenos());
		}

		@Test
		@DisplayName("RF-EMP-01 / HU-EMP-02 – caso 3: Se rechaza sugerir \"Balón Fútbol\" (ya está en el inventario)")
		void rfEmp01_caso3_yaEnInventario() throws Exception {
			ControladorEmpleado mario = marioSugiereTobillera();
			assertThrows(ValorInvalidoException.class, () -> mario.sugerirArticulo("Tienda", new ArticuloTienda("Balón Fútbol", 80000, "Fútbol")));
		}

		@Test
		@DisplayName("RF-EMP-01 / HU-EMP-02 – caso 3: Se rechaza volver a sugerir \"Tobillera\"")
		void rfEmp01_caso3_repetida() throws Exception {
			ControladorEmpleado mario = marioSugiereTobillera();
			assertThrows(ValorInvalidoException.class, () -> mario.sugerirArticulo("Tienda", new ArticuloTienda("Tobillera", 40000, "Protección")));
		}

		@Test
		@DisplayName("RF-EMP-01 / HU-EMP-02 – caso 3: Se rechaza sugerir un snack sin alérgenos registrados")
		void rfEmp01_caso3_snackSinAlergenos() throws Exception {
			ControladorEmpleado mario = marioSugiereTobillera();
			assertThrows(ValorInvalidoException.class, () -> mario.sugerirArticulo("Cafeteria", new Snack("Galleta", 2000, new String[0])));
		}

		@Test
		@DisplayName("RF-EMP-01 / HU-EMP-02 – caso 4: Un Socio no tiene la opción de sugerir")
		void rfEmp01_caso4_socio() throws Exception {
			club.socio("ana", "20");
			ControladorRol socio = club.login("ana");
			assertFalse(socio instanceof ControladorEmpleado);
			assertTrue(Arrays.stream(ControladorSocio.class.getMethods()).noneMatch(m -> m.getName().equals("sugerirArticulo")));
		}
	}

	@Nested
	@DisplayName("RF-EMP-02 Solicitar cambio de turno")
	class RfEmp02 {
		@Test
		@DisplayName("RF-EMP-02 / HU-EMP-03 – caso 2: Se crea una SolicitudCambioTurno \"pendiente\" sin modificar el turno actual")
		void rfEmp02_caso2_solicitud() throws Exception {
			lauraSolicitaCambio();
			SolicitudCambioTurno solicitud = club.central.getUsuarios().getSolicitudes().get(0);
			assertEquals("pendiente", solicitud.getEstado());
			assertArrayEquals(Club.horas(12, 15), solicitud.getTurno());
			assertArrayEquals(Club.horas(8, 11), lauraEmp02.getTurno(LUNES));
		}

		@Test
		@DisplayName("RF-EMP-02 / HU-EMP-03 – caso 3: La solicitud aparece en la lista del administrador")
		void rfEmp02_caso3_listaAdministrador() throws Exception {
			lauraSolicitaCambio();
			List<SolicitudCambioTurno> pendientes = club.adm.consultarSolicitudesCambioTurno();
			assertEquals(1, pendientes.size());
			assertSame(lauraEmp02, pendientes.get(0).getEmpleado());
		}

		@Test
		@DisplayName("RF-EMP-02 / HU-EMP-03 – caso 4: Se rechaza solicitar un turno de h20 a h22 (fuera del horario de atención)")
		void rfEmp02_caso4_fueraDeAtencion() throws Exception {
			club.usuario(new Fisioterapeuta("laura", Club.PW, "C-laura"));
			ControladorEmpleado laura = club.login("laura");
			assertThrows(ValorInvalidoException.class, () -> laura.solicitarCambioTurno(LUNES, Club.horas(20, 22)));
		}
	}
}
