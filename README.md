# HFW Proxy Install

El propósito es redirigir update a un servidor customizado que alberga el firmware modificado (**HFW 4.92 ACTUALMENTE**). Si sale un nuevo HFW lo actualizaré automaticamente.




<p align="center">
    <img width="25%" height="25%" alt="logo2" src="https://github.com/user-attachments/assets/3d79f637-aab2-438a-982c-ab1447bbad85" />
</p>

<h1 align="center"> HFW Proxy Install 3.0 </h1>
<p  align="center">
</p>



## 🛠️ Guía de Instalación: HFW 4.92 vía Proxy

Sigue estos pasos cuidadosamente para redirigir la actualización de tu consola hacia el servidor personalizado.

### 1. Preparación de Red

* **Misma Red:** Asegúrate de que tanto tu celular (donde corre la app) como tu PS3 estén conectados exactamente a la misma red Wi-Fi.

### 2. Activar el Servidor (App)

1. En tu celular, presiona el botón verde **"INICIAR"**.
2. El estado de la aplicación debe cambiar a **ONLINE**.
3. **No cierres la app:** Toma nota de la IP (Servidor) y el Puerto que aparecen en pantalla.

### 3. Configuración del Proxy en PS3

En la consola, realiza los siguientes ajustes:

1. Ve a `Ajustes de Red` > `Ajustes de conexión a Internet` > `Personalizar`.
2. Avanza por las opciones predeterminadas hasta llegar a **"Servidor Proxy"**.
3. Selecciona **"Usar"**.
4. Introduce los datos que anotaste de la aplicación:
* **Dirección:** (Tu IP)
* **Puerto:** (Tu Puerto)


5. Termina de avanzar y guarda los ajustes (no es necesario realizar la prueba de conexión si el servidor ya está iniciado).

### 4. Búsqueda de Actualización

1. En el menú de la PS3, ve a `Actualización del sistema`.
2. Selecciona **Actualizar mediante Internet**.

### 5. Instalación del Hybrid Firmware (HFW)

* **Detección:** La consola detectará una actualización.

> **Nota importante:** Puede aparecer como versión 9.00 o similar; esto es normal, es el identificador que usamos para inyectar el HFW 4.92.

* **Proceso:** Acepta los términos y procede con la descarga e instalación.

### 6. Finalización

* Una vez que la instalación termine y la PS3 se reinicie, el proceso habrá concluido.
* Ya puedes presionar **"DETENER"** en la aplicación del celular para apagar el servidor.

---

## Arquitectura

El servidor proxy opera bajo un modelo de inspección y reenvío (*man-in-the-middle*). Cuando detecta el archivo oficial (`ps3-updatelist.txt`), responde con un código `HTTP/1.1 302 Found` apuntando al HFW.

## Créditos y Fuentes de Inspiración

* **PSX-Place:** [PS3 Proxy Server for Android v2.2 (Universal)](https://www.psx-place.com/resources/ps3-proxy-server-for-android-v2-2-universal.795/) | [Thread](https://www.psx-place.com/threads/ps3-proxy-server-for-android-v2-2-universal.23020/)
* **GitHub:** [Edw590/PS3-Proxy-Server-for-Android](https://github.com/Edw590/PS3-Proxy-Server-for-Android)

## Comunidad y Solución de Problemas

* **El Otro Lado:** [Acceso a PSN con PS3 Proxy Server](https://www.elotrolado.net/hilo_acceso-a-psn-con-ps3-proxy-server-en-firmware-4-82-ofw_2306364)
