package ipu.result.parser;

import ipu.result.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything one result PDF yields, ready to be persisted. */
public class ParsedResult {
    public String examSession;
    public final Map<String, College>   colleges   = new LinkedHashMap<>();
    public final Map<String, Programme> programmes = new LinkedHashMap<>();
    public final Map<String, Subject>   subjects   = new LinkedHashMap<>(); // by paperId
    public final List<Student> students = new ArrayList<>();
    public final List<Mark>    marks    = new ArrayList<>();

    @Override
    public String toString() {
        return "ParsedResult{session=" + examSession
                + ", colleges=" + colleges.size()
                + ", programmes=" + programmes.size()
                + ", subjects=" + subjects.size()
                + ", students=" + students.size()
                + ", marks=" + marks.size() + "}";
    }
}
