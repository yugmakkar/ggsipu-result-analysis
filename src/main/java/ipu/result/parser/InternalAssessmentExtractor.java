package ipu.result.parser;

import ipu.result.model.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a GGSIPU <b>Internal Assessment</b> result PDF (as produced by the
 * university's portal) into a {@link ParsedResult}.
 *
 * <p>One PDF holds one college + one programme and the internal marks of its
 * students across several papers. The text PDFBox yields (with
 * {@code setSortByPosition(true)}) looks like:
 *
 * <pre>
 * Guru Gobind Singh Indraprastha University, Delhi
 * Internal Assessment for Examination JUNE, 2026
 * &lt;INSTITUTE NAME&gt; (999)
 * &lt;PROGRAMME NAME&gt; (020)
 * Paper Code : BCAP312      Paper Name : DATA VISUALIZATION &amp; ANALYTICS LAB
 * BATCH SEM/ANNUAL PAPERID ENROLLMENT STUDENT NAME MAX MARKS MARKS
 * 2023 6 098312 00199902023 AARAV SHARMA 40 37     (illustrative, synthetic)
 * ...
 * Programme - BCA (020)
 * School/Institute - DIT (999)
 * </pre>
 *
 * <p>Each data row carries the 6-digit paper id and the 11-digit enrollment
 * number. The enrollment number itself encodes the college code at positions
 * 4-6 and the programme code at positions 7-9, so student placement never
 * depends on reading the (repeated) page headers correctly. The parser is the
 * single place to re-tune if the university changes this layout.
 */
public class InternalAssessmentExtractor {

    /** Marker that routes a PDF to this parser (see {@link ImportService}). */
    public static final String MARKER = "Internal Assessment for Examination";

    private static final Pattern SESSION =
            Pattern.compile("Internal Assessment for Examination\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAPER =
            Pattern.compile("Paper Code\\s*:\\s*(\\S+)\\s+Paper Name\\s*:\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROG_FOOTER =
            Pattern.compile("Programme\\s*-\\s*(.+?)\\s*\\((\\d{3})\\)\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_CODE =
            Pattern.compile("(.+?)\\s*\\((\\d{3})\\)\\s*");
    // BATCH SEM PAPERID(6) ENROLLMENT(11) <tail = name ... MAX MARKS>
    private static final Pattern ROW_PREFIX =
            Pattern.compile("(\\d{4})\\s+(\\d+)\\s+(\\d{6})\\s+(\\d{11})\\s+(.*)");

    public ParsedResult extract(String text) {
        ParsedResult out = new ParsedResult();

        String paperCode = null, paperName = null;
        boolean afterSession = false;   // the two header "(code)" lines follow the session line
        int headerCodeLines = 0;

        for (String rawLine : text.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            Matcher mSession = SESSION.matcher(line);
            if (mSession.matches()) {
                out.examSession = mSession.group(1).trim();
                afterSession = true;
                headerCodeLines = 0;
                continue;
            }

            Matcher mPaper = PAPER.matcher(line);
            if (mPaper.matches()) {
                paperCode = mPaper.group(1).trim();
                paperName = mPaper.group(2).trim();
                afterSession = false;
                continue;
            }

            Matcher mProgFoot = PROG_FOOTER.matcher(line);
            if (mProgFoot.matches()) {
                // Footer carries the short programme name the user wants on the home page (e.g. "BCA").
                String code = mProgFoot.group(2);
                out.programmes.put(code, new Programme(code, mProgFoot.group(1).trim(), 0));
                continue;
            }

            Matcher mRow = ROW_PREFIX.matcher(line);
            if (mRow.matches()) {
                afterSession = false;
                handleRow(out, mRow, paperCode, paperName);
                continue;
            }

            // The two lines right after the session line are the college then the
            // programme header ("<FULL NAME> (code)"). Use them only for display names.
            if (afterSession) {
                Matcher mNc = NAME_CODE.matcher(line);
                if (mNc.matches()) {
                    String name = mNc.group(1).trim();
                    String code = mNc.group(2);
                    if (headerCodeLines == 0) {
                        out.colleges.put(code, new College(code, name)); // full institute name
                    } else {
                        out.programmes.putIfAbsent(code, new Programme(code, name, 0));
                        afterSession = false;
                    }
                    headerCodeLines++;
                }
            }
        }
        return out;
    }

    private void handleRow(ParsedResult out, Matcher mRow, String paperCode, String paperName) {
        int batch = Integer.parseInt(mRow.group(1));
        int semester = Integer.parseInt(mRow.group(2));
        String paperId = mRow.group(3);
        String enroll = mRow.group(4);
        String tail = mRow.group(5).trim();

        String[] tok = tail.split("\\s+");
        if (tok.length < 2) return; // need at least MAX and MARKS
        String marksTok = tok[tok.length - 1];
        String maxTok = tok[tok.length - 2];
        if (!maxTok.matches("\\d{1,3}")) return; // malformed row, skip quietly
        int internalMax = Integer.parseInt(maxTok);

        boolean absent = isAbsent(marksTok);
        Integer marks = absent ? null : (marksTok.matches("\\d{1,3}") ? Integer.valueOf(marksTok) : null);
        if (!absent && marks == null) return; // trailing token is neither a number nor an absence marker

        StringBuilder nm = new StringBuilder();
        for (int i = 0; i < tok.length - 2; i++) {
            if (nm.length() > 0) nm.append(' ');
            nm.append(tok[i]);
        }
        String name = nm.toString().trim();
        if (name.isEmpty()) name = enroll; // keep NOT NULL satisfied if a sheet omits the name

        String collegeCode = enroll.substring(3, 6);
        String programmeCode = enroll.substring(6, 9);

        // guarantee referential targets exist even if a header/footer line is missing
        out.colleges.putIfAbsent(collegeCode, new College(collegeCode, collegeCode));
        out.programmes.putIfAbsent(programmeCode, new Programme(programmeCode, programmeCode, 0));

        out.subjects.putIfAbsent(paperId, new Subject(
                paperId, paperCode, paperName == null ? paperId : paperName,
                0, semester, programmeCode, internalMax));

        out.students.add(new Student(enroll, name, collegeCode, programmeCode, batch));
        out.marks.add(new Mark(enroll, paperId, marks, null, null, null, absent));
    }

    private static boolean isAbsent(String s) {
        return s != null && (s.equalsIgnoreCase("A") || s.equalsIgnoreCase("AB") || s.equalsIgnoreCase("ABSENT"));
    }
}
