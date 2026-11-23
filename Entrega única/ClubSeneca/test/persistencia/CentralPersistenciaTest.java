package persistencia;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import controladores.CentralClubSeneca;
import controladores.ControladorAdministrador;
import jugadores.JugadorIndividual;
import tiendas.ArticuloTienda;
import tiendas.Bebida;
import usuarios.Administrador;
import usuarios.Entrenador;
import usuarios.Socio;

class CentralPersistenciaTest {

	@TempDir
	Path dir;

	@Test
	void guardarYCargarConservaLosDatos() throws Exception {
		CentralClubSeneca club = new CentralClubSeneca(dir.toString());
		ControladorAdministrador adm = new ControladorAdministrador(club);
		adm.registrarUsuario(new Administrador("admin", "pw"));
		adm.registrarUsuario(new Socio("ana", "pw", "Ana", "20"));
		adm.registrarUsuario(new Entrenador("mario", "pw", "ENT-1"));
		adm.crearEquipo("Fútbol Femenino", "Fútbol", "Mayores", 20);
		adm.crearJugador("ana", new JugadorIndividual("tmp", null, "tenis", "Intermedio", 1016));
		adm.agregarInstalacion("tenis", 4, false);
		club.getTienda().agregarArticulo(new ArticuloTienda("Grip", 12000, "Accesorios"));
		club.getTienda().abastecer("Grip", 3);
		club.getCafeteria().agregarArticulo(new Bebida("Café", 5000, true));
		adm.crearTorneo("competitivo", "T1", "tenis", "individual", "sencillos", "Intermedio", 10, 20000, LocalDate.of(2026, 12, 5));
		club.guardarInformacion();
		assertTrue(Files.list(dir).findAny().isPresent());

		CentralClubSeneca cargado = new CentralClubSeneca(dir.toString());
		cargado.cargarInformacion();
		assertEquals(3, cargado.getUsuarios().getUsuarios().size());
		assertEquals("Entrenador", cargado.getUsuarios().autenticar("mario", "pw").getTipo());
		assertEquals(1, cargado.getEquipos().getEquipos().size());
		JugadorIndividual ana = cargado.getJugadores().getJugadorIndividual("ana", "tenis");
		assertNotNull(ana);
		assertEquals(1016, ana.getElo(), 0.01);
		assertEquals(1, cargado.getInstalaciones().getInstalaciones().size());
		assertEquals(3, cargado.getTienda().getArticulo("Grip").getDisponibilidad());
		assertNotNull(cargado.getCafeteria().getArticulo("Café"));
		assertEquals(1, cargado.getTorneos().getTorneos().size());
		assertTrue(cargado.getTienda().validarCodigoDescuento("ENT-1"));
	}

	@Test
	void cargarDeDirectorioInexistenteNoFalla() throws Exception {
		CentralClubSeneca club = new CentralClubSeneca(dir.resolve("no-existe").toString());
		club.cargarInformacion();
		assertTrue(club.getUsuarios().getUsuarios().isEmpty());
	}
}
