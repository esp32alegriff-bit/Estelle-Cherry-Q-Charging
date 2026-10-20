# Estelle Cherry Q — Charging app

A home LAN Android control app for the **FARRUSCO Cherry Q** relay system.

## Approved design

EF_1.2 uses a compact dark automotive dashboard with purple, cyan, green, and red neon states. All navigation and control icons are vectors.

Screens:

- **Home** — clear Eskom and EV relay status
- **Control** — compact individual relay controls plus START/STOP CHARGING
- **Settings** — local relay address, local control token, connection test
- **About** — app/device information

## Home network only

The app is designed for use at home on either:

- `Farrusco` 2.4 GHz
- `Farrusco_5G` 5 GHz

Both Wi-Fi bands must be on the same LAN and able to reach the relay.

Default relay address:

```
http://farrusco-cherryq-relay.local
```

The app contains **no Wi-Fi password, OTA password or relay token**.

## Important integration status

The relay already has working Wi-Fi + OTA + ESP-NOW. Android cannot send ESP-NOW directly, so the app uses a tiny local HTTP control API defined in:

`docs/relay-api-contract.md`

The relay LAN API is installed and confirmed working. The app continues to use the address and local control token saved on the phone. Updating the app does not replace the saved address, including a working IP such as `http://192.168.1.127`.

## Build the APK for free

GitHub Actions builds a permanently release-signed APK on every push to `main`. A version tag publishes it as a GitHub Release. See [release signing](docs/release-signing.md).

1. Open the repository **Actions** tab.
2. Open the latest **Android APK** workflow run or the EF_1.2 GitHub Release.
3. Download `Estelle-Cherry-Q-Charging-EF_1.2.apk` from the release.
4. An existing EF_1.1 debug-signed install may need one uninstall; EF_1.2 and later releases use the same permanent key.

No paid Android tooling is required.

## Safety

- Home LAN only
- No cloud relay control
- No AUTO SOC switching
- Existing relay fail-safes remain authoritative
- Relay firmware validates a local API token
- START/STOP charging uses a defined safe sequencing contract

## Package

`za.co.farrusco.estellecherryq`

## Version

`EF_1.2` (versionCode 3)
