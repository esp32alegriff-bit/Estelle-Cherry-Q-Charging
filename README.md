# Estelle Cherry Q — Charging app

A simple Android home-control app for the **FARRUSCO Cherry Q** relay system.

## Approved design

Dark automotive UI with cherry-pink branding, bright green ON states and bright red OFF states.

Screens:

- **Home** — clear Eskom and EV relay status
- **Control** — large individual relay controls plus START/STOP CHARGING
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

That API still needs to be added to the relay firmware via OTA. Until then, the app will show the relay as unavailable rather than pretending a command succeeded.

## Build the APK for free

GitHub Actions builds a debug APK on every push to `main`.

1. Open the repository **Actions** tab.
2. Open the latest **Android APK** workflow run.
3. Download the `Estelle-Cherry-Q-debug-apk` artifact.
4. Install `app-debug.apk` on the Android phone.

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

`1.0.0`
