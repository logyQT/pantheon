function fetch(input, init) {
    var url, options = {};
    if (typeof input === "string") {
        url = input;
        if (init) options = init;
    } else if (input && typeof input === "object") {
        url = input.url;
        if (input.method) options = input;
    }
    var method = (options.method || "GET").toUpperCase();
    var headers = options.headers ? JSON.stringify(options.headers) : null;
    var body = options.body || null;
    return new Promise(function(resolve, reject) {
        core.httpRequestAsync(method, url, body, headers, function(result) {
            if (result.hasError()) {
                reject(new Error(result.errorMessage));
                return;
            }
            resolve({
                ok: result.isOk(),
                status: result.status,
                text: function() { return result.body; },
                json: function() { return JSON.parse(result.body); }
            });
        });
    });
}
