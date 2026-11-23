package tiendas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import excepciones.InventarioInsuficienteException;
import usuarios.Socio;

class ArticuloTest {

	@Test
	void abastecerYDescontarActualizanDisponibilidadYUbicaciones() throws Exception {
		ArticuloTienda articulo = new ArticuloTienda("Raqueta", 150000, "Tenis");
		assertTrue(articulo.estaAgotado());
		articulo.abastecer(15);
		assertFalse(articulo.estaAgotado());
		assertEquals(15, articulo.getUnidadesEn(Articulo.UBICACION_BODEGA));
		articulo.descontar(4);
		assertEquals(11, articulo.getDisponibilidad());
		assertEquals(11, articulo.getUnidadesEn(Articulo.UBICACION_BODEGA));
		assertThrows(InventarioInsuficienteException.class, () -> articulo.descontar(12));
		assertThrows(IllegalArgumentException.class, () -> articulo.abastecer(0));
	}

	@Test
	void moverUnidadesConservaElTotal() throws Exception {
		ArticuloTienda articulo = new ArticuloTienda("Balón", 80000, "Fútbol");
		articulo.abastecer(10);
		articulo.moverUnidades(Articulo.UBICACION_BODEGA, "Vitrina", 5);
		assertEquals(5, articulo.getUnidadesEn(Articulo.UBICACION_BODEGA));
		assertEquals(5, articulo.getUnidadesEn("Vitrina"));
		assertEquals(10, articulo.getDisponibilidad());
		assertThrows(InventarioInsuficienteException.class, () -> articulo.moverUnidades("Vitrina", "Bodega", 8));
		assertThrows(InventarioInsuficienteException.class, () -> articulo.moverUnidades("Vitrina", "Bodega", 0));
		assertEquals(5, articulo.getUnidadesEn("Vitrina"));
	}

	@Test
	void totalesDeVentasYMensualidad() {
		Socio socio = new Socio("ana", "pw", "Ana", "20");
		ArticuloTienda balon = new ArticuloTienda("Balón", 80000, "Fútbol");
		VentaTienda venta = new VentaTienda("VT-1", socio, List.of(balon), new int[] { 2 }, LocalDate.of(2026, 10, 5), 0, 0, 30400);
		assertEquals(190400, venta.calcularTotal());
		assertEquals(3200, venta.calcularPuntosGenerados(), 0.01);
		Bebida cafe = new Bebida("Café", 5000, true);
		VentaCafeteria cafeteria = new VentaCafeteria("VC-1", socio, List.of(cafe), new int[] { 1 }, LocalDate.of(2026, 10, 7), 500, 0, 400);
		assertEquals(5900, cafeteria.calcularTotal());
		PagoMensualidad pago = new PagoMensualidad(socio, 150000, LocalDate.of(2026, 10, 20), "2026-10");
		assertEquals(150000, pago.calcularTotal());
		assertEquals("mensualidades", pago.getRubro());
	}

	@Test
	void propinaNegativaSeRechaza() {
		Socio socio = new Socio("ana", "pw", "Ana", "20");
		VentaCafeteria venta = new VentaCafeteria("VC-1", socio, List.of(new Bebida("Té", 3000, true)), new int[] { 1 }, LocalDate.now(), 0, 0, 240);
		assertThrows(excepciones.ValorInvalidoException.class, () -> venta.setPropina(-100));
	}
}
