# Pantheon - Hypixel Skyblock QOL Mod

**Pantheon** is an advanced Quality of Life (QOL) modification created for Hypixel Skyblock players. The mod runs on the **Fabric (Minecraft 26.1.2)** engine and focuses on providing automation tools and an extensive social game system.

---

## 🎮 Party Games (Social Game System)
The core feature of the mod is a custom game engine triggered directly in party chat (default prefix: `!`). Games are integrated with a local **SQLite** database that stores players' coin balances.

### Available Games:
* **Blackjack (`!blackjack`)**: A comprehensive casino simulation with support for insurance, doubling down, and splitting hands.
* **Wheel of Fortune (`!wheel`)**: A game featuring a registration phase, spinning for various stakes (including bankruptcy risk), and guessing phrases.
* **Word Chain (`!wordchain`)**: A "word chain" game where players must provide Skyblock-related words starting with the last letter of the previous word.
* **Who Am I (`!whoami`)**: A guessing game based on descriptions and hints regarding items and mechanics from Hypixel Skyblock.
* **Hangman (`!hangman`)**: Classic hangman with a life system, hints, and definitions loaded from configuration files.
* **Roulette (`!roulette`)**: A roulette simulator allowing bets on colors, specific numbers, and columns.
* **Math Game (`!math`)**: A mathematical race – the first person to provide the result of an equation wins coins.
* **Hack Game (`!hack`)**: A logic game where you must guess a digit combination based on a checksum.
* **Speed Typing (`!speedtype`)**: An agility test – a reward for the person who types the indicated word the fastest.
* **Guessing Game (`!guess`)**: Classic number guessing (1-100) with "higher/lower" messages.

---

## 🛠️ QOL Features and Tools
* **Auto Experiments**: A module that automates solving mini-games in the "Table of Experiments" (Chronomatron and Ultrasequencer) with configurable delays and auto-closing.
* **TPS Monitor**: A server performance monitor (Ticks Per Second) integrated into the game interface.
* **Local Economy**: Each player has their own coin wallet (saved in `config/pantheon/economy.db`), and coins can be transferred to others using the `!pay` command.
* **Meow Feature**: An automatic sound reaction triggered when the word "meow" is detected in chat.

---

## ⚙️ Configuration and Interface
The mod uses **Yet Another ConfigLib (YACL)** for settings management.

* **Client Commands (`/`)**:
    * `/pantheon` – Opens the graphical configuration interface (ModMenu).
* **Party Commands (`!`)**:
    * `!bal` – Check account balance.
    * `!pay <player> <amount>` – Transfer coins to another player.
    * `!test <item_id>` – (Developer) Check the quantity of a specific item in the inventory.
    * `!gay <ign>` – A humorous percentage meter.

---

## 📋 Technical Requirements
* **Version**: Minecraft 26.1.2.
* **Engine**: Fabric Loader (>=0.19.3).
* **Libraries**: Fabric API, YACL v3, ModMenu.