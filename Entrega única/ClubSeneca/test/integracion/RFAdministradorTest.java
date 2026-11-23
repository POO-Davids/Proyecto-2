package integracion;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import competencia.Competencia;
import competencia.CompetenciaASCUN;
import competencia.EquipoCompetencia;
import competencia.EquipoPropioCompetencia;
import controladores.ControladorAdministrador;
import controladores.ControladorEmpleado;
import controladores.ControladorEntrenador;
import controladores.ControladorSocio;
import entrenamientos.SesionEntrenamiento;
import equipos.Equipo;
import estadisticas.Estadistica;
import estadisticas.EstadisticaBasket;
import estadisticas.EstadisticaFutbol;
import estadisticas.EstadisticaTenisPadel;
import estadisticas.EstadisticaVolley;
import excepciones.CamisetaRepetidaException;
import excepciones.CapacidadInstalacionException;
import excepciones.CategoriaEdadException;
import excepciones.CoberturaTurnosException;
import excepciones.CupoEquipoExcedidoException;
import excepciones.ElementoDuplicadoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.EquipoPorDeporteException;
import excepciones.EquiposIncompatiblesException;
import excepciones.FueraDeTurnoException;
import excepciones.InstalacionConReservasException;
import excepciones.InstalacionNoDisponibleException;
import excepciones.InventarioInsuficienteException;
import excepciones.JugadorNoInscritoException;
import excepciones.JugadorSuspendidoException;
import excepciones.MarcadorInvalidoException;
import excepciones.MinimoJugadoresException;
import excepciones.RequisitosInscripcionException;
import excepciones.ValorInvalidoException;
import instalaciones.Instalacion;
import jugadores.CentralJugadores;
import jugadores.JugadorConjunto;
import jugadores.JugadorIndividual;
import partidos.Partido;
import partidos.PartidoCompetencia;
import partidos.PartidoInterno;
import partidos.PartidoPractica;
import reservas.Reserva;
import tiendas.Articulo;
import tiendas.ArticuloTienda;
import tiendas.Bebida;
import tiendas.Snack;
import tiendas.VentaCafeteria;
import tiendas.VentaTienda;
import usuarios.CentralCambioTurno;
import usuarios.Entrenador;
import usuarios.Fisioterapeuta;
import usuarios.Socio;
import usuarios.SolicitudCambioTurno;

/** Casos de prueba de RF-A01 a RF-A18 (sección 4 del análisis). */
class RFAdministradorTest {

	@TempDir
	Path dir;
	Club club;
	ControladorAdministrador adm;

	@BeforeEach
	void setUp() throws Exception {
		club = new Club(dir);
		adm = club.adm;
	}

	private Equipo equipo(String nombre) {
		return club.central.getEquipos().buscarPorNombre(nombre);
	}

	private String[] socios(Equipo equipo, String edad, String... usernames) throws Exception {
		String[] ids = new String[usernames.length];
		for (int i = 0; i < usernames.length; i++) {
			club.socio(usernames[i], edad);
			ids[i] = club.conjunto(usernames[i], equipo, i + 1);
		}
		return ids;
	}

	private List<PartidoPractica> practicas() {
		return club.central.getPartidos().getPartidos().stream().filter(p -> p instanceof PartidoPractica).map(p -> (PartidoPractica) p)
				.toList();
	}

	private String jugadorRaqueta(String username, String disciplina, String nivel) throws Exception {
		if (club.central.getUsuarios().getUsuario(username) == null) {
			club.socio(username, "25");
		}
		return club.individual(username, disciplina, nivel, 1000);
	}

	private void creaFutbolMasculino() throws Exception {
		adm.crearEquipo("Fútbol Masculino", "Fútbol", "Sub-17", 20);
	}

	private Equipo voleibolDadoDeBaja() throws Exception {
		Equipo femenino = club.equipo("Voleibol Femenino", "Voleibol", "Mayores", 20);
		Equipo b = club.equipo("Voleibol Femenino B", "Voleibol", "Mayores", 20);
		String[] ids = socios(femenino, "20", "vale", "vero");
		PartidoInterno partido = new PartidoInterno("PI-1", femenino, b, LocalDateTime.of(2026, 9, 1, 10, 0), ids, new String[0]);
		partido.registrarMarcador(3, 1);
		partido.setCerrado();
		club.central.getPartidos().agregarPartido(partido);
		assertEquals(2, femenino.getJugadoresIds().length);
		adm.darDeBajaEquipo(femenino.getIdEquipo());
		return femenino;
	}

	private Socio ana;
	private Equipo futbolFemenino;

	private void anaConDosJugadores() throws Exception {
		ana = club.socio("ana", "20");
		futbolFemenino = club.equipo("Fútbol Femenino", "Fútbol", "Mayores", 20);
		adm.crearJugador("ana", new JugadorConjunto("tmp", null, "Delantera", 9, futbolFemenino.getIdEquipo(), "Fútbol"));
		adm.crearJugador("ana", new JugadorIndividual("tmp", null, "tenis", "Intermedio", 1000));
	}

	private String jugadorDeBaja;
	private String partidoBasket;
	private Equipo baloncestoA;

	private void bajaDeJugadorConEstadisticas() throws Exception {
		baloncestoA = club.equipo("Baloncesto A", "Baloncesto", "Mayores", 20);
		Equipo b = club.equipo("Baloncesto B", "Baloncesto", "Mayores", 20);
		String[] ia = socios(baloncestoA, "20", "a1", "a2", "a3", "a4", "a5");
		String[] ib = socios(b, "20", "b1", "b2", "b3", "b4", "b5");
		adm.programarPartidoInterno(baloncestoA.getIdEquipo(), b.getIdEquipo(), LocalDateTime.of(2026, 10, 1, 18, 0), ia, ib);
		partidoBasket = club.central.getPartidos().getPartidos().get(0).getIdPartido();
		jugadorDeBaja = ia[0];
		adm.registrarEstadistica(partidoBasket, jugadorDeBaja, new EstadisticaBasket(jugadorDeBaja, 20, 7));
		adm.darDeBajaJugador(jugadorDeBaja);
	}

	private String inventario() throws Exception {
		adm.agregarInstalacion("tenis", 4, false);
		adm.agregarInstalacion("mesa", 4, true);
		adm.agregarArticulo("Tienda", new ArticuloTienda("Balón Fútbol", 80000, "Fútbol"));
		adm.agregarArticulo("Tienda", new ArticuloTienda("Raqueta Tenis", 150000, "Tenis"));
		adm.agregarArticulo("Tienda", new ArticuloTienda("Raqueta Pádel", 200000, "Pádel"));
		adm.abastecer("Tienda", "Balón Fútbol", 10);
		adm.abastecer("Tienda", "Raqueta Tenis", 3);
		adm.agregarArticulo("Cafeteria", new Bebida("Café", 5000, true));
		adm.agregarArticulo("Cafeteria", new Snack("Barra de granola", 6000, new String[] { "maní" }));
		adm.abastecer("Cafeteria", "Café", 10);
		adm.abastecer("Cafeteria", "Barra de granola", 10);
		return adm.consultarInventario();
	}

	private static String linea(String reporte, String inicio) {
		return reporte.lines().filter(l -> l.startsWith(inicio)).findFirst().orElseThrow();
	}

	private ArticuloTienda balon;

	private void balonDiezYDos() throws Exception {
		balon = new ArticuloTienda("Balón Fútbol", 80000, "Fútbol");
		adm.agregarArticulo("Tienda", balon);
		adm.abastecer("Tienda", "Balón Fútbol", 12);
		balon.moverUnidades(Articulo.UBICACION_BODEGA, "Vitrina", 2);
		assertEquals(10, balon.getUnidadesEn("Bodega"));
		assertEquals(2, balon.getUnidadesEn("Vitrina"));
	}

	private void inventarioSinCambios() {
		assertEquals(5, balon.getUnidadesEn("Bodega"));
		assertEquals(7, balon.getUnidadesEn("Vitrina"));
		assertEquals(12, balon.getDisponibilidad());
	}

	private void abastecerRaquetaYGranola() throws Exception {
		adm.agregarArticulo("Tienda", new ArticuloTienda("Raqueta Pádel", 200000, "Pádel"));
		adm.agregarArticulo("Cafeteria", new Snack("Barra de granola", 6000, new String[] { "maní" }));
		adm.abastecer("Cafeteria", "Barra de granola", 2);
		adm.abastecer("Tienda", "Raqueta Pádel", 15);
		adm.abastecer("Cafeteria", "Barra de granola", 20);
	}

	private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
	private Fisioterapeuta laura;
	private Equipo equipoSesion;

	private void personalConCobertura() throws Exception {
		for (String nombre : List.of("e1", "e2")) {
			club.usuario(new Entrenador(nombre, Club.PW, "C-" + nombre)).setTurno(null, Club.horas(8, 19));
		}
		club.usuario(new Entrenador("mario", Club.PW, "C-mario"));
		laura = club.usuario(new Fisioterapeuta("laura", Club.PW, "C-laura"));
		club.usuario(new Fisioterapeuta("f2", Club.PW, "C-f2"));
		adm.asignarTurno("laura", LUNES, Club.horas(8, 11));
		adm.asignarTurno("f2", LUNES, Club.horas(12, 19));
		adm.agregarInstalacion("grupal", 30, true);
		equipoSesion = club.equipo("Fútbol Sub-17 A", "Fútbol", "Sub-17", 20);
	}

	private void crearSesion(ControladorEntrenador mario, int hora) throws Exception {
		mario.crearSesionEntrenamiento("equipo", LUNES, hora, 60, "INS-1", new String[] { equipoSesion.getIdEquipo() });
	}

	private static final LocalDate OCT1 = LocalDate.of(2026, 10, 1);
	private static final LocalDate OCT31 = LocalDate.of(2026, 10, 31);
	private String[] futbolMayores;

	private void datosReporteDeportivo() throws Exception {
		Equipo mayores = club.equipo("Fútbol Mayores", "Fútbol", "Mayores", 20);
		futbolMayores = socios(mayores, "20", "pedro", "luis", "carlos", "m4", "m5", "m6", "m7", "m8", "m9", "m10", "m11", "m12");
		adm.crearCompetencia("ASCUN", "ASCUN Fútbol 2026", "ASCUN", "2026-2", "Fútbol", "Mayores", "Grupos y eliminación directa", 12, 2);
		adm.inscribirEquipoACompetencia("COMP-1", mayores.getIdEquipo(), futbolMayores);
		adm.agregarEquipoAfuera("COMP-1", "Universidad Nacional");
		adm.agregarEquipoAfuera("COMP-1", "Javeriana");
		String[] once = Arrays.copyOf(futbolMayores, 11);
		adm.registrarPartidoCompetencia("COMP-1", LocalDateTime.of(2026, 10, 20, 15, 0), "Universidad Nacional", "Estadio Alfonso López", once);
		String p1 = ultimoPartido("COMP-1");
		adm.registrarMarcador(p1, 3, 1);
		adm.registrarEstadistica(p1, futbolMayores[0], new EstadisticaFutbol(futbolMayores[0], 2, 0));
		adm.registrarEstadistica(p1, futbolMayores[1], new EstadisticaFutbol(futbolMayores[1], 1, 1));
		adm.registrarEstadistica(p1, futbolMayores[2], new EstadisticaFutbol(futbolMayores[2], 0, 1));
		adm.registrarAmonestacion(p1, futbolMayores[2], 1, 0);
		adm.registrarPartidoCompetencia("COMP-1", LocalDateTime.of(2026, 10, 27, 15, 0), "Javeriana", "Estadio Javeriana", once);
		adm.registrarMarcador(ultimoPartido("COMP-1"), 0, 2);
		assertEquals(3, club.central.getCompetencias().getCompetencia("COMP-1").getEquipoParticipante(mayores.getIdEquipo()).getPuntos());

		Entrenador entrenador = club.usuario(new Entrenador("mario", Club.PW, "C-mario"));
		Instalacion grupal = new Instalacion("G1", "grupal", 30, true);
		String[] estados = { "asistió", "asistió", "faltó", "asistió" };
		ControladorEntrenador ctrl = club.login("mario");
		for (int i = 0; i < 4; i++) {
			LocalDate dia = LocalDate.of(2026, 10, 5 + i);
			SesionEntrenamiento sesion = club.central.getSesionesEntrenamiento().crearSesionEquipo(entrenador, dia, 16, 60,
					new Reserva("R" + i, grupal, dia, new int[] { 16 }, "mario"), mayores.getIdEquipo());
			ctrl.registrarAsistencia(sesion.getIdSesion(), futbolMayores[0], estados[i]);
		}

		adm.agregarInstalacion("tenis", 4, false);
		String ana = jugadorRaqueta("ana", "tenis", "Intermedio");
		String sofia = jugadorRaqueta("sofia", "tenis", "Intermedio");
		ControladorSocio socio = club.login("ana");
		socio.reservarInstalacion("INS-1", LocalDate.of(2026, 10, 10), new int[] { 10 }, "sencillos", new String[] { ana },
				new String[] { sofia });
		socio.registrarResultadoPartidoPractica(practicas().get(0).getIdPartido(),
				List.<Estadistica>of(new EstadisticaTenisPadel(ana, 2, 12, 3, 5), new EstadisticaTenisPadel(sofia, 0, 7, 1, 9)));
	}

	private void cuatroMovimientos() throws Exception {
		Socio socio = club.socio("ana", "20");
		Entrenador entrenador = club.usuario(new Entrenador("mario", Club.PW, "ENT-0042"));
		club.central.getTienda().agregarVenta(new VentaTienda("VT-1", socio, List.of(new ArticuloTienda("Balón Fútbol", 80000, "Fútbol")),
				new int[] { 2 }, LocalDate.of(2026, 10, 5), 0, 0, 30400));
		club.central.getTienda().agregarVenta(new VentaTienda("VT-2", entrenador, List.of(new ArticuloTienda("Raqueta", 200000, "Tenis")),
				new int[] { 1 }, LocalDate.of(2026, 10, 5), 30000, 0, 32300));
		club.central.getCafeteria().agregarVenta(new VentaCafeteria("VC-1", socio, List.of(new Bebida("Café", 5000, true)), new int[] { 1 },
				LocalDate.of(2026, 10, 7), 500, 0, 400));
		adm.registrarPagoMensualidad("ana", 150000, LocalDate.of(2026, 10, 20));
	}

	private String reporteVentas(LocalDate inicio, LocalDate fin) throws Exception {
		return adm.generarReporte("ventas", null, inicio, fin);
	}

	private void sugerenciasDeEmpleados() throws Exception {
		club.usuario(new Entrenador("mario", Club.PW, "C-mario"));
		club.usuario(new Fisioterapeuta("laura", Club.PW, "C-laura"));
		((ControladorEmpleado) club.login("mario")).sugerirArticulo("Tienda", new ArticuloTienda("Tobillera", 40000, "Protección"));
		((ControladorEmpleado) club.login("laura")).sugerirArticulo("Cafeteria", new Bebida("Té frío", 4000, false));
	}

	private static final LocalDate SABADO = LocalDate.of(2026, 10, 17);
	private Instalacion padel;
	private String[] padelistas;
	private Reserva reservaPartido;
	private String idPartidoPadel;
	private String idSesionPadel;
	private ControladorSocio socioPadel;

	private void cambioDeHorarioPadel() throws Exception {
		adm.agregarInstalacion("padel", 4, true);
		padel = club.central.getInstalaciones().getInstalacion("INS-1");
		padelistas = new String[] { jugadorRaqueta("p1", "padel", "Avanzado"), jugadorRaqueta("p2", "padel", "Avanzado"),
				jugadorRaqueta("p3", "padel", "Avanzado"), jugadorRaqueta("p4", "padel", "Avanzado") };
		socioPadel = club.login("p1");
		socioPadel.reservarInstalacion("INS-1", SABADO, new int[] { 20 }, "dobles", new String[] { padelistas[0], padelistas[1] },
				new String[] { padelistas[2], padelistas[3] });
		assertArrayEquals(Club.horas(6, 21), padel.getHorario().get(SABADO));
		reservaPartido = club.central.getReservas().getReservas().get(0);
		idPartidoPadel = practicas().get(0).getIdPartido();
		club.usuario(new Entrenador("mario", Club.PW, "C-mario"));
		adm.asignarTurno("mario", SABADO, Club.horas(21, 21));
		((ControladorEntrenador) club.login("mario")).crearSesionEntrenamiento("raqueta", SABADO, 21, 60, "INS-1",
				new String[] { padelistas[0] });
		idSesionPadel = club.central.getSesionesEntrenamiento().getSesiones().get(0).getIdSesion();
		adm.cambiarHorarioInstalacion("INS-1", SABADO, Club.horas(7, 19));
	}

	private void mesaYTenisConReserva() throws Exception {
		adm.agregarInstalacion("mesa", 4, true);
		adm.agregarInstalacion("tenis", 4, false);
		String ana = jugadorRaqueta("ana", "tenis", "Intermedio");
		String sofia = jugadorRaqueta("sofia", "tenis", "Intermedio");
		((ControladorSocio) club.login("ana")).reservarInstalacion("INS-2", LocalDate.now().plusMonths(2), new int[] { 10 }, "sencillos",
				new String[] { ana }, new String[] { sofia });
	}

	private String[] jugadores(String disciplina, int n) throws Exception {
		String[] ids = new String[n];
		for (int i = 0; i < n; i++) {
			ids[i] = jugadorRaqueta("j" + i, disciplina, "Avanzado");
		}
		return ids;
	}

	private String anaTenis, sofiaTenis;
	private String[] sub17A;

	private String historialSeptiembre() throws Exception {
		adm.agregarInstalacion("tenis", 4, false);
		adm.agregarInstalacion("grupal", 40, true);
		anaTenis = jugadorRaqueta("ana", "tenis", "Intermedio");
		sofiaTenis = jugadorRaqueta("sofia", "tenis", "Intermedio");
		((ControladorSocio) club.login("ana")).reservarInstalacion("INS-1", LocalDate.of(2026, 9, 20), new int[] { 10 }, "sencillos",
				new String[] { anaTenis }, new String[] { sofiaTenis });

		Equipo a = club.equipo("Fútbol Sub-17 A", "Fútbol", "Sub-17", 20);
		Equipo b = club.equipo("Fútbol Sub-17 B", "Fútbol", "Sub-17", 20);
		sub17A = club.llenar(a, "a", "16", 7);
		String[] ib = club.llenar(b, "b", "16", 7);
		adm.programarPartidoInterno(a.getIdEquipo(), b.getIdEquipo(), LocalDateTime.of(2026, 9, 21, 15, 0), sub17A, ib);
		adm.registrarMarcador(club.central.getPartidos().getPartidosEntre(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 21)).get(0)
				.getIdPartido(), 3, 1);

		club.usuario(new Entrenador("mario", Club.PW, "C-mario")).setTurno(null, Club.horas(8, 19));
		ControladorEntrenador entrenador = club.login("mario");
		entrenador.crearSesionEntrenamiento("equipo", LocalDate.of(2026, 9, 22), 16, 90, "INS-2", new String[] { a.getIdEquipo() });
		String idSesion = club.central.getSesionesEntrenamiento().getSesiones().get(0).getIdSesion();
		for (int i = 0; i < 3; i++) {
			entrenador.registrarAsistencia(idSesion, sub17A[i], "asistió");
		}

		adm.crearCompetencia("ASCUN", "ASCUN Sub-17", "ASCUN", "2026-2", "Fútbol", "Sub-17", "Liga", 7, 2);
		adm.inscribirEquipoACompetencia("COMP-1", a.getIdEquipo(), sub17A);
		adm.agregarEquipoAfuera("COMP-1", "Universidad Nacional");
		adm.registrarPartidoCompetencia("COMP-1", LocalDateTime.of(2026, 9, 27, 15, 0), "Universidad Nacional", "Estadio Alfonso López", sub17A);
		adm.registrarMarcador(ultimoPartido("COMP-1"), 2, 0);
		return adm.consultarHistorial("todo", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
	}

	private Equipo fa, fb, ba, bb, va, vb, f15;
	private String[] ifa, ifb, iba, ibb, iva, ivb;
	private static final LocalDateTime FECHA_A17 = LocalDateTime.of(2026, 10, 15, 15, 0);

	private void equiposSub17() throws Exception {
		fa = club.equipo("Fútbol Sub-17 A", "Fútbol", "Sub-17", 20);
		fb = club.equipo("Fútbol Sub-17 B", "Fútbol", "Sub-17", 20);
		ba = club.equipo("Baloncesto Sub-17 A", "Baloncesto", "Sub-17", 20);
		bb = club.equipo("Baloncesto Sub-17 B", "Baloncesto", "Sub-17", 20);
		va = club.equipo("Voleibol Sub-17 A", "Voleibol", "Sub-17", 20);
		vb = club.equipo("Voleibol Sub-17 B", "Voleibol", "Sub-17", 20);
		f15 = club.equipo("Fútbol Sub-15", "Fútbol", "Sub-15", 20);
		ifa = new String[8];
		for (int i = 0; i < 7; i++) {
			club.socio("fa" + (i + 1), "16");
			ifa[i] = club.conjunto("fa" + (i + 1), fa, i + 1);
		}
		club.socio("pedro", "16");
		ifa[7] = club.conjunto("pedro", fa, 9);
		ifb = socios(fb, "16", "luis", "carlos", "mario", "andres", "fb5", "fb6", "fb7", "fb8", "fb9");
		iba = socios(ba, "16", "ba1", "ba2", "ba3", "ba4", "ba5", "ba6");
		ibb = socios(bb, "16", "bb1", "bb2", "bb3", "bb4", "bb5", "bb6");
		iva = socios(va, "16", "va1", "va2", "va3", "va4", "va5", "va6", "va7");
		ivb = socios(vb, "16", "vb1", "vb2", "vb3", "vb4", "vb5", "vb6", "vb7");
	}

	private String pedro() {
		return ifa[7];
	}

	private String[] habilitadosA() {
		return new String[] { ifa[0], ifa[1], ifa[2], ifa[3], ifa[4], ifa[5], pedro() };
	}

	private String[] habilitadosB() {
		return Arrays.copyOf(ifb, 7);
	}

	private String ultimoInterno() {
		return club.central.getPartidos().getPartidos().stream().filter(p -> p instanceof PartidoInterno && !p.getCerrado())
				.map(Partido::getIdPartido).findFirst().orElseThrow();
	}

	private String programarFutbol() throws Exception {
		adm.programarPartidoInterno(fa.getIdEquipo(), fb.getIdEquipo(), FECHA_A17, habilitadosA(), habilitadosB());
		return ultimoInterno();
	}

	private String marcadorYEstadisticas() throws Exception {
		equiposSub17();
		String id = programarFutbol();
		adm.registrarMarcador(id, 2, 1);
		for (String j : habilitadosA()) {
			adm.registrarEstadistica(id, j, new EstadisticaFutbol(j, j.equals(pedro()) ? 2 : 0, 0));
		}
		for (String j : habilitadosB()) {
			adm.registrarEstadistica(id, j, new EstadisticaFutbol(j, j.equals(ifb[0]) ? 1 : 0, 0));
		}
		return id;
	}

	private String tarjetas() throws Exception {
		String id = marcadorYEstadisticas();
		adm.registrarAmonestacion(id, ifb[1], 2, 0);
		adm.registrarAmonestacion(id, ifb[2], 0, 1);
		adm.registrarAmonestacion(id, ifb[3], 1, 0);
		return id;
	}

	private String[] bSinSuspendidos() {
		return new String[] { ifb[0], ifb[3], ifb[4], ifb[5], ifb[6], ifb[7], ifb[8] };
	}

	private String partidoBaloncesto() throws Exception {
		equiposSub17();
		adm.programarPartidoInterno(ba.getIdEquipo(), bb.getIdEquipo(), FECHA_A17, Arrays.copyOf(iba, 5), Arrays.copyOf(ibb, 5));
		return ultimoInterno();
	}

	private String partidoVoleibol() throws Exception {
		equiposSub17();
		adm.programarPartidoInterno(va.getIdEquipo(), vb.getIdEquipo(), FECHA_A17, Arrays.copyOf(iva, 6), Arrays.copyOf(ivb, 6));
		return ultimoInterno();
	}

	private String[] mayores;
	private String idMayores;
	private Equipo baloncestoMayores;

	private String ultimoPartido(String idCompetencia) {
		List<PartidoCompetencia> partidos = club.central.getCompetencias().getCompetencia(idCompetencia).getPartidos();
		return partidos.get(partidos.size() - 1).getIdPartido();
	}

	private CompetenciaASCUN ascun() {
		return (CompetenciaASCUN) club.central.getCompetencias().getCompetencia("COMP-1");
	}

	private String pedroA18() {
		return mayores[0];
	}

	private String luisA18() {
		return mayores[1];
	}

	private String carlosA18() {
		return mayores[2];
	}

	private void a18Paso1() throws Exception {
		adm.crearCompetencia("ASCUN", "ASCUN Fútbol 2026", "ASCUN", "2026-2", "Fútbol", "Mayores", "Grupos y eliminación directa", 12, 2);
		adm.crearCompetencia("Cerros", "Torneo de los Cerros 2026", "Cerros", "2026-2", "Baloncesto", "Mayores", "Liga", 8, 2);
		Equipo futbol = club.equipo("Fútbol Mayores", "Fútbol", "Mayores", 20);
		idMayores = futbol.getIdEquipo();
		mayores = socios(futbol, "22", "pedro", "luis", "carlos", "m4", "m5", "m6", "m7", "m8", "m9", "m10", "m11", "m12", "m13", "m14", "m15");
		baloncestoMayores = club.equipo("Baloncesto Mayores", "Baloncesto", "Mayores", 20);
		socios(baloncestoMayores, "22", "bm1", "bm2", "bm3", "bm4", "bm5", "bm6", "bm7", "bm8", "bm9", "bm10");
	}

	private void a18Paso2() throws Exception {
		a18Paso1();
		adm.inscribirEquipoACompetencia("COMP-1", idMayores, Arrays.copyOf(mayores, 14));
	}

	private void a18Paso3() throws Exception {
		a18Paso2();
		for (String rival : List.of("Universidad Nacional", "Javeriana", "Rosario")) {
			adm.agregarEquipoAfuera("COMP-1", rival);
		}
		adm.crearGrupo("COMP-1", "A");
		for (String id : List.of(idMayores, "Universidad Nacional", "Javeriana", "Rosario")) {
			adm.agregarEquipoAGrupo("COMP-1", "A", id);
		}
	}

	private String a18Paso4() throws Exception {
		a18Paso3();
		adm.registrarPartidoCompetencia("COMP-1", LocalDateTime.of(2026, 10, 20, 15, 0), "Universidad Nacional", "Estadio Alfonso López",
				Arrays.copyOf(mayores, 11));
		String id = ultimoPartido("COMP-1");
		adm.registrarEstadistica(id, pedroA18(), new EstadisticaFutbol(pedroA18(), 2, 0));
		adm.registrarEstadistica(id, luisA18(), new EstadisticaFutbol(luisA18(), 1, 1));
		adm.registrarAmonestacion(id, carlosA18(), 1, 0);
		adm.registrarMarcador(id, 3, 1);
		return id;
	}

	private void a18Paso5() throws Exception {
		a18Paso4();
		adm.registrarPartidoCompetencia("COMP-1", LocalDateTime.of(2026, 10, 27, 15, 0), "Javeriana", "Estadio Javeriana",
				Arrays.copyOf(mayores, 11));
		String id = ultimoPartido("COMP-1");
		adm.registrarEstadistica(id, pedroA18(), new EstadisticaFutbol(pedroA18(), 1, 0));
		adm.registrarAmonestacion(id, carlosA18(), 1, 0);
		adm.registrarMarcador(id, 1, 1);
	}

	private void a18Paso6() throws Exception {
		a18Paso5();
		adm.registrarPartidoCompetencia("COMP-1", LocalDateTime.of(2026, 11, 3, 15, 0), "Rosario", "Estadio Rosario",
				Arrays.copyOfRange(mayores, 3, 14));
		adm.registrarMarcador(ultimoPartido("COMP-1"), 0, 1);
		ascun().getEquipoParticipante("Rosario").sumarPuntos(1);
		ascun().getEquipoParticipante("Universidad Nacional").sumarPuntos(1);
		ascun().getEquipoParticipante("Universidad Nacional").sumarPuntos(3);
		ascun().getEquipoParticipante("Rosario").sumarPuntos(3);
	}

	@Nested
	@DisplayName("RF-A01 Crear Equipos")
	class RfA01 {
		@Test
		@DisplayName("RF-A01 / HU-ADM-01 – caso 1: El Administrador inicia sesión con su login y password")
		void rfA01_caso1_inicioSesion() throws Exception {
			assertInstanceOf(ControladorAdministrador.class, club.central.iniciarSesion("admin", Club.PW));
		}

		@Test
		@DisplayName("RF-A01 / HU-ADM-01 – caso 2: Se crea el Equipo \"Fútbol Masculino\" (Fútbol, Sub-17, cupo 20)")
		void rfA01_caso2_crear() throws Exception {
			creaFutbolMasculino();
			Equipo equipo = equipo("Fútbol Masculino");
			assertEquals("Fútbol", equipo.getDeporte());
			assertEquals("Sub-17", equipo.getCategoria());
			assertEquals(20, equipo.getCupo());
		}

		@Test
		@DisplayName("RF-A01 / HU-ADM-01 – caso 3: El equipo existe con la lista de jugadores vacía")
		void rfA01_caso3_sinJugadores() throws Exception {
			creaFutbolMasculino();
			assertEquals(0, equipo("Fútbol Masculino").getJugadoresIds().length);
		}

		@Test
		@DisplayName("RF-A01 / HU-ADM-01 – caso 4: El equipo se agrega a ClubSeneca.equipos")
		void rfA01_caso4_enElClub() throws Exception {
			creaFutbolMasculino();
			assertTrue(club.central.getEquipos().getEquipos().contains(equipo("Fútbol Masculino")));
		}

		@Test
		@DisplayName("RF-A01 / HU-ADM-01 – caso 5: Se rechaza otro equipo con el mismo nombre, deporte y categoría")
		void rfA01_caso5_duplicado() throws Exception {
			creaFutbolMasculino();
			assertThrows(ElementoDuplicadoException.class, () -> adm.crearEquipo("Fútbol Masculino", "Fútbol", "Sub-17", 20));
			assertEquals(1, club.central.getEquipos().getEquipos().size());
		}

		@Test
		@DisplayName("RF-A01 / HU-ADM-01 – caso 5: Se rechaza un equipo con cupo=0")
		void rfA01_caso5_cupoCero() {
			assertThrows(ValorInvalidoException.class, () -> adm.crearEquipo("Fútbol Masculino", "Fútbol", "Sub-17", 0));
			assertTrue(club.central.getEquipos().getEquipos().isEmpty());
		}
	}

	@Nested
	@DisplayName("RF-A02 Dar de Baja Equipos")
	class RfA02 {
		@Test
		@DisplayName("RF-A02 / HU-ADM-01 – caso 3: El equipo ya no aparece en ClubSeneca.equipos")
		void rfA02_caso3_noApareceEnEquipos() throws Exception {
			Equipo femenino = voleibolDadoDeBaja();
			assertFalse(club.central.getEquipos().getEquipos().contains(femenino));
			assertNull(equipo("Voleibol Femenino"));
		}

		@Test
		@DisplayName("RF-A02 / HU-ADM-01 – caso 3: Sus 2 jugadores salen del jugandoComo de sus socios")
		void rfA02_caso3_jugadoresSalenDeSusSocios() throws Exception {
			voleibolDadoDeBaja();
			for (String username : List.of("vale", "vero")) {
				assertEquals(0, ((Socio) club.central.getUsuarios().getUsuario(username)).getJugandoComo().length, username);
			}
		}

		@Test
		@DisplayName("RF-A02 / HU-ADM-01 – caso 4: El partido interno no se elimina: el equipo se reemplaza por el equipo anónimo (único)")
		void rfA02_caso4_equipoAnonimo() throws Exception {
			voleibolDadoDeBaja();
			PartidoInterno partido = (PartidoInterno) club.central.getPartidos().getPartido("PI-1");
			assertNotNull(partido);
			assertSame(club.central.getEquipos().getEquipoAnonimo(), partido.getEquipo1());
			assertSame(club.central.getEquipos().getEquipoAnonimo(), club.central.getEquipos().getEquipoAnonimo());
		}

		@Test
		@DisplayName("RF-A02 / HU-ADM-01 – caso 5: Se rechaza dar de baja un equipo inexistente")
		void rfA02_caso5_inexistente() {
			assertThrows(ElementoNoEncontradoException.class, () -> adm.darDeBajaEquipo("EQ-999"));
		}
	}

	@Nested
	@DisplayName("RF-A03 Crear Jugadores")
	class RfA03 {
		@Test
		@DisplayName("RF-A03 / HU-ADM-01 – caso 1: El Socio Ana (edad=20) tiene 0 jugadores en jugandoComo")
		void rfA03_caso1_sinJugadores() throws Exception {
			assertEquals(0, club.socio("ana", "20").getJugandoComo().length);
		}

		@Test
		@DisplayName("RF-A03 / HU-ADM-01 – caso 2: Ana tiene 2 jugadores y el JugadorConjunto aparece en los jugadores de \"Fútbol Femenino\"")
		void rfA03_caso2_dosJugadores() throws Exception {
			anaConDosJugadores();
			assertEquals(2, ana.getJugandoComo().length);
			JugadorConjunto conjunto = (JugadorConjunto) club.jugador(futbolFemenino.getJugadoresIds()[0]);
			assertSame(ana, conjunto.getUsuario());
			assertEquals("Delantera", conjunto.getPosicion());
			assertEquals(9, conjunto.getCamiseta());
			JugadorIndividual individual = club.central.getJugadores().getJugadorIndividual("ana", "tenis");
			assertEquals("Intermedio", individual.getNivel());
			assertEquals(1000, individual.getElo());
		}

		@Test
		@DisplayName("RF-A03 / HU-ADM-01 – caso 3: Se rechaza crearle a Ana otro jugador de fútbol (un equipo por deporte)")
		void rfA03_caso3_otroDeFutbol() throws Exception {
			anaConDosJugadores();
			Equipo otro = club.equipo("Fútbol Femenino B", "Fútbol", "Mayores", 20);
			assertThrows(EquipoPorDeporteException.class,
					() -> adm.crearJugador("ana", new JugadorConjunto("tmp", null, "Defensa", 4, otro.getIdEquipo(), "Fútbol")));
		}

		@Test
		@DisplayName("RF-A03 / HU-ADM-01 – caso 3: Se rechaza la camiseta 9 repetida en \"Fútbol Femenino\"")
		void rfA03_caso3_camisetaRepetida() throws Exception {
			anaConDosJugadores();
			club.socio("eva", "22");
			assertThrows(CamisetaRepetidaException.class,
					() -> adm.crearJugador("eva", new JugadorConjunto("tmp", null, "Defensa", 9, futbolFemenino.getIdEquipo(), "Fútbol")));
		}

		@Test
		@DisplayName("RF-A03 / HU-ADM-01 – caso 3: Se rechaza un jugador en un equipo que ya llegó a su cupo")
		void rfA03_caso3_cupo() throws Exception {
			anaConDosJugadores();
			Equipo lleno = club.equipo("Baloncesto Femenino", "Baloncesto", "Mayores", 1);
			socios(lleno, "22", "lina");
			club.socio("eva", "22");
			assertThrows(CupoEquipoExcedidoException.class,
					() -> adm.crearJugador("eva", new JugadorConjunto("tmp", null, "Base", 5, lleno.getIdEquipo(), "Baloncesto")));
		}

		@Test
		@DisplayName("RF-A03 / HU-ADM-01 – caso 3: Se rechaza a Ana en un equipo Sub-17 (categoría no corresponde a la edad)")
		void rfA03_caso3_categoria() throws Exception {
			anaConDosJugadores();
			Equipo sub17 = club.equipo("Voleibol Sub-17", "Voleibol", "Sub-17", 20);
			assertThrows(CategoriaEdadException.class,
					() -> adm.crearJugador("ana", new JugadorConjunto("tmp", null, "Líbero", 1, sub17.getIdEquipo(), "Voleibol")));
		}
	}

	@Nested
	@DisplayName("RF-A04 Dar de Baja Jugadores")
	class RfA04 {
		@Test
		@DisplayName("RF-A04 / HU-ADM-01 – caso 3: El jugador sale de los jugadores del equipo y del jugandoComo del socio")
		void rfA04_caso3_saleDeEquipoYSocio() throws Exception {
			bajaDeJugadorConEstadisticas();
			assertFalse(Arrays.asList(baloncestoA.getJugadoresIds()).contains(jugadorDeBaja));
			assertEquals(0, ((Socio) club.central.getUsuarios().getUsuario("a1")).getJugandoComo().length);
		}

		@Test
		@DisplayName("RF-A04 / HU-ADM-01 – caso 4: En los partidos que jugó queda reemplazado por el jugador anónimo, único en el sistema")
		void rfA04_caso4_jugadorAnonimo() throws Exception {
			bajaDeJugadorConEstadisticas();
			PartidoInterno partido = (PartidoInterno) club.central.getPartidos().getPartido(partidoBasket);
			assertFalse(partido.estaHabilitado(jugadorDeBaja));
			assertTrue(partido.estaHabilitado(CentralJugadores.ID_ANONIMO));
			assertEquals(CentralJugadores.ID_ANONIMO, partido.getEstadisticas().get(0).getUsuarioUsername());
			assertSame(club.central.getJugadores().getJugadorAnonimo(), club.central.getJugadores().getJugadorAnonimo());
		}
	}

	@Nested
	@DisplayName("RF-A05 Consultar inventario")
	class RfA05 {
		@Test
		@DisplayName("RF-A05 / HU-ADM-02 – caso 3: El reporte muestra cada instalación (tipo, capacidades por modalidad, techada, horario) y cada artículo")
		void rfA05_caso3_reporte() throws Exception {
			String reporte = inventario();
			String tenis = linea(reporte, "INS-1");
			String mesa = linea(reporte, "INS-2");
			assertTrue(tenis.contains("tipo: tenis") && tenis.contains("techada: no") && tenis.contains("horario"));
			assertTrue(mesa.contains("tipo: mesa") && mesa.contains("techada: sí") && mesa.contains("horario"));
			assertTrue(tenis.contains("sencillos") && tenis.contains("dobles"), "Capacidades por modalidad: " + tenis);
			String balon = linea(reporte, "Balón Fútbol");
			assertTrue(balon.contains("categoria: Fútbol") && balon.contains("precio: 80000") && balon.contains("disponibilidad: 10"));
			assertTrue(linea(reporte, "Café").contains("precio: 5000"));
			assertTrue(linea(reporte, "Barra de granola").contains("precio: 6000"));
		}

		@Test
		@DisplayName("RF-A05 / HU-ADM-02 – caso 4: Un artículo con disponibilidad=0 aparece marcado como agotado")
		void rfA05_caso4_agotado() throws Exception {
			String reporte = inventario();
			assertTrue(linea(reporte, "Raqueta Pádel").contains("AGOTADO"));
			assertFalse(linea(reporte, "Balón Fútbol").contains("AGOTADO"));
		}
	}

	@Nested
	@DisplayName("RF-A06 Mover Artículos")
	class RfA06 {
		@Test
		@DisplayName("RF-A06 / HU-ADM-02 – casos 2 y 3: Al mover 5 unidades el origen queda con 5, el destino con 7 y el total no cambia")
		void rfA06_caso3_mover() throws Exception {
			balonDiezYDos();
			adm.moverArticulo("Balón Fútbol", "Bodega", "Vitrina", 5);
			assertEquals(5, balon.getUnidadesEn("Bodega"));
			assertEquals(7, balon.getUnidadesEn("Vitrina"));
			assertEquals(12, balon.getDisponibilidad());
		}

		@Test
		@DisplayName("RF-A06 / HU-ADM-02 – caso 4: Se rechaza mover 8 unidades desde un origen que tiene 5")
		void rfA06_caso4_insuficiente() throws Exception {
			balonDiezYDos();
			adm.moverArticulo("Balón Fútbol", "Bodega", "Vitrina", 5);
			assertThrows(InventarioInsuficienteException.class, () -> adm.moverArticulo("Balón Fútbol", "Bodega", "Vitrina", 8));
			inventarioSinCambios();
		}

		@Test
		@DisplayName("RF-A06 / HU-ADM-02 – caso 4: Se rechaza mover un artículo que no existe")
		void rfA06_caso4_articuloInexistente() throws Exception {
			balonDiezYDos();
			adm.moverArticulo("Balón Fútbol", "Bodega", "Vitrina", 5);
			assertThrows(ElementoNoEncontradoException.class, () -> adm.moverArticulo("Balón Voleibol", "Bodega", "Vitrina", 1));
			inventarioSinCambios();
		}

		@Test
		@DisplayName("RF-A06 / HU-ADM-02 – caso 4: Se rechaza mover a una ubicación inexistente")
		void rfA06_caso4_ubicacionInexistente() throws Exception {
			balonDiezYDos();
			adm.moverArticulo("Balón Fútbol", "Bodega", "Vitrina", 5);
			assertThrows(ElementoNoEncontradoException.class, () -> adm.moverArticulo("Balón Fútbol", "Bodega", "Ubicación inventada", 1));
			inventarioSinCambios();
		}
	}

	@Nested
	@DisplayName("RF-A07 Abastecer La Tienda y Cafetería")
	class RfA07 {
		@Test
		@DisplayName("RF-A07 / HU-ADM-02 – caso 3: La raqueta queda con 15 y se puede volver a comprar; el snack queda con 22")
		void rfA07_caso3_abastecer() throws Exception {
			abastecerRaquetaYGranola();
			assertEquals(15, club.central.getTienda().getArticulo("Raqueta Pádel").getDisponibilidad());
			assertEquals(22, club.central.getCafeteria().getArticulo("Barra de granola").getDisponibilidad());
			club.socio("ana", "20");
			((ControladorSocio) club.login("ana")).comprar("Tienda", new String[] { "Raqueta Pádel" }, new int[] { 1 }, null, 0, 0);
			assertEquals(14, club.central.getTienda().getArticulo("Raqueta Pádel").getDisponibilidad());
		}

		@Test
		@DisplayName("RF-A07 / HU-ADM-02 – caso 4: El artículo nuevo \"Grip\" (Accesorios, 12000) se agrega a Tienda.inventario")
		void rfA07_caso4_articuloNuevo() throws Exception {
			abastecerRaquetaYGranola();
			adm.agregarArticulo("Tienda", new ArticuloTienda("Grip", 12000, "Accesorios"));
			ArticuloTienda grip = club.central.getTienda().getArticulo("Grip");
			assertTrue(club.central.getTienda().getInventario().contains(grip));
			assertEquals("Accesorios", grip.getCategoria());
			assertEquals(12000, grip.getPrecio());
		}

		@Test
		@DisplayName("RF-A07 / HU-ADM-02 – caso 5: Se rechaza abastecer con una cantidad ≤ 0")
		void rfA07_caso5_cantidadInvalida() throws Exception {
			abastecerRaquetaYGranola();
			assertThrows(ValorInvalidoException.class, () -> adm.abastecer("Tienda", "Raqueta Pádel", 0));
			assertThrows(ValorInvalidoException.class, () -> adm.abastecer("Cafeteria", "Barra de granola", -5));
			assertEquals(15, club.central.getTienda().getArticulo("Raqueta Pádel").getDisponibilidad());
		}
	}

	@Nested
	@DisplayName("RF-A08 Administrar Turnos")
	class RfA08 {
		@Test
		@DisplayName("RF-A08 / HU-ADM-04 – caso 1: 3 entrenadores y 2 fisioterapeutas; horario h8-h19; cada hora con ≥2 entrenadores y ≥1 fisioterapeuta")
		void rfA08_caso1_cobertura() throws Exception {
			personalConCobertura();
			assertEquals(3, club.central.getUsuarios().getEmpleados("Entrenador").size());
			assertEquals(2, club.central.getUsuarios().getEmpleados("Fisioterapeuta").size());
			assertArrayEquals(Club.horas(8, 19), club.central.getHorarioAtencion());
			assertTrue(new CentralCambioTurno().verificarCobertura(LUNES, club.central.getUsuarios().getEmpleados("Todos"),
					club.central.getHorarioAtencion()));
		}

		@Test
		@DisplayName("RF-A08 / HU-ADM-04 – casos 2 y 3: Con turno h8-h11, Mario se asigna a una sesión a las 9 y se rechaza a las 14")
		void rfA08_caso3_asignarTurno() throws Exception {
			personalConCobertura();
			adm.asignarTurno("mario", LUNES, Club.horas(8, 11));
			ControladorEntrenador mario = club.login("mario");
			crearSesion(mario, 9);
			assertEquals(1, club.central.getSesionesEntrenamiento().getSesiones().size());
			assertThrows(FueraDeTurnoException.class, () -> crearSesion(mario, 14));
		}

		@Test
		@DisplayName("RF-A08 / HU-ADM-04 – caso 4: Sin turno (todas las horas en false), Mario no se puede asignar a sesiones nuevas ese día")
		void rfA08_caso4_quitarTurno() throws Exception {
			personalConCobertura();
			adm.asignarTurno("mario", LUNES, Club.horas(8, 11));
			adm.asignarTurno("mario", LUNES, Club.horas(-1, -1));
			ControladorEntrenador mario = club.login("mario");
			assertThrows(FueraDeTurnoException.class, () -> crearSesion(mario, 9));
		}

		@Test
		@DisplayName("RF-A08 / HU-ADM-04 – caso 5: Una solicitud que deja 0 fisioterapeutas a las 10:00 se rechaza; queda \"rechazada\" y el turno no cambia")
		void rfA08_caso5_sinCobertura() throws Exception {
			personalConCobertura();
			((ControladorEmpleado) club.login("laura")).solicitarCambioTurno(LUNES, Club.horas(12, 15));
			SolicitudCambioTurno solicitud = adm.consultarSolicitudesCambioTurno().get(0);
			assertThrows(CoberturaTurnosException.class, () -> adm.resolverCambioTurno(solicitud.getIdSolicitud(), true));
			assertEquals("rechazada", solicitud.getEstado());
			assertArrayEquals(Club.horas(8, 11), laura.getTurno(LUNES));
		}

		@Test
		@DisplayName("RF-A08 / HU-ADM-04 – caso 6: Otra solicitud que mantiene la cobertura se aprueba y el turno se actualiza")
		void rfA08_caso6_conCobertura() throws Exception {
			personalConCobertura();
			((ControladorEmpleado) club.login("laura")).solicitarCambioTurno(LUNES, Club.horas(8, 15));
			SolicitudCambioTurno solicitud = adm.consultarSolicitudesCambioTurno().get(0);
			assertTrue(adm.resolverCambioTurno(solicitud.getIdSolicitud(), true));
			assertEquals("aprobada", solicitud.getEstado());
			assertArrayEquals(Club.horas(8, 15), laura.getTurno(LUNES));
		}
	}

	@Nested
	@DisplayName("RF-A09 Generar Reportes Deportivos")
	class RfA09 {
		@Test
		@DisplayName("RF-A09 / HU-ADM-05 – caso 3: El reporte de la competencia muestra resultados, tabla de puntos y goleadores ordenados")
		void rfA09_caso3_competencia() throws Exception {
			datosReporteDeportivo();
			String reporte = adm.generarReporte("deportivo", "COMP-1", OCT1, OCT31);
			assertTrue(reporte.contains("Fútbol Mayores 3 - 1 Universidad Nacional") && reporte.contains("Fútbol Mayores 0 - 2 Javeriana"));
			assertTrue(reporte.contains("Fútbol Mayores [") && reporte.contains("]: 3 puntos"));
			int pedro = reporte.indexOf("Pedro [" + futbolMayores[0] + "]: 2");
			int luis = reporte.indexOf("Luis [" + futbolMayores[1] + "]: 1");
			int carlos = reporte.indexOf("Carlos [" + futbolMayores[2] + "]: 0");
			assertTrue(pedro > 0 && pedro < luis && luis < carlos, reporte);
		}

		@Test
		@DisplayName("RF-A09 / HU-ADM-05 – caso 4: El reporte de asistencia muestra que Pedro tiene 75 % de asistencia")
		void rfA09_caso4_asistencia() throws Exception {
			datosReporteDeportivo();
			String reporte = adm.generarReporte("asistencia", null, OCT1, OCT31);
			assertTrue(reporte.contains("Pedro [" + futbolMayores[0] + "]: 3/4 sesiones"), reporte);
		}

		@Test
		@DisplayName("RF-A09 / HU-ADM-05 – caso 5: El reporte de rankings de tenis muestra la evolución de Ana: 1000 → 1016")
		void rfA09_caso5_evolucionElo() throws Exception {
			datosReporteDeportivo();
			String reporte = adm.generarReporte("asistencia", null, OCT1, OCT31);
			String lineaAna = reporte.lines().filter(l -> l.contains("Ana [")).findFirst().orElseThrow();
			assertTrue(lineaAna.contains("1016"));
			assertTrue(lineaAna.contains("1000"), "No se muestra la evolución: " + lineaAna);
		}

		@Test
		@DisplayName("RF-A09 / HU-ADM-05 – caso 6: Se rechaza el reporte de una competencia inexistente")
		void rfA09_caso6_competenciaInexistente() throws Exception {
			datosReporteDeportivo();
			assertThrows(ElementoNoEncontradoException.class, () -> adm.generarReporte("deportivo", "COMP-404", OCT1, OCT31));
		}
	}

	@Nested
	@DisplayName("RF-A10 Generar Reportes De Ventas")
	class RfA10 {
		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 0: Al inicio no hay ventas")
		void rfA10_caso0_sinVentas() throws Exception {
			assertTrue(reporteVentas(OCT1, OCT31).contains("Ventas: 0"));
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 1: Un socio compra 2 \"Balón Fútbol\": subtotal 160000, IVA 30400, total 190400")
		void rfA10_caso1_socioTienda() throws Exception {
			club.socio("ana", "20");
			adm.agregarArticulo("Tienda", new ArticuloTienda("Balón Fútbol", 80000, "Fútbol"));
			adm.abastecer("Tienda", "Balón Fútbol", 5);
			((ControladorSocio) club.login("ana")).comprar("Tienda", new String[] { "Balón Fútbol" }, new int[] { 2 }, null, 0, 0);
			VentaTienda venta = club.central.getTienda().getVentas().get(0);
			assertEquals(160000, venta.calcularSubtotal());
			assertEquals(30400, venta.getIva());
			assertEquals(190400, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 1: Un entrenador compra 1 \"Raqueta\" con su código (15 %): descuento 30000, subtotal 170000, IVA 32300, total 202300")
		void rfA10_caso1_entrenadorTienda() throws Exception {
			club.usuario(new Entrenador("mario", Club.PW, "ENT-0042"));
			adm.agregarArticulo("Tienda", new ArticuloTienda("Raqueta", 200000, "Tenis"));
			adm.abastecer("Tienda", "Raqueta", 5);
			((ControladorEntrenador) club.login("mario")).comprar("Tienda", new String[] { "Raqueta" }, new int[] { 1 }, "ENT-0042", 0, 0);
			VentaTienda venta = club.central.getTienda().getVentas().get(0);
			assertEquals(30000, venta.getDescuento());
			assertEquals(170000, venta.calcularSubtotal() - venta.getDescuento());
			assertEquals(32300, venta.getIva());
			assertEquals(202300, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 1: Un socio compra un café (5000): impuesto al consumo 400, propina 500, total 5900")
		void rfA10_caso1_socioCafeteria() throws Exception {
			club.socio("ana", "20");
			adm.agregarArticulo("Cafeteria", new Bebida("Café", 5000, true));
			adm.abastecer("Cafeteria", "Café", 5);
			((ControladorSocio) club.login("ana")).comprar("Cafeteria", new String[] { "Café" }, new int[] { 1 }, null, 500, 0);
			VentaCafeteria venta = club.central.getCafeteria().getVentas().get(0);
			assertEquals(400, venta.getIva());
			assertEquals(500, venta.getPropina());
			assertEquals(5900, venta.calcularTotal());
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 1: El 2026-10-20 se paga una mensualidad de 150000")
		void rfA10_caso1_mensualidad() throws Exception {
			club.socio("ana", "20");
			adm.registrarPagoMensualidad("ana", 150000, LocalDate.of(2026, 10, 20));
			assertEquals(150000, club.central.getMensualidades().get(0).calcularTotal());
			assertEquals(LocalDate.of(2026, 10, 20), club.central.getMensualidades().get(0).getFecha());
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – casos 2 y 3: Aparece cada venta con fecha, comprador, subtotal, impuesto, descuento aplicado y total")
		void rfA10_caso3_detalle() throws Exception {
			cuatroMovimientos();
			String reporte = reporteVentas(OCT1, OCT31);
			String raqueta = reporte.lines().filter(l -> l.contains("VT-2")).findFirst().orElseThrow();
			for (String campo : List.of("2026-10-05", "comprador: mario", "descuento: 30000", "subtotal: 170000", "impuesto: 32300", "total: 202300")) {
				assertTrue(raqueta.contains(campo), campo);
			}
			assertTrue(reporte.lines().anyMatch(l -> l.contains("VT-1") && l.contains("comprador: ana") && l.contains("total: 190400")));
			assertTrue(reporte.lines().anyMatch(l -> l.contains("VC-1") && l.contains("2026-10-07") && l.contains("impuesto: 400")
					&& l.contains("total: 5900")));
			assertTrue(reporte.lines().anyMatch(l -> l.contains("2026-10-20") && l.contains("mensualidades") && l.contains("total: 150000")));
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 4: Totales por rubro (tienda 392700, cafetería 5900, mensualidades 150000) y total general 548600")
		void rfA10_caso4_totales() throws Exception {
			cuatroMovimientos();
			String reporte = reporteVentas(OCT1, OCT31);
			assertTrue(reporte.contains("Total tienda: 392700"));
			assertTrue(reporte.contains("Total cafeteria: 5900"));
			assertTrue(reporte.contains("Total mensualidades: 150000"));
			assertTrue(reporte.contains("Total general: 548600"));
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 5: El reporte diario del 2026-10-05 muestra solo las 2 ventas de tienda")
		void rfA10_caso5_diario() throws Exception {
			cuatroMovimientos();
			String reporte = reporteVentas(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5));
			assertTrue(reporte.contains("Ventas: 2") && reporte.contains("VT-1") && reporte.contains("VT-2"));
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 5: El reporte semanal del 5 al 11 de octubre muestra 3 ventas")
		void rfA10_caso5_semanal() throws Exception {
			cuatroMovimientos();
			assertTrue(reporteVentas(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11)).contains("Ventas: 3"));
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 5: El reporte mensual de octubre muestra las 4")
		void rfA10_caso5_mensual() throws Exception {
			cuatroMovimientos();
			assertTrue(reporteVentas(OCT1, OCT31).contains("Ventas: 4"));
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 6: Se rechaza un periodo con fecha final anterior a la inicial")
		void rfA10_caso6_periodoInvalido() throws Exception {
			cuatroMovimientos();
			assertThrows(ValorInvalidoException.class, () -> reporteVentas(OCT31, OCT1));
		}

		@Test
		@DisplayName("RF-A10 / HU-ADM-05 – caso 6: Un periodo sin ventas muestra un reporte vacío")
		void rfA10_caso6_periodoVacio() throws Exception {
			cuatroMovimientos();
			String reporte = reporteVentas(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
			assertTrue(reporte.contains("Ventas: 0") && reporte.contains("Total general: 0"));
		}
	}

	@Nested
	@DisplayName("RF-A11 Aceptar sugerencias de las tiendas")
	class RfA11 {
		@Test
		@DisplayName("RF-A11 / HU-ADM-02 – casos 2 y 3: Al aceptar, cada artículo pasa al inventario de su tienda con disponibilidad=0 y sale de sugerencias")
		void rfA11_caso3_aceptar() throws Exception {
			sugerenciasDeEmpleados();
			adm.resolverSugerencia("Tienda", "Tobillera", true);
			adm.resolverSugerencia("Cafeteria", "Té frío", true);
			assertEquals(0, club.central.getTienda().getArticulo("Tobillera").getDisponibilidad());
			assertEquals(0, club.central.getCafeteria().getArticulo("Té frío").getDisponibilidad());
			assertFalse(club.central.getCafeteria().getArticulo("Té frío").getEsCaliente());
			assertTrue(club.central.getTienda().getSugerencias().isEmpty());
			assertTrue(club.central.getCafeteria().getSugerencias().isEmpty());
		}

		@Test
		@DisplayName("RF-A11 / HU-ADM-02 – caso 4: Se rechaza aceptar la sugerencia de un artículo que ya existe en el inventario")
		void rfA11_caso4_yaExiste() throws Exception {
			sugerenciasDeEmpleados();
			adm.agregarArticulo("Tienda", new ArticuloTienda("Tobillera", 40000, "Protección"));
			assertThrows(ElementoDuplicadoException.class, () -> adm.resolverSugerencia("Tienda", "Tobillera", true));
		}
	}

	@Nested
	@DisplayName("RF-A12 Denegar sugerencias de las tiendas")
	class RfA12 {
		@Test
		@DisplayName("RF-A12 / HU-ADM-02 – casos 2 y 3: Al denegar \"Tobillera\" sale de sugerencias y no se agrega a Tienda.inventario")
		void rfA12_caso3_denegar() throws Exception {
			sugerenciasDeEmpleados();
			adm.resolverSugerencia("Tienda", "Tobillera", false);
			assertTrue(club.central.getTienda().getSugerencias().isEmpty());
			assertNull(club.central.getTienda().getArticulo("Tobillera"));
		}
	}

	@Nested
	@DisplayName("RF-A13 Cambiar horario de instalaciones")
	class RfA13 {
		@Test
		@DisplayName("RF-A13 / HU-ADM-03 – casos 2 y 3: El Horario del 2026-10-17 queda h7-h19 y un socio no puede reservar ese día a las 21:00")
		void rfA13_caso3_horario() throws Exception {
			cambioDeHorarioPadel();
			assertArrayEquals(Club.horas(7, 19), padel.getHorario().get(SABADO));
			assertThrows(InstalacionNoDisponibleException.class, () -> socioPadel.reservarInstalacion("INS-1", SABADO, new int[] { 21 }, "dobles",
					new String[] { padelistas[0], padelistas[1] }, new String[] { padelistas[2], padelistas[3] }));
		}

		@Test
		@DisplayName("RF-A13 / HU-ADM-03 – caso 4: La reserva de las 20:00 se cancela y el PartidoPracticaRaqueta asociado también")
		void rfA13_caso4_partidoCancelado() throws Exception {
			cambioDeHorarioPadel();
			assertNull(club.central.getReservas().getReserva(reservaPartido.getIdReserva()));
			Partido partido = club.central.getPartidos().getPartido(idPartidoPadel);
			assertTrue(partido == null || ((PartidoPractica) partido).getReserva() == null);
		}

		@Test
		@DisplayName("RF-A13 / HU-ADM-03 – caso 4: Una SesionEntrenamiento cuya reserva queda fuera del nuevo horario también se cancela")
		void rfA13_caso4_sesionCancelada() throws Exception {
			cambioDeHorarioPadel();
			assertNull(club.central.getSesionesEntrenamiento().getSesion(idSesionPadel));
		}
	}

	@Nested
	@DisplayName("RF-A14 Eliminar instalación")
	class RfA14 {
		@Test
		@DisplayName("RF-A14 / HU-ADM-03 – caso 2: La mesa sin reservas futuras se elimina de ClubSeneca.inventarioInstalaciones")
		void rfA14_caso2_eliminarMesa() throws Exception {
			mesaYTenisConReserva();
			adm.eliminarInstalacion("INS-1");
			assertNull(club.central.getInstalaciones().getInstalacion("INS-1"));
		}

		@Test
		@DisplayName("RF-A14 / HU-ADM-03 – caso 3: Se rechaza eliminar la cancha de tenis con reservas futuras")
		void rfA14_caso3_tenisConReservas() throws Exception {
			mesaYTenisConReserva();
			assertThrows(InstalacionConReservasException.class, () -> adm.eliminarInstalacion("INS-2"));
			assertNotNull(club.central.getInstalaciones().getInstalacion("INS-2"));
		}
	}

	@Nested
	@DisplayName("RF-A15 Agregar instalación")
	class RfA15 {
		@Test
		@DisplayName("RF-A15 / HU-ADM-03 – caso 1: Pádel techada: aparece en el inventario con una sola modalidad (dobles, 4): acepta 4 y rechaza 2")
		void rfA15_caso1_padel() throws Exception {
			adm.agregarInstalacion("padel", 4, true);
			Instalacion instalacion = club.central.getInstalaciones().getInstalaciones().get(0);
			assertEquals("padel", instalacion.getTipo());
			assertTrue(instalacion.getTechada());
			String[] j = jugadores("padel", 4);
			ControladorSocio socio = club.login("j0");
			socio.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "dobles", new String[] { j[0], j[1] }, new String[] { j[2], j[3] });
			assertThrows(CapacidadInstalacionException.class,
					() -> socio.reservarInstalacion("INS-1", SABADO, new int[] { 11 }, "sencillos", new String[] { j[0] }, new String[] { j[1] }));
		}

		@Test
		@DisplayName("RF-A15 / HU-ADM-03 – caso 2: Tenis acepta sencillos (2 jugadores) y dobles (4 jugadores)")
		void rfA15_caso2_tenis() throws Exception {
			adm.agregarInstalacion("tenis", 4, false);
			String[] j = jugadores("tenis", 4);
			ControladorSocio socio = club.login("j0");
			socio.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "sencillos", new String[] { j[0] }, new String[] { j[1] });
			socio.reservarInstalacion("INS-1", SABADO, new int[] { 11 }, "dobles", new String[] { j[0], j[1] }, new String[] { j[2], j[3] });
			assertEquals(2, club.central.getReservas().getReservas().size());
		}

		@Test
		@DisplayName("RF-A15 / HU-ADM-03 – caso 3: Mesa con techada=false acepta 2 o 4 jugadores")
		void rfA15_caso3_mesa() throws Exception {
			adm.agregarInstalacion("mesa", 4, false);
			assertFalse(club.central.getInstalaciones().getInstalacion("INS-1").getTechada());
			String[] j = jugadores("tenis de mesa", 4);
			ControladorSocio socio = club.login("j0");
			socio.reservarInstalacion("INS-1", SABADO, new int[] { 10 }, "sencillos", new String[] { j[0] }, new String[] { j[1] });
			socio.reservarInstalacion("INS-1", SABADO, new int[] { 11 }, "dobles", new String[] { j[0], j[1] }, new String[] { j[2], j[3] });
			assertEquals(2, club.central.getReservas().getReservas().size());
		}

		@Test
		@DisplayName("RF-A15 / HU-ADM-03 – caso 4: Se rechaza una instalación de tipo \"squash\"")
		void rfA15_caso4_squash() {
			assertThrows(ValorInvalidoException.class, () -> adm.agregarInstalacion("squash", 4, true));
			assertTrue(club.central.getInstalaciones().getInstalaciones().isEmpty());
		}
	}

	@Nested
	@DisplayName("RF-A16 Consultar historial")
	class RfA16 {
		@Test
		@DisplayName("RF-A16 / HU-ADM-06 – caso 3: Aparecen las 2 reservas con fecha, hora, tipo de instalación y quién reservó")
		void rfA16_caso3_reservas() throws Exception {
			String historial = historialSeptiembre();
			assertTrue(historial.contains("2026-09-20 | horas: [10] | instalación: INS-1 (tenis) | reservada por: ana"));
			assertTrue(historial.contains("2026-09-22 | horas: [16, 17] | instalación: INS-2 (grupal) | reservada por: mario"));
		}

		@Test
		@DisplayName("RF-A16 / HU-ADM-06 – caso 3: Aparecen los 3 partidos con fecha, deporte, categoría y los datos propios de cada tipo")
		void rfA16_caso3_partidos() throws Exception {
			String historial = historialSeptiembre();
			assertTrue(historial.contains("2026-09-21 15:00 | PartidoInterno") && historial.contains("Fútbol | Sub-17")
					&& historial.contains("Fútbol Sub-17 A 3 - 1 Fútbol Sub-17 B"));
			assertTrue(historial.contains("2026-09-27 15:00 | PartidoCompetencia")
					&& historial.contains("2 - 0 Universidad Nacional (rival) | escenario: Estadio Alfonso López"));
			assertTrue(historial.contains("2026-09-20 10:00 | PartidoPractica") && historial.contains(anaTenis + " vs " + sofiaTenis));
		}

		@Test
		@DisplayName("RF-A16 / HU-ADM-06 – caso 3: Aparece la sesión con su entrenador, instalación, duración y la asistencia de cada jugador")
		void rfA16_caso3_sesion() throws Exception {
			String historial = historialSeptiembre();
			assertTrue(historial.contains("entrenador: mario | instalación: INS-2 (grupal) | duración: 90 min"));
			for (int i = 0; i < 3; i++) {
				assertTrue(historial.contains(sub17A[i] + ": asistió"), sub17A[i]);
			}
		}

		@Test
		@DisplayName("RF-A16 / HU-ADM-06 – caso 3: Todo sale ordenado por fecha")
		void rfA16_caso3_orden() throws Exception {
			String historial = historialSeptiembre();
			int[] orden = { historial.indexOf("2026-09-20"), historial.indexOf("2026-09-21"), historial.indexOf("2026-09-22"),
					historial.indexOf("2026-09-27") };
			for (int i = 1; i < orden.length; i++) {
				assertTrue(orden[i - 1] >= 0 && orden[i - 1] < orden[i]);
			}
		}

		@Test
		@DisplayName("RF-A16 / HU-ADM-06 – caso 4: Filtrando partidos del 2026-09-25 al 2026-09-30 aparece únicamente el partido de competencia")
		void rfA16_caso4_filtro() throws Exception {
			historialSeptiembre();
			String filtrado = adm.consultarHistorial("partidos", LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 30));
			assertEquals(1, filtrado.lines().filter(l -> l.startsWith("[Partido]")).count());
			assertTrue(filtrado.contains("PartidoCompetencia"));
		}

		@Test
		@DisplayName("RF-A16 / HU-ADM-06 – caso 4: Con un rango sin registros se muestra un historial vacío sin error")
		void rfA16_caso4_vacio() throws Exception {
			historialSeptiembre();
			assertTrue(adm.consultarHistorial("todo", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)).contains("(sin registros)"));
		}
	}

	@Nested
	@DisplayName("RF-A17 Gestionar partidos internos")
	class RfA17 {
		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 2: El partido Sub-17 A vs B queda en historialPartidos con deporte \"Fútbol\" y categoría \"Sub-17\"")
		void rfA17_caso2_programar() throws Exception {
			equiposSub17();
			Partido partido = club.central.getPartidos().getPartido(programarFutbol());
			assertEquals(FECHA_A17, partido.getFecha());
			assertEquals("Fútbol", partido.getDeporte());
			assertEquals("Sub-17", partido.getCategoria());
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 3: Marcador 2-1: 14 estadísticas en el partido y la acumulada de Pedro aumenta en 2 goles")
		void rfA17_caso3_marcadorYEstadisticas() throws Exception {
			String id = marcadorYEstadisticas();
			PartidoInterno partido = (PartidoInterno) club.central.getPartidos().getPartido(id);
			assertEquals(2, partido.getPuntuacionEquipo1());
			assertEquals(1, partido.getPuntuacionEquipo2());
			assertEquals(14, partido.getEstadisticas().size());
			assertEquals(2, ((EstadisticaFutbol) club.jugador(pedro()).getEstadisticaAcumulada()).getGoles());
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 4: Carlos (2 amarillas) y Mario (1 roja) quedan suspendidos; Andrés (1 amarilla) no")
		void rfA17_caso4_suspensiones() throws Exception {
			tarjetas();
			assertTrue(club.jugador(ifb[1]).estaSuspendido());
			assertTrue(club.jugador(ifb[2]).estaSuspendido());
			assertFalse(club.jugador(ifb[3]).estaSuspendido());
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 5: En el siguiente partido de B se rechaza a Carlos como habilitado")
		void rfA17_caso5_rechazaCarlos() throws Exception {
			tarjetas();
			String[] conCarlos = bSinSuspendidos().clone();
			conCarlos[0] = ifb[1];
			assertThrows(JugadorSuspendidoException.class,
					() -> adm.programarPartidoInterno(fa.getIdEquipo(), fb.getIdEquipo(), FECHA_A17.plusDays(7), habilitadosA(), conCarlos));
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 5: En el siguiente partido de B se rechaza a Mario como habilitado")
		void rfA17_caso5_rechazaMario() throws Exception {
			tarjetas();
			String[] conMario = bSinSuspendidos().clone();
			conMario[0] = ifb[2];
			assertThrows(JugadorSuspendidoException.class,
					() -> adm.programarPartidoInterno(fa.getIdEquipo(), fb.getIdEquipo(), FECHA_A17.plusDays(7), habilitadosA(), conMario));
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 5: Se acepta a Andrés y, registrado ese partido, Carlos y Mario quedan habilitados")
		void rfA17_caso5_aceptaAndres() throws Exception {
			tarjetas();
			adm.programarPartidoInterno(fa.getIdEquipo(), fb.getIdEquipo(), FECHA_A17.plusDays(7), habilitadosA(), bSinSuspendidos());
			String siguiente = ultimoInterno();
			assertTrue(((PartidoInterno) club.central.getPartidos().getPartido(siguiente)).estaHabilitado(ifb[3]));
			adm.registrarMarcador(siguiente, 0, 1);
			assertFalse(club.jugador(ifb[1]).estaSuspendido());
			assertFalse(club.jugador(ifb[2]).estaSuspendido());
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 6: Baloncesto: se rechaza 80-80 y se acepta 82-80 con una EstadisticaBasket por participante")
		void rfA17_caso6_baloncesto() throws Exception {
			String id = partidoBaloncesto();
			assertThrows(MarcadorInvalidoException.class, () -> adm.registrarMarcador(id, 80, 80));
			adm.registrarMarcador(id, 82, 80);
			for (String j : Arrays.copyOf(iba, 5)) {
				adm.registrarEstadistica(id, j, new EstadisticaBasket(j, 16, 4));
			}
			for (String j : Arrays.copyOf(ibb, 5)) {
				adm.registrarEstadistica(id, j, new EstadisticaBasket(j, 16, 4));
			}
			Partido partido = club.central.getPartidos().getPartido(id);
			assertTrue(partido.getCerrado());
			assertEquals(10, partido.getEstadisticas().size());
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 7: Voleibol: se acepta 3-1 en sets con EstadisticaVolley")
		void rfA17_caso7_tresAUno() throws Exception {
			String id = partidoVoleibol();
			adm.registrarMarcador(id, 3, 1);
			adm.registrarEstadistica(id, iva[0], new EstadisticaVolley(iva[0], 4, 2));
			assertTrue(club.central.getPartidos().getPartido(id).getCerrado());
			assertEquals(4, ((EstadisticaVolley) club.jugador(iva[0]).getEstadisticaAcumulada()).getAces());
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 7: Voleibol: se rechaza 2-2")
		void rfA17_caso7_dosADos() throws Exception {
			String id = partidoVoleibol();
			assertThrows(MarcadorInvalidoException.class, () -> adm.registrarMarcador(id, 2, 2));
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 7: Voleibol: se rechaza 4-1 (gana quien llega a 3 sets)")
		void rfA17_caso7_cuatroAUno() throws Exception {
			String id = partidoVoleibol();
			assertThrows(MarcadorInvalidoException.class, () -> adm.registrarMarcador(id, 4, 1));
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 8: Se rechaza un partido entre \"Fútbol Sub-17 A\" y \"Baloncesto Sub-17 A\" (deporte distinto)")
		void rfA17_caso8_deporteDistinto() throws Exception {
			equiposSub17();
			assertThrows(EquiposIncompatiblesException.class,
					() -> adm.programarPartidoInterno(fa.getIdEquipo(), ba.getIdEquipo(), FECHA_A17, habilitadosA(), Arrays.copyOf(iba, 5)));
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 8: Se rechaza un partido entre \"Fútbol Sub-17 A\" y \"Fútbol Sub-15\" (categoría distinta)")
		void rfA17_caso8_categoriaDistinta() throws Exception {
			equiposSub17();
			assertThrows(EquiposIncompatiblesException.class,
					() -> adm.programarPartidoInterno(fa.getIdEquipo(), f15.getIdEquipo(), FECHA_A17, habilitadosA(), new String[0]));
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 8: Se rechaza un partido de fútbol con un equipo de solo 6 habilitados")
		void rfA17_caso8_futbolSeis() throws Exception {
			equiposSub17();
			assertThrows(MinimoJugadoresException.class, () -> adm.programarPartidoInterno(fa.getIdEquipo(), fb.getIdEquipo(), FECHA_A17,
					Arrays.copyOf(habilitadosA(), 6), habilitadosB()));
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 8: Se rechaza un partido de baloncesto con 4 habilitados")
		void rfA17_caso8_baloncestoCuatro() throws Exception {
			equiposSub17();
			assertThrows(MinimoJugadoresException.class, () -> adm.programarPartidoInterno(ba.getIdEquipo(), bb.getIdEquipo(), FECHA_A17,
					Arrays.copyOf(iba, 4), Arrays.copyOf(ibb, 5)));
		}

		@Test
		@DisplayName("RF-A17 / HU-ADM-07 – caso 8: Se rechaza un marcador negativo")
		void rfA17_caso8_marcadorNegativo() throws Exception {
			String id = partidoBaloncesto();
			assertThrows(MarcadorInvalidoException.class, () -> adm.registrarMarcador(id, -1, 80));
		}
	}

	@Nested
	@DisplayName("RF-A18 Gestionar competencias")
	class RfA18 {
		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 1: Existen la competencia ASCUN y el Torneo de los Cerros con sus datos")
		void rfA18_caso1_competencias() throws Exception {
			a18Paso1();
			Competencia futbol = club.central.getCompetencias().getCompetencia("COMP-1");
			assertInstanceOf(CompetenciaASCUN.class, futbol);
			assertEquals(List.of("ASCUN Fútbol 2026", "ASCUN", "2026-2", "Fútbol", "Mayores", "Grupos y eliminación directa"),
					List.of(futbol.getNombre(), futbol.getEntidad(), futbol.getTemporada(), futbol.getDeporte(), futbol.getCategoria(), futbol.getFormato()));
			assertEquals(12, futbol.getNominaMinima());
			assertEquals(2, futbol.getEquivalenciaSuspension());
			Competencia cerros = club.central.getCompetencias().getCompetencia("COMP-2");
			assertEquals("Torneo de los Cerros 2026", cerros.getNombre());
			assertEquals(8, cerros.getNominaMinima());
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 2: Se crea un EquipoPropio con \"Fútbol Mayores\", 14 inscritos y puntos=0, en equiposParticipantes")
		void rfA18_caso2_inscripcion() throws Exception {
			a18Paso2();
			EquipoPropioCompetencia propio = (EquipoPropioCompetencia) ascun().getEquipoParticipante(idMayores);
			assertEquals("Fútbol Mayores", propio.getNombre());
			assertEquals(14, propio.getNumInscritos());
			assertEquals(0, propio.getPuntos());
			assertTrue(ascun().getEquiposParticipantes().contains(propio));
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 3: La FaseGrupos tiene un Grupo \"A\" con el EquipoPropio y 3 EquipoAfuera")
		void rfA18_caso3_grupo() throws Exception {
			a18Paso3();
			assertEquals(List.of(idMayores, "Universidad Nacional", "Javeriana", "Rosario"),
					Arrays.asList(ascun().getFaseGrupos().getGrupo("A").getEquipos()));
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 4: El partido queda en competencia.partidos y en ClubSeneca.historialPartidos")
		void rfA18_caso4_partidoRegistrado() throws Exception {
			String id = a18Paso4();
			assertTrue(ascun().getPartidos().stream().anyMatch(p -> p.getIdPartido().equals(id)));
			PartidoCompetencia partido = (PartidoCompetencia) club.central.getPartidos().getPartido(id);
			assertEquals("Universidad Nacional", partido.getEquipoAfuera().getNombre());
			assertEquals("Estadio Alfonso López", partido.getEscenario());
			assertEquals(11, partido.getJugadoresHabilitados().length);
			assertEquals(3, partido.getPuntuacionEquipoPropio());
			assertEquals(1, partido.getPuntuacionEquipoAfuera());
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 4: El EquipoPropio tiene puntos=3 (victoria en fútbol)")
		void rfA18_caso4_puntos() throws Exception {
			a18Paso4();
			assertEquals(3, ascun().getEquipoParticipante(idMayores).getPuntos());
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 4: Las estadísticas se acumulan en estadisticasAcumuladasJugadores")
		void rfA18_caso4_estadisticas() throws Exception {
			a18Paso4();
			List<Estadistica> goleadores = ascun().getGoleadores();
			EstadisticaFutbol luis = (EstadisticaFutbol) goleadores.stream().filter(e -> e.getUsuarioUsername().equals(luisA18())).findFirst()
					.orElseThrow();
			assertEquals(2, goleadores.stream().filter(e -> e.getUsuarioUsername().equals(pedroA18())).findFirst().orElseThrow().getValorPrincipal());
			assertEquals(1, luis.getGoles());
			assertEquals(1, luis.getAsistencias());
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 5: Tras el 1-1 con Javeriana, puntos=4 y Carlos llega a 2 amarillas y queda suspendido")
		void rfA18_caso5_empateYSuspension() throws Exception {
			a18Paso5();
			assertEquals(4, ascun().getEquipoParticipante(idMayores).getPuntos());
			assertEquals(2, ascun().getEquipoParticipante(idMayores) instanceof EquipoPropioCompetencia propio
					? propio.getAmonestaciones(carlosA18()).size() : 0);
			assertTrue(club.jugador(carlosA18()).estaSuspendido());
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 6: Tabla del Grupo A: Rosario 7, EquipoPropio 4, Universidad Nacional 4 y Javeriana 1")
		void rfA18_caso6_tabla() throws Exception {
			a18Paso6();
			List<EquipoCompetencia> tabla = club.central.getCompetencias().getTablaPosiciones("COMP-1", "A");
			assertEquals(List.of(7, 4, 4, 1), tabla.stream().map(EquipoCompetencia::getPuntos).toList());
			assertEquals("Rosario", tabla.get(0).getEquipoCompetenciaId());
			assertEquals("Javeriana", tabla.get(3).getEquipoCompetenciaId());
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 6: El empate a 4 se resuelve por enfrentamiento directo: el EquipoPropio queda sobre Universidad Nacional")
		void rfA18_caso6_desempate() throws Exception {
			a18Paso6();
			List<EquipoCompetencia> tabla = club.central.getCompetencias().getTablaPosiciones("COMP-1", "A");
			assertEquals(idMayores, tabla.get(1).getEquipoCompetenciaId());
			assertEquals("Universidad Nacional", tabla.get(2).getEquipoCompetenciaId());
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 6: Rosario y el EquipoPropio pasan a FaseEliminacionDirecta.equiposRestantes")
		void rfA18_caso6_clasificados() throws Exception {
			a18Paso6();
			adm.terminarFaseGrupos("COMP-1");
			assertEquals(List.of("Rosario", idMayores), Arrays.asList(ascun().getFaseEliminacion().getEquiposRestantes()));
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 7: El goleador es Pedro con 3 goles, seguido de Luis con 1; el destacado sale de las estadísticas acumuladas")
		void rfA18_caso7_goleadores() throws Exception {
			a18Paso6();
			List<Estadistica> goleadores = ascun().getGoleadores();
			assertEquals(pedroA18(), goleadores.get(0).getUsuarioUsername());
			assertEquals(3, goleadores.get(0).getValorPrincipal());
			assertEquals(luisA18(), goleadores.get(1).getUsuarioUsername());
			assertEquals(1, goleadores.get(1).getValorPrincipal());
			assertEquals(pedroA18(), ascun().getJugadorDestacado());
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 8: \"Baloncesto Mayores\" (9 jugadores) gana 70-65 en el Torneo de los Cerros y aparece en la tabla")
		void rfA18_caso8_cerros() throws Exception {
			a18Paso1();
			String[] ids = baloncestoMayores.getJugadoresIds();
			adm.inscribirEquipoACompetencia("COMP-2", baloncestoMayores.getIdEquipo(), Arrays.copyOf(ids, 9));
			adm.agregarEquipoAfuera("COMP-2", "Universidad de los Andes");
			adm.registrarPartidoCompetencia("COMP-2", LocalDateTime.of(2026, 10, 21, 18, 0), "Universidad de los Andes", "Coliseo",
					Arrays.copyOf(ids, 5));
			adm.registrarMarcador(ultimoPartido("COMP-2"), 70, 65);
			String tabla = adm.consultarTablaPosiciones("COMP-2", null);
			assertTrue(tabla.contains("1. Baloncesto Mayores [" + baloncestoMayores.getIdEquipo() + "]: 3 puntos"), tabla);
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 9: Se rechaza inscribir \"Fútbol Sub-17\" en la competencia ASCUN (categoría no permitida)")
		void rfA18_caso9_categoria() throws Exception {
			a18Paso1();
			Equipo sub17 = club.equipo("Fútbol Sub-17", "Fútbol", "Sub-17", 20);
			String[] ids = club.llenar(sub17, "s", "16", 12);
			assertThrows(RequisitosInscripcionException.class, () -> adm.inscribirEquipoACompetencia("COMP-1", sub17.getIdEquipo(), ids));
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 9: Se rechaza una nómina de 10 jugadores")
		void rfA18_caso9_nomina() throws Exception {
			a18Paso1();
			assertThrows(RequisitosInscripcionException.class, () -> adm.inscribirEquipoACompetencia("COMP-1", idMayores, Arrays.copyOf(mayores, 10)));
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 9: Se rechaza alinear en un partido oficial a un jugador que no está en inscritos")
		void rfA18_caso9_noInscrito() throws Exception {
			a18Paso3();
			String[] conNoInscrito = Arrays.copyOf(mayores, 11);
			conNoInscrito[10] = mayores[14];
			assertThrows(JugadorNoInscritoException.class, () -> adm.registrarPartidoCompetencia("COMP-1", LocalDateTime.of(2026, 10, 20, 15, 0),
					"Universidad Nacional", "Estadio Alfonso López", conNoInscrito));
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 9: Se rechaza alinear a Carlos en el partido siguiente a su suspensión")
		void rfA18_caso9_carlosSuspendido() throws Exception {
			a18Paso5();
			assertThrows(JugadorSuspendidoException.class, () -> adm.registrarPartidoCompetencia("COMP-1", LocalDateTime.of(2026, 11, 3, 15, 0),
					"Rosario", "Estadio Rosario", Arrays.copyOf(mayores, 11)));
		}

		@Test
		@DisplayName("RF-A18 / HU-ADM-08 – caso 9: Se rechaza registrar un empate en el Torneo de los Cerros de baloncesto")
		void rfA18_caso9_empateCerros() throws Exception {
			a18Paso1();
			String[] ids = baloncestoMayores.getJugadoresIds();
			adm.inscribirEquipoACompetencia("COMP-2", baloncestoMayores.getIdEquipo(), Arrays.copyOf(ids, 9));
			adm.agregarEquipoAfuera("COMP-2", "Universidad de los Andes");
			adm.registrarPartidoCompetencia("COMP-2", LocalDateTime.of(2026, 10, 21, 18, 0), "Universidad de los Andes", "Coliseo",
					Arrays.copyOf(ids, 5));
			String id = ultimoPartido("COMP-2");
			assertThrows(MarcadorInvalidoException.class, () -> adm.registrarMarcador(id, 70, 70));
		}
	}
}
