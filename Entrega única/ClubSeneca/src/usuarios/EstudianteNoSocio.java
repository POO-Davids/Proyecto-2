package usuarios;

public class EstudianteNoSocio extends Usuario {

	private String nombre;

	public EstudianteNoSocio(String username, String password, String nombre) {
		super(username, password);
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}

	@Override
	public String getTipo() {
		return "EstudianteNoSocio";
	}
}
