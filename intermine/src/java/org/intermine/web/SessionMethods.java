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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.MessageResources;
import org.apache.struts.Globals;

import org.apache.log4j.Logger;

import org.intermine.objectstore.ObjectStore;
import org.intermine.objectstore.ObjectStoreException;
import org.intermine.objectstore.ObjectStoreQueryDurationException;
import org.intermine.objectstore.query.Query;

import org.intermine.objectstore.query.Results;
import org.intermine.web.results.PagedResults;
import org.intermine.web.results.TableHelper;

/**
 * Business logic that interacts with session data. These methods are generally
 * called by actions to perform business logic. Obviously, a lot of the business
 * logic happens in other parts of intermine, called from here, then the users
 * session is updated accordingly.
 *
 * @author  Thomas Riley
 */
public class SessionMethods
{
    protected static final Logger LOG = Logger.getLogger(SessionMethods.class);
    
    /**
     * Base class for query thread runnable.
     */
    private abstract static class RunQueryThread implements Runnable
    {
        protected Results r;
        protected PagedResults pr;
        protected boolean done = false;
        protected boolean error = false;
        
        protected Results getResults() {
            return r;
        }
        protected PagedResults getPagedResults() {
            return pr;
        }
        protected boolean isDone() {
            return done;
        }
        protected boolean isError() {
            return error;
        }
    };
    
    /**
     * Executes current query and sets session attributes QUERY_RESULTS and RESULTS_TABLE. If the
     * query fails for some reason, this method returns false and ActionErrors are set on the
     * request. If the parameter <code>saveQuery</code> is true then the query is
     * automatically saved in the user's query history and a message is added to the
     * request. The <code>monitor</code> parameter is an optional callback interface that will be
     * notified repeatedly (every 100 milliseconds) while the query executes. 
     *
     * @param action    the current action
     * @param session   the http session
     * @param request   the current http request
     * @param saveQuery if true, query will be saved automatically
     * @param monitor   object that will receive a periodic call while the query runs
     * @return  true if query ran successfully, false if an error occured
     * @throws  Exception if getting results info from paged results fails
     */
    public static boolean runQuery(final InterMineAction action,
                                   final HttpSession session,
                                   final HttpServletRequest request,
                                   final boolean saveQuery,
                                   final RunQueryMonitor monitor)
        throws Exception {
        final ServletContext servletContext = session.getServletContext();
        final Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        final PathQuery query = (PathQuery) session.getAttribute(Constants.QUERY);
        final ObjectStore os = (ObjectStore) servletContext.getAttribute(Constants.OBJECTSTORE);
        final MessageResources resources =
            (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);

        RunQueryThread runnable = new RunQueryThread() {
            public void run () {
                try {
                    Query q = MainHelper.makeQuery(query, profile.getSavedBags());
                    r = TableHelper.makeResults(os, q);
                    TableHelper.initResults(r);
                    pr = new PagedResults(query.getView(), r, os.getModel());
                } catch (ObjectStoreException e) {
                    String key = (e instanceof ObjectStoreQueryDurationException)
                        ? "errors.query.estimatetimetoolong"
                        : "errors.query.objectstoreerror";
                    action.recordError(new ActionMessage(key), request);
                    
                    recordError(resources.getMessage(key), session);

                    // put stack trace in the log
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    LOG.error(sw.toString());
                    
                    error = true;
                } catch (Throwable err) {
                    StringWriter sw = new StringWriter();
                    err.printStackTrace(new PrintWriter(sw));
                    recordError(sw.toString(), session);
                    LOG.error(sw.toString());
                    error = true;
                }
                
                // debug pause
                /*try {
                    Thread.currentThread().sleep(5000);
                } catch (Exception _) {
                    
                }*/
            }
        };
        Thread thread = null;
        thread = new Thread(runnable);
        thread.start();
        
        // Wait for Results object to become available
        while (thread.isAlive() && runnable.getResults() == null) {
            Thread.sleep(25);
        }
        
        Results r = runnable.getResults();
        
        while (thread.isAlive()) {
            Thread.sleep(100);
            if (monitor != null) {
                monitor.queryProgress(r);
            }
        }

        if (runnable.isError()) {
            return false;
        }
        
        PagedResults pr = runnable.getPagedResults();
        session.setAttribute(Constants.QUERY_RESULTS, pr);
        session.setAttribute(Constants.RESULTS_TABLE, pr);

        if (saveQuery) {
            String queryName = SaveQueryHelper.findNewQueryName(profile.getSavedQueries());
            query.setInfo(pr.getResultsInfo());
            saveQuery(request, queryName, query);
            action.recordMessage(new ActionMessage("saveQuery.message", queryName), request);
        }
        
        return true;
    }
    
    /**
     * Load a query into the session, cloning to avoid modifying the original
     * @param query the query
     * @param session the session
     */
    public static void loadQuery(PathQuery query, HttpSession session) {
        session.setAttribute(Constants.QUERY, query.clone());
        //at the moment we can only load queries that have saved using the webapp
        //this is because the webapp satisfies our (dumb) assumption that the view list is not empty
        String path = (String) query.getView().iterator().next();
        if (path.indexOf(".") != -1) {
            path = path.substring(0, path.indexOf("."));
        }
        session.setAttribute("path", path);
        session.removeAttribute("prefix");
    }
    
    /**
     * Save a query in the Map on the session, and clone it to allow further editing
     * @param request The HTTP request we are processing
     * @param queryName the name to save the query under
     * @param query the PathQuery
     */
    public static void saveQuery(HttpServletRequest request,
                                 String queryName,
                                 PathQuery query) {
        HttpSession session = request.getSession();
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);

        profile.saveQuery(queryName, query);
        session.setAttribute(Constants.QUERY, query.clone());
    }
    
    /**
     * Record a message that will be stored in the session until it is displayed to
     * the user. This allows actions that result in a redirect to display
     * messages to the user after the redirect. Messages are stored in a Collection
     * session attribute so you may call this method multiple times to display
     * multiple messages.<p>
     *
     * <code>recordMessage</code> and <code>recordError</code> are used by
     * <code>InterMineRequestProcessor.processForwardConfig</code> to store errors
     * and messages in the session when a redirecting forward is about to occur.
     *
     * @param session The Session object in which to store the message
     * @param message The message to store
     * @see InterMineRequestProcessor#processForwardConfig
     */
    public static void recordMessage(String message, HttpSession session) {
        recordMessage(message, Constants.MESSAGES, session);
    }
    
    /**
     * @see SessionMethods#recordMessage
     */
    public static void recordError(String error, HttpSession session) {
        recordMessage(error, Constants.ERRORS, session);
    }
    
    /**
     * Record a message that will be stored in the session until it is displayed to
     * the user. This allows actions that result in a redirect to display
     * message to the user after the redirect. Messages are stored in a Set
     * session attribute so you may call this method multiple times to display
     * multiple errors. Identical errors will be ignored.<p>
     *
     * The <code>attrib</code> parameter specifies the name of the session attribute
     * used to store the set of messages.
     *
     * @param session The Session object in which to store the message
     * @param attrib The name of the session attribute in which to store message
     * @param message The message to store
     */
    private static void recordMessage(String message, String attrib, HttpSession session) {
        Set set = (Set) session.getAttribute(attrib);
        if (set == null) {
            set = Collections.synchronizedSet(new HashSet());
            session.setAttribute(attrib, set);
        }
        set.add(message);
    }
    
    /**
     * Log use of a template query.
     *
     * @param session The session of the user running the template query
     * @param templateType The type of template 'global' or 'user'
     * @param templateName The name of the template
     */
    public static void logTemplateQueryUse(HttpSession session, String templateType,
            String templateName) {
        Logger log = Logger.getLogger(TemplateAction.class);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String username = profile.getUsername();
        
        if (username == null) {
            username = "anonymous";
        }
        
        log.info(username + "\t" + templateType + "\t" + templateName);
    }
    
    /**
     * Log load of an example query. If user is not logged in then lo
     *
     * @param session The session of the user loading the example query
     * @param exampleName The name of the example query loaded
     */
    public static void logExampleQueryUse(HttpSession session, String exampleName) {
        Logger log = Logger.getLogger(LoadQueryAction.class);
        Profile profile = (Profile) session.getAttribute(Constants.PROFILE);
        String username = profile.getUsername();
        
        if (username == null) {
            username = "anonymous";
        }
        
        log.info(username + "\t" + exampleName);
    }
    
    /**
     * Get the 
     *
    public static ObjectDetailsTrail getObjectDetailsTrail(HttpSession session) {
        ObjectDetailsTrail trail
                = (ObjectDetailsTrail) session.getAttribute(Constants.OBJECT_DETAILS_TRAIL);
        if (trail == null) {
            trail = new ObjectDetailsTrail();
            session.setAttribute(Constants.OBJECT_DETAILS_TRAIL, trail);
        }
        return trail;
    }*/
    
    /**
     * Get the COLLAPSED map from the session. If the attribute is not present then a new
     * map will be created.
     *
     * @param session the curren session
     * @return the COLLAPSED map attribute
     */
    public static Map getCollapsedMap(HttpSession session) {
        Map collapsed = (Map) session.getAttribute(Constants.COLLAPSED);
        if (collapsed == null) {
            collapsed = new HashMap();
            session.setAttribute(Constants.COLLAPSED, collapsed);
        }
        return collapsed;
    }
    
    /**
     * Initialise a new session. Adds a profile to the session.
     * 
     * @param session the new session to initialise
     */
    public static void initSession(HttpSession session) {
        ServletContext servletContext = session.getServletContext();
        ProfileManager pm = (ProfileManager) servletContext.getAttribute(Constants.PROFILE_MANAGER);
        session.setAttribute(Constants.PROFILE,
                             new Profile(pm, null, new HashMap(), new HashMap(), new HashMap()));
        session.setAttribute(Constants.COLLAPSED, new HashMap());
    }
}
