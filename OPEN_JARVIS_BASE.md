# JARVIS — definitive build base

This branch keeps the original JARVIS Android ZIP untouched and prepares the repository around Open JARVIS as the working base.

## Current base

Upstream: https://github.com/tokenarc/open-jarvis

The upstream project provides Android 8+, Accessibility control, Compose UI, Anthropic/Claude support, OCR/screen vision, voice STT/TTS, memory, skills, MCP, scheduling, notification intelligence, confirmation for risky actions, and other agent features.

## Build

The GitHub Actions workflow in `.github/workflows/build-jarvis.yml` checks out the upstream base and builds a debug APK. The APK is published as the workflow artifact `JARVIS-debug-apk`.

## Next customization

After the first successful build we will move the source into this repository and customize:

- JARVIS name/branding and Iron Man HUD
- Claude as primary brain
- voice activation
- camera/screen vision
- Android actions
- persistent memory
- secure API-key storage
- Spanish voice and interface
- APK release pipeline
