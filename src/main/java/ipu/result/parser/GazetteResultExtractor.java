package ipu.result.parser;

import ipu.result.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a GGSIPU end-semester <b>Result Tabulation Sheet</b> (the gazette the
 * university publishes once an exam result is declared) into a {@link ParsedResult}.
 *
 * <h2>Why this parser exists</h2>
 * The gazette is a dense grid. When PDFBox extracts it with
 * {@code setSortByPosition(true)} each student's single visual row is flattened
 * into several text lines. A typical student looks like this (one college, one
 * semester; the numbers here are illustrative):
 *
 * <pre>
 * 00199904420 044101(4) 044161(1) 044171(1)  6
 * AARAV SHARMA
 * SID: 000000000001  20  53  35  50 -  84
 * SchemeID: ...
 *  1 73(A) 85(A+) 84(A+)
 * </pre>
 *
 * Three data lines carry one student:
 * <ol>
 *   <li><b>anchor</b> &mdash; the 11-digit enrollment number, then one
 *       {@code paperid(credits)} token per paper, then the credits secured (CS);</li>
 *   <li><b>SID line</b> &mdash; {@code internal external} marks <i>interleaved</i>,
 *       two tokens per paper (a dash or {@code A} where a component is absent);</li>
 *   <li><b>totals line</b> &mdash; a leading serial number, then one
 *       {@code total(grade)} token per paper, or a bare {@code A} for an absent paper.</li>
 * </ol>
 *
 * <h2>Key decisions</h2>
 * <ul>
 *   <li>Credits are read from the anchor's {@code (n)}, so elective papers (where
 *       classmates sit different subjects) need no special handling.</li>
 *   <li>The college (institution) and programme codes are derived from the
 *       enrollment number itself &mdash; positions 4-6 and 7-9 &mdash; exactly as
 *       {@link InternalAssessmentExtractor} does, so placement never depends on
 *       reading a page header correctly.</li>
 *   <li>Subject names, codes and internal maxima come from the Scheme of
 *       Examinations table(s) at the top; a paper absent from the scheme falls
 *       back to its paper id.</li>
 *   <li>A printed grade is trusted as-is (it already reflects any grace marks);
 *       the grade is only derived from the total when the sheet omits it.</li>
 * </ul>
 *
 * This is the single place to re-tune if the university changes the gazette layout.
 */
public class GazetteResultExtractor {

    /** Marker that routes a PDF to this parser (see {@link ipu.result.service.ImportService}). */
    public static final String MARKER = "RESULT TABULATION SHEET";

    private static final Pattern SCHEME_HEADER = Pattern.compile(
            "Scheme of Programme Code:\\s*(\\S+).*?Sem\\./Year:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESULT_HEADER = Pattern.compile(
            "Result of Programme Code:\\s*(\\S+)\\s+Programme Name:\\s*(.+?)\\s+Sem\\./Year:\\s*(\\d+)"
            + "\\s*SEMESTER\\s+Batch:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSTITUTION = Pattern.compile(
            "Institution Code:\\s*(\\d+)\\s+Institution:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    // A scheme row: "01 044101 MCA-101 DISCRETE STRUCTURES  4 THEORY ...  25  75  100  40"
    private static final Pattern SCHEME_ROW = Pattern.compile("\\d{1,3}\\s+(\\d{6})\\s+(\\S+)\\s+(.+)");
    // A student anchor: "00199904420 044101(4) 044161(1) 044171(1)  6"
    private static final Pattern ANCHOR = Pattern.compile(
            "(\\d{11})\\s+((?:\\d{6}\\(\\d+\\)\\s+)+)(\\S*)\\s*");
    private static final Pattern PAPER_CREDIT = Pattern.compile("(\\d{6})\\((\\d+)\\)");
    private static final Pattern SID_LINE = Pattern.compile("SID:\\s*(\\S+)\\s+(.*)");
    // A totals token: "73(A)" or "94(O)", with an optional trailing "*" marking grace
    // marks. Parentheses tell it apart from a bare status letter.
    private static final Pattern TOTAL_TOKEN = Pattern.compile("(\\d+)\\(([A-Za-z+]+)\\)(\\*?)");

    /** Counts of per-student consistency checks, for the import log and the dev harness. */
    public static final class Stats {
        public int students, papers, tokenMismatches, sumMismatches;
        @Override public String toString() {
            return "students=" + students + " papers=" + papers
                    + " tokenMismatches=" + tokenMismatches + " sumMismatches=" + sumMismatches;
        }
    }

    private final Stats stats = new Stats();
    public Stats stats() { return stats; }

    public ParsedResult extract(String text) {
        ParsedResult out = new ParsedResult();

        int schemeSemester = 0;           // semester of the scheme table currently being read
        boolean inScheme = false;
        String currentProgramme = null;

        Pending cur = null;               // the student whose lines we are still collecting

        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            Matcher mResHdr = RESULT_HEADER.matcher(line);
            if (mResHdr.find()) {
                cur = flush(out, cur);
                currentProgramme = mResHdr.group(1);
                out.examSession = sessionFrom(line);
                out.programmes.putIfAbsent(currentProgramme,
                        new Programme(currentProgramme, mResHdr.group(2).trim(), 0));
                inScheme = false;
            }

            Matcher mSchemeHdr = SCHEME_HEADER.matcher(line);
            if (mSchemeHdr.find()) {
                currentProgramme = mSchemeHdr.group(1);
                schemeSemester = Integer.parseInt(mSchemeHdr.group(2));
                inScheme = true;
                continue;
            }

            Matcher mInst = INSTITUTION.matcher(line);
            if (mInst.find()) {
                String name = mInst.group(2).replaceAll("\\s*CS/Remarks.*$", "").trim();
                if (!name.isEmpty()) out.colleges.put(mInst.group(1), new College(mInst.group(1), name));
            }

            // A student anchor closes the previous student and opens a new one.
            Matcher mAnchor = ANCHOR.matcher(line);
            if (mAnchor.matches()) {
                cur = flush(out, cur);
                inScheme = false;
                cur = new Pending(mAnchor.group(1), currentProgramme, out.examSession);
                for (Matcher pm = PAPER_CREDIT.matcher(mAnchor.group(2)); pm.find(); ) {
                    cur.paperIds.add(pm.group(1));
                    cur.credits.add(Integer.parseInt(pm.group(2)));
                }
                continue;
            }

            if (cur != null) {
                Matcher mSid = SID_LINE.matcher(line);
                if (mSid.matches()) { cur.ieTokens = split(mSid.group(2)); continue; }
                if (line.startsWith("SchemeID")) continue;
                if (cur.ieTokens != null && looksLikeTotalsLine(line)) {
                    cur.totalTokens = split(stripLeadingSerial(line));
                    cur = flush(out, cur);   // the totals line is the last line of a block
                    continue;
                }
                if (cur.totalTokens == null && cur.ieTokens == null) {
                    cur.appendName(line);    // name may wrap across lines
                }
            }

            // Scheme rows are read only while inside a scheme table.
            if (inScheme) {
                Matcher mRow = SCHEME_ROW.matcher(line);
                if (mRow.matches()) addSubject(out, mRow, schemeSemester, currentProgramme);
            }
        }
        flush(out, cur);
        return out;
    }

    /** The lines of one student, gathered until the totals line completes the block. */
    private static final class Pending {
        final String enroll, programme, session;
        final List<String> paperIds = new ArrayList<>();
        final List<Integer> credits = new ArrayList<>();
        final StringBuilder name = new StringBuilder();
        String[] ieTokens;      // interleaved internal/external, 2 per paper
        String[] totalTokens;   // total(grade) or bare status, 1 per paper

        Pending(String enroll, String programme, String session) {
            this.enroll = enroll; this.programme = programme; this.session = session;
        }
        void appendName(String s) {
            if (name.length() > 0) name.append(' ');
            name.append(s);
        }
    }

    /** Emits a finished student's students/marks rows, or does nothing if the block is incomplete. */
    private Pending flush(ParsedResult out, Pending cur) {
        if (cur == null || cur.paperIds.isEmpty() || cur.totalTokens == null) return null;

        int n = cur.paperIds.size();
        String collegeCode = cur.enroll.substring(3, 6);
        String programmeCode = cur.enroll.length() >= 9 ? cur.enroll.substring(6, 9) : cur.programme;
        out.colleges.putIfAbsent(collegeCode, new College(collegeCode, collegeCode));
        if (programmeCode != null) {
            out.programmes.putIfAbsent(programmeCode, new Programme(programmeCode, programmeCode, 0));
        }
        String name = cur.name.length() == 0 ? cur.enroll : cur.name.toString().trim();
        out.students.add(new Student(cur.enroll, name, collegeCode, programmeCode, batchYear(cur.session)));

        stats.students++;
        stats.papers += n;
        if (cur.totalTokens.length != n) stats.tokenMismatches++;
        boolean ieUsable = cur.ieTokens != null && cur.ieTokens.length == 2 * n;
        if (!ieUsable && cur.ieTokens != null) stats.tokenMismatches++;

        for (int i = 0; i < n && i < cur.totalTokens.length; i++) {
            String paperId = cur.paperIds.get(i);
            // Guarantee the subject exists even if it was missing from the scheme table.
            out.subjects.putIfAbsent(paperId, new Subject(
                    paperId, paperId, paperId, cur.credits.get(i), 0, programmeCode, null));

            Integer internal = null, external = null, total = null;
            String grade = null;
            boolean absent = false;

            boolean grace = false;
            Matcher mt = TOTAL_TOKEN.matcher(cur.totalTokens[i]);
            if (mt.matches()) {
                total = Integer.valueOf(mt.group(1));
                grade = mt.group(2);
                grace = !mt.group(3).isEmpty();
            } else if (isAbsent(cur.totalTokens[i])) {
                absent = true;              // graded F downstream
            } else {
                continue;                   // unknown status (AP/RL/D/C): write nothing for this paper
            }

            if (ieUsable) {
                internal = number(cur.ieTokens[2 * i]);
                external = number(cur.ieTokens[2 * i + 1]);
                // Grace marks are added to the total but not to the components, so
                // only cross-check the sum on non-grace papers.
                if (!grace && internal != null && external != null && total != null
                        && internal + external != total) {
                    stats.sumMismatches++;
                }
            }
            out.marks.add(new Mark(cur.enroll, paperId, internal, external, total, grade, absent));
        }
        return null;
    }

    private void addSubject(ParsedResult out, Matcher mRow, int semester, String programme) {
        String paperId = mRow.group(1);
        String code = mRow.group(2);
        List<String> tail = new ArrayList<>(List.of(split(mRow.group(3))));
        // The last four numeric columns are Minor, Major, Max. Marks, Pass Marks.
        Integer internalMax = null;
        if (tail.size() >= 4) {
            String minor = tail.get(tail.size() - 4);
            internalMax = minor.matches("\\d+") ? Integer.valueOf(minor) : null; // "--" => no internal
        }
        // Credits are the first bare integer after the subject name; the name holds no digits.
        int creditIdx = -1, credits = 0;
        for (int i = 0; i < tail.size(); i++) {
            if (tail.get(i).matches("\\d+")) { creditIdx = i; credits = Integer.parseInt(tail.get(i)); break; }
        }
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < (creditIdx < 0 ? tail.size() : creditIdx); i++) {
            if (name.length() > 0) name.append(' ');
            name.append(tail.get(i));
        }
        out.subjects.put(paperId, new Subject(
                paperId, code, name.length() == 0 ? paperId : name.toString(),
                credits, semester, programme, internalMax));
    }

    // ---- small helpers ----

    private static boolean looksLikeTotalsLine(String line) {
        return TOTAL_TOKEN.matcher(line).find() || line.matches("\\s*\\d+(\\s+[A-Za-z]{1,2})+\\s*");
    }

    private static String stripLeadingSerial(String line) {
        return line.replaceFirst("^\\s*\\d+\\s+", "");
    }

    private static String sessionFrom(String resultHeaderLine) {
        int i = resultHeaderLine.indexOf("Examination:");
        return i < 0 ? null : resultHeaderLine.substring(i + "Examination:".length()).trim();
    }

    private static Integer batchYear(String session) {
        if (session == null) return null;
        Matcher m = Pattern.compile("(20\\d{2})").matcher(session);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static boolean isAbsent(String s) {
        return s != null && (s.equalsIgnoreCase("A") || s.equalsIgnoreCase("AB") || s.equalsIgnoreCase("ABSENT"));
    }

    /** A marks component that is a plain number, else null (covers "-", "--", "A", "AB"). */
    private static Integer number(String s) {
        return s != null && s.matches("\\d+") ? Integer.valueOf(s) : null;
    }

    private static String[] split(String s) {
        String t = s.trim();
        return t.isEmpty() ? new String[0] : t.split("\\s+");
    }
}
