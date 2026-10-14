package ipu.result.model;

/** A degree programme, e.g. B.Tech CSE. */
public record Programme(String code, String name, int durationYears) {
    // bean getters for JSP EL (Tomcat 10.1 / Jakarta EL 5.0)
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getDurationYears() { return durationYears; }
}
