package com.ravi.notesapp.util;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyntaxHighlighter {

    private SyntaxHighlighter() {}

    private static final String[] GENERIC_KEYWORDS = {
        // C-family, Java, C#, JS, PHP
        "abstract","assert","boolean","break","byte","case","catch","char","class",
        "const","continue","default","do","double","else","enum","extends","final",
        "finally","float","for","goto","if","implements","import","instanceof","int",
        "interface","long","native","new","package","private","protected","public",
        "return","short","static","strictfp","super","switch","synchronized","this",
        "throw","throws","transient","try","var","void","volatile","while",
        "true","false","null","record","sealed","permits","yield",
        "as","base","bool","checked","decimal","delegate","event","explicit",
        "extern","fixed","foreach","implicit","in","is","lock","object","operator",
        "out","override","params","readonly","ref","sbyte","sizeof","stackalloc",
        "string","struct","typeof","uint","ulong","unchecked","unsafe","ushort","using",
        "virtual","let","function","async","await","export","constructor","delete",
        "echo","print","isset","empty","include","require","namespace","use","trait",
        // Python
        "and","def","del","elif","except","False","from","global","lambda","None",
        "nonlocal","not","or","pass","raise","True","with",
        // Ruby
        "end","module","require","include","extend","attr_accessor","attr_reader","attr_writer","self","nil",
        // Lua
        "local","then","repeat","until","elseif",
        // SQL
        "select","from","where","insert","into","update","delete","create","table",
        "index","view","drop","alter","grant","revoke","commit","rollback","begin",
        "declare","exception","loop","cursor","procedure",
        // Go / Rust / Swift / Scala / Kotlin
        "func","defer","go","chan","select","fallthrough","range","type","struct","map",
        "fn","mut","let","impl","match","pub","use","crate","loop","move","where",
        "let","var","func","guard","defer","do","catch","throw","throws","rethrows",
        "val","var","def","type","trait","object","implicit","match","yield","sealed",
        "fun","val","var","open","data","sealed","inner","tailrec","suspend","inline"
    };
    private static final String KEYWORD_PATTERN  = "\\b(" + String.join("|", GENERIC_KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN    = "[()]";
    private static final String BRACE_PATTERN    = "[{}]";
    private static final String BRACKET_PATTERN  = "\\[|\\]";
    private static final String STRING_PATTERN   = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN  = "//[^\n]*|/\\*(.|\\R)*?\\*/|--[^\n]*|#[^\n]*";
    private static final String NUMBER_PATTERN   = "\\b\\d+(\\.\\d+)?[LlFfDd]?\\b";
    private static final String ANNOTATION_PATTERN = "@\\w+";
    private static final String TAG_PATTERN = "</?\\w+[^>]*>";

    private static final Pattern GENERIC_PATTERN = Pattern.compile(
        "(?<KEYWORD>"    + KEYWORD_PATTERN   + ")"
        + "|(?<STRING>"  + STRING_PATTERN    + ")"
        + "|(?<COMMENT>" + COMMENT_PATTERN   + ")"
        + "|(?<NUMBER>"  + NUMBER_PATTERN    + ")"
        + "|(?<ANNOTATION>" + ANNOTATION_PATTERN + ")"
        + "|(?<TAG>"     + TAG_PATTERN       + ")"
        + "|(?<PAREN>"   + PAREN_PATTERN     + ")"
        + "|(?<BRACE>"   + BRACE_PATTERN     + ")"
        + "|(?<BRACKET>" + BRACKET_PATTERN   + ")",
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );

    public static StyleSpans<Collection<String>> computeHighlighting(String text, String extension) {
        if (text == null) text = "";
        return highlight(text, GENERIC_PATTERN);
    }

    private static StyleSpans<Collection<String>> highlight(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        while (matcher.find()) {
            String styleClass = getStyleClass(matcher);
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastEnd);
        return spansBuilder.create();
    }

    private static String getStyleClass(Matcher matcher) {
        for (String group : new String[]{"KEYWORD","STRING","COMMENT","NUMBER","ANNOTATION","TAG","PAREN","BRACE","BRACKET"}) {
            try {
                if (matcher.group(group) != null) return group.toLowerCase();
            } catch (IllegalArgumentException ignored) {}
        }
        return "plain";
    }

    private static StyleSpans<Collection<String>> noHighlight(String text) {
        StyleSpansBuilder<Collection<String>> sb = new StyleSpansBuilder<>();
        sb.add(Collections.emptyList(), text.length());
        return sb.create();
    }
}
