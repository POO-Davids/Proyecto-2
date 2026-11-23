package persistencia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import controladores.CentralClubSeneca;
import equipos.Equipo;
import excepciones.PersistenciaException;
import partidos.PartidoTorneo;
import premios.BonoDescuento;
import torneos.CentralTorneos;
import torneos.EquipoTorneo;
import torneos.InscripcionTorneo;
import torneos.ParejaTorneo;
import torneos.ParticipanteIndividual;
import torneos.ParticipanteTorneo;
import premios.Premio;
import premios.PremioEfectivo;
import torneos.Torneo;
import torneos.TorneoCompetitivo;
import torneos.TorneoRecreativo;

public class PersistenciaTorneos extends Persistencia {

	private static final String ARCHIVO = "torneos.json";

	private Set<BonoDescuento> bonosEnCuenta = new HashSet<>();
	private LinkedHashMap<String, ParticipanteTorneo> participantesCargados = new LinkedHashMap<>();

	public PersistenciaTorneos() {
		super(ARCHIVO);
	}

	@Override
	public void guardar(String ruta, CentralClubSeneca club) throws PersistenciaException {
		CentralTorneos central = club.getTorneos();
		bonosEnCuenta = new HashSet<>(central.getBonos());
		JSONArray lista = new JSONArray();
		for (Torneo torneo : central.getTorneos()) {
			lista.put(torneoAJSON(torneo));
		}
		escribirJSON(ruta, new JSONObject().put("contador", central.getContador()).put("torneos", lista));
	}

	@Override
	public void cargar(String ruta, CentralClubSeneca club) throws PersistenciaException {
		JSONObject json = leerJSON(ruta);
		CentralTorneos central = club.getTorneos();
		try {
			JSONArray lista = json.getJSONArray("torneos");
			for (int i = 0; i < lista.length(); i++) {
				JSONObject jsonTorneo = lista.getJSONObject(i);
				Torneo torneo = torneoDesdeJSON(jsonTorneo, club);
				central.agregarTorneo(torneo);
				JSONArray premios = jsonTorneo.getJSONArray("premios");
				for (int k = 0; k < premios.length(); k++) {
					JSONObject jsonPremio = premios.getJSONObject(k);
					Premio premio = premioDesdeJSON(jsonPremio);
					torneo.agregarPremio(premio);
					if (premio instanceof BonoDescuento bono && jsonPremio.optBoolean("enCuenta", false)) {
						central.agregarBono(bono);
					}
				}
			}
			central.setContador(json.getInt("contador"));
		} catch (JSONException | IllegalArgumentException e) {
			throw new PersistenciaException("Archivo " + ARCHIVO + " inválido: " + e.getMessage());
		}
	}

	private JSONObject torneoAJSON(Torneo torneo) {
		JSONObject json = new JSONObject().put("idTorneo", torneo.getIdTorneo()).put("nombre", torneo.getNombre())
				.put("deporte", torneo.getDeporte()).put("tipoParticipante", torneo.getTipoParticipante())
				.put("modalidad", torneo.getModalidad()).put("categoria", torneo.getCategoria()).put("nivel", torneo.getNivel())
				.put("cupos", torneo.getCupos()).put("cuposPreferenciales", torneo.getCuposPreferenciales())
				.put("fecha", fechaAJSON(torneo.getFecha())).put("estado", torneo.getEstado());
		if (torneo instanceof TorneoCompetitivo competitivo) {
			json.put("tipo", "competitivo").put("tarifa", competitivo.getTarifa()).put("recaudo", competitivo.getRecaudo());
		} else if (torneo instanceof TorneoRecreativo recreativo) {
			json.put("tipo", "recreativo").put("porcentajeBono", recreativo.getPorcentajeBono());
		}
		LinkedHashMap<String, ParticipanteTorneo> union = new LinkedHashMap<>();
		JSONArray inscripciones = new JSONArray();
		for (InscripcionTorneo inscripcion : torneo.getInscripciones()) {
			union.putIfAbsent(inscripcion.getParticipante().getIdParticipante(), inscripcion.getParticipante());
			inscripciones.put(inscripcionAJSON(inscripcion));
		}
		JSONArray partidos = new JSONArray();
		for (PartidoTorneo partido : torneo.getPartidos()) {
			union.putIfAbsent(partido.getParticipante1().getIdParticipante(), partido.getParticipante1());
			union.putIfAbsent(partido.getParticipante2().getIdParticipante(), partido.getParticipante2());
			partidos.put(partidoAJSON(partido));
		}
		JSONArray posiciones = new JSONArray();
		for (ParticipanteTorneo participante : torneo.getPosiciones()) {
			union.putIfAbsent(participante.getIdParticipante(), participante);
			posiciones.put(participante.getIdParticipante());
		}
		JSONArray participantes = new JSONArray();
		for (ParticipanteTorneo participante : union.values()) {
			participantes.put(participanteAJSON(participante));
		}
		JSONArray premios = new JSONArray();
		for (Premio premio : torneo.getPremios()) {
			premios.put(premioAJSON(premio));
		}
		return json.put("participantes", participantes).put("inscripciones", inscripciones).put("partidos", partidos)
				.put("posiciones", posiciones).put("premios", premios);
	}

	private Torneo torneoDesdeJSON(JSONObject json, CentralClubSeneca club) {
		String id = json.getString("idTorneo");
		Torneo torneo;
		if (json.getString("tipo").equals("competitivo")) {
			TorneoCompetitivo competitivo = new TorneoCompetitivo(id, json.getString("nombre"), json.getString("deporte"),
					json.getString("tipoParticipante"), json.getString("modalidad"), json.getString("categoria"),
					json.getString("nivel"), json.getInt("cupos"), json.getInt("cuposPreferenciales"),
					fechaDesdeJSON(json.getString("fecha")), json.getFloat("tarifa"));
			competitivo.restaurarRecaudo(json.getFloat("recaudo"));
			torneo = competitivo;
		} else {
			torneo = new TorneoRecreativo(id, json.getString("nombre"), json.getString("deporte"), json.getString("tipoParticipante"),
					json.getString("modalidad"), json.getString("categoria"), json.getString("nivel"), json.getInt("cupos"),
					json.getInt("cuposPreferenciales"), fechaDesdeJSON(json.getString("fecha")), json.getFloat("porcentajeBono"));
		}
		participantesCargados = new LinkedHashMap<>();
		JSONArray participantes = json.getJSONArray("participantes");
		for (int i = 0; i < participantes.length(); i++) {
			ParticipanteTorneo participante = participanteDesdeJSON(participantes.getJSONObject(i), club);
			participantesCargados.put(participante.getIdParticipante(), participante);
		}
		JSONArray inscripciones = json.getJSONArray("inscripciones");
		for (int i = 0; i < inscripciones.length(); i++) {
			torneo.agregarInscripcion(inscripcionDesdeJSON(inscripciones.getJSONObject(i), torneo));
		}
		JSONArray partidos = json.getJSONArray("partidos");
		for (int i = 0; i < partidos.length(); i++) {
			torneo.agregarPartidoCargado(partidoDesdeJSON(partidos.getJSONObject(i), torneo));
		}
		List<ParticipanteTorneo> posiciones = new ArrayList<>();
		for (String idParticipante : textosDesdeJSON(json.getJSONArray("posiciones"))) {
			posiciones.add(participantesCargados.get(idParticipante));
		}
		torneo.setPosiciones(posiciones);
		torneo.restaurarEstado(json.getString("estado"));
		return torneo;
	}

	private JSONObject inscripcionAJSON(InscripcionTorneo inscripcion) {
		return new JSONObject().put("idInscripcion", inscripcion.getIdInscripcion())
				.put("idParticipante", inscripcion.getParticipante().getIdParticipante()).put("tipoCupo", inscripcion.getTipoCupo())
				.put("tarifaPagada", inscripcion.getTarifaPagada()).put("fecha", fechaAJSON(inscripcion.getFecha()));
	}

	private InscripcionTorneo inscripcionDesdeJSON(JSONObject json, Torneo torneo) {
		ParticipanteTorneo participante = participantesCargados.get(json.getString("idParticipante"));
		if (participante == null) {
			throw new IllegalArgumentException("participante " + json.getString("idParticipante") + " sin datos en " + torneo.getIdTorneo());
		}
		return new InscripcionTorneo(json.getString("idInscripcion"), participante, json.getString("tipoCupo"),
				json.getFloat("tarifaPagada"), fechaDesdeJSON(json.getString("fecha")));
	}

	private JSONObject participanteAJSON(ParticipanteTorneo participante) {
		JSONObject json = new JSONObject().put("idParticipante", participante.getIdParticipante());
		if (participante instanceof ParticipanteIndividual individual) {
			json.put("clase", "ParticipanteIndividual").put("username", individual.getUsername()).put("nivel", individual.getNivel())
					.put("esEmpleado", individual.getEsEmpleado());
		} else if (participante instanceof ParejaTorneo pareja) {
			String[] usernames = pareja.getUsernames();
			json.put("clase", "ParejaTorneo").put("username1", usernames[0]).put("username2", usernames[1]).put("nivel", pareja.getNivel());
		} else if (participante instanceof EquipoTorneo equipo) {
			json.put("clase", "EquipoTorneo").put("idEquipoClub", equipo.getEquipoClub().getIdEquipo())
					.put("integrantes", textosAJSON(equipo.getUsernames()));
		}
		return json;
	}

	private ParticipanteTorneo participanteDesdeJSON(JSONObject json, CentralClubSeneca club) {
		String id = json.getString("idParticipante");
		switch (json.getString("clase")) {
		case "ParticipanteIndividual":
			return new ParticipanteIndividual(id, json.getString("username"), json.getString("nivel"), json.getBoolean("esEmpleado"));
		case "ParejaTorneo":
			return new ParejaTorneo(id, json.getString("username1"), json.getString("username2"), json.getString("nivel"));
		default:
			Equipo equipoClub = club.getEquipos().getEquipo(json.getString("idEquipoClub"));
			if (equipoClub == null) {
				throw new IllegalArgumentException("no existe el equipo " + json.getString("idEquipoClub"));
			}
			EquipoTorneo equipo = new EquipoTorneo(id, equipoClub);
			for (String username : textosDesdeJSON(json.getJSONArray("integrantes"))) {
				equipo.agregarIntegrante(username);
			}
			return equipo;
		}
	}

	private JSONObject partidoAJSON(PartidoTorneo partido) {
		return new JSONObject().put("idPartido", partido.getIdPartido())
				.put("idParticipante1", partido.getParticipante1().getIdParticipante())
				.put("idParticipante2", partido.getParticipante2().getIdParticipante()).put("fecha", partido.getFecha().toString())
				.put("marcador1", partido.getMarcador1()).put("marcador2", partido.getMarcador2());
	}

	private PartidoTorneo partidoDesdeJSON(JSONObject json, Torneo torneo) {
		ParticipanteTorneo participante1 = participantesCargados.get(json.getString("idParticipante1"));
		ParticipanteTorneo participante2 = participantesCargados.get(json.getString("idParticipante2"));
		String categoria = torneo.getTipoParticipante().equalsIgnoreCase("equipo") ? torneo.getCategoria() : torneo.getNivel();
		PartidoTorneo partido = new PartidoTorneo(json.getString("idPartido"), torneo.getIdTorneo(), participante1, participante2,
				LocalDateTime.parse(json.getString("fecha")), torneo.getDeporte(), categoria);
		partido.registrarMarcador(json.getInt("marcador1"), json.getInt("marcador2"));
		return partido;
	}

	private JSONObject premioAJSON(Premio premio) {
		JSONObject json = new JSONObject().put("idPremio", premio.getIdPremio()).put("idTorneoOrigen", premio.getTorneoOrigen())
				.put("posicion", premio.getPosicion()).put("beneficiario", premio.getBeneficiario())
				.put("fecha", fechaAJSON(premio.getFecha()));
		if (premio instanceof PremioEfectivo efectivo) {
			json.put("clase", "PremioEfectivo").put("valor", efectivo.getValor());
		} else if (premio instanceof BonoDescuento bono) {
			json.put("clase", "BonoDescuento").put("porcentaje", bono.getPorcentaje()).put("estado", bono.getEstado())
					.put("enCuenta", bonosEnCuenta.contains(bono));
		}
		return json;
	}

	private Premio premioDesdeJSON(JSONObject json) {
		String idPremio = json.getString("idPremio");
		String origen = json.optString("idTorneoOrigen", idPremio.substring(0, Math.max(0, idPremio.lastIndexOf('-'))));
		if (json.getString("clase").equals("PremioEfectivo")) {
			return new PremioEfectivo(idPremio, origen, json.getInt("posicion"), json.getString("beneficiario"),
					fechaDesdeJSON(json.getString("fecha")), json.getFloat("valor"));
		}
		BonoDescuento bono = new BonoDescuento(idPremio, origen, json.getInt("posicion"), json.getString("beneficiario"),
				fechaDesdeJSON(json.getString("fecha")), json.getFloat("porcentaje"));
		bono.restaurarEstado(json.getString("estado"));
		return bono;
	}
}
