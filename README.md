# HFW-Proxy-Install
El propósito es interceptar la petición oficial del archivo de actualización del sistema y redirigirla a un servidor customizado que alberga el firmware modificado (HFW).

## ⚙️ Configuración y Uso (PS3 HFW)

            Este proceso desvía la actualización oficial de Sony hacia el Hybrid Firmware (HFW) 4.92.

            1.  **Conexión de Red:**
                Conecta tu celular y tu consola PS3 a la **misma red Wi-Fi**.

            2.  **Configuración de Proxy en PS3:**
                * En la PS3, ve a **Ajustes** > **Ajustes de Red** > **Ajustes de conexión a Internet** > **Personalizar**.
                * Avanza hasta el **Servidor Proxy**.
                * Selecciona **Usar** e introduce la **IP (Servidor)** y el **Puerto (8080)** que aparecen en la pantalla principal de la aplicación en tu celular.

            3.  **Activación del Servidor:**
                Pulsa el botón **INICIAR** en tu aplicación de Android. El estado cambiará a **ONLINE**.

            4.  **Descarga del HFW:**
                * En la PS3, ve a **Actualización del sistema** > **Actualizar mediante Internet**.
                * La consola detectará la actualización. **IMPORTANTE:** La versión mostrada puede ser 9.00 (esto es solo un identificador), pero se instalará el **HFW 4.92**.

            5.  **Instalación y Fin:**
                Espera a que termine la instalación. Una vez finalizada, puedes pulsar **DETENER** en la aplicación para apagar el servid



Documentación Técnica Completa del PS3 Proxy Server para Android (Modo Redirección HFW)

La presente documentación detalla la arquitectura, el código fuente y el proceso de depuración del proyecto de aplicación Android, cuyo objetivo es replicar el funcionamiento de un Proxy HTTP especializado para la consola PlayStation 3 (PS3). El propósito es interceptar la petición oficial del archivo de actualización del sistema (ps3-updatelist.txt) y redirigirla a un servidor customizado que alberga el firmware modificado (HFW), permitiendo su instalación.

Créditos y Fuentes de Inspiración:

Este proyecto se basa en la necesidad de portar la funcionalidad de scripts de escritorio (como el Python suministrado) a una plataforma móvil. El concepto de utilizar un dispositivo Android como proxy para la PS3 y la comunidad que lo soporta es fundamental.

    Concepto Base del Proxy PS3 en Android: Proviene de proyectos como el PS3 Proxy Server for Android de Edw590.

        PSX-Place: PS3 Proxy Server for Android v2.2 (Universal)
https://www.psx-place.com/resources/ps3-proxy-server-for-android-v2-2-universal.795/

        GitHub: Edw590/PS3-Proxy-Server-for-Android
https://github.com/Edw590/PS3-Proxy-Server-for-Android
    Comunidad y Solución de Problemas:

        El Otro Lado: Acceso a PSN con PS3 Proxy Server en firmware 4.82 OFW
https://www.elotrolado.net/hilo_acceso-a-psn-con-ps3-proxy-server-en-firmware-4-82-ofw_2306364

        PSX-Place: Hilo de discusión PS3 Proxy Server for Android v2.2 Universal
https://www.psx-place.com/resources/ps3-proxy-server-for-android-v2-2-universal.795/
    Referencia Funcional: El script en Python suministrado por el usuario (Kizeo) fue el modelo exacto de comportamiento a replicar, especialmente en el manejo de peticiones HEAD y el tráfico de "passthrough".

1. Arquitectura y Principios de Operación

El servidor proxy opera bajo un modelo de inspección y reenvío (man-in-the-middle). Cada petición HTTP/HTTPS que la PS3 intenta realizar, pasa primero por el servidor Android (el proxy) que decide cómo responder.
A. Proxy Passthrough (Tráfico Transparente)

La PS3 realiza múltiples comprobaciones de red (DNS, conexión a servidores de Sony, etc.) antes de solicitar el archivo de actualización. Si estas peticiones fallan o son rechazadas, la PS3 arroja errores genéricos de conexión (como el famoso Error 8071053D).

Solución Implementada: Si la petición no es el archivo de actualización, el proxy de Android actúa como un intermediario transparente. Recoge la petición HTTP de la PS3, la realiza a Internet (a los servidores reales de Sony, Google, etc.), recibe la respuesta y reenvía exactamente esa respuesta (incluyendo el código de estado, headers y cuerpo) de vuelta a la PS3. Esto garantiza que las comprobaciones de red pasen con éxito.
B. Intercepción y Redirección (El Ataque)

    Intercepción: El proxy escanea la URL de cada petición. Cuando detecta el patrón del archivo de actualización oficial (por ejemplo, fmx01.ps3.update.playstation.net/.../ps3-updatelist.txt), detiene la comunicación.

    Redirección: En lugar de reenviar la petición a Sony, el proxy responde directamente a la PS3 con un código de estado HTTP/1.1 302 Found. Este código incluye un nuevo header llamado Location que apunta al enlace del firmware modificado (REDIRECT_URL). La PS3, al recibir un 302, automáticamente ignora la URL original y sigue la nueva URL suministrada en Location.

    Control de Bucle: Se implementó una verificación crucial para evitar que el proxy redirija su propio enlace (REDIRECT_URL), previniendo un bucle de redirección infinito que causaba fallos en la PS3.

2. Estructura del Código

El proyecto está escrito en Kotlin para la lógica de la aplicación y el servidor, utilizando la librería estándar de Java (java.net.*) para la funcionalidad de red.
2.1. AndroidManifest.xml (Permisos)

El manifiesto es vital para asegurar que la aplicación pueda ejecutar un servidor de red.
Permiso	Propósito
android.permission.INTERNET	Permitir a la aplicación acceder a Internet para el tráfico de passthrough.
android.permission.ACCESS_NETWORK_STATE	Obtener información sobre la IP local del dispositivo (esencial para mostrar la IP en la UI).
android:usesCleartextTraffic="true"	Necesario porque la PS3 generalmente usa HTTP simple (no encriptado) para las peticiones de actualización.
2.2. res/layout/activity_main.xml (Interfaz de Usuario)

El diseño se creó para ser limpio y funcional, siguiendo la referencia visual del usuario (IP y Puerto grandes, botón central de START/STOP).

    txtIp y txtPort: Muestran la información clave de configuración para la PS3.

    btnStartStop: Inicia/Detiene el servidor en un hilo de ejecución separado.

    txtLogs dentro de un ScrollView: Es el componente de diagnóstico más importante, ya que muestra el log en tiempo real con el mismo formato que el script de Python, facilitando la depuración.

    btnExportLogs: Utiliza la API moderna de almacenamiento de Android (ActivityResultContracts.CreateDocument) para guardar los registros en un archivo .txt sin requerir permisos de almacenamiento obsoletos.

2.3. MainActivity.kt (Lógica Central del Proxy)

La clase MainActivity es el corazón de la aplicación, manejando la UI, el ciclo de vida del servidor y la lógica de red.
Variables de Configuración
Kotlin

private val REDIRECT_URL = "xxxxxxxxxxxxxxxxx/ps3-updatelist.txt"
private val SERVER_PORT = 8080
private val threadPool = Executors.newCachedThreadPool() // Para manejar múltiples clientes concurrentes

Función handleClient(client: Socket)

Esta es la función más crítica, que se ejecuta en un hilo separado para cada conexión entrante de la PS3.
Lógica	Descripción	Objetivo
Paso 1: Lectura de Petición	Lee la línea de estado (GET /url HTTP/1.1) y todos los headers del cliente (PS3).	Obtener method, urlString y todos los headers para reenvío.
Paso 2: Manejo de CONNECT	Si method == "CONNECT", responde HTTP/1.1 200 Connection Established.	Permite a la PS3 proceder con conexiones HTTPS/PSN (aunque solo sea para el handshake inicial), evitando el error de conexión.
Paso 3: Condición de Redirección	Verifica si la URL contiene ps3-updatelist.txt Y si no contiene el dominio de la URL de redirección. Soluciona el error de Bucle Infinito de Redirección (v3). Asegura que solo se intercepte el enlace oficial de Sony.
Paso 4: Redirección (302)	Si la condición se cumple, el proxy responde con un HTTP/1.1 302 Found y el header Location: $REDIRECT_URL. Ejecuta el objetivo del exploit: desvía la petición de actualización.
Paso 5: Proxy Passthrough	Si la petición no es CONNECT ni el archivo oficial, llama a proxyPassThrough.	Permite que el tráfico normal (pruebas de conexión, otros archivos) fluya de manera transparente, solucionando el Error 8071053D.
Función proxyPassThrough() (Replicando Python/urllib)

Esta función es el corazón del passthrough transparente:

    Establecimiento de Conexión: Abre una conexión HttpURLConnection a la URL solicitada por la PS3.

    Copia de Headers (PS3 -> Internet): Itera sobre los headers recibidos de la PS3 y los establece en la conexión real a Internet. Esto es vital para enviar User-Agent o Host correctos, lo que evita que los servidores de Sony rechacen la conexión.

    Copia de Headers (Internet -> PS3): Una vez que se recibe la respuesta de Internet, copia la línea de estado (HTTP/1.1 200 OK) y todos los headers (excluyendo los problemáticos como Transfer-Encoding y Connection) de vuelta a la PS3.

    Manejo de HEAD: Si el método es HEAD (usado por la PS3 para comprobar si un archivo existe y su tamaño antes de descargarlo), el código envía los headers y omite el cuerpo, replicando el comportamiento de do_HEAD en el script de Python.

    Reenvío del Cuerpo: Para GET, el cuerpo de la respuesta se lee del servidor real y se escribe en el stream de salida del socket de la PS3, completando la transferencia.

    Manejo de Errores: Cualquier fallo en la conexión a Internet resulta en un HTTP/1.1 502 Bad Gateway a la PS3, dando una respuesta controlada.

3. Depuración y Evolución de las Soluciones

El desarrollo de este proxy requirió solucionar tres problemas clave:
Problema Reportado	Causa Técnica	Solución en Kotlin
Error 8071053D (Fallo de conexión en PS3)	El proxy inicial solo manejaba la redirección y rechazaba las peticiones HTTP normales (pruebas de conexión, DNS), haciendo que la PS3 asumiera que no había Internet.	Se implementó la función proxyPassThrough para actuar como un proxy transparente para todo el tráfico no crítico, permitiendo que las pruebas de conexión pasen con éxito.
Fallo en la Redirección/Comprobación	El proxy no enviaba los encabezados (HEAD, Host, User-Agent) correctamente al servidor real durante el Passthrough.	Se implementó la copia manual de todos los clientHeaders de la PS3 a la conexión de HttpURLConnection, asegurando que la PS3 obtenga respuestas precisas.
Bucle de Redirección (Visto en los logs del usuario)	La lógica de intercepción usaba solo la palabra clave ps3-updatelist.txt. Después de la primera redirección, la PS3 pedía la nueva URL, que ¡también contenía esa palabra clave!, causando que el proxy la redirigiera de nuevo a sí mismo sin fin.	Se agregó la condición de exclusión: if (isUpdateList && !isMyCustomLink). Esto garantiza que la redirección solo ocurra si la URL es el servidor oficial de Sony y no el servidor custom.
