# GwXL

Simple, fast weather app inspired by the classic Dark Sky experience.

- Current conditions
- Next-hour precipitation feel
- Clean hourly strip
- 7-day forecast
- Dark mode only
- Temperatures in **Fahrenheit**
- Wind speed in **knots**
- Powered by free Open-Meteo data
- No ads

## How to get the APK on your phone

1. Open **Releases** on this repo on your phone: https://github.com/gsingle/GwXL/releases/latest
2. Tap **GwXL-debug.apk** to download it.
3. Open it and install (you may need to allow "Install unknown apps" for your browser).

Every push to `main` also builds a fresh APK in the **Actions** tab (artifact `GwXL-debug`).
To publish a new release, push a tag like `v0.2.0`; the workflow attaches the APK to a GitHub Release automatically.

## Notes

- First build can take 3–6 minutes.
- Location permission is requested so it can show weather for where you are.
- If location is denied it falls back to a central US location.
- This is a debug APK (fine for personal use).
