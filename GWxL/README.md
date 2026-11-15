# GWxL

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

## How to get an APK on your phone (no computer needed)

1. Create a new **public** repository on GitHub named `GWxL` (or any name).
2. Upload all the files from this project into the repository (you can do this from the GitHub mobile website or app).
3. Go to the **Actions** tab of your repository.
4. You should see a workflow called **Build APK**. Run it (or just push any small change).
5. When it finishes, open the successful run → scroll to **Artifacts** → download `GWxL-debug`.
6. On your phone, open the downloaded zip, extract `app-debug.apk`, and install it (you may need to allow “Install from unknown sources” for your browser/file manager).

That’s it. Every time you push a change, a new APK will be built automatically.

## Notes

- First build can take 3–6 minutes.
- Location permission is requested so it can show weather for where you are.
- If location is denied it falls back to a central US location.
- This is a debug APK (fine for personal use).
