/**
 *
 * Copyright 2006
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.codehaus.plexus.swizzle;

import java.io.PrintStream;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.swizzle.jirareport.Main;
import org.apache.velocity.VelocityContext;

/**
 * Concrete implementation of a <tt>JiraReport</tt> component.  The
 * component is configured via the Plexus container.
 *
 * @author jtolentino
 * @version $$Id: DefaultJiraReport.java 3353 2006-05-31 14:17:11Z jtolentino $$
 */
public class DefaultJiraReport
    extends AbstractLogEnabled
    implements JiraReport, Startable
{
    /**
     * Jira project Key
     */
    private String projectKey;

    /**
     * Version of the project where the generate report will be based
     */
    private String projectVersion;

    /**
     * URL of the jira server. e.g. http://jira.codehaus.org
     */
    private String jiraServerUrl;

    /**
     * Velocity template to use to generate the reports
     */
    private String template;

    /**
     * Velocity context to use
     */
    private VelocityContext context;

    private static final String RESOLVED_ISSUES_TEMPLATE = "org/codehaus/plexus/swizzle/ResolvedIssues.vm";

    private static final String VOTES_REPORT_TEMPLATE = "org/codehaus/plexus/swizzle/Votes.vm";

    // ----------------------------------------------------------------------
    // Component Lifecycle
    // ----------------------------------------------------------------------

    public void start()
    {
        getLogger().info( "Starting DefaultJiraReport component." );
    }

    public void stop()
    {
        getLogger().info( "Stopping DefaultJiraReport component." );
    }

    // ----------------------------------------------------------------------
    // JiraReport Implementation
    // ----------------------------------------------------------------------

    /**
     * Generates a report on all resolved issues. Writes the result of a jira report to a PrintStream in xdoc format.
     *
     * @throws Exception
     */
    public void generateResolvedIssuesReport( PrintStream result )
        throws Exception
    {
        try
        {
            context.put( "projectKey", projectKey );
            context.put( "projectVersion", projectVersion );
            context.put( "jiraServerUrl", jiraServerUrl );

            Main.generate( context, RESOLVED_ISSUES_TEMPLATE, result );
        }
        catch ( Exception e )
        {
            getLogger().error( "Error encountered while generating Resolved Issues Report: " + e.getMessage() );
        }
    }

    /**
     * Generates a report on all resolved issues. Writes the result of a jira report to a PrintStream in xdoc format.
     *
     * @throws Exception
     */
    public void generateVotesReport( PrintStream result )
        throws Exception
    {
        try
        {
            context.put( "projectKey", projectKey );
            context.put( "projectVersion", projectVersion );
            context.put( "jiraServerUrl", jiraServerUrl );

            Main.generate( context, VOTES_REPORT_TEMPLATE, result );
        }
        catch ( Exception e )
        {
            getLogger().error( "Error encountered while generating Votes Report: " + e.getMessage() );
        }
    }

}
