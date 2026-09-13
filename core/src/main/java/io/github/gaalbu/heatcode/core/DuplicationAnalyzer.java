package io.github.gaalbu.heatcode.core;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class DuplicationAnalyzer {

    public Map<String, Double> ratios(List<MethodOwner> methods) {
        Map<String, Integer> occurrences = new HashMap<>();
        Map<MethodOwner, String> hashes = new HashMap<>();
        for (MethodOwner method : methods) {
            if (!eligible(method.method())) continue;
            String hash = hash(normalize(method.method()));
            hashes.put(method, hash);
            occurrences.merge(hash, 1, Integer::sum);
        }
        Map<String, int[]> counts = new HashMap<>();
        for (MethodOwner method : methods) {
            int[] count = counts.computeIfAbsent(method.className(), ignored -> new int[2]);
            if (eligible(method.method())) {
                count[1]++;
                if (occurrences.getOrDefault(hashes.get(method), 0) > 1) count[0]++;
            }
        }
        return counts.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey, entry -> entry.getValue()[1] == 0 ? 0 : (double) entry.getValue()[0] / entry.getValue()[1]));
    }

    private static boolean eligible(MethodDeclaration method) {
        if (method.getRange().isEmpty() || method.getRange().get().end.line - method.getRange().get().begin.line + 1 < 10) return false;
        String name = method.getNameAsString().toLowerCase();
        return !(name.startsWith("get") || name.startsWith("is") || name.startsWith("has") || name.startsWith("set"));
    }

    private static String normalize(MethodDeclaration method) {
        String source = method.getBody().map(Object::toString).orElse("");
        java.util.Set<String> localNames = new java.util.HashSet<>();
        method.getParameters().forEach(parameter -> localNames.add(parameter.getNameAsString()));
        method.findAll(com.github.javaparser.ast.body.VariableDeclarator.class)
                .forEach(variable -> localNames.add(variable.getNameAsString()));
        for (String name : localNames) source = source.replaceAll("\\b" + Pattern.quote(name) + "\\b", "VAR");
        return source;
    }

    private static String hash(String text) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }

    public record MethodOwner(String className, MethodDeclaration method) { }
}
