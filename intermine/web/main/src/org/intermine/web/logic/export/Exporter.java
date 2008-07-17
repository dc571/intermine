package org.intermine.web.logic.export;

/*
 * Copyright (C) 2002-2008 FlyMine
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  See the LICENSE file for more
 * information or http://www.gnu.org/copyleft/lesser.html.
 *
 */
import java.util.List;

import org.intermine.web.logic.results.Column;
import org.intermine.web.logic.results.ResultElement;


/**
 * Simple exporter interface. Objects implementing this interface are
 * able to make export.
 * @author Jakub Kulaviak
 **/
public interface Exporter
{
    /** Windows line separator  CR+LF **/
    public static final String WINDOWS_SEPARATOR = "\r\n";

    /** Windows line separator  only LF **/
    public static final String UNIX_SEPARATOR = "\n";

    /**
     * Do export.
     * @param results to be exported
     * @param columns the columns descriptions
     */
    public void export(List<List<ResultElement>> results, List<Column> columns);

    /**
     * This method finds out if result row composed from instances of these
     * classes can be exported with actual implementation of exporter.
     * @param clazzes classes in row
     * @return true if result row can be exported or false
     */
    public boolean canExport(List<Class> clazzes);

    /**
     * @return count of written results
     */
    public int getWrittenResultsCount();
}
