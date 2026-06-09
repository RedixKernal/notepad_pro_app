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
            pb.command(isWin ? "python" : "python3", fileName);
        } else if (fileName.endsWith(".js") || fileName.endsWith(".mjs")) {
            pb.command("node", fileName);
        } else if (fileName.endsWith(".cpp") || fileName.endsWith(".cxx") || fileName.endsWith(".cc")) {
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            String exeName = isWin ? base + ".exe" : "./" + base;
            if (isWin)
                pb.command("cmd", "/c", "g++ " + fileName + " -o " + base + ".exe && " + base + ".exe");
            else
                pb.command("sh", "-c", "g++ " + fileName + " -o " + base + " && ./" + base);
        } else if (fileName.endsWith(".c")) {
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            if (isWin)
                pb.command("cmd", "/c", "gcc " + fileName + " -o " + base + ".exe && " + base + ".exe");
            else
                pb.command("sh", "-c", "gcc " + fileName + " -o " + base + " && ./" + base);
        } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            if (isWin)
                pb.command("cmd", "/c", "start " + fileName);
            else if (os.contains("mac"))
                pb.command("open", fileName);
            else
                pb.command("xdg-open", fileName);
        } else if (fileName.endsWith(".php")) {
            pb.command("php", fileName);
        } else if (fileName.endsWith(".cs")) {
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            if (isWin)
                pb.command("cmd", "/c", "csc " + fileName + " && " + base + ".exe");
            else
                pb.command("sh", "-c", "mcs " + fileName + " && mono " + base + ".exe");
        } else if (fileName.endsWith(".asm") || fileName.endsWith(".s")) {
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            String objName = base + (isWin ? ".obj" : ".o");
            String exeName = base + (isWin ? ".exe" : "");
            if (isWin)
                pb.command("cmd", "/c", "nasm -f win64 " + fileName + " -o " + objName + " && gcc " + objName + " -o " + exeName + " && " + exeName);
            else
                pb.command("sh", "-c", "nasm -f elf64 " + fileName + " -o " + objName + " && gcc " + objName + " -o " + base + " && ./" + base);
        } else if (fileName.endsWith(".lua")) {
            pb.command("lua", fileName);
        } else if (fileName.endsWith(".rb")) {
            pb.command("ruby", fileName);
        } else if (fileName.endsWith(".kt") || fileName.endsWith(".kts")) {
            if (isWin)
                pb.command("cmd", "/c", "kotlinc " + fileName + " -include-runtime -d out.jar && java -jar out.jar");
            else
                pb.command("sh", "-c", "kotlinc " + fileName + " -include-runtime -d out.jar && java -jar out.jar");
        } else if (fileName.endsWith(".rs")) {
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            if (isWin)
                pb.command("cmd", "/c", "rustc " + fileName + " && " + base + ".exe");
            else
                pb.command("sh", "-c", "rustc " + fileName + " && ./" + base);
        } else if (fileName.endsWith(".go")) {
            pb.command("go", "run", fileName);
        } else if (fileName.endsWith(".ts")) {
            pb.command("node", fileName);
        } else if (fileName.endsWith(".dart")) {
            pb.command("dart", "run", fileName);
        } else if (fileName.endsWith(".sh") || fileName.endsWith(".bash")) {
            pb.command("bash", fileName);
        } else {
            throw new IllegalArgumentException("Unsupported file type for execution: " + fileName);
        }

        return pb.start();
    }
}
