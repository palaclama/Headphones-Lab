# Headphones Lab

Prototipo Android sin permisos especiales:
- Generador de tonos ajustable (60–12.000 Hz).
- Prueba de canal izquierdo y derecho.
- Barrido de frecuencias de 60 Hz a 12 kHz en 10 segundos.
- Las pruebas se detienen automáticamente tras 10 o 15 segundos.

## Compilar con GitHub Actions
1. Subí todos los archivos y carpetas al repositorio GitHub.
2. Abrí Actions y ejecutá “Build APK”, o hacé push a `main`.
3. Descargá el artifact `Headphones-Lab-APK` y extraé `app-debug.apk`.

## Seguridad
Empezá con volumen bajo. Los tonos no están calibrados a un nivel de presión sonora exacto. No uses tonos para diagnosticar tu audición. Detené la prueba si sentís molestia, zumbido o mareo. La app no añade ANC ni controla funciones internas de los auriculares.
