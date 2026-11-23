package usuarios;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import excepciones.CoberturaTurnosException;
import excepciones.CredencialesInvalidasException;
import excepciones.ElementoNoEncontradoException;
import excepciones.PuntosInsuficientesException;
import excepciones.ValorInvalidoException;

class CentralUsuariosTest {

	private static final LocalDate LUNES = LocalDate.of(2026, 10, 12);
	private CentralUsuarios usuarios;
	private Boolean[] atencion;

	static Boolean[] horas(int desde, int hasta) {
		Boolean[] turno = new Boolean[24];
		for (int h = 0; h < 24; h++) {
			turno[h] = h >= desde && h <= hasta;
		}
		return turno;
	}

	@BeforeEach
	void setUp() {
		usuarios = new CentralUsuarios();
		atencion = horas(8, 19);
		for (String nombre : List.of("e1", "e2")) {
			Entrenador e = new Entrenador(nombre, "pw", "C-" + nombre);
			e.setTurno(null, horas(8, 19));
			usuarios.agregarUsuario(e);
		}
		Fisioterapeuta f1 = new Fisioterapeuta("f1", "pw", "C-f1");
		f1.setTurno(null, horas(8, 19));
		usuarios.agregarUsuario(f1);
		Fisioterapeuta laura = new Fisioterapeuta("laura", "pw", "C-laura");
		laura.setTurno(LUNES, horas(12, 15));
		usuarios.agregarUsuario(laura);
		usuarios.agregarUsuario(new Socio("ana", "pw", "Ana", "20"));
	}

	@Test
	void autenticar() throws Exception {
		assertEquals("Socio", usuarios.autenticar("ana", "pw").getTipo());
		assertThrows(CredencialesInvalidasException.class, () -> usuarios.autenticar("ana", "mal"));
		assertThrows(CredencialesInvalidasException.class, () -> usuarios.autenticar("nadie", "pw"));
		assertThrows(IllegalArgumentException.class, () -> usuarios.agregarUsuario(new Socio("ana", "x", "Otra", "30")));
	}

	@Test
	void cambioDeTurnoQueDejaSinCoberturaSeRechaza() throws Exception {
		usuarios.solicitarCambioTurno("f1", LUNES, horas(11, 19));
		SolicitudCambioTurno solicitud = usuarios.getSolicitudesPendientes().get(0);
		assertThrows(CoberturaTurnosException.class,
				() -> usuarios.aprobarSolicitudCambioTurno(solicitud.getIdSolicitud(), atencion));
		assertEquals("rechazada", solicitud.getEstado());
		assertTrue(((Empleado) usuarios.getUsuario("f1")).getTurno(LUNES)[10]);
		assertFalse(((Empleado) usuarios.getUsuario("f1")).getTurnosPorFecha().containsKey(LUNES));
	}

	@Test
	void cambioDeTurnoConCoberturaSeAprueba() throws Exception {
		usuarios.solicitarCambioTurno("f1", LUNES, horas(7, 19));
		assertThrows(ValorInvalidoException.class, () -> usuarios.solicitarCambioTurno("f1", LUNES, horas(8, 19)));
		SolicitudCambioTurno solicitud = usuarios.getSolicitudesPendientes().get(0);
		assertTrue(usuarios.aprobarSolicitudCambioTurno(solicitud.getIdSolicitud(), atencion));
		assertEquals("aprobada", solicitud.getEstado());
		assertTrue(((Empleado) usuarios.getUsuario("f1")).getTurno(LUNES)[7]);
		assertThrows(ElementoNoEncontradoException.class,
				() -> usuarios.aprobarSolicitudCambioTurno(solicitud.getIdSolicitud(), atencion));
	}

	@Test
	void coberturaMinimaDeEntrenadoresYFisioterapeutas() {
		CentralCambioTurno cambio = new CentralCambioTurno();
		List<Empleado> empleados = usuarios.getEmpleados("Todos");
		assertTrue(cambio.verificarCobertura(LUNES, empleados, atencion));
		((Empleado) usuarios.getUsuario("e2")).setTurno(LUNES, horas(8, 12));
		assertFalse(cambio.verificarCobertura(LUNES, empleados, atencion));
		assertTrue(cambio.verificarCobertura(LUNES.plusDays(1), empleados, atencion));
		assertFalse(cambio.interfazCambioTurno(new Boolean[10]));
	}

	@Test
	void puntosDeFidelidad() throws Exception {
		usuarios.acumularPuntos("ana", 500);
		usuarios.redimirPuntos("ana", 300);
		assertEquals(200, ((Socio) usuarios.getUsuario("ana")).getPuntosFidelidad());
		assertThrows(PuntosInsuficientesException.class, () -> usuarios.redimirPuntos("ana", 1000));
		assertThrows(IllegalArgumentException.class, () -> usuarios.acumularPuntos("ana", -1));
	}

	@Test
	void compartirCodigo() throws Exception {
		usuarios.compartirCodigoDescuento("ana", "e1");
		assertEquals("C-e1", ((Socio) usuarios.getUsuario("ana")).getCodigoDescuentoDado());
		assertTrue(Arrays.asList(((Empleado) usuarios.getUsuario("e1")).getSociosConCodigo()).contains("ana"));
		assertThrows(ElementoNoEncontradoException.class, () -> usuarios.compartirCodigoDescuento("e2", "e1"));
		assertThrows(ElementoNoEncontradoException.class, () -> usuarios.compartirCodigoDescuento("ana", "ana"));
	}
}
