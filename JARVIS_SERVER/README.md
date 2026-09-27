# JARVIS AI Server

Servidor HTTP para conectar la app Android JARVIS con un modelo de IA mediante la Responses API.

## 1. Instalar

Desde la raíz del repositorio:

```bash
cd JARVIS_SERVER
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## 2. Configurar las claves de forma segura

No guardes claves reales en Git ni las pegues en el chat.

En GitHub, ve a **Settings → Codespaces → Codespaces secrets → New secret** y crea:

- `OPENAI_API_KEY` — tu clave de API de OpenAI.
- `JARVIS_API_KEY` — una clave larga y aleatoria que usará la app Android para autenticarse.

Concede acceso de los secretos al repositorio `Aaron-debug-sdg/Jarvis-android`. GitHub expone estos secretos como variables de entorno dentro de Codespaces. Si los creas mientras el Codespace ya está abierto, reinícialo para que aparezcan.

El modelo predeterminado es `gpt-5.6-luna`. Es un modelo disponible mediante la Responses API.

## 3. Arrancar en Codespaces

Después de reiniciar el Codespace y volver a abrir el terminal:

```bash
cd JARVIS_SERVER
source .venv/bin/activate
uvicorn main:app --host 0.0.0.0 --port 8000
```

El servidor **no arranca una conversación con OpenAI hasta recibir una petición** y la clave de OpenAI nunca se envía a la aplicación Android.

## 4. Exponer el servidor para el móvil

En la pestaña **Ports** de Codespaces, localiza el puerto `8000` y configura su visibilidad como **Public**. Codespaces genera una URL del tipo:

```
https://NOMBRE-DEL-CODESPACE-8000.app.github.dev
```

Para JARVIS, el endpoint será esa URL seguida de:

```
/chat
```

Por ejemplo:

```
https://NOMBRE-DEL-CODESPACE-8000.app.github.dev/chat
```

Los puertos reenviados públicos son accesibles por cualquiera que conozca la URL, por eso JARVIS exige `JARVIS_API_KEY` antes de aceptar peticiones.

## 5. Probar

Comprobar estado:

```bash
curl http://127.0.0.1:8000/health
```

Debe mostrar que `openai_configured` y `jarvis_key_configured` son `true`.

Probar el chat desde el propio Codespace:

```bash
curl -X POST http://127.0.0.1:8000/chat \
  -H "Content-Type: application/json" \
  -H "X-Jarvis-Key: $JARVIS_API_KEY" \
  -d '{"message":"Hola JARVIS"}'
```

La app Android ya envía `{"message":"..."}`, añade `X-Jarvis-Key` y acepta la respuesta JSON `{"reply":"..."}`, por lo que este servidor encaja directamente con `JarvisBrain.kt`.
