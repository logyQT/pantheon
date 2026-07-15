settings.category("Chat Hooks");
settings.displayName("Meow Sound");

settings.toggle({ id: "enabled", default: true });

gui.toggle({ id: "enabled", display: "Enabled" });

chat.onMessage("meow", (rawText) => {
    audio.playSound({ sound: "minecraft:entity.cat.ambient" });
});
