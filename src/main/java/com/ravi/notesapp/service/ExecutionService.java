package com.ravi.notesapp.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ExecutionService {

    public Process startProcess(Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        String parentDir = filePath.getParent().toString();

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(parentDir));
        pb.redirectErrorStream(true); // merge stderr into stdout

        String os = System.getProperty("os.name").toLowerCase();
        boolean isWin = os.contains("win");

        if (fileName.endsWith(".java")) {
            pb.command("java", fileName);
        } else if (fileName.endsWith(".py")) {
            pb.command("python", fileName);
        } else if (fileName.endsWith(".js") || fileName.endsWith(".mjs")) {
            pb.command("node", fileName);
        } else if (fileName.endsWith(".cpp") || fileName.endsWith(".cxx") || fileName.endsWith(".cc")) {
            String exeName = fileName.substring(0, fileName.lastIndexOf('.')) + ".exe";
            if (isWin)
                pb.command("cmd", "/c", "g++ " + fileName + " -o " + exeName + " && " + exeName);
            else
                pb.command("sh", "-c", "g++ " + fileName + " -o " + exeName + " && ./" + exeName);
        } else if (fileName.endsWith(".c")) {
            String exeName = fileName.substring(0, fileName.lastIndexOf('.')) + ".exe";
            if (isWin)
                pb.command("cmd", "/c", "gcc " + fileName + " -o " + exeName + " && " + exeName);
            else
                pb.command("sh", "-c", "gcc " + fileName + " -o " + exeName + " && ./" + exeName);
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            if (isWin)
                pb.command("cmd", "/c", "start " + fileName);
            else
                pb.command("xdg-open", fileName);
        } else if (fileName.endsWith(".php")) {
            pb.command("php", fileName);
        } else if (fileName.endsWith(".cs")) {
            String exeName = fileName.substring(0, fileName.lastIndexOf('.')) + ".exe";
            if (isWin)
                pb.command("cmd", "/c", "csc " + fileName + " && " + exeName);
            else
                pb.command("sh", "-c", "mcs " + fileName + " && mono " + exeName);
        } else if (fileName.endsWith(".asm") || fileName.endsWith(".s")) {
            String objName = fileName.substring(0, fileName.lastIndexOf('.')) + (isWin ? ".obj" : ".o");
            String exeName = fileName.substring(0, fileName.lastIndexOf('.')) + (isWin ? ".exe" : "");
            if (isWin)
                pb.command("cmd", "/c", "nasm -f win64 " + fileName + " -o " + objName + " && gcc " + objName + " -o "
                        + exeName + " && " + exeName);
            else
                pb.command("sh", "-c", "nasm -f elf64 " + fileName + " -o " + objName + " && gcc " + objName + " -o "
                        + exeName + " && ./" + exeName);
        } else if (fileName.endsWith(".lua")) {
            pb.command("lua", fileName);
        } else if (fileName.endsWith(".groovy")) {
            pb.command("groovy", fileName);
        } else if (fileName.endsWith(".rb")) {
            pb.command("ruby", fileName);
        } else if (fileName.endsWith(".jsx") || fileName.endsWith(".tsx")) {
            pb.command("cmd", "/c",
                    "echo React files should typically be run via 'npm start' in the project directory, or compiled using tsc/babel.");
        } else if (fileName.endsWith(".sql") || fileName.endsWith(".pls") || fileName.endsWith(".plsql")) {
            pb.command("cmd", "/c", "echo Please run SQL / PL/SQL files using your database client (e.g., mysql < "
                    + fileName + ", psql -f " + fileName + ", or sqlplus @" + fileName + ")");
        } else if (fileName.endsWith(".kt") || fileName.endsWith(".kts")) {
            pb.command("cmd", "/c", "kotlinc " + fileName + " -include-runtime -d out.jar && java -jar out.jar");
        } else if (fileName.endsWith(".rs")) {
            String exeName = fileName.substring(0, fileName.lastIndexOf('.')) + (isWin ? ".exe" : "");
            pb.command("cmd", "/c", "rustc " + fileName + " && " + exeName);
        } else if (fileName.endsWith(".go")) {
            pb.command("go", "run", fileName);
        } else if (fileName.endsWith(".ts")) {
            pb.command("node", fileName);
        } else if (fileName.endsWith(".pl")) {
            pb.command("perl", fileName);
        } else if (fileName.endsWith(".r")) {
            pb.command("Rscript", fileName);
        } else if (fileName.endsWith(".dart")) {
            pb.command("dart", "run", fileName);
        } else if (fileName.endsWith(".jl")) {
            pb.command("julia", fileName);
        } else if (fileName.endsWith(".zig")) {
            pb.command("zig", "run", fileName);
        } else if (fileName.endsWith(".nim")) {
            pb.command("nim", "c", "-r", fileName);
        } else if (fileName.endsWith(".swift")) {
            pb.command("swift", fileName);
        } else if (fileName.endsWith(".ex") || fileName.endsWith(".exs")) {
            if (isWin)
                pb.command("cmd", "/c", "elixir " + fileName);
            else
                pb.command("elixir", fileName);
        } else if (fileName.endsWith(".clj")) {
            if (isWin)
                pb.command("cmd", "/c", "clojure -M " + fileName);
            else
                pb.command("clojure", "-M", fileName);
        } else if (fileName.endsWith(".hs")) {
            pb.command("runhaskell", fileName);
        } else if (fileName.endsWith(".sh") || fileName.endsWith(".bash")) {
            pb.command("bash", fileName);
        } else if (fileName.endsWith(".scala")) {
            pb.command("scala", fileName);
        } else if (fileName.endsWith(".pas")) {
            String exeName = fileName.substring(0, fileName.lastIndexOf('.')) + (isWin ? ".exe" : "");
            pb.command("cmd", "/c", "fpc " + fileName + " && " + exeName);
        } else if (fileName.endsWith(".d")) {
            pb.command("dmd", "-run", fileName);
        } else if (fileName.endsWith(".erl")) {
            pb.command("escript", fileName);
        } else if (fileName.endsWith(".fs") || fileName.endsWith(".fsx")) {
            pb.command("dotnet", "fsi", fileName);
        } else if (fileName.endsWith(".f90") || fileName.endsWith(".f")) {
            String exeName = fileName.substring(0, fileName.lastIndexOf('.')) + (isWin ? ".exe" : "");
            pb.command("cmd", "/c", "gfortran " + fileName + " -o " + exeName + " && " + exeName);
        } else if (fileName.endsWith(".cr")) {
            pb.command("crystal", "run", fileName);
        } else if (fileName.endsWith(".rkt")) {
            pb.command("racket", fileName);
        } else if (fileName.endsWith(".ml")) {
            pb.command("ocaml", fileName);
        } else if (fileName.endsWith(".m")) {
            String exeName = fileName.substring(0, fileName.lastIndexOf('.')) + (isWin ? ".exe" : "");
            pb.command("cmd", "/c", "gcc -framework Foundation " + fileName + " -o " + exeName + " && " + exeName);
        } else if (fileName.endsWith(".bf") || fileName.endsWith(".b")) {
            pb.command("brainfuck", fileName);
        } else if (fileName.endsWith(".coffee")) {
            if (isWin)
                pb.command("cmd", "/c", "coffee " + fileName);
            else
                pb.command("coffee", fileName);
        } else if (fileName.endsWith(".v")) {
            pb.command("v", "run", fileName);
        } else if (fileName.endsWith(".raku")) {
            pb.command("raku", fileName);
        } else if (fileName.endsWith(".hx")) {
            pb.command("haxe", "--main", fileName, "--interp");
        } else if (fileName.endsWith(".mongo")) {
            pb.command("cmd", "/c",
                    "echo Please run MongoDB scripts using 'mongosh < " + fileName + "' or 'mongo < " + fileName + "'");
        } else {
            throw new IllegalArgumentException("Unsupported file type for execution: " + fileName);
        }

        return pb.start();
    }
}
