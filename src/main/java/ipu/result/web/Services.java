package ipu.result.web;

import ipu.result.config.AppConfig;
import ipu.result.dao.AdminDAO;
import ipu.result.dao.Db;
import ipu.result.service.ImportService;
import ipu.result.service.TopperService;

import jakarta.servlet.ServletContext;

/** Application services shared across servlets via the ServletContext. */
public class Services {
    public static final String KEY = "ipu.services";

    public final AppConfig config;
    public final Db db;
    public final ImportService importService;
    public final TopperService toppers;
    public final AdminDAO adminDAO;

    public Services(AppConfig config, Db db) {
        this.config = config;
        this.db = db;
        this.importService = new ImportService(db);
        this.toppers = new TopperService(db);
        this.adminDAO = new AdminDAO(db);
    }

    public static Services from(ServletContext ctx) {
        return (Services) ctx.getAttribute(KEY);
    }
}
