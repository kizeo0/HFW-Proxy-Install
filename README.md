HFW Proxy Install
El propósito es Redirigir update a un servidor customizado que alberga el firmware modificado (HFW 4.92 ACTUALMENTE) si sale un nuevo HFW lo actualizaré automaticamente



🛠️ Guía de Instalación: HFW 4.92 vía Proxy
Sigue estos pasos cuidadosamente para redirigir la actualización de tu consola hacia el servidor personalizado.

1. Preparación de Red
Misma Red: Asegúrate de que tanto tu celular (donde corre la app) como tu PS3 estén conectados exactamente a la misma red Wi-Fi.

2. Activar el Servidor (App)
En tu celular, presiona el botón verde "INICIAR".

El estado de la aplicación debe cambiar a ONLINE.

No cierres la app: Toma nota de la IP (Servidor) y el Puerto que aparecen en pantalla.

3. Configuración del Proxy en PS3
En la consola, realiza los siguientes ajustes:

Ve a Ajustes de Red > Ajustes de conexión a Internet > Personalizar.

Avanza por las opciones predeterminadas hasta llegar a "Servidor Proxy".

Selecciona "Usar".

Introduce los datos que anotaste de la aplicación:

Dirección: (Tu IP)

Puerto: (Tu Puerto)

Termina de avanzar y guarda los ajustes (no es necesario realizar la prueba de conexión si el servidor ya está iniciado).

4. Búsqueda de Actualización
En el menú de la PS3, ve a Actualización del sistema.

Selecciona Actualizar mediante Internet.

5. Instalación del Hybrid Firmware (HFW)
Detección: La consola detectará una actualización.

Nota importante: Puede aparecer como versión 9.00 o similar; esto es normal, es el identificador que usamos para inyectar el HFW 4.92.

Proceso: Acepta los términos y procede con la descarga e instalación.

6. Finalización
Una vez que la instalación termine y la PS3 se reinicie, el proceso habrá concluido.

Ya puedes presionar "DETENER" en la aplicación del celular para apagar el servidor.









Créditos y Fuentes de Inspiración:
    
    Concepto Base del Proxy PS3 en Android: Proviene de proyectos como el PS3 Proxy Server for Android de Edw590.

        PSX-Place: PS3 Proxy Server for Android v2.2 (Universal)

        GitHub: Edw590/PS3-Proxy-Server-for-Android

    Comunidad y Solución de Problemas:

        El Otro Lado: Acceso a PSN con PS3 Proxy Server en firmware 4.82 OFW

        PSX-Place: Hilo de discusión PS3 Proxy Server for Android v2.2 Universal

    Referencia Funcional: El script en Python suministrado por el usuario (ps3proxy (2).py) fue el modelo exacto de comportamiento a replicar, especialmente en el manejo de peticiones HEAD y el tráfico de "passthrough".
