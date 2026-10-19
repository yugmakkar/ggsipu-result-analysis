package ipu.result.model;

/**
 * A paper in a programme's scheme, with its credit weight and semester.
 * {@code internalMax} is the maximum internal-assessment mark (e.g. 40 for a
 * lab, 100 for a project); it is null for the end-semester (Sem-2) path.
 */
public record Subject(String paperId, String paperCode, String name,
                      int credits, int semester, String programmeCode, Integer internalMax) {

    /** End-semester path: no internal maximum recorded. */
    public Subject(String paperId, String paperCode, String name,
                   int credits, int semester, String programmeCode) {
        this(paperId, paperCode, name, credits, semester, programmeCode, null);
    }
}
