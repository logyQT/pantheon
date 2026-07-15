settings.category("Party Games");
settings.displayName("Blackjack");

settings.slider({ id: "min_bet", default: 10, min: 1, max: 1000, step: 1 });
settings.slider({ id: "max_bet", default: 1000000, min: 1000, max: 5000000, step: 1000 });
settings.slider({ id: "bj_payout", default: 25, min: 10, max: 50, step: 1 });
settings.slider({ id: "insurance_payout", default: 30, min: 10, max: 50, step: 1 });
settings.slider({ id: "betting_time", default: 11000, min: 5000, max: 60000, step: 1000 });
settings.slider({ id: "turn_time", default: 30000, min: 10000, max: 120000, step: 1000 });
settings.slider({ id: "insurance_time", default: 10000, min: 3000, max: 30000, step: 1000 });
settings.slider({ id: "game_timeout", default: 60000, min: 10000, max: 120000, step: 1000 });

gui.slider({ id: "min_bet", display: "Min Bet" });
gui.slider({ id: "max_bet", display: "Max Bet" });
gui.slider({ id: "bj_payout", display: "BJ Payout (x10)" });
gui.slider({ id: "insurance_payout", display: "Insurance (x10)" });
gui.slider({ id: "betting_time", display: "Betting Time (ms)" });
gui.slider({ id: "turn_time", display: "Turn Time (ms)" });
gui.slider({ id: "insurance_time", display: "Insurance Time (ms)" });
gui.slider({ id: "game_timeout", display: "Game Timeout (ms)" });

const SUITS = ["♥", "♦", "♣", "♠"];
const RANKS = ["2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"];
const VALUES = { "2": 2, "3": 3, "4": 4, "5": 5, "6": 6, "7": 7, "8": 8, "9": 9, "10": 10, "J": 10, "Q": 10, "K": 10, "A": 11 };

let phase = "BETTING";
let deck = [];
let hands = [];
let dealerHand = [];
let insuranceBets = {};
let currentHandIndex = 0;
let timerId = -1;

function createDeck() {
    const d = [];
    for (let i = 0; i < 4; i++) {
        for (const suit of SUITS) {
            for (const rank of RANKS) {
                d.push({ rank, suit });
            }
        }
    }
    for (let i = d.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [d[i], d[j]] = [d[j], d[i]];
    }
    return d;
}

function drawCard() {
    if (deck.length === 0) deck = createDeck();
    return deck.pop();
}

function cardStr(card) {
    return `[${card.rank} ${card.suit}]`;
}

function handStr(cards) {
    return cards.map(cardStr).join(" ");
}

function calculateScore(cards) {
    let s = 0, aces = 0;
    for (const c of cards) {
        s += VALUES[c.rank];
        if (c.rank === "A") aces++;
    }
    while (s > 21 && aces > 0) {
        s -= 10;
        aces--;
    }
    return s;
}

function isBlackjack(cards) {
    return cards.length === 2 && calculateScore(cards) === 21;
}

function clearTimer() {
    if (timerId >= 0) {
        clearTimeout(timerId);
        timerId = -1;
    }
}

function beginMatch() {
    if (hands.length === 0) {
        chat.party("No players joined. Game cancelled.");
        game.stop();
        return;
    }
    deck = createDeck();
    insuranceBets = {};
    currentHandIndex = 0;
    for (const hand of hands) {
        hand.cards = [drawCard(), drawCard()];
        hand.finished = false;
        if (isBlackjack(hand.cards)) {
            chat.party("✮ " + hand.name + " has BLACKJACK! " + handStr(hand.cards));
            hand.finished = true;
        } else {
            chat.party(hand.name + " received: " + handStr(hand.cards) + " (" + calculateScore(hand.cards) + ")");
        }
    }
    dealerHand = [drawCard(), drawCard()];
    chat.party("Dealer's hand: " + cardStr(dealerHand[0]) + " [?]");
    if (dealerHand[0].rank === "A") {
        phase = "INSURANCE";
        chat.party("Dealer shows ACE. .ins to buy insurance (" + (settings.get("insurance_time") / 1000) + "s).");
        timerId = setTimeout(() => {
            startPlayerTurns();
        }, settings.get("insurance_time"));
    } else {
        startPlayerTurns();
    }
}

function startPlayerTurns() {
    phase = "PLAYER_TURNS";
    processTurns();
}

function processTurns() {
    while (currentHandIndex < hands.length) {
        const h = hands[currentHandIndex];
        if (h.finished) {
            currentHandIndex++;
            continue;
        }
        const score = calculateScore(h.cards);
        if (score === 21) {
            chat.party(h.name + " has 21!");
            h.finished = true;
            currentHandIndex++;
            continue;
        }
        chat.party("Turn: " + h.name + " | Hand: " + handStr(h.cards) + " (" + score + ")");
        timerId = setTimeout(() => {
            if (currentHandIndex < hands.length) {
                const timedOut = hands[currentHandIndex];
                timedOut.finished = true;
                chat.party(timedOut.name + " timed out! Standing.");
                currentHandIndex++;
                processTurns();
            }
        }, settings.get("turn_time"));
        return;
    }
    phase = "DEALER_TURN";
    handleDealerFinal();
}

function handleBet(sender, msg) {
    if (hands.some(h => h.name.toLowerCase() === sender.toLowerCase())) {
        chat.party(sender + ", you already placed a bet!");
        return;
    }
    const parts = msg.split(" ");
    const amount = parts.length > 1 ? parseInt(parts[1]) : 100;
    if (isNaN(amount) || amount < settings.get("min_bet") || amount > settings.get("max_bet")) {
        chat.party("Bet limit: " + settings.get("min_bet") + " - " + settings.get("max_bet"));
        return;
    }
    if (economy.take(sender, amount)) {
        hands.push({ name: sender, bet: amount, cards: [], finished: false, isSplit: false });
        chat.party(sender + " joined with " + amount);
    } else {
        chat.party(sender + ", insufficient funds!");
    }
}

function handleInsurance(sender) {
    const hand = hands.find(h => h.name.toLowerCase() === sender.toLowerCase());
    if (!hand || insuranceBets[sender] !== undefined) return;
    const insAmount = Math.floor(hand.bet / 2);
    if (economy.take(sender, insAmount)) {
        insuranceBets[sender] = insAmount;
        chat.party(sender + " bought Insurance.");
    }
}

function handlePlayerAction(sender, msg) {
    if (currentHandIndex >= hands.length) return;
    const cur = hands[currentHandIndex];
    if (sender.toLowerCase() !== cur.name.toLowerCase() || cur.finished) return;
    clearTimer();
    if (msg === ".hit") {
        cur.cards.push(drawCard());
        const score = calculateScore(cur.cards);
        chat.party(cur.name + " drew " + cardStr(cur.cards[cur.cards.length - 1]) + " (Total: " + score + ")");
        if (score >= 21) {
            cur.finished = true;
            currentHandIndex++;
            processTurns();
        } else {
            timerId = setTimeout(() => {
                if (currentHandIndex < hands.length) {
                    const timedOut = hands[currentHandIndex];
                    timedOut.finished = true;
                    chat.party(timedOut.name + " timed out! Standing.");
                    currentHandIndex++;
                    processTurns();
                }
            }, settings.get("turn_time"));
        }
    } else if (msg === ".stand") {
        cur.finished = true;
        currentHandIndex++;
        processTurns();
    } else if (msg === ".double") {
        if (cur.cards.length !== 2) return;
        if (!economy.take(cur.name, cur.bet)) return;
        cur.bet *= 2;
        cur.cards.push(drawCard());
        chat.party(cur.name + " DOUBLED: " + calculateScore(cur.cards));
        cur.finished = true;
        currentHandIndex++;
        processTurns();
    } else if (msg === ".split") {
        if (cur.isSplit || cur.cards.length !== 2) return;
        if (cur.cards[0].rank !== cur.cards[1].rank) return;
        if (!economy.take(cur.name, cur.bet)) return;
        const splitHand = { name: cur.name, bet: cur.bet, cards: [cur.cards.pop()], finished: false, isSplit: true };
        cur.cards.push(drawCard());
        splitHand.cards.push(drawCard());
        hands.splice(currentHandIndex + 1, 0, splitHand);
        chat.party("Split successful!");
        processTurns();
    }
}

function handleDealerFinal() {
    const dScore = calculateScore(dealerHand);
    chat.party("Dealer reveals: " + handStr(dealerHand) + " (" + dScore + ")");
    if (dScore === 21 && dealerHand.length === 2) {
        chat.party("Dealer has BLACKJACK!");
    } else {
        while (calculateScore(dealerHand) < 17) {
            const drawn = drawCard();
            dealerHand.push(drawn);
            chat.party("Dealer draws: " + cardStr(drawn) + " (" + calculateScore(dealerHand) + ")");
        }
    }
    finalizeResults();
}

function finalizeResults() {
    const dScore = calculateScore(dealerHand);
    const dealerBJ = isBlackjack(dealerHand);
    for (const h of hands) {
        const pScore = calculateScore(h.cards);
        const playerBJ = isBlackjack(h.cards);
        if (pScore > 21) {
            chat.party(h.name + ": BUSTED! (" + pScore + ")");
        } else if (playerBJ && dealerBJ) {
            economy.add(h.name, h.bet);
            chat.party(h.name + ": PUSH (Both have Blackjack)!");
        } else if (playerBJ) {
            const payout = Math.floor(h.bet * (settings.get("bj_payout") / 10));
            economy.add(h.name, payout);
            chat.party(h.name + ": BLACKJACK! Wins " + payout);
        } else if (dealerBJ) {
            chat.party(h.name + ": LOST vs Dealer Blackjack!");
        } else if (dScore > 21 || pScore > dScore) {
            economy.add(h.name, h.bet * 2);
            chat.party(h.name + ": WON! (" + pScore + " vs " + dScore + ")");
        } else if (pScore === dScore) {
            economy.add(h.name, h.bet);
            chat.party(h.name + ": PUSH (" + pScore + " each)!");
        } else {
            chat.party(h.name + ": LOST (" + pScore + " vs " + dScore + ")");
        }
    }
    if (dealerBJ) {
        for (const [name, amount] of Object.entries(insuranceBets)) {
            const payout = Math.floor(amount * (settings.get("insurance_payout") / 10));
            economy.add(name, payout);
            chat.party(name + ": Insurance payout +" + payout);
        }
    }
    game.stop();
}

command.onCommand({
    invoker: "blackjack",
    description: "Start a blackjack game.",
    callback: (sender, args) => {
        game.start();
    }
});

game.onStart((sender) => {
    phase = "BETTING";
    hands = [];
    dealerHand = [];
    insuranceBets = {};
    currentHandIndex = 0;
    clearTimer();
    chat.party("Blackjack! Betting open (" + (settings.get("betting_time") / 1000) + "s). Type .bet <amount>");
    timerId = setTimeout(() => {
        beginMatch();
    }, settings.get("betting_time"));
});

game.onChat((sender, message) => {
    const msg = message.toLowerCase().trim();
    if (phase === "BETTING" && msg.startsWith(".bet")) {
        handleBet(sender, msg);
    } else if (phase === "INSURANCE" && msg === ".ins") {
        handleInsurance(sender);
    } else if (phase === "PLAYER_TURNS") {
        handlePlayerAction(sender, msg);
    }
});

game.onStop((reason) => {
    clearTimer();
});
