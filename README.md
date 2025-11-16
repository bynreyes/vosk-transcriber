# UTS-transcribe 🚀

Proyecto de clase de OOP with JAVA

Autores: noria y ingcognito 🧑‍🎓

Descripción
-----------
UTS-transcribe es un proyecto de transcripción de audio basado en Vosk para Java. Permite transcribir audios desde archivos y desde el micrófono en tiempo real. Las transcripciones se guardan en la carpeta `transcriptions/` en la raíz del proyecto. 🎧✍️

Requisitos ✅
----------
- Java 11+ instalado
- Gradle wrapper incluido (se utiliza `gradlew.bat` en Windows)
- Modelo de Vosk (por ejemplo `vosk-model-es-0.42`) colocado en la carpeta `models/` en la raíz del proyecto

Instalación rápida 🛠️
------------------
1. Clona o descarga este repositorio.
2. Coloca el modelo de Vosk para español en `./models/vosk-model-es-0.42`.
3. En Windows (cmd.exe) compila:

```cmd
.\gradlew.bat build
```

Cómo usar ▶️
---------
Hay 3 modos principales:

1) Transcribir un archivo concreto 📁

```cmd
.\gradlew.bat run --args="--file C:\ruta\a\audio.m4a"
```

Esto generará `transcriptions/<nombre_del_archivo>.txt` con la transcripción.

2) Transcribir todos los archivos de una carpeta 📂

```cmd
.\gradlew.bat run --args="--dir src\main\resources\audio"
```

Los textos resultantes se guardan en `transcriptions/`.

3) Transcripción en tiempo real desde el micrófono 🎙️

```cmd
.\gradlew.bat run --args="--mic"
```

Mientras se transcribe en tiempo real, las frases completas se mostrarán por consola y se irán guardando (append) en `transcriptions/live_transcription.txt`. Pulsa Ctrl+C para detener; la transcripción final también se guardará.

Estructura del proyecto (resumen) 🗂️
---------------------------------
- `src/main/java/...` : código fuente Java
- `models/`           : modelos Vosk (no incluido por licencia)
- `transcriptions/`   : carpeta donde se guardan los textos generados

Roadmap / Características propuestas 🛣️
------------------------------------
Prioridad alta 🔥
- 🖥️ Interfaz gráfica de usuario (GUI) para:
  - Seleccionar archivos o carpetas
  - Iniciar/parar transcripción en tiempo real
  - Visualizar y editar las transcripciones
- 🎙️ Detección de interlocutores (speaker diarization)
- 📝 Generar un resumen automático del contenido del audio o conversación
- 🧾 Integración con una plantilla de Writer para su posterior edicion.
- 🧾 considerar otros formatos, html, MarckDown, etc.

Ideas a medio plazo ⚙️
- 🌍 Soporte multi-idioma con selección de modelo
- ✍️ Mejor gestión de puntuación y mayúsculas a partir del texto bruto
- ⏱️ Marcas de tiempo por frase y exportación a formatos con timestamps (subtitulos, archivos .srt)

Ideas a largo plazo 🚧
- 🌐 Interfaz web para subir audios y ver transcripciones
- ☁️ Integración con servicios de almacenamiento (Google Drive, Dropbox)
- 🤖 Integración con asistentes virtuales (Alexa, Google Assistant, otros)

Contribuir 🤝
----------
1. Haz un fork del repositorio
2. Crea una rama con la funcionalidad: `feature/mi-feature`
3. Envía un pull request describiendo los cambios

Licencia 📜
--------
MIT - revisa `LICENSE` si deseas añadirla. Se recomienda incluir un archivo `LICENSE` con el texto estándar si quieres publicar el proyecto.

Notas técnicas y recomendaciones 🧭
--------------------------------
- El proyecto usa la librería Vosk; los modelos no están incluidos por razones de licencia y tamaño. Descarga el modelo de español desde https://alphacephei.com/vosk/models y colócalo en `models/`.
- Para transcripción en tiempo real, asegúrate de que tu micrófono esté accesible y que Java Sound pueda abrir la línea.
- Si ves errores relacionados con dependencias nativas (Vosk), revisa la versión de la dependencia en `build.gradle.kts` y las instrucciones de Vosk para Java.

Contacto y autores ✉️
------------------


Agradecimientos 🙏
----------------
- Vosk (https://alphacephei.com/vosk) por el motor de reconocimiento de voz.
