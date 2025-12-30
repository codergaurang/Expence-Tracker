# Expense Tracker (Android)

A lightweight, **offline-first** Expense Tracker Android app built with **Java + XML** and **SQLite**, designed to automatically capture UPI transactions from SMS and let you add cash entries manually.

## Download APK

Download the latest APK from here:  
https://github.com/codergaurang/Expence-Tracker/blob/main/apk/expensetracker.apk [web:294]

> If GitHub shows the file preview page, tap the **Download** button on that page.

## Features

- Auto-detects and saves **UPI debit/credit** transactions by reading SMS (transactional SMS parsing).
- Manual **Cash** entry using the floating **+** button.
- First launch: scans existing inbox and imports previous UPI transactions.
- Shows a loading indicator during SMS import/parsing.
- Mandatory reason/description for **new** UPI transactions detected after installation (imported history does not ask).
- Click any transaction to view full details.
- Export transactions in a selected date range to **CSV** (Download button).

## Permissions

- `READ_SMS` / `RECEIVE_SMS`: Used only to detect and import transactional UPI SMS.
- `POST_NOTIFICATIONS` (Android 13+): Used to notify you when a new transaction needs a reason. [web:277]

## Tech Stack

- Language: Java
- UI: XML + Material Components
- Database: SQLite (local only)
- Storage: Export CSV to Downloads

## How to Run (Android Studio)

1. Clone the repo.
2. Open the project in Android Studio.
3. Build & Run on a physical device (recommended for SMS features).
4. Grant SMS permission when prompted.

## Project Structure (high level)

- `db/` SQLite helper and queries
- `sms/` SMS receiver + parser + importer
- `ui/` Activities + dialogs
- `adapter/` RecyclerView adapter
- `util/` CSV export + notifications + time formatting

## Notes

- The app stores everything locally on your device (no server / no cloud).
- SMS parsing supports common UPI debit/credit SMS formats and ignores non-transactional messages.

## License

Add a license if you want (MIT / Apache-2.0 / GPL etc.).
