package estadisticas;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import amonestaciones.AmonestacionFutbol;

class EstadisticaTest {

	@Test
	void fabricaSegunDeporte() {
		assertInstanceOf(EstadisticaBasket.class, Estadistica.crear("Baloncesto", "j"));
		assertInstanceOf(EstadisticaVolley.class, Estadistica.crear("voleibol", "j"));
		assertInstanceOf(EstadisticaTenisPadel.class, Estadistica.crear("pádel", "j"));
		assertInstanceOf(EstadisticaTenisMesa.class, Estadistica.crear("tenis de mesa", "j"));
		assertInstanceOf(EstadisticaFutbol.class, Estadistica.crear("Fútbol", "j"));
	}

	@Test
	void sumarAcumulaCadaCampo() {
		EstadisticaFutbol futbol = new EstadisticaFutbol("j", 2, 0);
		futbol.sumar(new EstadisticaFutbol("j", 1, 1));
		assertEquals(3, futbol.getGoles());
		assertEquals(1, futbol.getAsistencias());
		assertEquals(3, futbol.getValorPrincipal());
		EstadisticaBasket basket = new EstadisticaBasket("j", 20, 5);
		basket.sumar(new EstadisticaBasket("j", 10, 2));
		assertEquals(30, basket.getPuntos());
		assertEquals(7, basket.getRebotes());
		EstadisticaTenisMesa mesa = new EstadisticaTenisMesa("j", 3, 33);
		mesa.sumar(new EstadisticaTenisMesa("j", 1, 20));
		assertEquals(4, mesa.getSetGanados());
		assertEquals(53, mesa.getPuntosTotales());
	}

	@Test
	void amonestacionGeneraSuspension() {
		assertTrue(new AmonestacionFutbol("j", 2, 0).generaSuspension(2));
		assertFalse(new AmonestacionFutbol("j", 1, 0).generaSuspension(2));
		assertTrue(new AmonestacionFutbol("j", 0, 1).generaSuspension(2));
	}
}
