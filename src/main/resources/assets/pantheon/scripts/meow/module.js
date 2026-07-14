pantheon.settings.setCategory("Chat Hooks");
pantheon.settings.setDisplayName("Meow Sound");

pantheon.settings.addToggle({ id: "enabled", display: "Enabled", default: true });

pantheon.chat.onMessage("meow", function(rawText) {
    pantheon.audio.playSound({ sound: "minecraft:entity.cat.ambient" });
});
