package com.nuodb.storefront.servlet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.tool.hbm2ddl.SchemaExport;

import com.nuodb.storefront.StorefrontFactory;
import com.nuodb.storefront.model.Customer;
import com.nuodb.storefront.model.MessageSeverity;
import com.nuodb.storefront.model.Workload;
import com.nuodb.storefront.service.ISimulatorService;

public class WelcomeServlet extends BaseServlet {
    private static final long serialVersionUID = 4369262156023258885L;
    private static volatile String s_ddl;

    /**
     * GET: Shows the welcome screen, including the list of simulated workloads.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> pageData = new HashMap<String, Object>();
        pageData.put("workloads", getSimulator().getWorkloadStats().values());
        pageData.put("ddl", getDdl());
        showPage(req, resp, null, "welcome", pageData, new Customer());
    }

    /**
     * POST: Adjusts the number of users in each simulated workload
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            ISimulatorService simulator = getSimulator();
            int updatedWorkloadCount = 0;
            for (Map.Entry<String, String[]> param : req.getParameterMap().entrySet()) {
                if (param.getKey().startsWith("workload-")) {
                    String workloadName = param.getKey().substring(9);
                    int quantity = Integer.parseInt(param.getValue()[0]);
                    Workload workload = simulator.getWorkload(workloadName);
                    if (workload != null) {
                        simulator.adjustWorkers(workload, quantity, quantity);
                        updatedWorkloadCount++;
                    }
                }
            }
            if (updatedWorkloadCount > 0) {
                addMessage(req, MessageSeverity.INFO, "Workloads updated successfully.");
            }
        } catch (Exception e) {
            addErrorMessage(req, e);
        }

        doGet(req, resp);
    }

    private static synchronized String getDdl() {
        if (s_ddl == null) {
            StringBuilder buff = new StringBuilder();
            SchemaExport export = StorefrontFactory.createSchemaExport();
            appendDdlScript(export, "createSQL", buff);
            appendDdlScript(export, "dropSQL", buff);
            s_ddl = buff.toString();
        }
        return s_ddl;
    }

    private static void appendDdlScript(SchemaExport export, String fieldName, StringBuilder buffer) {
        try {
            Field createSqlField = export.getClass().getDeclaredField(fieldName);
            createSqlField.setAccessible(true);
            for (String stmt : (String[]) createSqlField.get(export)) {
                buffer.append(stmt);
                buffer.append("\n");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
