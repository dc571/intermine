package org.intermine.web;

/*
 * Copyright (C) 2002-2005 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */

import java.util.Map;
import java.util.Collection;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.intermine.objectstore.ObjectStore;
import org.intermine.web.bag.BagHelper;
import org.intermine.web.bag.InterMineBag;

/**
 * Implementation of <strong>Action</strong> to modify bags
 * @author Mark Woodbridge
 */
public class ModifyBagAction extends InterMineAction
{
    /**
     * Forward to the correct method based on the button pressed
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        if (request.getParameter("union") != null) {
            union(mapping, form, request, response);
        } else if (request.getParameter("intersect") != null) {
            intersect(mapping, form, request, response);
        } else if (request.getParameter("delete") != null) {
            delete(mapping, form, request, response);
        }

        return mapping.findForward("history");
    }

    /**
     * Union the selected bags
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward union(ActionMapping mapping,
                               ActionForm form,
                               HttpServletRequest request,
                               HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        
        Map savedBags = profile.getSavedBags();
        String[] selectedBags = mbf.getSelectedBags();
        
        if (!typesMatch(savedBags, selectedBags)) {
            recordError(new ActionMessage("bag.typesDontMatch"), request);
            return mapping.findForward("history");
        }
        
        // Now combine
        Class type = savedBags.get(selectedBags[0]).getClass();
        InterMineBag combined = (InterMineBag) type.newInstance();
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            combined.addAll((Collection) savedBags.get(selectedBags[i]));
        }

        int defaultMax = 10000;

        int maxBagSize = WebUtil.getIntSessionProperty(session, "max.bag.size", defaultMax);

        if (combined.size () > maxBagSize) {
            ActionMessage actionMessage =
                new ActionMessage("bag.tooBig", new Integer(maxBagSize));
            recordError(actionMessage, request);

            return mapping.findForward("history");
        }

        profile.saveBag(BagHelper.findNewBagName(savedBags), combined);

        return mapping.findForward("history");
    }
    
    /**
     * Given a set of bag names, find out whether they are all of the same type.
     * 
     * @param bags map from bag name to InterMineBag subclass
     * @param selectedBags names of bags to match
     * @return true if all named bags are of the same type, false if not
     */
    private static boolean typesMatch(Map bags, String selectedBags[]) {
        // Check that all selected bags are of the same type
        Class type = bags.get(selectedBags[0]).getClass();
        for (int i = 1; i < selectedBags.length; i++) {
            if (bags.get(selectedBags[i]).getClass() != type) {
                return false;
            }
        }
        return true;
    }

    /**
     * Intersect the selected bags
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward intersect(ActionMapping mapping,
                                   ActionForm form,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        ServletContext servletContext = session.getServletContext();
        ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        ModifyBagForm mbf = (ModifyBagForm) form;

        Map savedBags = profile.getSavedBags();
        String[] selectedBags = mbf.getSelectedBags();
        
        if (!typesMatch(savedBags, selectedBags)) {
            recordError(new ActionMessage("bag.typesDontMatch"), request);
            return mapping.findForward("history");
        }
        
        Class type = savedBags.get(selectedBags[0]).getClass();
        InterMineBag combined = (InterMineBag) type.newInstance();
        combined.addAll((Collection) savedBags.get(selectedBags[0]));
        for (int i = 1; i < selectedBags.length; i++) {
            combined.retainAll((Collection) savedBags.get(selectedBags[i]));
        }
        profile.saveBag(BagHelper.findNewBagName(savedBags), combined);

        return mapping.findForward("history");
    }

    /**
     * Delete the selected bags
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param request The HTTP request we are processing
     * @param response The HTTP response we are creating
     * @return an ActionForward object defining where control goes next
     * @exception Exception if the application business logic throws
     *  an exception
     */
    public ActionForward delete(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response)
        throws Exception {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        ModifyBagForm mbf = (ModifyBagForm) form;
        for (int i = 0; i < mbf.getSelectedBags().length; i++) {
            profile.deleteBag(mbf.getSelectedBags()[i]);
        }

        return mapping.findForward("history");
    }
}
