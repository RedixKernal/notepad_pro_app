package com.ravi.notesapp.util;

import javafx.scene.image.Image;
import java.util.HashMap;
import java.util.Map;

public class LanguageIconMap {

    private static final Map<String, String> EXTENSION_TO_DOMAIN = new HashMap<>();
    private static final Map<String, Image> ICON_CACHE = new HashMap<>();

    static {
        // Map common extensions to their official language/tool domain for favicon
        // fetching
        EXTENSION_TO_DOMAIN.put("py", "python.org");
        EXTENSION_TO_DOMAIN.put("java", "java.com");
        EXTENSION_TO_DOMAIN.put("c", "isocpp.org");
        EXTENSION_TO_DOMAIN.put("cpp", "isocpp.org");
        EXTENSION_TO_DOMAIN.put("cxx", "isocpp.org");
        EXTENSION_TO_DOMAIN.put("cc", "isocpp.org");
        EXTENSION_TO_DOMAIN.put("js", "js.org");
        EXTENSION_TO_DOMAIN.put("mjs", "nodejs.org");
        EXTENSION_TO_DOMAIN.put("lua", "lua.org");
        EXTENSION_TO_DOMAIN.put("php", "php.net");
        EXTENSION_TO_DOMAIN.put("cs", "microsoft.com");
        EXTENSION_TO_DOMAIN.put("asm", "nasm.us");
        EXTENSION_TO_DOMAIN.put("s", "nasm.us");
        EXTENSION_TO_DOMAIN.put("sh", "gnu.org/software/bash");
        EXTENSION_TO_DOMAIN.put("bat", "microsoft.com");
        EXTENSION_TO_DOMAIN.put("kt", "kotlinlang.org");
        EXTENSION_TO_DOMAIN.put("pas", "freepascal.org");
        EXTENSION_TO_DOMAIN.put("rb", "ruby-lang.org");
        EXTENSION_TO_DOMAIN.put("groovy", "groovy-lang.org");
        EXTENSION_TO_DOMAIN.put("scala", "scala-lang.org");
        EXTENSION_TO_DOMAIN.put("pro", "swi-prolog.org");
        EXTENSION_TO_DOMAIN.put("tcl", "tcl.tk");
        EXTENSION_TO_DOMAIN.put("ts", "typescriptlang.org");
        EXTENSION_TO_DOMAIN.put("hs", "haskell.org");
        EXTENSION_TO_DOMAIN.put("ada", "adacore.com");
        EXTENSION_TO_DOMAIN.put("lisp", "common-lisp.net");
        EXTENSION_TO_DOMAIN.put("d", "dlang.org");
        EXTENSION_TO_DOMAIN.put("ex", "elixir-lang.org");
        EXTENSION_TO_DOMAIN.put("exs", "elixir-lang.org");
        EXTENSION_TO_DOMAIN.put("erl", "erlang.org");
        EXTENSION_TO_DOMAIN.put("fs", "fsharp.org");
        EXTENSION_TO_DOMAIN.put("f90", "fortran-lang.org");
        EXTENSION_TO_DOMAIN.put("pl", "perl.org");
        EXTENSION_TO_DOMAIN.put("go", "go.dev");
        EXTENSION_TO_DOMAIN.put("r", "r-project.org");
        EXTENSION_TO_DOMAIN.put("rkt", "racket-lang.org");
        EXTENSION_TO_DOMAIN.put("ml", "ocaml.org");
        EXTENSION_TO_DOMAIN.put("bas", "visualbasic.net");
        EXTENSION_TO_DOMAIN.put("clj", "clojure.org");
        EXTENSION_TO_DOMAIN.put("cbl", "openmainframeproject.org");
        EXTENSION_TO_DOMAIN.put("cob", "openmainframeproject.org");
        EXTENSION_TO_DOMAIN.put("rs", "rust-lang.org");
        EXTENSION_TO_DOMAIN.put("swift", "swift.org");
        EXTENSION_TO_DOMAIN.put("m", "apple.com"); // Objective-C
        EXTENSION_TO_DOMAIN.put("txt", "notepad-plus-plus.org");
        EXTENSION_TO_DOMAIN.put("coffee", "coffeescript.org");
        EXTENSION_TO_DOMAIN.put("ejs", "ejs.co");
        EXTENSION_TO_DOMAIN.put("dart", "dart.dev");
        EXTENSION_TO_DOMAIN.put("cr", "crystal-lang.org");
        EXTENSION_TO_DOMAIN.put("jl", "julialang.org");
        EXTENSION_TO_DOMAIN.put("zig", "ziglang.org");
        EXTENSION_TO_DOMAIN.put("awk", "gnu.org");
        EXTENSION_TO_DOMAIN.put("st", "pharo.org"); // Smalltalk
        EXTENSION_TO_DOMAIN.put("nim", "nim-lang.org");
        EXTENSION_TO_DOMAIN.put("scm", "scheme.in");
        EXTENSION_TO_DOMAIN.put("ijs", "jsoftware.com"); // J
        EXTENSION_TO_DOMAIN.put("v", "vlang.io");
        EXTENSION_TO_DOMAIN.put("raku", "raku.org");
        EXTENSION_TO_DOMAIN.put("v", "verilog.com");
        EXTENSION_TO_DOMAIN.put("hx", "haxe.org");
        EXTENSION_TO_DOMAIN.put("fs", "forth.info");
        EXTENSION_TO_DOMAIN.put("icn", "cs.arizona.edu/icon"); // Icon
        EXTENSION_TO_DOMAIN.put("odin", "odin.us");
        EXTENSION_TO_DOMAIN.put("html", "w3.org");
        EXTENSION_TO_DOMAIN.put("htm", "w3.org");
        EXTENSION_TO_DOMAIN.put("css", "w3.org");
        EXTENSION_TO_DOMAIN.put("xml", "w3.org");
        EXTENSION_TO_DOMAIN.put("json", "json.com");
        EXTENSION_TO_DOMAIN.put("sql", "mysql.com");
        EXTENSION_TO_DOMAIN.put("jsx", "reactjs.org");
        EXTENSION_TO_DOMAIN.put("tsx", "reactjs.org");
        EXTENSION_TO_DOMAIN.put("mongo", "mongodb.com");
    }

    public static Image getIconForExtension(String extension) {
        if (extension == null || extension.isEmpty())
            return null;
        String ext = extension.toLowerCase();

        if (ICON_CACHE.containsKey(ext)) {
            return ICON_CACHE.get(ext);
        }

        String domain = EXTENSION_TO_DOMAIN.get(ext);
        if (domain != null) {
            String url = "https://www.google.com/s2/favicons?domain=" + domain + "&sz=24";
            try {
                // Background loading enabled
                Image img = new Image(url, true);
                ICON_CACHE.put(ext, img);
                return img;
            } catch (Exception e) {
                // Ignore load errors
            }
        }

        return null;
    }
}
