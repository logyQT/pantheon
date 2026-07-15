settings.category("ASCII Pics");
settings.displayName("ASCII Pictures");

settings.toggle({ id: "enabled", default: true });

gui.toggle({ id: "enabled", display: "Enabled" });
gui.button({ id: "open_folder", display: "Open Folder", callback: () => {
    gui.openFolder();
} });
gui.button({ id: "refresh", display: "Refresh", callback: () => {
    refreshPictures();
} });

const pictures = {};

function loadPictures() {
    for (const key of Object.keys(pictures)) {
        delete pictures[key];
    }
    const content = core.readFile("picture-commands.json");
    if (!content) return;
    try {
        const data = JSON.parse(content);
        if (data && data.pictures) {
            for (const p of data.pictures) {
                if (p.name && p.picture) {
                    pictures[p.name] = p.picture;
                }
            }
        }
    } catch (e) {
        console.log("Failed to parse picture-commands.json:", e);
    }
}

function registerPictureCommands() {
    const names = Object.keys(pictures);
    for (const name of names) {
        const lines = pictures[name].split("\n");
        command.onCommand({
            invoker: name,
            callback: (sender, args) => {
                for (const line of lines) {
                    chat.party(line);
                }
            }
        });
    }
}

function unregisterPictureCommands() {
    const names = Object.keys(pictures);
    for (const name of names) {
        command.off(name);
    }
}

function refreshPictures() {
    unregisterPictureCommands();
    loadPictures();
    registerPictureCommands();
    chat.client(`Pictures reloaded (${Object.keys(pictures).length} commands).`);
}

loadPictures();
registerPictureCommands();

command.onCommand({
    invoker: "pictures",
    description: "ASCII picture commands. Loads from picture-commands.json. Use !pictures refresh to reload.",
    callback: (sender, args) => {
        const names = Object.keys(pictures);
        if (names.length === 0) {
            chat.party("No pictures loaded. Add them to picture-commands.json and use the GUI to refresh.");
        } else {
            chat.party(`Available: ${names.join(", ")}`);
        }
    }
});
