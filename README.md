| Integrante | Código |
| --- | --- |
| David Triana | 202410344 |
| David Herrera | 202314421 |
| David Martínez Jaramillo | 202513297 |

# Proyecto 2 - Entrega única - Club Deportivo Séneca

## ¿De qué trata?

Sistema del Club Deportivo Séneca que trabajamos en la primera entrega. Es una aplicación de consola hecha en Java para manejar usuarios, equipos, jugadores, reservas, entrenamientos, tienda, cafetería y otras actividades del club (como funciones relacionadas con torneos recreativos y competitivos) y la inscripción de participantes, los cupos preferenciales y los premios. También se permite registrar estudiantes de la universidad que no sean socios del club y personas externas.

El programa tiene distintos menús dependiendo del usuario que inicie sesión.

## 1. Organización de las carpetas

La carpeta que se debe abrir en Eclipse es `ClubSeneca`, que está dentro de `Entrega única`.

```text
Entrega única/
├── ClubSeneca/
│   ├── src/           Código de la aplicación
│   ├── test/          Pruebas JUnit
│   ├── lib/           Librerías que utiliza Java
│   └── datos/         Archivos JSON
├── Docs/              Diagramas y documentos del proyecto
├── evidencias/        Capturas de ejecución
└── README.md
```

Al revisar el código se pueden encontrar paquetes como `usuarios`, `interfaz`, `controladores`, `torneos`, `premios` y `persistencia`.

## 2. Cómo abrir el proyecto

Para ejecutarlo se necesita:

- Eclipse IDE para Java.
- JDK 17.
- El proyecto descargado y descomprimido.

Las librerías que necesita el proyecto vienen en `ClubSeneca/lib`, por lo que no hay que descargarlas aparte.

Pasos para importarlo:

1. Abrir Eclipse.
2. Ir a File > Import.
3. Seleccionar General > Existing Projects into Workspace.
4. En Select root directory, buscar la carpeta `Entrega única/ClubSeneca`.
5. Seleccionar el proyecto que aparece y dar clic en Finish.
6. Verificar que Eclipse no muestre errores en el proyecto.


## 3. Cómo ejecutar el programa

Dentro de `ClubSeneca/src/interfaz`, buscar `CentralInterfaz.java`. Hacer clic derecho y seleccionar Run As > Java Application.

La consola mostrará un menú para iniciar sesión, registrarse o salir. También existen otras clases con `main` para abrir directamente la interfaz de cada rol:

| Clase | Para qué sirve |
| --- | --- |
| `CentralInterfaz` | Menú general y registro de nuevos participantes |
| `InterfazAdministrador` | Opciones del administrador |
| `InterfazSocio` | Opciones del socio |
| `InterfazEmpleado` | Opciones de entrenadores y fisioterapeutas |
| `InterfazParticipante` | Opciones de participantes de torneos |

Todas están en el paquete `interfaz`.

## 4. Ejemplo de uso: registrar un participante

Este ejemplo se hizo desde `CentralInterfaz.java`. Las siguientes imágenes muestran los pasos que seguimos en la consola de Eclipse.

**Paso 1. Registro.** Escogemos la opción `2. Registrarse`, seleccionamos `externo` y escribimos los datos. El sistema confirma que el registro fue exitoso.

![Registro del usuario](evidencias/pasos/paso1_registro.png)

**Paso 2. Inicio de sesión.** Seleccionamos `1. Iniciar sesión` e ingresamos con el usuario creado. Se muestra el menú del participante.

![Inicio de sesión](evidencias/pasos/paso2_ingreso.png)

**Paso 3. Consulta de torneos.** Elegimos `1. Consultar torneos`. Dejamos los filtros vacíos para ver todos los torneos disponibles. En esta prueba no se encontraron torneos que coincidieran.

![Consulta de torneos](evidencias/pasos/paso3_consulta.png)

**Paso 4. Cerrar sesión.** Elegimos `0. Cerrar sesión` y regresamos al menú inicial.

![Cierre de sesión](evidencias/pasos/paso4_cierre.png)

## 5. Comprobación de persistencia

El programa usa archivos JSON que están en `ClubSeneca/datos`. La idea es que los datos registrados se conserven después de cerrar la aplicación.

Para comprobarlo hicimos lo siguiente:

1. Registramos un participante como se muestra arriba.
2. Cerramos la sesión y salimos completamente del programa.
3. Ejecutamos otra vez `CentralInterfaz.java`.
4. Iniciamos sesión con las mismas credenciales, sin registrar de nuevo el usuario.

El inicio de sesión funcionó después de volver a abrir la aplicación.

![Ingreso después de reiniciar](evidencias/pasos/paso5_reinicio.png)

## 6. Cómo ejecutar las pruebas JUnit

Las pruebas están dentro de la carpeta `ClubSeneca/test`. Para ejecutarlas:

1. En Eclipse, desplegar el proyecto y localizar `test`.
2. Hacer clic derecho sobre la carpeta.
3. Seleccionar Run As > JUnit Test.
4. Esperar a que terminen y revisar la ventana de JUnit.

![Opción para ejecutar las pruebas](evidencias/pasos/paso6_junit_ejecucion.png)

En la ejecución que hicimos se completaron 341 de 341 pruebas, sin errores ni fallos. Si se modifica el código, es necesario volver a ejecutarlas para verificar que todo siga funcionando.

![Resultado de JUnit](evidencias/pasos/paso7_junit_resultado.png)

Las pruebas incluyen clases del funcionamiento general del club, torneos, usuarios, persistencia y pruebas de integración de los requerimientos.

## 7. Diagramas y documentación

En `Docs` están los documentos del diseño y los diagramas UML. Hay diagramas generales y detallados para las diferentes partes de la aplicación, incluidos los paquetes nuevos de torneos y premios.

También se encuentran:

- `NuevaInformacion.odt`: información agregada para el Proyecto 2.
- `Responsabilidades nuevas.ods`: tablas de responsabilidades y cambios.
- `UML/`: diagramas de clases y archivos editables.
- `colaboraciones/`: diagramas que muestran cómo interactúan los objetos en algunos casos.

