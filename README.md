# Lodge Billing System

**GROUP-1-LAB-3**

A desktop application (JavaFX) that lets lodge staff sign in, record a guest's stay, calculate the bill automatically, and view or print a receipt.

## Features

- Staff sign-in screen
- Guest form with validation (name, room type, nights, extras, amount paid, notes)
- Automatic calculation of room charge, discount, total and balance
- Table of all saved receipts (R001, R002, ...)
- View and print a receipt, delete a receipt
- Light and dark mode
- Keyboard shortcuts for common actions
- Receipts are now saved between sessions

## Billing rules

| Room type | Price per night |
|-----------|-----------------|
| Standard  | K 350.00 |
| Deluxe    | K 550.00 |
| Family    | K 750.00 |

- **Room charge** = price per night x number of nights
- **Discount:** 5% off the room charge for stays of **5 nights or more** (extras are not discounted)
- **Total** = room charge - discount + extras (no tax)
- **Balance** = total - amount paid
- Maximum stay is 30 nights, and the amount paid cannot be more than the total

## Requirements

- Java 21 (JDK)
- Internet connection the first time you build (Gradle downloads JavaFX and its dependencies)

Gradle does not need to be installed. The project includes the Gradle wrapper.

## How to run

```
git clone https://github.com/hopemusenga26-cell/GROUP-1-LAB-3.git
cd GROUP-1-LAB-3
./gradlew run
```

On Windows (Command Prompt or PowerShell), use `gradlew.bat run` instead of `./gradlew run`.

## Default login

| Username | Password |
|----------|----------|
| admin    | lodge123 |

The password is stored as a salted PBKDF2 hash, but the starting account is set in `AuthService.java`. Change it there before real use.

## Keyboard shortcuts

| Shortcut | Action |
|----------|--------|
| Ctrl+N | New guest |
| Ctrl+R | View receipt |
| Ctrl+P | Print receipt |
| Ctrl+D | Delete selected receipt |
| Ctrl+T | Switch light/dark mode |
| Ctrl+L | Log out |
| Ctrl+Q | Close |

(On macOS, use Cmd instead of Ctrl.)

## Project structure

The project follows the MVC pattern, with a DAO for storage.

```
src/main/java/lodgebilling/
├── LodgeBillingApp.java      Entry point
├── BillDao.java              Stores the receipts (in memory)
├── model/                    Bill, RoomType, validation, login (AuthService)
├── view/                     JavaFX screens: login, main window, form, table, receipt
└── controller/               Connects the views to the model
src/main/resources/           Stylesheet (lodge.css) and background images
```

## Known limitations

- Receipts are kept in memory only, so they are lost when the app closes.

## Team

