package org.codehaus.plexus.util;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;
import java.io.StringReader;
import java.io.StringWriter;

import org.codehaus.plexus.context.DefaultContext;

/**
 * Generated by JUnitDoclet, a tool provided by ObjectFab GmbH under LGPL.
 * Please see www.junitdoclet.org, www.gnu.org and www.objectfab.de for
 * informations about the tool, the licence and the authors.
 */
public class ContextMapAdapterTest
    extends TestCase
{
    public ContextMapAdapterTest( String name )
    {
        super( name );
    }

    /**
     * The JUnit setup method
     */
    protected void setUp()
        throws Exception
    {
    }

    /**
     * The teardown method for JUnit
     */
    protected void tearDown()
        throws Exception
    {
    }

    public void testInterpolation()
        throws Exception
    {
        DefaultContext context = new DefaultContext();
        context.put( "name", "jason" );
        context.put( "occupation", "exotic dancer" );

        ContextMapAdapter adapter = new ContextMapAdapter( context );

        assertEquals( "jason", (String) adapter.get( "name" ) );
        assertEquals( "exotic dancer", (String) adapter.get( "occupation" ) );
    }
}