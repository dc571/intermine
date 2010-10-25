package org.intermine.web.struts;

/*
 * Copyright (C) 2002-2010 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.intermine.api.InterMineAPI;
import org.intermine.api.tracker.TemplateTrack;
import org.intermine.api.tracker.TrackerManager;
import org.intermine.web.logic.session.SessionMethods;

/**
 * Display the query builder (if there is a curernt query) or redirect to project.sitePrefix.
 *
 * @author Tom Riley
 */
public class BeginAction extends InterMineAction
{
    /**
     * Either display the query builder or redirect to project.sitePrefix.
     *
     * @param mapping
     *            The ActionMapping used to select this instance
     * @param form
     *            The optional ActionForm bean for this request (if any)
     * @param request
     *            The HTTP request we are processing
     * @param response
     *            The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception
     *                if the application business logic throws an exception
     */
    public ActionForward execute(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
        throws Exception {

        HttpSession session = request.getSession();
        final InterMineAPI im = SessionMethods.getInterMineAPI(session);

        Properties properties = SessionMethods.getWebProperties(session.getServletContext());

        // If GALAXY_URL is sent from a Galaxy server, then save it in the session; if not, read
        // the default value from web.properties and save it in the session
        if (request.getParameter("GALAXY_URL") != null) {
            request.getSession().setAttribute("GALAXY_URL",
                    request.getParameter("GALAXY_URL"));
            String msg = properties.getProperty("galaxy.welcomeMessage");
            SessionMethods.recordMessage(msg, session);
        } else {
            request.getSession().setAttribute(
                    "GALAXY_URL",
                    properties.getProperty("galaxy.baseurl.default")
                            + properties.getProperty("galaxy.url.value"));
        }

        /* count number of templates and bags */
        request.setAttribute("bagCount", new Integer(im.getBagManager()
                .getGlobalBags().size()));
        request.setAttribute("templateCount", new Integer(im
                .getTemplateManager().getGlobalTemplates().size()));

        /*most popular template*/
        TrackerManager trackerManager = im.getTrackerManager();
        if (trackerManager != null) {
            TemplateTrack tt = trackerManager.getMostPopularTemplate();
            if (tt != null) {
                request.setAttribute("mostPopularTemplate", tt.getTemplateName());
            }
        }

        String[] beginQueryClasses = (properties.get("begin.query.classes").toString())
            .split("[ ,]+");
        request.setAttribute("beginQueryClasses", beginQueryClasses);
        return mapping.findForward("begin");
    }
}
