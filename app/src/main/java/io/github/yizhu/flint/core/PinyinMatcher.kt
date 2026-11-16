package io.github.yizhu.flint.core

/**
 * DP pinyin matcher: does the query (typed letters) prefix-match the label's
 * pinyin tokens, where each Chinese character may be consumed either as its
 * full syllable ("zhong") or its first letter ("z")? Mixed forms like
 * "zhongguoyh" work naturally. Returns a score, lower = better; null = no match.
 *
 * Score model (design doc priority: abbr > full > label text):
 *  - tier 0: pure abbr prefix (zgyh)
 *  - tier 1: any full-syllable use (zhongguoyinhang / zhongguoyh)
 *  - tier 2: raw label text prefix (case-insensitive)
 *  Within a tier: prefer shorter labels, and more full syllables when mixed.
 */
object PinyinMatcher {

    fun score(queryRaw: String, label: String, tokens: List<String>): Int? {
        val q = queryRaw.lowercase()
        if (q.isEmpty()) return null

        if (label.lowercase().startsWith(q)) {
            return 2_000_000 - label.length * 100
        }

        val n = tokens.size
        val m = q.length
        // dp[i][j] == 1 if tokens[i..] can consume q[j..] (abbr), 2 if via full syllable
        val dp = Array(n + 1) { IntArray(m + 1) }
        dp[n][m] = 1
        for (i in n - 1 downTo 0) {
            val tok = tokens[i]
            for (j in m downTo 0) {
                // abbr: one letter
                if (j < m && q[j] == tok[0]) {
                    if (dp[i + 1][j + 1] != 0) dp[i][j] = 1
                }
                // full syllable (only meaningful for single-char pinyin tokens)
                if (tok.length > 1) {
                    val len = tok.length
                    if (j + len <= m && q.regionMatches(j, tok, 0, len)) {
                        if (dp[i + 1][j + len] != 0) dp[i][j] = 2
                    }
                }
            }
        }
        if (dp[0][0] == 0) return null

        val usedFull = dp[0][0] == 2 || run {
            // walk to count full-syllable consumption
            var i = 0; var j = 0; var full = false
            while (i < n && j < m) {
                val tok = tokens[i]
                val abbrOk = q[j] == tok[0] && dp[i + 1][j + 1] != 0
                val len = tok.length
                val fullOk = len > 1 && j + len <= m && q.regionMatches(j, tok, 0, len) && dp[i + 1][j + len] != 0
                if (fullOk && (!abbrOk || dp[i + 1][j + len] == 2)) {
                    full = true; j += len
                } else if (abbrOk) {
                    j += 1
                } else if (fullOk) {
                    full = true; j += len
                } else break
                i++
            }
            full
        }

        return if (usedFull) 1_000_000 - label.length * 100 else -label.length * 100
    }
}
