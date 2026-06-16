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
        
        java.util.List<String> command = new java.util.ArrayList<>();

        if (fileName.endsWith(".java")) {
            command.add("java");
            command.add(fileName);
        } else if (fileName.endsWith(".py")) {
            command.add(isWin ? "python" : "python3");
            command.add(fileName);
        } else if (fileName.endsWith(".js") || fileName.endsWith(".mjs") || fileName.endsWith(".ts")) {
            command.add("node");
            command.add(fileName);
        } else if (fileName.endsWith(".cpp") || fileName.endsWith(".cxx") || fileName.endsWith(".cc") || fileName.endsWith(".c")) {
            String compiler = fileName.endsWith(".c") ? "gcc" : "g++";
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            String exeName = isWin ? base + ".exe" : base;
            String exePath = isWin ? base + ".exe" : "./" + base;

            // Compile first
            ProcessBuilder compilePb = new ProcessBuilder(compiler, fileName, "-o", exeName);
            compilePb.directory(new File(parentDir));
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            try {
                int exitCode = compileProcess.waitFor();
                if (exitCode != 0) {
                    return compileProcess; // Return compile process so errors can be seen
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Compilation interrupted");
            }
            command.add(exePath);
        } else if (fileName.endsWith(".php")) {
            command.add("php");
            command.add(fileName);
        } else if (fileName.endsWith(".cs")) {
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            String exeName = base + ".exe";
            
            ProcessBuilder compilePb = new ProcessBuilder(isWin ? "csc" : "mcs", fileName);
            compilePb.directory(new File(parentDir));
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            try {
                if (compileProcess.waitFor() != 0) return compileProcess;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Compilation interrupted");
            }
            
            if (isWin) {
                command.add(exeName);
            } else {
                command.add("mono");
                command.add(exeName);
            }
        } else if (fileName.endsWith(".rs")) {
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            String exeName = isWin ? base + ".exe" : "./" + base;
            
            ProcessBuilder compilePb = new ProcessBuilder("rustc", fileName);
            compilePb.directory(new File(parentDir));
            compilePb.redirectErrorStream(true);
            Process compileProcess = compilePb.start();
            try {
                if (compileProcess.waitFor() != 0) return compileProcess;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Compilation interrupted");
            }
            command.add(exeName);
        } else if (fileName.endsWith(".go")) {
            command.add("go");
            command.add("run");
            command.add(fileName);
        } else if (fileName.endsWith(".dart")) {
            command.add("dart");
            command.add("run");
            command.add(fileName);
        } else if (fileName.endsWith(".sh") || fileName.endsWith(".bash")) {
            command.add("bash");
            command.add(fileName);
        } else if (fileName.endsWith(".rb")) {
            command.add("ruby");
            command.add(fileName);
        } else if (fileName.endsWith(".lua")) {
            command.add("lua");
            command.add(fileName);
        } else {
            throw new IllegalArgumentException("Unsupported file type for execution: " + fileName);
        }

        pb.command(command);
        return pb.start();
    }
}
