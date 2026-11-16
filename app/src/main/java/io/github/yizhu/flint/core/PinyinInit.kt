package io.github.yizhu.flint.core

import android.content.Context
import com.github.promeg.pinyinhelper.Pinyin
import com.github.promeg.pinyinhelper.PinyinMapDict

/**
 * Loads the disambiguation word list (assets/pinyin_words.txt) into TinyPinyin:
 * only words whose real reading differs from the naive per-char reading
 * (银行 -> yin hang, not yin xing). Longest-match segmentation does the rest.
 */
object PinyinInit {
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val dict = object : PinyinMapDict() {
            override fun mapping(): Map<String, Array<String>> {
                val map = HashMap<String, Array<String>>(8192)
                context.assets.open("pinyin_words.txt").bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (line.isEmpty()) continue
                        val tab = line.indexOf('\t')
                        if (tab <= 0) continue
                        map[line.substring(0, tab)] =
                            line.substring(tab + 1).split(' ').toTypedArray()
                    }
                }
                return map
            }
        }
        Pinyin.init(Pinyin.newConfig().with(dict))
        initialized = true
    }
}


/**
 * Splits a display label into match tokens:
 *  - each Chinese character -> one pinyin syllable ("wei"), dict-disambiguated
 *  - each run of consecutive Latin/digit chars -> one token, lowercased
 *  - other symbols are skipped
 */
fun labelToTokens(label: String): List<String> {
    val out = ArrayList<String>(label.length)
    val sb = StringBuilder()
    fun flush() {
        if (sb.isNotEmpty()) {
            out.add(sb.toString().lowercase())
            sb.clear()
        }
    }
    var i = 0
    while (i < label.length) {
        val c = label[i]
        when {
            Pinyin.isChinese(c) -> {
                flush()
                out.add(Pinyin.toPinyin(c).lowercase())
            }
            c.isLetter() || c.isDigit() -> sb.append(c)
            else -> flush()
        }
        i++
    }
    flush()
    return out
}
