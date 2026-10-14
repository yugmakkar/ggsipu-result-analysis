package ipu.result.model;

/** A GGSIPU affiliated institution. */
public record College(String code, String name) {
    // bean getters for JSP EL (Tomcat 10.1 / Jakarta EL 5.0)
    public String getCode() { return code; }
    public String getName() { return name; }
}
