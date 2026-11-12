package phlingo.api;

import java.math.BigDecimal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small JSON reader/writer to keep the API framework-free. */
final class Json {
    private Json() { }

    static Object parse(String source) {
        Parser p = new Parser(source == null ? "" : source);
        Object value = p.value();
        p.space();
        if (p.at < p.text.length()) throw new IllegalArgumentException("JSON contains extra data");
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(String source) {
        Object value = parse(source);
        if (!(value instanceof Map)) throw new IllegalArgumentException("JSON body must be an object");
        return (Map<String, Object>) value;
    }

    static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof String || value instanceof Character || value instanceof TemporalAccessor) {
            quote(String.valueOf(value), out); return;
        }
        if (value instanceof Number || value instanceof Boolean) { out.append(value); return; }
        if (value instanceof Map) {
            out.append('{'); boolean first = true;
            for (Object entryObject : ((Map<?, ?>) value).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObject;
                if (!first) out.append(','); first = false;
                quote(String.valueOf(entry.getKey()), out); out.append(':'); write(entry.getValue(), out);
            }
            out.append('}'); return;
        }
        if (value instanceof Collection) {
            out.append('['); boolean first = true;
            for (Object item : (Collection<?>) value) { if (!first) out.append(','); first = false; write(item, out); }
            out.append(']'); return;
        }
        if (value.getClass().isArray()) {
            out.append('['); int n = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < n; i++) { if (i > 0) out.append(','); write(java.lang.reflect.Array.get(value, i), out); }
            out.append(']'); return;
        }
        quote(String.valueOf(value), out);
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: if (c < 0x20) out.append(String.format("\\u%04x", (int) c)); else out.append(c);
            }
        }
        out.append('"');
    }

    private static final class Parser {
        final String text; int at;
        Parser(String text) { this.text = text; }
        void space() { while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++; }
        Object value() {
            space(); if (at >= text.length()) throw bad();
            char c = text.charAt(at);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return string();
            if (c == 't' && literal("true")) return Boolean.TRUE;
            if (c == 'f' && literal("false")) return Boolean.FALSE;
            if (c == 'n' && literal("null")) return null;
            if (c == '-' || Character.isDigit(c)) return number();
            throw bad();
        }
        Map<String, Object> object() {
            at++; LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>(); space();
            if (take('}')) return result;
            while (true) {
                space(); if (at >= text.length() || text.charAt(at) != '"') throw bad();
                String key = string(); space(); if (!take(':')) throw bad();
                result.put(key, value()); space(); if (take('}')) return result; if (!take(',')) throw bad();
            }
        }
        ArrayList<Object> array() {
            at++; ArrayList<Object> result = new ArrayList<Object>(); space();
            if (take(']')) return result;
            while (true) { result.add(value()); space(); if (take(']')) return result; if (!take(',')) throw bad(); }
        }
        String string() {
            at++; StringBuilder s = new StringBuilder();
            while (at < text.length()) {
                char c = text.charAt(at++);
                if (c == '"') return s.toString();
                if (c != '\\') { if (c < 0x20) throw bad(); s.append(c); continue; }
                if (at >= text.length()) throw bad(); char e = text.charAt(at++);
                switch (e) {
                    case '"': s.append('"'); break; case '\\': s.append('\\'); break; case '/': s.append('/'); break;
                    case 'b': s.append('\b'); break; case 'f': s.append('\f'); break; case 'n': s.append('\n'); break;
                    case 'r': s.append('\r'); break; case 't': s.append('\t'); break;
                    case 'u': if (at + 4 > text.length()) throw bad();
                        try { s.append((char) Integer.parseInt(text.substring(at, at + 4), 16)); } catch (NumberFormatException ex) { throw bad(); }
                        at += 4; break;
                    default: throw bad();
                }
            }
            throw bad();
        }
        Number number() {
            int start = at;
            if (take('-')) { }
            if (take('0')) { } else { digits(); }
            boolean decimal = false;
            if (take('.')) { decimal = true; digits(); }
            if (at < text.length() && (text.charAt(at) == 'e' || text.charAt(at) == 'E')) {
                decimal = true; at++; if (at < text.length() && (text.charAt(at) == '+' || text.charAt(at) == '-')) at++; digits();
            }
            String n = text.substring(start, at);
            try { if (!decimal) return Long.valueOf(n); return new BigDecimal(n); }
            catch (NumberFormatException ex) { throw bad(); }
        }
        void digits() { int start = at; while (at < text.length() && Character.isDigit(text.charAt(at))) at++; if (start == at) throw bad(); }
        boolean literal(String expected) { if (!text.startsWith(expected, at)) return false; at += expected.length(); return true; }
        boolean take(char c) { if (at < text.length() && text.charAt(at) == c) { at++; return true; } return false; }
        IllegalArgumentException bad() { return new IllegalArgumentException("Malformed JSON at offset " + at); }
    }
}
