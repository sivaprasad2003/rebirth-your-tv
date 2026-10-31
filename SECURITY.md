# Security Policy

RebirthYourTV communicates with Android TVs over network ADB and installs a lightweight home screen, so security and stability are our highest priorities. Please report any potential vulnerabilities privately.

## Reporting a Vulnerability

Please report vulnerabilities directly via GitHub Private Vulnerability Reporting or via email:
- **Email:** [sivaprasad10072003@gmail.com](mailto:sivaprasad10072003@gmail.com) with `[Security] RebirthYourTV Vulnerability Report` in the subject line.
- **GitHub Security:** Open the [Security tab](https://github.com/sivaprasad2003/rebirth-your-tv/security) and click **Report a vulnerability**.

Please include:
1. Affected component (launcher APK, python/shell scripts in `tools/`, debloat list).
2. Exact steps to reproduce.
3. Proof-of-concept or potential impact.

## Security Guarantees & Safety Rules

- **Zero Network Permissions in Launcher:** The RebirthYourTV reference launcher does not request or require `android.permission.INTERNET` for core operation, runs completely offline, and contains zero trackers, analytics, or background telemetry.
- **Non-Destructive Debloat:** RebirthYourTV only uses `pm disable-user --user 0`. No packages are ever uninstalled or permanently removed, and core Android system services are strictly protected.
- **ADB Safety:** After setting up RebirthYourTV, users are instructed to turn off USB and Network Debugging and revoke authorisations, ensuring port 5555 is closed and secured.

## Response Timeline

Security inquiries are typically reviewed within 48 to 72 hours. Valid fixes will be committed promptly to `main` with appropriate attribution.
