# Lodge Billing System

**GROUP-1-LAB-3**

A desktop application (JavaFX) that lets lodge staff sign in, record a guest's stay, calculate the bill automatically, and view or print a receipt.

## Features

- First-run setup: whoever installs the app creates the admin account (there is no built-in password)
- Staff accounts with roles: **Admin** and **Front desk**
- Account lockout: 5 wrong passwords lock the account for 5 minutes
- Change your own password; an admin can reset a password and force a change at next sign-in
- Guest form with validation (name, room type, nights, extras, amount paid, notes)
- Automatic calculation of room charge, discount, total and balance
- Receipts saved in a SQLite database, so they survive closing the app (R001, R002, ...)
- View, print and delete receipts (deleting is admin-only)
- Audit log of sign-ins, lockouts, saved and deleted receipts and account changes (admin-only)
- Light and dark mode over an animated aurora background
- Keyboard shortcuts for common actions

## Billing rules

| Room type | Price per night |
| --------- | --------------- |
| Standard  | K 350.00        |
| Deluxe    | K 550.00        |
| Family    | K 750.00        |

- **Room charge** = price per night x number of nights
- **Discount:** 5% off the room charge for stays of **5 nights or more** (extras are not discounted)
- **Total** = room charge - discount + extras (no tax)
- **Balance** = total - amount paid
- Maximum stay is 30 nights, and the amount paid cannot be more than the total

## Requirements

- JDK 21 or newer (the code is compiled for Java 21, so it also builds on JDK 25)
- Internet connection the first time you build (Gradle downloads JavaFX, SQLite and JUnit)

Gradle does not need to be installed. The project includes the Gradle wrapper.

## How to run

```
git clone https://github.com/hopemusenga26-cell/GROUP-1-LAB-3.git
cd GROUP-1-LAB-3
./gradlew run
```

On Windows (Command Prompt or PowerShell), use `gradlew.bat run` instead of `./gradlew run`.

In IntelliJ, run `Launcher` (not `LodgeBillingApp`): see the comment in `Launcher.java` for why.

Run the tests with `gradlew.bat test` (or `./gradlew test`).

## First start

The first time the app opens it asks you to create the **admin account**. Passwords need at least
8 characters with a letter and a number. The admin can then add front-desk accounts from
**Admin > Manage users**.

## Where the data is kept

Everything is stored in `lodgebilling.db` in a `.lodgebilling` folder in your home folder
(for example `C:\Users\<you>\.lodgebilling\lodgebilling.db`). It is outside the project on purpose,
so guest details are never committed to Git.

Receipts saved by the earlier version of the app (`receipts.tsv` in the same folder) are copied into
the database automatically on the first start, keeping their receipt numbers. The old file is then
renamed to `receipts.tsv.imported`.

Passwords are never stored: each one is kept as a salted PBKDF2 hash.

## Keyboard shortcuts

| Shortcut | Action                                |
| -------- | ------------------------------------- |
| Ctrl+N   | New guest                             |
| Ctrl+R   | View receipt                          |
| Ctrl+P   | Print receipt                         |
| Ctrl+D   | Delete selected receipt (admin)       |
| Ctrl+T   | Switch light/dark mode                |
| Ctrl+U   | Manage users (admin)                  |
| Ctrl+L   | Log out                               |
| Ctrl+Q   | Close                                 |

In the receipts table, Enter opens the selected receipt and Delete asks to delete it.
(On macOS, use Cmd instead of Ctrl.)

## Project structure

The project follows the MVC pattern, with repositories for storage.

```
src/main/java/lodgebilling/
├── LodgeBillingApp.java      Entry point: opens the database, then setup -> sign in -> main window
├── Launcher.java             Start this one from IntelliJ
├── model/                    Bill, RoomType, validation, the observable app state
├── data/                     SQLite: the database, receipts, users, audit log, old-file import
├── security/                 Roles, permissions, password hashing and rules, the signed-in session
├── service/                  AuthService: sign-in, lockout, accounts
├── view/                     JavaFX screens: setup, login, main window, form, table, receipt, dialogs
└── controller/               Connects the views to the model
src/main/resources/           Stylesheet (lodge.css)
src/test/java/lodgebilling/   JUnit tests
```

## Team
