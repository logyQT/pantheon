pantheon.settings.setCategory("Economy");
pantheon.settings.setDisplayName("Economy");
pantheon.settings.addSlider({ id: "default_balance", display: "Default Balance", default: 100, min: 0, max: 10000, step: 10 });

pantheon.command.name("economy");
pantheon.command.description("Coin economy commands.");

var admins = [];

function loadAdmins() {
    admins = [];
    var content = pantheon.readFile("admins.json");
    if (!content) return;
    try {
        var data = JSON.parse(content);
        if (Array.isArray(data)) {
            for (var i = 0; i < data.length; i++) {
                admins.push(data[i].toLowerCase());
            }
        }
    } catch (e) {
        pantheon.log("Failed to parse admins.json: " + e);
    }
}

function isAdmin(player) {
    var lower = player.toLowerCase();
    for (var i = 0; i < admins.length; i++) {
        if (admins[i] === lower) return true;
    }
    return false;
}

pantheon.commands.register("bal", function(sender, args) {
    var target = args.length > 0 ? args[0] : sender;
    var bal = pantheon.economy.balance(target);
    if (target.toLowerCase() === sender.toLowerCase()) {
        pantheon.chat.party("Your balance: " + bal + " coins");
    } else {
        pantheon.chat.party(target + "'s balance: " + bal + " coins");
    }
});

pantheon.commands.register("pay", function(sender, args) {
    if (args.length < 2) {
        pantheon.chat.party("Usage: !pay <player> <amount>");
        return;
    }
    var target = args[0];
    var amount = parseInt(args[1]);
    if (isNaN(amount) || amount <= 0) {
        pantheon.chat.party("Invalid amount.");
        return;
    }
    if (target.toLowerCase() === sender.toLowerCase()) {
        pantheon.chat.party("You can't pay yourself.");
        return;
    }
    if (!pantheon.economy.has(sender, amount)) {
        pantheon.chat.party("You don't have enough coins.");
        return;
    }
    pantheon.economy.take(sender, amount);
    pantheon.economy.add(target, amount);
    pantheon.chat.party("Paid " + amount + " coins to " + target + ".");
});

pantheon.commands.register("give", function(sender, args) {
    if (!isAdmin(sender)) {
        pantheon.chat.party("You don't have permission.");
        return;
    }
    if (args.length < 2) {
        pantheon.chat.party("Usage: !give <player> <amount>");
        return;
    }
    var target = args[0];
    var amount = parseInt(args[1]);
    if (isNaN(amount) || amount <= 0) {
        pantheon.chat.party("Invalid amount.");
        return;
    }
    pantheon.economy.add(target, amount);
    pantheon.chat.party("Gave " + amount + " coins to " + target + ".");
});

pantheon.commands.register("set", function(sender, args) {
    if (!isAdmin(sender)) {
        pantheon.chat.party("You don't have permission.");
        return;
    }
    if (args.length < 2) {
        pantheon.chat.party("Usage: !set <player> <amount>");
        return;
    }
    var target = args[0];
    var amount = parseInt(args[1]);
    if (isNaN(amount) || amount < 0) {
        pantheon.chat.party("Invalid amount.");
        return;
    }
    pantheon.economy.set(target, amount);
    pantheon.chat.party("Set " + target + "'s balance to " + amount + " coins.");
});

pantheon.command.onCommand(function(sender, args) {
    pantheon.chat.party("Economy commands: !bal, !pay, !give (admin), !set (admin)");
});

loadAdmins();
