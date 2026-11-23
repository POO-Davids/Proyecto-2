package tiendas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import excepciones.CodigoDescuentoInvalidoException;
import excepciones.ElementoDuplicadoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.InventarioInsuficienteException;
import excepciones.ValorInvalidoException;
import premios.BonoDescuento;
import usuarios.Entrenador;
import usuarios.Socio;

class TiendaTest {

	private Tienda tienda;
	private Socio socio;
	private Entrenador entrenador;

	@BeforeEach
	void setUp() throws Exception {
		tienda = new Tienda();
		socio = new Socio("ana", "pw", "Ana", "20");
		entrenador = new Entrenador("mario", "pw", "ENT-1");
		tienda.agregarCodigoDescuento("ENT-1");
		tienda.agregarArticulo(new ArticuloTienda("Balón", 80000, "Fútbol"));
		tienda.abastecer("Balón", 10);
	}

	@Test
	void ventaSinDescuentoAplicaIva19() throws Exception {
		VentaTienda venta = tienda.registrarVenta(socio, new String[] { "Balón" }, new int[] { 2 }, "", 0);
		assertEquals(160000, venta.calcularSubtotal());
		assertEquals(30400, venta.getIva());
		assertEquals(190400, venta.calcularTotal());
		assertEquals(8, tienda.getArticulo("balón").getDisponibilidad());
		assertEquals(1, tienda.getVentas().size());
	}

	@Test
	void empleadoConSuCodigoRecibe15PorCiento() throws Exception {
		VentaTienda venta = tienda.registrarVenta(entrenador, new String[] { "Balón" }, new int[] { 1 }, "ENT-1", 0);
		assertEquals(12000, venta.getDescuento());
		assertEquals(Math.round(68000 * 0.19f), venta.getIva());
		assertEquals(68000 + Math.round(68000 * 0.19f), venta.calcularTotal(), 0.5);
	}

	@Test
	void socioConCodigoCompartidoRecibe8PorCiento() throws Exception {
		socio.setCodigoDescuento("ENT-1");
		assertEquals(6400, tienda.calcularDescuentoCodigo(socio, "ENT-1", 80000));
	}

	@Test
	void codigoInexistenteOAjenoSeRechaza() {
		assertThrows(CodigoDescuentoInvalidoException.class,
				() -> tienda.registrarVenta(socio, new String[] { "Balón" }, new int[] { 1 }, "NO-EXISTE", 0));
		assertThrows(CodigoDescuentoInvalidoException.class, () -> tienda.calcularDescuentoCodigo(socio, "ENT-1", 1000));
	}

	@Test
	void inventarioInsuficienteNoModificaNada() {
		assertThrows(InventarioInsuficienteException.class,
				() -> tienda.registrarVenta(socio, new String[] { "Balón", "balón" }, new int[] { 6, 5 }, "", 0));
		assertEquals(10, tienda.getArticulo("Balón").getDisponibilidad());
		assertTrue(tienda.getVentas().isEmpty());
	}

	@Test
	void puntosRedimidosSeDescuentanAntesDelIva() throws Exception {
		VentaTienda venta = tienda.registrarVenta(socio, new String[] { "Balón" }, new int[] { 1 }, "", 1000);
		assertEquals(Math.round(79000 * 0.19f), venta.getIva());
		assertEquals(79000 * 0.02f, venta.calcularPuntosGenerados(), 0.01);
		assertThrows(ValorInvalidoException.class,
				() -> tienda.registrarVenta(socio, new String[] { "Balón" }, new int[] { 1 }, "", 90000));
	}

	@Test
	void bonoTienePrioridadSobreCodigo() throws Exception {
		BonoDescuento bono = new BonoDescuento("B-1", "T-1", 1, "ana", LocalDate.now(), 10);
		VentaTienda venta = tienda.registrarVenta(socio, new String[] { "Balón" }, new int[] { 1 }, "", 0, bono);
		assertEquals(8000, venta.getDescuento());
		assertEquals(Math.round(72000 * 0.19f), venta.getIva());
	}

	@Test
	void datosDeCompraInvalidos() {
		assertThrows(ValorInvalidoException.class, () -> tienda.registrarVenta(socio, new String[0], new int[0], "", 0));
		assertThrows(ValorInvalidoException.class, () -> tienda.registrarVenta(socio, new String[] { "Balón" }, new int[] { 0 }, "", 0));
		assertThrows(ElementoNoEncontradoException.class,
				() -> tienda.registrarVenta(socio, new String[] { "Nada" }, new int[] { 1 }, "", 0));
	}

	@Test
	void abastecerYSugerencias() throws Exception {
		assertThrows(ValorInvalidoException.class, () -> tienda.abastecer("Balón", 0));
		assertThrows(ElementoNoEncontradoException.class, () -> tienda.abastecer("Nada", 1));
		assertThrows(ElementoDuplicadoException.class, () -> tienda.agregarArticulo(new ArticuloTienda("BALÓN", 1, "x")));
		tienda.sugerirArticulo(new ArticuloTienda("Grip", 12000, "Accesorios"));
		tienda.aceptarSugerencia("grip");
		assertNotNull(tienda.getArticulo("Grip"));
		assertEquals(0, tienda.getArticulo("Grip").getDisponibilidad());
		assertTrue(tienda.getSugerencias().isEmpty());
		assertThrows(ElementoNoEncontradoException.class, () -> tienda.denegarSugerencia("Grip"));
	}

	@Test
	void ventasEntreFechas() throws Exception {
		tienda.registrarVenta(socio, new String[] { "Balón" }, new int[] { 1 }, "", 0);
		LocalDate hoy = LocalDate.now();
		assertEquals(1, tienda.getVentasEntre(hoy, hoy).size());
		assertEquals(0, tienda.getVentasEntre(hoy.plusDays(1), hoy.plusDays(5)).size());
	}
}
