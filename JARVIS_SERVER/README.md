# JARVIS AI Server

Servidor HTTP para conectar la app Android JARVIS con un modelo de IA.

## 1. Instalar

Desde la raíz del repositorio:

```bash
cd JARVIS_SERVER
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## 2. Configurar la clave

Copia `.env.example` a `.env` y define `OPENAI_API_KEY`.

No subas nunca la clave real a GitHub.

## 3. Arrancar en Codespaces

```bash
export OPENAI_API_KEY="TU_CLAVE"
uvicorn main:app --host 0.0.0.0 --port 8000
```

Cuando Codespaces detecte el puerto 8000, abre/usa la URL reenviada públicamente y añade `/chat`.

Ejemplo de endpoint para JARVIS:

```
https://TU-URL-DE-CODESPACES/chat
```

## 4. Probar

```bash
curl http://127.0.0.1:8000/health
```

Después:

```bash
curl -X POST http://127.0.0.1:8000/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hola JARVIS"}'
```

La app Android ya envía el formato `{"message":"..."}` y acepta una respuesta JSON con el campo `reply`, por lo que este servidor encaja directamente con `JarvisBrain.kt`.
