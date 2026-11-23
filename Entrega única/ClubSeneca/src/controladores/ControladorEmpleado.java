package controladores;

import java.time.LocalDate;

import excepciones.ElementoNoEncontradoException;
import excepciones.ValorInvalidoException;
import tiendas.Articulo;
import tiendas.ArticuloCafeteria;
import tiendas.ArticuloTienda;
import tiendas.Snack;
import usuarios.Usuario;

public class ControladorEmpleado extends ControladorUsuario {

	public ControladorEmpleado(CentralClubSeneca central, Usuario usuario) {
		super(central, usuario);
	}

	public void compartirCodigoDescuento(String usernameSocio) throws ElementoNoEncontradoException {
		central.getUsuarios().compartirCodigoDescuento(usernameSocio, usuario.getUsername());
	}

	public void solicitarCambioTurno(LocalDate fecha, Boolean[] turnoNuevo) throws ElementoNoEncontradoException, ValorInvalidoException {
		Boolean[] atencion = central.getHorarioAtencion();
		for (int h = 0; turnoNuevo != null && h < Math.min(turnoNuevo.length, atencion.length); h++) {
			if (Boolean.TRUE.equals(turnoNuevo[h]) && !Boolean.TRUE.equals(atencion[h])) {
				throw new ValorInvalidoException("La hora " + h + " está fuera del horario de atención del club");
			}
		}
		central.getUsuarios().solicitarCambioTurno(usuario.getUsername(), fecha, turnoNuevo);
	}

	public void sugerirArticulo(String tienda, Articulo articulo) throws ValorInvalidoException {
		String nombre = articulo.getNombre();
		boolean repetido = central.getTienda().getArticulo(nombre) != null || central.getCafeteria().getArticulo(nombre) != null
				|| central.getTienda().getSugerencias().stream().anyMatch(a -> a.getNombre().equalsIgnoreCase(nombre))
				|| central.getCafeteria().getSugerencias().stream().anyMatch(a -> a.getNombre().equalsIgnoreCase(nombre));
		if (repetido) {
			throw new ValorInvalidoException("El artículo " + nombre + " ya está en el inventario o ya fue sugerido");
		}
		if (articulo instanceof Snack snack && (snack.getAlergenos() == null || snack.getAlergenos().length == 0)) {
			throw new ValorInvalidoException("El snack " + nombre + " debe tener sus alérgenos registrados");
		}
		articulo.setSugeridoPor(usuario.getUsername());
		if (tienda.equalsIgnoreCase("Tienda") && articulo instanceof ArticuloTienda articuloTienda) {
			central.getTienda().sugerirArticulo(articuloTienda);
		} else if (tienda.toLowerCase().startsWith("cafeter") && articulo instanceof ArticuloCafeteria articuloCafeteria) {
			central.getCafeteria().sugerirArticulo(articuloCafeteria);
		} else {
			throw new ValorInvalidoException("El artículo no corresponde a la tienda " + tienda);
		}
	}

	@Override
	public String getRol() {
		return usuario.getTipo();
	}
}
