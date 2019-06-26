package io.nebulas.wallet.android.util;

import android.text.TextUtils;
import android.util.Log;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 *
 * Created by heinoc on 14-6-17.
 */
public abstract class StringUtils {

    public static final String EMPTY = "";

    public static final int INDEX_NOT_FOUND = -1;

    private static final int PAD_LIMIT = 8192;

    private StringUtils() {
        // u son of bitch, do not initialize me, i am just a tool, r u an idiot
        // ?
    }

    // Empty checks
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 校验是否是空字符串或者 NULL
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     *
     * @param str 待校验的字符串
     * @return 如果字符串为 NULL 或者空字符串则返回 TRUE
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * <p>
     * 校验是否不是 NULL 且不为空字符串
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isNotEmpty(null)      = false
     * StringUtils.isNotEmpty("")        = false
     * StringUtils.isNotEmpty(" ")       = true
     * StringUtils.isNotEmpty("bob")     = true
     * StringUtils.isNotEmpty("  bob  ") = true
     * </pre>
     *
     * @param str 待校验的字符串
     * @return 如果字符串不为 NULL 且不是空字符串则返回 TRUE
     */
    public static boolean isNotEmpty(String str) {
        return !StringUtils.isEmpty(str);
    }

    /**
     * <p>
     * 校验是否是 NULL 或者空白字符串
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param str 待校验的字符串
     * @return 是 NULL 或者空白字符串则返回 TRUE
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * 校验是否不是 NULL 且不是空白字符串
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param str 待校验的字符串
     * @return 不是 NULL 且不是空白字符串则返回 TRUE
     */
    public static boolean isNotBlank(String str) {
        return !StringUtils.isBlank(str);
    }

    // Trim AND strip
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 去年字符串两边的空白，加强 NULL 判断
     * </p>
     * <p/>
     * <pre>
     * StringUtils.trim(null)          = null
     * StringUtils.trim("")            = ""
     * StringUtils.trim("     ")       = ""
     * StringUtils.trim("abc")         = "abc"
     * StringUtils.trim("    abc    ") = "abc"
     * </pre>
     *
     * @param str 待处理的字符串
     * @return 去掉空白的字符串或者NULL
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * <p>
     * 去掉字符串两边指定的字符，注意，并不是整个匹配时才去掉，而是所有在 <code>stripChars</code>中 出现的字符都去掉
     * </p>
     * <p/>
     * <pre>
     * StringUtils.strip(null, *)          = null
     * StringUtils.strip("", *)            = ""
     * StringUtils.strip("abc", null)      = "abc"
     * StringUtils.strip("  abc", null)    = "abc"
     * StringUtils.strip("abc  ", null)    = "abc"
     * StringUtils.strip(" abc ", null)    = "abc"
     * StringUtils.strip("  abcyx", "xyz") = "  abc"
     * </pre>
     *
     * @param str        待处理的字符
     * @param stripChars 要去掉的字符
     * @return 处理后字符
     */
    public static String strip(String str, String stripChars) {
        if (isEmpty(str)) {
            return str;
        }
        str = stripStart(str, stripChars);
        return stripEnd(str, stripChars);
    }

    /**
     * <p>
     * 去掉字符串头部指定的字符，注意，并不是整个匹配时才去掉，而是所有在 <code>stripChars</code>中 出现的字符都去掉
     * </p>
     * <p/>
     * <pre>
     * StringUtils.stripStart(null, *)          = null
     * StringUtils.stripStart("", *)            = ""
     * StringUtils.stripStart("abc", "")        = "abc"
     * StringUtils.stripStart("abc", null)      = "abc"
     * StringUtils.stripStart("  abc", null)    = "abc"
     * StringUtils.stripStart("abc  ", null)    = "abc  "
     * StringUtils.stripStart(" abc ", null)    = "abc "
     * StringUtils.stripStart("yxabc  ", "xyz") = "abc  "
     * </pre>
     *
     * @param str        待处理的字符
     * @param stripChars 要去掉的字符
     * @return 处理后字符
     */
    public static String stripStart(String str, String stripChars) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        int start = 0;
        if (stripChars == null) {
            while ((start != strLen)
                    && Character.isWhitespace(str.charAt(start))) {
                start++;
            }
        } else if (stripChars.length() == 0) {
            return str;
        } else {
            while ((start != strLen)
                    && (stripChars.indexOf(str.charAt(start)) != -1)) {
                start++;
            }
        }
        return str.substring(start);
    }

    /**
     * <p>
     * 去掉字符串尾部指定的字符，注意，并不是整个匹配时才去掉，而是所有在 <code>stripChars</code>中 出现的字符都去掉
     * </p>
     * <p/>
     * <pre>
     * StringUtils.stripEnd(null, *)          = null
     * StringUtils.stripEnd("", *)            = ""
     * StringUtils.stripEnd("abc", "")        = "abc"
     * StringUtils.stripEnd("abc", null)      = "abc"
     * StringUtils.stripEnd("  abc", null)    = "  abc"
     * StringUtils.stripEnd("abc  ", null)    = "abc"
     * StringUtils.stripEnd(" abc ", null)    = " abc"
     * StringUtils.stripEnd("  abcyx", "xyz") = "  abc"
     * </pre>
     *
     * @param str        待处理的字符
     * @param stripChars 要去掉的字符
     * @return 处理后字符
     */
    public static String stripEnd(String str, String stripChars) {
        int end;
        if (str == null || (end = str.length()) == 0) {
            return str;
        }

        if (stripChars == null) {
            while ((end != 0) && Character.isWhitespace(str.charAt(end - 1))) {
                end--;
            }
        } else if (stripChars.length() == 0) {
            return str;
        } else {
            while ((end != 0)
                    && (stripChars.indexOf(str.charAt(end - 1)) != -1)) {
                end--;
            }
        }
        return str.substring(0, end);
    }

    // Left/Right/Mid
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 获取字符串开头的 len 个字符组成的子串，如果不足 len 个，则全部返回
     * </p>
     * <p/>
     * <p>
     * 如果源串为 null 或者 len 小于源串长度都不会抛异常，但是如果 len 小于０则会抛异常
     * </p>
     * <p/>
     * <pre>
     * StringUtils.left(null, *)    = null
     * StringUtils.left(*, -ve)     = ""
     * StringUtils.left("", *)      = ""
     * StringUtils.left("abc", 0)   = ""
     * StringUtils.left("abc", 2)   = "ab"
     * StringUtils.left("abc", 4)   = "abc"
     * </pre>
     *
     * @param str 源字符串
     * @param len 要获取的长度
     * @return 字符串开头的 len 个字符组成的子串
     */
    public static String left(String str, int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return EMPTY;
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(0, len);
    }

    /**
     * <p>
     * 获取字符串结尾的 len 个字符组成的子串，如果不足 len 个，则全部返回
     * </p>
     * <p/>
     * <p>
     * 如果源串为 null 或者 len 小于源串长度都不会抛异常，但是如果 len 小于０则会抛异常
     * </p>
     * <p/>
     * <pre>
     * StringUtils.right(null, *)    = null
     * StringUtils.right(*, -ve)     = ""
     * StringUtils.right("", *)      = ""
     * StringUtils.right("abc", 0)   = ""
     * StringUtils.right("abc", 2)   = "bc"
     * StringUtils.right("abc", 4)   = "abc"
     * </pre>
     *
     * @param str 源字符串
     * @param len 要获取的长度
     * @return 字符串结尾的 len 个字符组成的子串
     */
    public static String right(String str, int len) {
        if (str == null) {
            return null;
        }
        if (len < 0) {
            return EMPTY;
        }
        if (str.length() <= len) {
            return str;
        }
        return str.substring(str.length() - len);
    }

    /**
     * <p>
     * 获取字符串中间的 len 个字符组成的子串，如果不足 len 个，则全部返回
     * </p>
     * <p/>
     * <p>
     * 如果源串为 null 或者 len 小于源串长度都不会抛异常，但是如果 len 小于０则会抛异常
     * </p>
     * <p/>
     * <pre>
     * StringUtils.mid(null, *, *)    = null
     * StringUtils.mid(*, *, -ve)     = ""
     * StringUtils.mid("", 0, *)      = ""
     * StringUtils.mid("abc", 0, 2)   = "ab"
     * StringUtils.mid("abc", 0, 4)   = "abc"
     * StringUtils.mid("abc", 2, 4)   = "c"
     * StringUtils.mid("abc", 4, 2)   = ""
     * StringUtils.mid("abc", -2, 2)  = "ab"
     * </pre>
     *
     * @param str 源字符串
     * @param pos 开始位置
     * @param len 要获取的长度
     * @return 字符串中间的 len 个字符组成的子串
     */
    public static String mid(String str, int pos, int len) {
        if (str == null) {
            return null;
        }
        if (len < 0 || pos > str.length()) {
            return EMPTY;
        }
        if (pos < 0) {
            pos = 0;
        }
        if (str.length() <= (pos + len)) {
            return str.substring(pos);
        }
        return str.substring(pos, pos + len);
    }

    // Joining
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 连接数组对象的元素为字符串
     * </p>
     * <p/>
     * <pre>
     * StringUtils.join(null)            = null
     * StringUtils.join([])              = ""
     * StringUtils.join([null])          = ""
     * StringUtils.join(["a", "b", "c"]) = "abc"
     * StringUtils.join([null, "", "a"]) = "a"
     * </pre>
     *
     * @param array 数组对象
     * @return 串接后的字符串，如果数组对象为 NULL 则返回 NULL
     */
    public static String join(Object[] array) {
        return join(array, null);
    }

    /**
     * <p>
     * 以指定的分隔符串接数组对象
     * </p>
     * <p/>
     * <p>
     * 生成的字符串头尾不会添加分隔符
     * </p>
     * <p/>
     * <pre>
     * StringUtils.join(null, *)               = null
     * StringUtils.join([], *)                 = ""
     * StringUtils.join([null], *)             = ""
     * StringUtils.join(["a", "b", "c"], ';')  = "a;b;c"
     * StringUtils.join(["a", "b", "c"], null) = "abc"
     * StringUtils.join([null, "", "a"], ';')  = ";;a"
     * </pre>
     *
     * @param array     数组对象
     * @param separator 分隔符
     * @return 串接后的字符串，如果数组对象为 NULL 则返回 NULL
     */
    public static String join(Object[] array, char separator) {
        if (array == null) {
            return null;
        }

        return join(array, separator, 0, array.length);
    }

    /**
     * <p>
     * 以指定的分隔符串接数组对象
     * </p>
     * <p/>
     * <p>
     * 生成的字符串头尾不会添加分隔符
     * </p>
     * <p/>
     * <pre>
     * StringUtils.join(null, *)               = null
     * StringUtils.join([], *)                 = ""
     * StringUtils.join([null], *)             = ""
     * StringUtils.join(["a", "b", "c"], ';')  = "a;b;c"
     * StringUtils.join(["a", "b", "c"], null) = "abc"
     * StringUtils.join([null, "", "a"], ';')  = ";;a"
     * </pre>
     *
     * @param array      数组对象
     * @param separator  分隔符
     * @param startIndex 数组开始下标
     * @param endIndex   数组结束下标
     * @return 串接后的字符串，如果数组对象为 NULL 则返回 NULL
     */
    public static String join(Object[] array, char separator, int startIndex,
                              int endIndex) {
        if (array == null) {
            return null;
        }
        int bufSize = (endIndex - startIndex);
        if (bufSize <= 0) {
            return EMPTY;
        }

        bufSize *= ((array[startIndex] == null ? 16 : array[startIndex]
                .toString().length()) + 1);
        StringBuilder buf = new StringBuilder(bufSize);

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }

    /**
     * <p>
     * 以指定的分隔符串接数组对象
     * </p>
     * <p/>
     * <p>
     * 生成的字符串头尾不会添加分隔符
     * </p>
     * <p/>
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = ""
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], null)  = "abc"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = ",,a"
     * </pre>
     *
     * @param array     数组对象
     * @param separator 分隔符
     * @return 串接后的字符串，如果数组对象为 NULL 则返回 NULL
     */
    public static String join(Object[] array, String separator) {
        if (array == null) {
            return null;
        }
        return join(array, separator, 0, array.length);
    }

    /**
     * <p>
     * 以指定的分隔符串接数组对象
     * </p>
     * <p/>
     * <p>
     * 生成的字符串头尾不会添加分隔符
     * </p>
     * <p/>
     * <pre>
     * StringUtils.join(null, *)                = null
     * StringUtils.join([], *)                  = ""
     * StringUtils.join([null], *)              = ""
     * StringUtils.join(["a", "b", "c"], "--")  = "a--b--c"
     * StringUtils.join(["a", "b", "c"], null)  = "abc"
     * StringUtils.join(["a", "b", "c"], "")    = "abc"
     * StringUtils.join([null, "", "a"], ',')   = ",,a"
     * </pre>
     *
     * @param array      数组对象
     * @param separator  分隔符
     * @param startIndex 数组开始下标
     * @param endIndex   数组结束下标
     * @return 串接后的字符串，如果数组对象为 NULL 则返回 NULL
     */
    public static String join(Object[] array, String separator, int startIndex,
                              int endIndex) {
        if (array == null) {
            return null;
        }
        if (separator == null) {
            separator = EMPTY;
        }

        int bufSize = (endIndex - startIndex);
        if (bufSize <= 0) {
            return EMPTY;
        }

        bufSize *= ((array[startIndex] == null ? 16 : array[startIndex]
                .toString().length()) + separator.length());

        StringBuilder buf = new StringBuilder(bufSize);

        for (int i = startIndex; i < endIndex; i++) {
            if (i > startIndex) {
                buf.append(separator);
            }
            if (array[i] != null) {
                buf.append(array[i]);
            }
        }
        return buf.toString();
    }

    /**
     * <p>
     * 以指定的分隔符串接迭代对象
     * </p>
     * <p/>
     * <p>
     * 生成的字符串头尾不会添加分隔符
     * </p>
     *
     * @param iterator  迭代器
     * @param separator 分隔符
     * @return 串接后的字符串
     */
    public static String join(Iterator iterator, char separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return EMPTY;
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return objToString(first);
        }

        // two or more elements
        StringBuilder buf = new StringBuilder(256); // Java default is 16,
        // probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            buf.append(separator);
            Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }

        return buf.toString();
    }

    /**
     * <p>
     * 以指定的分隔符串接迭代对象
     * </p>
     * <p/>
     * <p>
     * 生成的字符串头尾不会添加分隔符
     * </p>
     *
     * @param iterator  迭代器
     * @param separator 分隔符
     * @return 串接后的字符串
     */
    public static String join(Iterator iterator, String separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return EMPTY;
        }
        Object first = iterator.next();
        if (!iterator.hasNext()) {
            return objToString(first);
        }

        // two or more elements
        StringBuilder buf = new StringBuilder(256); // Java default is 16,
        // probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }

    /**
     * <p>
     * 以指定的分隔符串接集合对象
     * </p>
     * <p/>
     * <p>
     * 生成的字符串头尾不会添加分隔符
     * </p>
     *
     * @param collection 集合
     * @param separator  分隔符
     * @return 串接后的字符串
     */
    public static String join(Collection collection, char separator) {
        if (collection == null) {
            return null;
        }
        return join(collection.iterator(), separator);
    }

    /**
     * <p>
     * 以指定的分隔符串接集合对象
     * </p>
     * <p/>
     * <p>
     * 生成的字符串头尾不会添加分隔符
     * </p>
     *
     * @param collection 集合
     * @param separator  分隔符
     * @return 串接后的字符串
     */
    public static String join(Collection collection, String separator) {
        if (collection == null) {
            return null;
        }
        return join(collection.iterator(), separator);
    }

    /**
     * <p>
     * 返回填充指定长度的 padChar.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.padding(0, 'e')  = ""
     * StringUtils.padding(3, 'e')  = "eee"
     * StringUtils.padding(-2, 'e') = IndexOutOfBoundsException
     * </pre>
     * <p/>
     * <p>
     * 该方法不支持 <a
     * href="http://www.unicode.org/glossary/#supplementary_character">Unicode
     * Supplementary Characters</a> 因为该类型的字符成对出现
     * </p>
     *
     * @param repeat  填充次数
     * @param padChar 填充的字符
     * @return 填充后的字符串
     * @throws IndexOutOfBoundsException 如果 <code>repeat &lt; 0</code> 则抛出该异常
     */
    private static String padding(int repeat, char padChar)
            throws IndexOutOfBoundsException {
        if (repeat < 0) {
            throw new IndexOutOfBoundsException(
                    "Cannot pad a negative amount: " + repeat);
        }
        final char[] buf = new char[repeat];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = padChar;
        }
        return new String(buf);
    }

    /**
     * <p>
     * 在字符串结尾填充空格以达到<code>size</code>长度.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.rightPad(null, *)   = null
     * StringUtils.rightPad("", 3)     = "   "
     * StringUtils.rightPad("bat", 3)  = "bat"
     * StringUtils.rightPad("bat", 5)  = "bat  "
     * StringUtils.rightPad("bat", 1)  = "bat"
     * StringUtils.rightPad("bat", -1) = "bat"
     * </pre>
     *
     * @param str  待填充的字符串
     * @param size 要达到的开席
     * @return 填充后的字符串
     */
    public static String rightPad(String str, int size) {
        return rightPad(str, size, ' ');
    }

    /**
     * <p>
     * 在字符串结尾填充指定字符以达到<code>size</code>长度.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.rightPad(null, *, *)     = null
     * StringUtils.rightPad("", 3, 'z')     = "zzz"
     * StringUtils.rightPad("bat", 3, 'z')  = "bat"
     * StringUtils.rightPad("bat", 5, 'z')  = "batzz"
     * StringUtils.rightPad("bat", 1, 'z')  = "bat"
     * StringUtils.rightPad("bat", -1, 'z') = "bat"
     * </pre>
     *
     * @param str     待填充的字符串
     * @param size    要达到的开席
     * @param padChar 填充字符
     * @return 填充后的字符串
     */
    public static String rightPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (pads > PAD_LIMIT) {
            return rightPad(str, size, String.valueOf(padChar));
        }
        return str.concat(padding(pads, padChar));
    }

    /**
     * <p>
     * 在字符串结尾填充指定字符以达到<code>size</code>长度.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.rightPad(null, *, *)      = null
     * StringUtils.rightPad("", 3, "z")      = "zzz"
     * StringUtils.rightPad("bat", 3, "yz")  = "bat"
     * StringUtils.rightPad("bat", 5, "yz")  = "batyz"
     * StringUtils.rightPad("bat", 8, "yz")  = "batyzyzy"
     * StringUtils.rightPad("bat", 1, "yz")  = "bat"
     * StringUtils.rightPad("bat", -1, "yz") = "bat"
     * StringUtils.rightPad("bat", 5, null)  = "bat  "
     * StringUtils.rightPad("bat", 5, "")    = "bat  "
     * </pre>
     *
     * @param str    待填充的字符串
     * @param size   要达到的开席
     * @param padStr 填充字符
     * @return 填充后的字符串
     */
    public static String rightPad(String str, int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (isEmpty(padStr)) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (padLen == 1 && pads <= PAD_LIMIT) {
            return rightPad(str, size, padStr.charAt(0));
        }

        if (pads == padLen) {
            return str.concat(padStr);
        } else if (pads < padLen) {
            return str.concat(padStr.substring(0, pads));
        } else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return str.concat(new String(padding));
        }
    }

    /**
     * <p>
     * 在字符串开头填充空格以达到<code>size</code>长度.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.leftPad(null, *)   = null
     * StringUtils.leftPad("", 3)     = "   "
     * StringUtils.leftPad("bat", 3)  = "bat"
     * StringUtils.leftPad("bat", 5)  = "  bat"
     * StringUtils.leftPad("bat", 1)  = "bat"
     * StringUtils.leftPad("bat", -1) = "bat"
     * </pre>
     *
     * @param str  待填充的字符串
     * @param size 要达到的开席
     * @return 填充后的字符串
     */
    public static String leftPad(String str, int size) {
        return leftPad(str, size, ' ');
    }

    /**
     * <p>
     * 在字符串开头填充指定字符以达到<code>size</code>长度.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.leftPad(null, *, *)     = null
     * StringUtils.leftPad("", 3, 'z')     = "zzz"
     * StringUtils.leftPad("bat", 3, 'z')  = "bat"
     * StringUtils.leftPad("bat", 5, 'z')  = "zzbat"
     * StringUtils.leftPad("bat", 1, 'z')  = "bat"
     * StringUtils.leftPad("bat", -1, 'z') = "bat"
     * </pre>
     *
     * @param str     待填充的字符串
     * @param size    要达到的开席
     * @param padChar 填充字符
     * @return 填充后的字符串
     */
    public static String leftPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        }
        int pads = size - str.length();
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (pads > PAD_LIMIT) {
            return leftPad(str, size, String.valueOf(padChar));
        }
        return padding(pads, padChar).concat(str);
    }

    /**
     * <p>
     * 在字符串开头填充指定字符以达到<code>size</code>长度.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.leftPad(null, *, *)      = null
     * StringUtils.leftPad("", 3, "z")      = "zzz"
     * StringUtils.leftPad("bat", 3, "yz")  = "bat"
     * StringUtils.leftPad("bat", 5, "yz")  = "yzbat"
     * StringUtils.leftPad("bat", 8, "yz")  = "yzyzybat"
     * StringUtils.leftPad("bat", 1, "yz")  = "bat"
     * StringUtils.leftPad("bat", -1, "yz") = "bat"
     * StringUtils.leftPad("bat", 5, null)  = "  bat"
     * StringUtils.leftPad("bat", 5, "")    = "  bat"
     * </pre>
     *
     * @param str    待填充的字符串
     * @param size   要达到的开席
     * @param padStr 填充字符
     * @return 填充后的字符串
     */
    public static String leftPad(String str, int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (isEmpty(padStr)) {
            padStr = " ";
        }
        int padLen = padStr.length();
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str; // returns original String when possible
        }
        if (padLen == 1 && pads <= PAD_LIMIT) {
            return leftPad(str, size, padStr.charAt(0));
        }

        if (pads == padLen) {
            return padStr.concat(str);
        } else if (pads < padLen) {
            return padStr.substring(0, pads).concat(str);
        } else {
            char[] padding = new char[pads];
            char[] padChars = padStr.toCharArray();
            for (int i = 0; i < pads; i++) {
                padding[i] = padChars[i % padLen];
            }
            return new String(padding).concat(str);
        }
    }

    // Centering
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 在字符串两侧填充空格以达到<code>size</code>长度，同时原字符串居中
     * </p>
     * <p/>
     * <pre>
     * StringUtils.center(null, *)   = null
     * StringUtils.center("", 4)     = "    "
     * StringUtils.center("ab", -1)  = "ab"
     * StringUtils.center("ab", 4)   = " ab "
     * StringUtils.center("abcd", 2) = "abcd"
     * StringUtils.center("a", 4)    = " a  "
     * </pre>
     *
     * @param str  待填充的字符串
     * @param size 要达到的开席
     * @return 填充后的字符串
     */
    public static String center(String str, int size) {
        return center(str, size, ' ');
    }

    /**
     * <p>
     * 在字符串两侧填充空格以达到<code>size</code>长度，同时原字符串居中
     * </p>
     * <p/>
     * <pre>
     * StringUtils.center(null, *, *)     = null
     * StringUtils.center("", 4, ' ')     = "    "
     * StringUtils.center("ab", -1, ' ')  = "ab"
     * StringUtils.center("ab", 4, ' ')   = " ab"
     * StringUtils.center("abcd", 2, ' ') = "abcd"
     * StringUtils.center("a", 4, ' ')    = " a  "
     * StringUtils.center("a", 4, 'y')    = "yayy"
     * </pre>
     *
     * @param str     待填充的字符串
     * @param size    要达到的开席
     * @param padChar 填充字符
     * @return 填充后的字符串
     */
    public static String center(String str, int size, char padChar) {
        if (str == null || size <= 0) {
            return str;
        }
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        str = leftPad(str, strLen + pads / 2, padChar);
        str = rightPad(str, size, padChar);
        return str;
    }

    /**
     * <p>
     * 在字符串两侧填充空格以达到<code>size</code>长度，同时原字符串居中
     * </p>
     * <p/>
     * <pre>
     * StringUtils.center(null, *, *)     = null
     * StringUtils.center("", 4, " ")     = "    "
     * StringUtils.center("ab", -1, " ")  = "ab"
     * StringUtils.center("ab", 4, " ")   = " ab"
     * StringUtils.center("abcd", 2, " ") = "abcd"
     * StringUtils.center("a", 4, " ")    = " a  "
     * StringUtils.center("a", 4, "yz")   = "yayz"
     * StringUtils.center("abc", 7, null) = "  abc  "
     * StringUtils.center("abc", 7, "")   = "  abc  "
     * </pre>
     *
     * @param str    待填充的字符串
     * @param size   要达到的开席
     * @param padStr 填充字符
     * @return 填充后的字符串
     * @throws IllegalArgumentException 填充字符串为 NULL 或空字符串
     */
    public static String center(String str, int size, String padStr) {
        if (str == null || size <= 0) {
            return str;
        }
        if (isEmpty(padStr)) {
            padStr = " ";
        }
        int strLen = str.length();
        int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        str = leftPad(str, strLen + pads / 2, padStr);
        str = rightPad(str, size, padStr);
        return str;
    }

    /**
     * <p>
     * 将字符串第一个字符变为大写，其它的不变
     * </p>
     * <p/>
     * <p/>
     * <pre>
     * StringUtils.capitalize(null)  = null
     * StringUtils.capitalize("")    = ""
     * StringUtils.capitalize("cat") = "Cat"
     * StringUtils.capitalize("cAt") = "CAt"
     * </pre>
     *
     * @param str 待处理的字符串
     * @return 处理后的字符串
     * @see #uncapitalize(String)
     */
    public static String capitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuffer(strLen)
                .append(Character.toTitleCase(str.charAt(0)))
                .append(str.substring(1)).toString();
    }

    /**
     * <p>
     * 将字符串第一个字符变为小写，其它的不变
     * </p>
     * <p/>
     * <pre>
     * StringUtils.uncapitalize(null)  = null
     * StringUtils.uncapitalize("")    = ""
     * StringUtils.uncapitalize("Cat") = "cat"
     * StringUtils.uncapitalize("CAT") = "cAT"
     * </pre>
     *
     * @param str 待处理的字符串
     * @return 处理后的字符串
     * @see #capitalize(String)
     */
    public static String uncapitalize(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        return new StringBuffer(strLen)
                .append(Character.toLowerCase(str.charAt(0)))
                .append(str.substring(1)).toString();
    }

    // Character Tests
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 判断字符串是否是字母组成
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isAlpha(null)   = false
     * StringUtils.isAlpha("")     = true
     * StringUtils.isAlpha("  ")   = false
     * StringUtils.isAlpha("abc")  = true
     * StringUtils.isAlpha("ab2c") = false
     * StringUtils.isAlpha("ab-c") = false
     * </pre>
     *
     * @param str 待校验字符串
     * @return 如果只包含字母则返回 TRUE，否则返回 FALSE
     */
    public static boolean isAlpha(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetter(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * 判断字符串是否是字母或者空格组成
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isAlphaSpace(null)   = false
     * StringUtils.isAlphaSpace("")     = true
     * StringUtils.isAlphaSpace("  ")   = true
     * StringUtils.isAlphaSpace("abc")  = true
     * StringUtils.isAlphaSpace("ab c") = true
     * StringUtils.isAlphaSpace("ab2c") = false
     * StringUtils.isAlphaSpace("ab-c") = false
     * </pre>
     *
     * @param str 待校验字符串
     * @return 如果只包含字母及空格则返回 TRUE，否则返回 FALSE
     */
    public static boolean isAlphaSpace(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if ((!Character.isLetter(str.charAt(i))) && (str.charAt(i) != ' ')) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * 判断字符串是否是字母或者数字组成
     * </p>
     * <p/>
     * <p>
     * <code>null</code> will return <code>false</code>. An empty String ("")
     * will return <code>true</code>.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isAlphanumeric(null)   = false
     * StringUtils.isAlphanumeric("")     = true
     * StringUtils.isAlphanumeric("  ")   = false
     * StringUtils.isAlphanumeric("abc")  = true
     * StringUtils.isAlphanumeric("ab c") = false
     * StringUtils.isAlphanumeric("ab2c") = true
     * StringUtils.isAlphanumeric("ab-c") = false
     * </pre>
     *
     * @param str 待校验字符串
     * @return 如果只包含字母及数字则返回 TRUE，否则返回 FALSE
     */
    public static boolean isAlphanumeric(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isLetterOrDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * 判断字符串是否是字母、空格或者数字组成
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isAlphanumeric(null)   = false
     * StringUtils.isAlphanumeric("")     = true
     * StringUtils.isAlphanumeric("  ")   = true
     * StringUtils.isAlphanumeric("abc")  = true
     * StringUtils.isAlphanumeric("ab c") = true
     * StringUtils.isAlphanumeric("ab2c") = true
     * StringUtils.isAlphanumeric("ab-c") = false
     * </pre>
     *
     * @param str 待校验字符串
     * @return 如果只包含字母、空格或者数字则返回 TRUE，否则返回 FALSE
     */
    public static boolean isAlphanumericSpace(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if ((!Character.isLetterOrDigit(str.charAt(i)))
                    && (str.charAt(i) != ' ')) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * 判断字符串是否是数字组成
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isNumeric(null)   = false
     * StringUtils.isNumeric("")     = true
     * StringUtils.isNumeric("  ")   = false
     * StringUtils.isNumeric("123")  = true
     * StringUtils.isNumeric("12 3") = false
     * StringUtils.isNumeric("ab2c") = false
     * StringUtils.isNumeric("12-3") = false
     * StringUtils.isNumeric("12.3") = false
     * </pre>
     *
     * @param str 待校验字符串
     * @return 如果只包含数字则返回 TRUE，否则返回 FALSE
     */
    public static boolean isNumeric(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * 判断字符串是否是数字或者空格组成
     * </p>
     * <p/>
     * <pre>
     * StringUtils.isNumeric(null)   = false
     * StringUtils.isNumeric("")     = true
     * StringUtils.isNumeric("  ")   = true
     * StringUtils.isNumeric("123")  = true
     * StringUtils.isNumeric("12 3") = true
     * StringUtils.isNumeric("ab2c") = false
     * StringUtils.isNumeric("12-3") = false
     * StringUtils.isNumeric("12.3") = false
     * </pre>
     *
     * @param str 待校验字符串
     * @return 如果只包含数字或者空格则返回 TRUE，否则返回 FALSE
     */
    public static boolean isNumericSpace(String str) {
        if (str == null) {
            return false;
        }
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            if ((!Character.isDigit(str.charAt(i))) && (str.charAt(i) != ' ')) {
                return false;
            }
        }
        return true;
    }

    // Defaults
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 判断字符串如果为空，则返回默认的字符串，否则返回原字符串
     * </p>
     * <p/>
     * <pre>
     * StringUtils.defaultIfEmpty(null, "NULL")  = "NULL"
     * StringUtils.defaultIfEmpty("", "NULL")    = "NULL"
     * StringUtils.defaultIfEmpty("bat", "NULL") = "bat"
     * </pre>
     *
     * @param str        待校验字符串
     * @param defaultStr 默认字符串
     * @return 原字符串或者默认字符串
     */
    public static String defaultIfEmpty(String str, String defaultStr) {
        return StringUtils.isEmpty(str) ? defaultStr : str;
    }

    // Reversing
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 反转字符串 {@link StringBuffer#reverse()}.
     * </p>
     * <p/>
     * <pre>
     * StringUtils.reverse(null)  = null
     * StringUtils.reverse("")    = ""
     * StringUtils.reverse("bat") = "tab"
     * </pre>
     *
     * @param str 待反转的字符串
     * @return 反转后的字符串
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuffer(str).reverse().toString();
    }

    // Abbreviating
    // -----------------------------------------------------------------------

    /**
     * <p>
     * 缩写字符串，超过部分采用 ... 代替
     * </p>
     * <p/>
     * <p>
     * 说明:
     * <ul>
     * <li>如果 <code>str</code> 长度小于 <code>maxWidth</code> 个字符，则直接返回</li>
     * <li>否则缩写为 <code>(substring(str, 0, max-3) + "...")</code>.</li>
     * <li>如果 <code>maxWidth</code> 小于 4 个字符，抛出
     * <code>IllegalArgumentException</code>异常.</li>
     * </ul>
     * </p>
     * <p/>
     * <pre>
     * StringUtils.abbreviate(null, *)      = null
     * StringUtils.abbreviate("", 4)        = ""
     * StringUtils.abbreviate("abcdefg", 6) = "abc..."
     * StringUtils.abbreviate("abcdefg", 7) = "abcdefg"
     * StringUtils.abbreviate("abcdefg", 8) = "abcdefg"
     * StringUtils.abbreviate("abcdefg", 4) = "a..."
     * StringUtils.abbreviate("abcdefg", 3) = IllegalArgumentException
     * </pre>
     *
     * @param str      待缩写的字符串
     * @param maxWidth 最大长度
     * @return 简写后的字符串
     * @throws IllegalArgumentException 指定的长度太小时抛出该异常
     */
    public static String abbreviate(String str, int maxWidth) {
        return abbreviate(str, 0, maxWidth);
    }

    /**
     * <p>
     * 根据一定的左偏移量缩写字符串，超过部分采用 ... 代替
     * </p>
     * <p/>
     * <pre>
     * StringUtils.abbreviate(null, *, *)                = null
     * StringUtils.abbreviate("", 0, 4)                  = ""
     * StringUtils.abbreviate("abcdefghijklmno", -1, 10) = "abcdefg..."
     * StringUtils.abbreviate("abcdefghijklmno", 0, 10)  = "abcdefg..."
     * StringUtils.abbreviate("abcdefghijklmno", 1, 10)  = "abcdefg..."
     * StringUtils.abbreviate("abcdefghijklmno", 4, 10)  = "abcdefg..."
     * StringUtils.abbreviate("abcdefghijklmno", 5, 10)  = "...fghi..."
     * StringUtils.abbreviate("abcdefghijklmno", 6, 10)  = "...ghij..."
     * StringUtils.abbreviate("abcdefghijklmno", 8, 10)  = "...ijklmno"
     * StringUtils.abbreviate("abcdefghijklmno", 10, 10) = "...ijklmno"
     * StringUtils.abbreviate("abcdefghijklmno", 12, 10) = "...ijklmno"
     * StringUtils.abbreviate("abcdefghij", 0, 3)        = IllegalArgumentException
     * StringUtils.abbreviate("abcdefghij", 5, 6)        = IllegalArgumentException
     * </pre>
     *
     * @param str      待缩写的字符串
     * @param offset   偏移量
     * @param maxWidth 最大长度
     * @return 简写后的字符串
     * @throws IllegalArgumentException 指定的长度太小时抛出该异常
     */
    public static String abbreviate(String str, int offset, int maxWidth) {
        if (str == null) {
            return null;
        }
        if (maxWidth < 4) {
            throw new IllegalArgumentException(
                    "Minimum abbreviation width is 4");
        }
        if (str.length() <= maxWidth) {
            return str;
        }
        if (offset > str.length()) {
            offset = str.length();
        }
        if ((str.length() - offset) < (maxWidth - 3)) {
            offset = str.length() - (maxWidth - 3);
        }
        if (offset <= 4) {
            return str.substring(0, maxWidth - 3) + "...";
        }
        if (maxWidth < 7) {
            throw new IllegalArgumentException(
                    "Minimum abbreviation width with offset is 7");
        }
        if ((offset + (maxWidth - 3)) < str.length()) {
            return "..." + abbreviate(str.substring(offset), maxWidth - 3);
        }
        return "..." + str.substring(str.length() - (maxWidth - 3));
    }

    /**
     * 对象转化成字符串，处理 null 对象
     *
     * @param o 对象
     * @return 字符串
     */
    private static String objToString(Object o) {
        return null == o ? "" : o.toString();
    }

    /**
     * 如果字符长度过长，就将字符截断，用省略号代替
     *
     * @param str 要进行判断的字符串
     * @param len 字符串的最大长度
     * @return 截断处理之后的字符串
     */
    public static String cutStringIfNeed(String str, int len) {
        str = str.trim();
        if (str.length() > len) {
            str = str.substring(0, len);
            str += "...";
        }

        return str;
    }

    /**
     * 将字符串转换成 long 型
     *
     * @param s 字符串
     * @return 正常情况返回 long 类型数据，如果出错返回 Long.MIN_VALUE，
     */
    public static long toLong(String s) {
        return toLong(s, 0);
    }

    /**
     * 将字符串转换成 long 型，如果发生转换的异常那么返回默认值
     *
     * @param s            字符串
     * @param defaultValue 默认值
     * @return 如果发生转换的异常那么返回默认值
     */
    public static long toLong(String s, long defaultValue) {
        long ret = defaultValue;

        try {
            if (s != null && s.trim().length() > 0)
                ret = Long.valueOf(s.trim());
        } catch (Exception e) {
        }

        return ret;
    }

    /**
     * 将字符串转换成 int 型
     *
     * @param s 字符串
     * @return 正常情况返回 long 类型数据，如果出错返回 Long.MIN_VALUE，
     */
    public static int toInt(String s) {
        return toInt(s, 0);
    }

    /**
     * 将字符串转换成 toInt 型，如果发生转换的异常那么返回默认值
     *
     * @param s            字符串
     * @param defaultValue 默认值
     * @return 如果发生转换的异常那么返回默认值
     */
    public static int toInt(String s, int defaultValue) {
        int ret = defaultValue;

        try {
            if (s != null && s.trim().length() > 0)
                ret = Integer.valueOf(s.trim());
        } catch (Exception e) {
        }

        return ret;
    }

    static Set<String> specialWords = new HashSet<String>() {
        {
            add("\\?");
            add("\\*");
            add("\\.");
            add("\\+");
            add("\\)");
            add("\\(");
            add("\\$");
            add("\\{");
            add("\\}");
            add("\\|");
            add("\\^");
            add("\\\\");
            add("@");
            add("&");
            add("#");
            add("（");
            add("）");
            add("｛");
            add("｝");
            add("＋");
            add("．");
            add("＊");
            add("？");
            add("＄");
            add("｜");
            add("＆");
            add("＃");
            add("'");
            add("\"");
        }
    };

    /**
     * 过滤搜索相关的敏感字符
     *
     * @param keyword 关键词
     * @return 过滤后的字符串
     */
    public static String escapeForSearch(String keyword) {
        if (isBlank(keyword)) {
            return keyword;
        }
        for (String specialWord : specialWords) {
            keyword = keyword.replaceAll(specialWord, "");
        }
        return keyword.trim();
    }

    /**
     * 处理字符串中的反斜杠以便于JSON处理后能够正常显示。
     * 参考JSON官网说明http://www.json.org/json-zh.html，处理逻辑如下：
     * 如果碰到[\"],[\\],[\/],[\b],[\f],[\n],[\r],[\t]
     * -以及[反斜杠+u+四位十六进制数字]这几种反斜杠形式的话就不过滤，否则，将单个'\'变成'\\'
     *
     * @param input
     * @return
     * @author ethonchan
     */
    public static String processBackSlashForJSON(String input) {
        StringBuilder proResult = new StringBuilder();
        /**
         * 过滤[\"],[\\],[\/],[\b],[\f],[\n],[\r],[\t]这几种
         */
        Pattern pattern = Pattern.compile("\\\\[^\"\b\f\n\r\t/\\\\]");
        Matcher matcher = pattern.matcher(input);

        int totalLen = input.length();
        int copyStart = 0;

        while (matcher.find()) {
            proResult.append(input.subSequence(copyStart, matcher.start()));

            String find = input.substring(matcher.start(), matcher.end());
            int tail = matcher.end() + 4;
            tail = tail < totalLen ? tail : totalLen - 1;
            String tmpStr = input.substring(matcher.start(), tail);

            // 过滤[反斜杠+u+四位十六进制数字]的情况
            if (!Pattern.matches("\\\\u[0-9a-fA-F]{4,4}", tmpStr)) {
                // 将这里的单个反斜杠变成两个反斜杠
                find = "\\\\" + find.substring(1);
            }
            proResult.append(find);
            copyStart = matcher.end();
        }

        if (copyStart < totalLen) {
            proResult.append(input.substring(copyStart));
        }

        return proResult.toString();
    }

    /**
     * 验证是否为固定电话。 格式：区号-电话号码-分机号（其中分机号可以不填）。区号3-4位，电话号码3-9位，分机号1-5位。
     *
     * @param str
     * @return
     * @author ethonchan
     */
    public static boolean isFixedPhone(String str) {
        String patternStr = "0\\d{2,3}-\\d{5,8}(-\\d{1,5})?";
        return validateString(str, patternStr);
    }

    /**
     * 验证手机号码的合法性 手机号码：数字，11位
     *
     * @param str
     * @return
     * @author ethonchan
     */
    public static boolean isMobileNumber(String str) {
        String patternStr = "1\\d{10}";
        return validateString(str, patternStr);
    }

    /**
     * 验证邮政编码的合法性 邮政编码：6位数字
     *
     * @param str
     * @return
     */
    public static boolean isPostNumber(String str) {
        String patternStr = "\\d{6}";
        return validateString(str, patternStr);
    }

    private static boolean validateString(String str, String patternStr) {
        if (str == null) {
            return false;
        } else if (patternStr == null) {
            return true;
        }

        Pattern p = Pattern.compile(patternStr);
        Matcher match = p.matcher(str);
        return match.matches();
    }

    /**
     * 格式化字符串
     *
     * @param format
     * @param param
     * @return
     */
    public static String formatString(String format, Object... param) {
        String result = "";
        if (!StringUtils.isBlank(format)) {
            try {
                result = String.format(Locale.CHINA, format, param);
            } catch (Exception e) {
                Log.e("Utils", e.getMessage(), e);
            }
        }

        return result;
    }


    /**
     * 截掉Json字符串后面的冗余字符
     *
     * @param jsonFrom
     * @return
     */
    public static String subJsonSuffix(String jsonFrom) {
        String jsonTo = "";
        String suffix_1 = "}";
        String suffix_2 = "]";
        int lastIndex = -1;

        if (TextUtils.isEmpty(jsonFrom)) {
            return "{\"error\":\"Json string is NULL or empty\"}";

        } else if (jsonFrom.endsWith(suffix_1) || jsonFrom.endsWith(suffix_2)) {
            jsonTo = jsonFrom;

        } else {
            lastIndex = Math.max(jsonFrom.lastIndexOf(suffix_1), jsonFrom.lastIndexOf(suffix_2));
            if (lastIndex == -1) {
                return "{\"error\":\"Not a Json form\"}";
            }
            jsonTo = jsonFrom.substring(0, lastIndex + 1);
        }

        return jsonTo.trim();
    }

    /**
     * 比较两个字符串是否相等
     *
     * @param s1
     * @param s2
     * @return
     */
    public static boolean equals(String s1, String s2) {
        if (s1 != null)
            return s1.equals(s2);

        return false;
    }

}
