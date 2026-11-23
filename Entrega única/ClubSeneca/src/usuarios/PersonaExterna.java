package usuarios;

public class PersonaExterna extends Usuario {

	private String nombre;

	public PersonaExterna(String username, String password, String nombre) {
		super(username, password);
		this.nombre = nombre;
	}

	public String getNombre() {
		return nombre;
	}

	@Override
	public String getTipo() {
		return "PersonaExterna";
	}
}
