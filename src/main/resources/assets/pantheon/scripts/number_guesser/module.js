pantheon.settings.setCategory("Party Games");
pantheon.settings.setDisplayName("Number Guesser");

pantheon.settings.addSlider({ id: "reward", display: "Reward", default: 10, min: 1, max: 100, step: 1 });
pantheon.settings.addSlider({ id: "timeout_ms", display: "Timeout (ms)", default: 15000, min: 5000, max: 60000, step: 1000 });
pantheon.settings.addSlider({ id: "max_number", display: "Max Number", default: 20, min: 5, max: 100, step: 1 });

pantheon.command.name("guess");
pantheon.command.description("Guess a number. Use !guess to start.");

pantheon.command.onCommand(function(sender, args) {
    pantheon.game.start();
});

var target = 0;
var hints = 0;
var timeoutId = -1;

pantheon.game.onStart(function(sender) {
    target = Math.floor(Math.random() * pantheon.settings.get("max_number")) + 1;
    hints = 0;
    pantheon.chat.party("NUMBER GUESSER! I'm thinking of 1-" + pantheon.settings.get("max_number") + ". " + sender + " guesses first.");
    timeoutId = pantheon.timers.after(pantheon.settings.get("timeout_ms"), function() {
        pantheon.chat.party("Time's up! The number was " + target + ".");
        pantheon.game.stop();
    });
});

pantheon.game.onChat(function(sender, message) {
    var guess = parseInt(message);
    if (isNaN(guess)) return;
    hints++;
    if (guess < target) {
        pantheon.chat.party("Higher!");
    } else if (guess > target) {
        pantheon.chat.party("Lower!");
    } else {
        if (timeoutId >= 0) pantheon.timers.cancel(timeoutId);
        var reward = pantheon.settings.get("reward");
        pantheon.economy.add(sender, reward);
        pantheon.chat.party(sender + " got it in " + hints + " tries! (+" + reward + " coins)");
        pantheon.game.stop();
        return;
    }
    // Reset timer on wrong guess
    if (timeoutId >= 0) pantheon.timers.cancel(timeoutId);
    timeoutId = pantheon.timers.after(pantheon.settings.get("timeout_ms"), function() {
        pantheon.chat.party("Time's up! The number was " + target + ".");
        pantheon.game.stop();
    });
});

pantheon.game.onStop(function(reason) {
    if (timeoutId >= 0) {
        pantheon.timers.cancel(timeoutId);
        timeoutId = -1;
    }
});
