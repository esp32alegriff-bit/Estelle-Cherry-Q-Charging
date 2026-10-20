# Relay LAN API contract

The Android app is intentionally **local-home-network only**. Android does not send ESP-NOW directly, so the relay firmware needs a tiny authenticated HTTP API alongside the existing OTA service.

## Device

- Hostname: `farrusco-cherryq-relay.local`
- Current LAN example: `192.168.1.127`
- Relay MAC: `30:76:F5:30:72:00`
- Wi-Fi/ESP-NOW channel: `6`
- Relay 1: GPIO17 / Eskom input
- Relay 2: GPIO16 / EV charger
- AUTO remains disabled.

## Authentication

Every command endpoint should accept:

```
X-Farrusco-Token: <local secret>
```

The secret **must not** be committed to GitHub. Store it in the relay's protected local secrets and enter the same value once in the Android app Settings screen.

## Endpoints

### GET /api/v1/status

Response:

```json
{
  "relay1_on": false,
  "relay2_on": false,
  "message": "Connected"
}
```

### POST /api/v1/relay/1/on
### POST /api/v1/relay/1/off
### POST /api/v1/relay/2/on
### POST /api/v1/relay/2/off

Return the same JSON state object after the command has been applied.

### POST /api/v1/charge/start

Safe sequence:

1. Relay 1 / Eskom ON.
2. Confirm actual GPIO17 readback HIGH.
3. Delay a short settling period.
4. Relay 2 / EV ON.
5. Confirm actual GPIO16 readback HIGH.
6. Return final state.

### POST /api/v1/charge/stop

Safe sequence:

1. Relay 2 / EV OFF.
2. Confirm actual GPIO16 readback LOW.
3. Delay a short settling period.
4. Relay 1 / Eskom OFF.
5. Confirm actual GPIO17 readback LOW.
6. Return final state.

## Safety requirements

The LAN API must reuse the relay's existing safe state-changing functions instead of writing GPIOs through a second code path.

Existing behavior must remain intact:

- both relays OFF at boot
- ESP-NOW channel 6
- Waveshare + CrowPanel controllers
- 5-second owner heartbeat fail-safe for ESP-NOW-owned ON states
- OTA forces both relays OFF
- OTA command lockout
- four physical status LEDs follow actual relay GPIO readback
- AUTO/SOC automation remains disabled

The LAN app must never bypass these rules.
