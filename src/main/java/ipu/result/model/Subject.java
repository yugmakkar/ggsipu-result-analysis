package ipu.result.model;

/** A paper in a programme's scheme, with its credit weight and semester. */
public record Subject(String paperId, String paperCode, String name,
                      int credits, int semester, String programmeCode) {}
