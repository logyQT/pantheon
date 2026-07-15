settings.category("Party Games");
settings.displayName("Number Guesser");

settings.slider({ id: "reward", default: 10, min: 1, max: 100, step: 1 });
settings.slider({ id: "timeout_ms", default: 15000, min: 5000, max: 60000, step: 1000 });
settings.slider({ id: "max_number", default: 20, min: 5, max: 100, step: 1 });

gui.slider({ id: "reward", display: "Reward" });
gui.slider({ id: "timeout_ms", display: "Timeout (ms)" });
gui.slider({ id: "max_number", display: "Max Number" });

command.onCommand({
    invoker: "guess",
    description: "Guess a number. Use !guess to start.",
    callback: (sender, args) => {
        game.start();
    }
});

let target = 0;
let hints = 0;
let timeoutId = -1;

game.onStart((sender) => {
    target = Math.floor(Math.random() * settings.get("max_number")) + 1;
    hints = 0;
    chat.party(`NUMBER GUESSER! I'm thinking of 1-${settings.get("max_number")}. ${sender} guesses first.`);
    timeoutId = setTimeout(() => {
        chat.party(`Time's up! The number was ${target}.`);
        game.stop();
    }, settings.get("timeout_ms"));
});

game.onChat((sender, message) => {
    const guess = parseInt(message);
    if (isNaN(guess)) return;
    hints++;
    if (guess < target) {
        chat.party("Higher!");
    } else if (guess > target) {
        chat.party("Lower!");
    } else {
        if (timeoutId >= 0) clearTimeout(timeoutId);
        const reward = settings.get("reward");
        economy.add(sender, reward);
        chat.party(`${sender} got it in ${hints} tries! (+${reward} coins)`);
        game.stop();
        return;
    }
    if (timeoutId >= 0) clearTimeout(timeoutId);
    timeoutId = setTimeout(() => {
        chat.party(`Time's up! The number was ${target}.`);
        game.stop();
    }, settings.get("timeout_ms"));
});

game.onStop((reason) => {
    if (timeoutId >= 0) {
        clearTimeout(timeoutId);
        timeoutId = -1;
    }
});
