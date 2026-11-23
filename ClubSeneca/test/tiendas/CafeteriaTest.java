package tiendas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import excepciones.ElementoDuplicadoException;
import excepciones.ElementoNoEncontradoException;
import excepciones.InventarioInsuficienteException;
import excepciones.ValorInvalidoException;
import premios.BonoDescuento;
import usuarios.Socio;

class CafeteriaTest {

	private Cafeteria cafeteria;
	private Socio socio;

	@BeforeEach
	void setUp() throws Exception {
		cafeteria = new Cafeteria();
		socio = new Socio("ana", "pw", "Ana", "20");
		cafeteria.agregarArticulo(new Snack("Barra de granola", 6000, new String[] { "maní", "gluten" }));
		cafeteria.agregarArticulo(new Snack("Galleta", 2000, new String[] { "gluten" }));
		cafeteria.agregarArticulo(new Bebida("Café", 5000, true));
		cafeteria.abastecer("Barra de granola", 10);
		cafeteria.abastecer("Galleta", 3);
		cafeteria.abastecer("Café", 10);
	}

	@Test
	void impuestoAlConsumoYPropinaSugerida() {
		assertEquals(880, cafeteria.calcularImpuesto(11000));
		assertEquals(1100, cafeteria.calcularPropinaSugerida(11000));
		assertEquals(288, cafeteria.calcularImpuesto(3600));
	}

	@Test
	void alergenosSinRepetir() throws Exception {
		assertArrayEquals(new String[] { "maní", "gluten" },
				cafeteria.consultarAlergenos(new String[] { "Barra de granola", "Galleta", "Café" }));
		assertThrows(ElementoNoEncontradoException.class, () -> cafeteria.consultarAlergenos(new String[] { "Nada" }));
	}

	@Test
	void ventaConPropinaPersonalizada() throws Exception {
		VentaCafeteria venta = cafeteria.registrarVenta(socio, new String[] { "Barra de granola", "Café" }, new int[] { 1, 1 }, 500, 0);
		assertEquals(11000, venta.calcularSubtotal());
		assertEquals(880, venta.getIva());
		assertEquals(500, venta.getPropina());
		assertEquals(12380, venta.calcularTotal());
		assertEquals(220, venta.calcularPuntosGenerados(), 0.01);
		assertEquals(9, cafeteria.getArticulo("Café").getDisponibilidad());
	}

	@Test
	void ventaConBono() throws Exception {
		BonoDescuento bono = new BonoDescuento("B-1", "T-2", 1, "ana", LocalDate.now(), 10);
		VentaCafeteria venta = cafeteria.registrarVenta(socio, new String[] { "Café" }, new int[] { 1 }, 0, 0, bono);
		assertEquals(500, venta.getDescuento());
		assertEquals(360, venta.getIva());
	}

	@Test
	void validaciones() {
		assertThrows(ValorInvalidoException.class,
				() -> cafeteria.registrarVenta(socio, new String[] { "Café" }, new int[] { 1 }, -1, 0));
		assertThrows(InventarioInsuficienteException.class,
				() -> cafeteria.registrarVenta(socio, new String[] { "Galleta" }, new int[] { 4 }, 0, 0));
		assertEquals(3, cafeteria.getArticulo("Galleta").getDisponibilidad());
		assertThrows(ValorInvalidoException.class, () -> cafeteria.abastecer("Café", -2));
	}

	@Test
	void sugerenciaDuplicadaEnInventarioSeRechazaAlAceptar() throws Exception {
		cafeteria.sugerirArticulo(new Bebida("Café", 4000, true));
		assertThrows(ElementoDuplicadoException.class, () -> cafeteria.aceptarSugerencia("Café"));
		cafeteria.sugerirArticulo(new Bebida("Té frío", 4000, false));
		cafeteria.denegarSugerencia("té frío");
		assertNull(cafeteria.getArticulo("Té frío"));
	}
}
