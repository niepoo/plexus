package org.codehaus.classworlds;

/*
 $Id$

 Copyright 2002 (C) The Werken Company. All Rights Reserved.
 
 Redistribution and use of this software and associated documentation
 ("Software"), with or without modification, are permitted provided
 that the following conditions are met:

 1. Redistributions of source code must retain copyright
    statements and notices.  Redistributions must also contain a
    copy of this document.
 
 2. Redistributions in binary form must reproduce the
    above copyright notice, this list of conditions and the
    following disclaimer in the documentation and/or other
    materials provided with the distribution.
 
 3. The name "classworlds" must not be used to endorse or promote
    products derived from this Software without prior written
    permission of The Werken Company.  For written permission,
    please contact bob@werken.com.
 
 4. Products derived from this Software may not be called "classworlds"
    nor may "classworlds" appear in their names without prior written
    permission of The Werken Company. "classworlds" is a registered
    trademark of The Werken Company.
 
 5. Due credit should be given to The Werken Company.
    (http://classworlds.werken.com/).
 
 THIS SOFTWARE IS PROVIDED BY THE WERKEN COMPANY AND CONTRIBUTORS
 ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 THE WERKEN COMPANY OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 OF THE POSSIBILITY OF SUCH DAMAGE.
 
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Command-line invokable application launcher.
 * <p/>
 * <p/>
 * This launcher class assists in the creation of classloaders and <code>ClassRealm</code>s
 * from a configuration file and the launching of the application's <code>main</code>
 * method from the correct class loaded through the correct classloader.
 * </p>
 * <p/>
 * <p/>
 * The path to the configuration file is specified using the <code>classworlds.conf</code>
 * system property, typically specified using the <code>-D</code> switch to
 * <code>java</code>.
 * </p>
 *
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter</a>
 * @version $Id$
 */
public class Launcher
{
    protected static final String CLASSWORLDS_CONF = "classworlds.conf";

    protected static final String UBERJAR_CONF_DIR = "WORLDS-INF/conf/";

    protected ClassLoader systemClassLoader;

    protected String mainClassName;

    protected String mainRealmName;

    protected ClassWorld world;

    private int exitCode = 0;

    public Launcher()
    {
    }

    public void setSystemClassLoader( ClassLoader loader )
    {
        this.systemClassLoader = loader;
    }

    public ClassLoader getSystemClassLoader()
    {
        return this.systemClassLoader;
    }

    public int getExitCode()
    {
        return exitCode;
    }

    public void setAppMain( String mainClassName, String mainRealmName )
    {
        this.mainClassName = mainClassName;

        this.mainRealmName = mainRealmName;
    }

    public String getMainRealmName()
    {
        return this.mainRealmName;
    }

    public String getMainClassName()
    {
        return this.mainClassName;
    }

    public void setWorld( ClassWorld world )
    {
        this.world = world;
    }

    public ClassWorld getWorld()
    {
        return this.world;
    }

    /**
     * Configure from a file.
     *
     * @param is The config input stream.
     * @throws IOException             If an error occurs reading the config file.
     * @throws MalformedURLException   If the config file contains invalid URLs.
     * @throws ConfigurationException  If the config file is corrupt.
     * @throws DuplicateRealmException If the config file defines two realms
     *                                 with the same id.
     * @throws NoSuchRealmException    If the config file defines a main entry
     *                                 point in a non-existent realm.
     */
    public void configure( InputStream is )
        throws IOException, MalformedURLException, ConfigurationException,
        DuplicateRealmException, NoSuchRealmException
    {
        Configurator configurator = new Configurator( this );

        configurator.configure( is );
    }

    /**
     * Retrieve the main entry class.
     *
     * @return The main entry class.
     * @throws ClassNotFoundException If the class cannot be found.
     * @throws NoSuchRealmException   If the specified main entry realm does not exist.
     */
    public Class getMainClass()
        throws ClassNotFoundException, NoSuchRealmException
    {
        return getMainRealm().loadClass( getMainClassName() );
    }

    /**
     * Retrieve the main entry realm.
     *
     * @return The main entry realm.
     * @throws NoSuchRealmException If the specified main entry realm does not exist.
     */
    public ClassRealm getMainRealm()
        throws NoSuchRealmException
    {
        return getWorld().getRealm( getMainRealmName() );
    }

    /**
     * Retrieve the enhanced main entry method.
     *
     * @return The enhanced main entry method.
     * @throws ClassNotFoundException If the main entry class cannot be found.
     * @throws NoSuchMethodException  If the main entry method cannot be found.
     * @throws NoSuchRealmException   If the main entry realm cannot be found.
     */
    protected Method getEnhancedMainMethod()
        throws ClassNotFoundException, NoSuchMethodException, NoSuchRealmException
    {
        Method[] methods = getMainClass().getMethods();
        Class cwClass = getMainRealm().loadClass( ClassWorld.class.getName() );

        Method m = getMainClass().getMethod( "main", new Class[] { String[].class, cwClass } );

        int modifiers = m.getModifiers();

        if ( Modifier.isStatic( modifiers ) && Modifier.isPublic( modifiers ) )
        {
            if ( m.getReturnType() == Integer.TYPE || m.getReturnType() == Void.TYPE )
            {
                return m;
            }
        }

        throw new NoSuchMethodException( "public static void main(String[] args, ClassWorld world)" );
    }

    /**
     * Retrieve the main entry method.
     *
     * @return The main entry method.
     * @throws ClassNotFoundException If the main entry class cannot be found.
     * @throws NoSuchMethodException  If the main entry method cannot be found.
     * @throws NoSuchRealmException   If the main entry realm cannot be found.
     */
    protected Method getMainMethod()
        throws ClassNotFoundException, NoSuchMethodException, NoSuchRealmException
    {
        Method m = getMainClass().getMethod( "main", new Class[] { String[].class } );

        int modifiers = m.getModifiers();

        if ( Modifier.isStatic( modifiers ) && Modifier.isPublic( modifiers ) )
        {
            if ( m.getReturnType() == Integer.TYPE || m.getReturnType() == Void.TYPE )
            {
                return m;
            }
        }

        throw new NoSuchMethodException( "public static void main(String[] args) in " + getMainClass() );
    }

    /**
     * Launch the application.
     *
     * @param args The application args.
     * @throws ClassNotFoundException    If the main entry class cannot be found.
     * @throws IllegalAccessException    If the method cannot be accessed.
     * @throws InvocationTargetException If the target of the invokation is invalid.
     * @throws NoSuchMethodException     If the main entry method cannot be found.
     * @throws NoSuchRealmException      If the main entry realm cannot be found.
     */
    public void launch( String[] args )
        throws ClassNotFoundException, IllegalAccessException,
        InvocationTargetException, NoSuchMethodException, NoSuchRealmException
    {
        try
        {
            launchEnhanced( args );

            return;
        }
        catch ( NoSuchMethodException e )
        {
            // ignore
        }

        launchStandard( args );
    }

    /**
     * Attempt to launch the application through the enhanced main method.
     * <p/>
     * <p/>
     * This will seek a method with the exact signature of:
     * </p>
     * <p/>
     * <pre>
     *  public static void main(String[] args, ClassWorld world)
     *  </pre>
     *
     * @param args The application args.
     * @throws ClassNotFoundException    If the main entry class cannot be found.
     * @throws IllegalAccessException    If the method cannot be accessed.
     * @throws InvocationTargetException If the target of the invokation is
     *                                   invalid.
     * @throws NoSuchMethodException     If the main entry method cannot be found.
     * @throws NoSuchRealmException      If the main entry realm cannot be found.
     */
    protected void launchEnhanced( String[] args )
        throws ClassNotFoundException, IllegalAccessException,
        InvocationTargetException, NoSuchMethodException, NoSuchRealmException
    {
        ClassRealm mainRealm = getMainRealm();

        Class mainClass = getMainClass();

        Method mainMethod = getEnhancedMainMethod();

        ClassLoader cl = mainRealm.getClassLoader();

        // ----------------------------------------------------------------------
        // This is what the classloader for the main realm looks like when we
        // boot from the command line:
        // ----------------------------------------------------------------------
        // [ AppLauncher$AppClassLoader ] : $CLASSPATH envar
        //           ^
        //           |
        //           |
        // [ AppLauncher$ExtClassLoader ] : ${java.home}/jre/lib/ext/*.jar
        //           ^
        //           |
        //           |
        // [ RealmClassLoader ]
        // ----------------------------------------------------------------------

        Thread.currentThread().setContextClassLoader( cl );

        Object ret = mainMethod.invoke( mainClass, new Object[]{args, getWorld()} );
        if ( ret instanceof Integer )
        {
            exitCode = ( ( Integer ) ret ).intValue();
        }
    }

    /**
     * Attempt to launch the application through the standard main method.
     * <p/>
     * <p/>
     * This will seek a method with the exact signature of:
     * </p>
     * <p/>
     * <pre>
     *  public static void main(String[] args)
     *  </pre>
     *
     * @param args The application args.
     * @throws ClassNotFoundException    If the main entry class cannot be found.
     * @throws IllegalAccessException    If the method cannot be accessed.
     * @throws InvocationTargetException If the target of the invokation is
     *                                   invalid.
     * @throws NoSuchMethodException     If the main entry method cannot be found.
     * @throws NoSuchRealmException      If the main entry realm cannot be found.
     */
    protected void launchStandard( String[] args )
        throws ClassNotFoundException, IllegalAccessException,
        InvocationTargetException, NoSuchMethodException, NoSuchRealmException
    {
        ClassRealm mainRealm = getMainRealm();

        Class mainClass = getMainClass();

        Method mainMethod = getMainMethod();

        Thread.currentThread().setContextClassLoader( mainRealm.getClassLoader() );

        Object ret = mainMethod.invoke( mainClass, new Object[]{args} );
        if ( ret instanceof Integer )
        {
            exitCode = ( ( Integer ) ret ).intValue();
        }
    }

    // ------------------------------------------------------------
    //     Class methods
    // ------------------------------------------------------------

    /**
     * Launch the launcher from the command line.
     * Will exit using System.exit with an exit code of 0 for success, 100 if there was an unknown exception,
     * or some other code for an application error.
     *
     * @param args The application command-line arguments.
     */
    public static void main( String[] args )
    {
        try
        {
            int exitCode = mainWithExitCode( args );
            System.exit( exitCode );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            System.exit( 100 );
        }
    }

    /**
     * Launch the launcher.
     *
     * @param args The application command-line arguments.
     * @return an integer exit code
     * @throws Exception If an error occurs.
     */
    public static int mainWithExitCode( String[] args )
        throws Exception
    {
        String classworldsConf = System.getProperty( CLASSWORLDS_CONF );

        InputStream is = null;

        Launcher launcher = new Launcher();

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        launcher.setSystemClassLoader( cl );

        if ( classworldsConf != null )
        {
            is = new FileInputStream( classworldsConf );
        }
        else
        {
            if ( "true".equals( System.getProperty( "classworlds.bootstrapped" ) ) )
            {
                is = cl.getResourceAsStream( UBERJAR_CONF_DIR + CLASSWORLDS_CONF );
            }
            else
            {
                is = cl.getResourceAsStream( CLASSWORLDS_CONF );
            }
        }

        if ( is == null )
        {
            throw new Exception( "classworlds configuration not specified nor found in the classpath" );
        }

        launcher.configure( is );

        try
        {
            launcher.launch( args );
        }
        catch ( InvocationTargetException e )
        {
            ClassRealm realm = launcher.getWorld().getRealm( launcher.getMainRealmName() );

            URL[] constituents = realm.getConstituents();

            System.out.println( "---------------------------------------------------" );

            for ( int i = 0; i < constituents.length; i++ )
            {
                System.out.println( "constituent[" + i + "]: " + constituents[i] );
            }

            System.out.println( "---------------------------------------------------" );

            // Decode ITE (if we can)
            Throwable t = e.getTargetException();

            if ( t instanceof Exception )
            {
                throw (Exception) t;
            }
            if ( t instanceof Error )
            {
                throw (Error) t;
            }

            // Else just toss the ITE
            throw e;
        }

        return launcher.getExitCode();
    }
}
