package ipu.result.parser;

import ipu.result.model.*;

/**
 * Turns the text extracted from a result PDF into a {@link ParsedResult}.
 *
 * It reads the document line by line, keeping track of the current college,
 * programme, semester and student. Because it is purely sequential it is
 * unaffected by a student's record spilling across pages.
 *
 * The line grammar (one record per line, pipe-delimited) is documented in
 * docs/pdf-format.md and is produced by {@link FixturePdfGenerator}. A real
 * GGSIPU gazette has a messier layout; this class is the single place to
 * re-tune when a real PDF is supplied, and nothing downstream has to change.
 */
public class ResultExtractor {

    public ParsedResult extract(String text) {
        ParsedResult out = new ParsedResult();

        String currentCollege = null;
        String currentProgramme = null;
        int currentSemester = 0;
        String currentStudent = null;

        int lineNo = 0;
        for (String rawLine : text.split("\\r?\\n")) {
            lineNo++;
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("SESSION:")) {
                out.examSession = line.substring("SESSION:".length()).trim();
                continue;
            }
            if (line.indexOf('|') < 0) continue; // headings, banners, page text

            String[] f = line.split("\\|");
            String tag = f[0].trim();

            switch (tag) {
                case "SESSION" -> out.examSession = val(f, 1);

                case "COLLEGE" -> {
                    currentCollege = req(f, 1, lineNo, "college code");
                    out.colleges.putIfAbsent(currentCollege, new College(currentCollege, val(f, 2)));
                }

                case "PROGRAMME" -> {
                    currentProgramme = req(f, 1, lineNo, "programme code");
                    String name = val(f, 2);
                    int duration = parseInt(val(f, 3), 4);
                    currentSemester = parseInt(val(f, 4), 0);
                    out.programmes.putIfAbsent(currentProgramme, new Programme(currentProgramme, name, duration));
                }

                case "PAPER" -> {
                    String paperId = req(f, 1, lineNo, "paper id");
                    out.subjects.putIfAbsent(paperId, new Subject(
                            paperId, val(f, 2), val(f, 3),
                            parseInt(val(f, 4), 0), currentSemester, currentProgramme));
                }

                case "STUDENT" -> {
                    currentStudent = req(f, 1, lineNo, "enrollment no");
                    Integer batch = batchYearFromSession(out.examSession);
                    out.students.add(new Student(currentStudent, val(f, 2),
                            currentCollege, currentProgramme, batch));
                }

                case "MARK" -> {
                    if (currentStudent == null) {
                        throw new ParseException("MARK before any STUDENT at line " + lineNo);
                    }
                    String paperId = req(f, 1, lineNo, "paper id");
                    boolean absent = isAbsent(val(f, 4)) || isAbsent(val(f, 3));
                    Integer internal = absent ? null : parseIntOrNull(val(f, 2));
                    Integer external = absent ? null : parseIntOrNull(val(f, 3));
                    Integer total    = absent ? null : parseIntOrNull(val(f, 4));
                    String grade = emptyToNull(val(f, 5));
                    out.marks.add(new Mark(currentStudent, paperId, internal, external, total, grade, absent));
                }

                default -> { /* unknown line, ignore */ }
            }
        }
        return out;
    }

    private static boolean isAbsent(String s) {
        return s != null && (s.equalsIgnoreCase("AB") || s.equalsIgnoreCase("ABSENT"));
    }

    private static Integer batchYearFromSession(String session) {
        if (session == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(20\\d{2})").matcher(session);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static String val(String[] f, int i) {
        return i < f.length ? f[i].trim() : "";
    }

    private static String req(String[] f, int i, int lineNo, String what) {
        String v = val(f, i);
        if (v.isEmpty()) throw new ParseException("missing " + what + " at line " + lineNo);
        return v;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private static int parseInt(String s, int dflt) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return dflt; }
    }

    private static Integer parseIntOrNull(String s) {
        try { return Integer.valueOf(s.trim()); } catch (Exception e) { return null; }
    }

    /** Thrown when the PDF text does not match the expected grammar. */
    public static class ParseException extends RuntimeException {
        public ParseException(String message) { super(message); }
    }
}
