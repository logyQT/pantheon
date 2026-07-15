settings.category("Economy");
settings.displayName("Economy");
settings.slider({ id: "default_balance", default: 100, min: 0, max: 10000, step: 10 });

gui.slider({ id: "default_balance", display: "Default Balance" });

const admins = [];

function loadAdmins() {
    admins.length = 0;
    const content = core.readFile("admins.json");
    if (!content) return;
    try {
        const data = JSON.parse(content);
        if (Array.isArray(data)) {
            for (const entry of data) {
                admins.push(entry.toLowerCase());
            }
        }
    } catch (e) {
        console.log(`Failed to parse admins.json: ${e}`);
    }
}

function isAdmin(player) {
    const lower = player.toLowerCase();
    return admins.includes(lower);
}

command.onCommand({
    invoker: "economy",
    description: "Coin economy commands.",
    callback: (sender, args) => {
        chat.party("Economy commands: !bal, !pay, !give (admin), !set (admin)");
    }
});

command.onCommand({
    invoker: "bal",
    callback: (sender, args) => {
        const target = args.length > 0 ? args[0] : sender;
        const bal = economy.balance(target);
        if (target.toLowerCase() === sender.toLowerCase()) {
            chat.party(`Your balance: ${bal} coins`);
        } else {
            chat.party(`${target}'s balance: ${bal} coins`);
        }
    }
});

command.onCommand({
    invoker: "pay",
    callback: (sender, args) => {
        if (args.length < 2) {
            chat.party("Usage: !pay <player> <amount>");
            return;
        }
        const target = args[0];
        const amount = parseInt(args[1]);
        if (isNaN(amount) || amount <= 0) {
            chat.party("Invalid amount.");
            return;
        }
        if (target.toLowerCase() === sender.toLowerCase()) {
            chat.party("You can't pay yourself.");
            return;
        }
        if (!economy.has(sender, amount)) {
            chat.party("You don't have enough coins.");
            return;
        }
        economy.take(sender, amount);
        economy.add(target, amount);
        chat.party(`Paid ${amount} coins to ${target}.`);
    }
});

command.onCommand({
    invoker: "give",
    callback: (sender, args) => {
        if (!isAdmin(sender)) {
            chat.party("You don't have permission.");
            return;
        }
        if (args.length < 2) {
            chat.party("Usage: !give <player> <amount>");
            return;
        }
        const target = args[0];
        const amount = parseInt(args[1]);
        if (isNaN(amount) || amount <= 0) {
            chat.party("Invalid amount.");
            return;
        }
        economy.add(target, amount);
        chat.party(`Gave ${amount} coins to ${target}.`);
    }
});

command.onCommand({
    invoker: "set",
    callback: (sender, args) => {
        if (!isAdmin(sender)) {
            chat.party("You don't have permission.");
            return;
        }
        if (args.length < 2) {
            chat.party("Usage: !set <player> <amount>");
            return;
        }
        const target = args[0];
        const amount = parseInt(args[1]);
        if (isNaN(amount) || amount < 0) {
            chat.party("Invalid amount.");
            return;
        }
        economy.set(target, amount);
        chat.party(`Set ${target}'s balance to ${amount} coins.`);
    }
});

loadAdmins();
