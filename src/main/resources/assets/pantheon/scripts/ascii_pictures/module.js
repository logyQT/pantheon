pantheon.settings.setCategory("ASCII Pics");
pantheon.settings.setDisplayName("ASCII Pictures");

pantheon.settings.addToggle({ id: "enabled", display: "Enabled", default: true });

pantheon.command.name("pictures");
pantheon.command.description("ASCII picture commands. Loads from picture-commands.json. Use !pictures refresh to reload.");

pantheon.gui.register("open_folder", "Open Folder", function() {
    pantheon.gui.openFolder();
});

pantheon.gui.register("refresh", "Refresh", function() {
    refreshPictures();
});

var pictures = {};

function loadPictures() {
    pictures = {};
    var content = pantheon.readFile("picture-commands.json");
    if (!content) return;
    try {
        var data = JSON.parse(content);
        if (data && data.pictures) {
            for (var i = 0; i < data.pictures.length; i++) {
                var p = data.pictures[i];
                if (p.name && p.picture) {
                    pictures[p.name] = p.picture;
                }
            }
        }
    } catch (e) {
        pantheon.log("Failed to parse picture-commands.json: " + e);
    }
}

function registerPictureCommands() {
    var names = Object.keys(pictures);
    for (var i = 0; i < names.length; i++) {
        var name = names[i];
        var picLines = pictures[name].split("\n");
        (function(lines) {
            pantheon.commands.register(name, function(sender, args) {
                for (var j = 0; j < lines.length; j++) {
                    pantheon.chat.party(lines[j]);
                }
            });
        })(picLines);
    }
}

function unregisterPictureCommands() {
    var names = Object.keys(pictures);
    for (var i = 0; i < names.length; i++) {
        pantheon.commands.unregister(names[i]);
    }
}

function refreshPictures() {
    unregisterPictureCommands();
    loadPictures();
    registerPictureCommands();
    pantheon.chat.party("Pictures reloaded (" + Object.keys(pictures).length + " commands).");
}

// Initial load
loadPictures();
registerPictureCommands();

pantheon.command.onCommand(function(sender, args) {
    if (args.length > 0 && args[0] === "refresh") {
        refreshPictures();
    } else {
        var names = Object.keys(pictures);
        if (names.length === 0) {
            pantheon.chat.party("No pictures loaded. Add them to picture-commands.json and use !pictures refresh.");
        } else {
            pantheon.chat.party("Available: " + names.join(", "));
        }
    }
});
