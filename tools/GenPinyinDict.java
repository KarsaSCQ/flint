import com.github.promeg.pinyinhelper.Pinyin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Builds the disambiguation word dictionary for Flint from phrase-pinyin-data.
 * Keeps only phrases whose actual pinyin differs from per-char default readings
 * (e.g. 银行 yin-hang vs naive yin-xing), which is all TinyPinyin needs
 * to resolve polyphones via its own longest-match segmentation.
 */
public class GenPinyinDict {

    static String norm(String py) {
        StringBuilder sb = new StringBuilder(py.length());
        for (int i = 0; i < py.length(); i++) {
            char c = py.charAt(i);
            switch (c) {
                case 'ā': case 'á': case 'ǎ': case 'à': sb.append('a'); break;
                case 'ē': case 'é': case 'ě': case 'è': sb.append('e'); break;
                case 'ī': case 'í': case 'ǐ': case 'ì': sb.append('i'); break;
                case 'ō': case 'ó': case 'ǒ': case 'ò': sb.append('o'); break;
                case 'ū': case 'ú': case 'ǔ': case 'ù': sb.append('u'); break;
                case 'ǖ': case 'ǘ': case 'ǚ': case 'ǜ': case 'ü': sb.append('v'); break;
                case 'ń': case 'ň': case 'ǹ': sb.append('n'); break;
                case '\u1E3F': sb.append('m'); break;
                default:
                    if (c >= '0' && c <= '9') continue; // tone digits
                    if (c >= '\u0300' && c <= '\u036F') continue; // combining diacritics
                    sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        List<String> kept = new ArrayList<>();
        int total = 0, keptCount = 0;
        try (BufferedReader r = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                int colon = line.indexOf(':');
                if (colon <= 0) continue;
                String word = line.substring(0, colon);
                String pyRaw = line.substring(colon + 1).trim();
                if (word.length() < 2 || word.length() > 10) continue;
                boolean allChinese = true;
                for (int i = 0; i < word.length(); i++) {
                    if (!Pinyin.isChinese(word.charAt(i))) { allChinese = false; break; }
                }
                if (!allChinese) continue;
                total++;
                String[] dict = pyRaw.split("\\s+");
                if (dict.length != word.length()) continue; // erhua/merges etc.
                StringBuilder normed = new StringBuilder();
                for (String s : dict) normed.append(norm(s)).append(' ');
                StringBuilder naive = new StringBuilder();
                for (int i = 0; i < word.length(); i++) {
                    naive.append(Pinyin.toPinyin(word.charAt(i)).toLowerCase()).append(' ');
                }
                if (!normed.toString().equals(naive.toString())) {
                    kept.add(word + "\t" + normed.toString().trim());
                    keptCount++;
                }
            }
        }
        // stable, dedup
        Set<String> seen = new TreeSet<>();
        for (String k : kept) seen.add(k);
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            for (String k : seen) w.write(k + "\n");
        }
        System.out.println("total=" + total + " kept=" + seen.size());
    }
}
