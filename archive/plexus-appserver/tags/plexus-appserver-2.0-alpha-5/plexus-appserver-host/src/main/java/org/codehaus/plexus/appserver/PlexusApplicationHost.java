package org.codehaus.plexus.appserver;

/*
 * Copyright (c) 2004, Codehaus.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.codehaus.classworlds.ClassWorld;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;

import java.io.File;
import java.io.FileReader;

// Container host plexus container is configured and initialized
// The appserver component is looked up

/**
 * A <code>ContainerHost</code>.
 *
 * @author Jason van Zyl
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class PlexusApplicationHost
    implements Runnable
{
    private DefaultPlexusContainer container;

    private boolean shouldStop;

    private boolean isStopped;

    private static final Object waitObj = new Object();

    private ApplicationServer applicationServer;

    private ClassWorld classWorld;

    private String configurationResource;

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public PlexusContainer getContainer()
    {
        return container;
    }

    public ApplicationServer getApplicationServer()
    {
        return applicationServer;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------


    public void start( ClassWorld classWorld )
        throws Exception
    {
        start( classWorld, null );
    }

    public void start( ClassWorld classWorld, String configurationResource )
        throws Exception
    {
        this.classWorld = classWorld;

        container = new DefaultPlexusContainer();

        container.setClassWorld( classWorld );

        String plexusHome = System.getProperty( "plexus.home" );
        plexusHome = new File( plexusHome ).getAbsolutePath();

        container.addContextValue( "plexus.home", plexusHome );

        File appserverHome = new File( System.getProperty( "appserver.home", plexusHome ) );
        container.addContextValue( "appserver.home", appserverHome.getAbsolutePath() );

        File appserverBase = new File( System.getProperty( "appserver.base", appserverHome.getAbsolutePath() ) );
        container.addContextValue( "appserver.base", appserverBase.getAbsolutePath() );

        container.addContextValue( "plexus.work",
                                   new File( appserverBase, PlexusRuntimeConstants.WORK_DIRECTORY ).getAbsolutePath() );

        File plexusTemp = new File( appserverBase, PlexusRuntimeConstants.TEMP_DIRECTORY );
        container.addContextValue( "plexus.temp", plexusTemp.getAbsolutePath() );

        File plexusLogs = new File( appserverBase, PlexusRuntimeConstants.LOGS_DIRECTORY );
        container.addContextValue( "plexus.logs", plexusLogs.getAbsolutePath() );

        if ( !plexusLogs.exists() )
        {
            plexusLogs.mkdirs();
        }

        if ( !plexusTemp.exists() )
        {
            plexusTemp.mkdirs();
        }

        if ( configurationResource == null )
        {
            File conf = new File( new File( appserverBase, PlexusRuntimeConstants.CONF_DIRECTORY ),
                                  PlexusRuntimeConstants.CONFIGURATION_FILE );
            if ( !conf.exists() )
            {
                conf = new File( new File( appserverHome, PlexusRuntimeConstants.CONF_DIRECTORY ),
                                 PlexusRuntimeConstants.CONFIGURATION_FILE );

                if ( !conf.exists() )
                {
                    throw new Exception( "Unable to find a default configuration file" );
                }
            }
            configurationResource = conf.getAbsolutePath();
        }

        this.configurationResource = configurationResource;

        container.setConfigurationResource( new FileReader( configurationResource ) );

        container.initialize();

        container.start();

        LoggerManager loggerManager = (LoggerManager) container.lookup( LoggerManager.ROLE );

        loggerManager.setThreshold( Logger.LEVEL_DEBUG );

        final Logger logger = loggerManager.getLoggerForComponent( this.getClass().getName() );

        // ----------------------------------------------------------------------
        // This lookup will start the appserver server
        // ----------------------------------------------------------------------

        applicationServer = (ApplicationServer) container.lookup( ApplicationServer.ROLE );

        // ----------------------------------------------------------------------
        //
        // ----------------------------------------------------------------------

        // TODO: Add timing.
        logger.info( "The appserver server has started." );

        Thread thread = new Thread( this );

        thread.setDaemon( false );

        Runtime.getRuntime().addShutdownHook( new Thread( new Runnable()
        {
            public void run()
            {
                try
                {
                    logger.info( "Shutting down the appserver container." );

                    shutdown();
                }
                catch ( Exception e )
                {
                    // do nothing.
                }
            }
        } ) );

        thread.start();
    }

    /**
     * Asynchronous hosting component loop.
     */
    public void run()
    {
        synchronized ( this )
        {
            while ( !shouldStop )
            {
                try
                {
                    wait();
                }
                catch ( InterruptedException e )
                {
                    //ignore
                }
            }
        }

        synchronized ( this )
        {
            isStopped = true;

            notifyAll();
        }
    }

    public void restart()
        throws Exception
    {
        shutdown();

        start( classWorld, configurationResource );
    }

    // ----------------------------------------------------------------------
    //  Startup
    // ----------------------------------------------------------------------

    /**
     * Shutdown this container.
     *
     * @throws java.lang.Exception If an error occurs while shutting down the container.
     */
    public void shutdown()
        throws Exception
    {
        synchronized ( this )
        {
            shouldStop = true;

            container.release( applicationServer );

            container.dispose();

            notifyAll();
        }

        synchronized ( this )
        {
            while ( !isStopped )
            {
                try
                {
                    wait();
                }
                catch ( InterruptedException e )
                {
                    //ignore
                }
            }

            synchronized ( waitObj )
            {
                waitObj.notifyAll();
            }
        }
    }

    private boolean isStopped()
    {
        return isStopped;
    }

    // ----------------------------------------------------------------------
    // Main: used by org.codehaus.classworlds.Launcher
    // ----------------------------------------------------------------------

    public static void main( String[] args, ClassWorld classWorld )
    {
        try
        {
            PlexusApplicationHost host = new PlexusApplicationHost();

            if ( args.length > 0 )
            {
                host.start( classWorld, args[0] );
            }
            else
            {
                host.start( classWorld );
            }

            while ( !host.isStopped() )
            {
                try
                {
                    synchronized ( waitObj )
                    {
                        waitObj.wait();
                    }
                }
                catch ( InterruptedException e )
                {
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();

            System.exit( 2 );
        }
    }

    public String getConfigurationResource()
    {
        return configurationResource;
    }
}
