package org.codehaus.classworlds;

import java.util.Collection;
import java.util.HashMap;

/**
 * An adapter for ClassWorlds
 *
 * @author Andrew Williams
 * @version $Id$
 */
public class ClassWorldAdapter
    extends ClassWorld
{
    private static HashMap instances = new HashMap();

    public static ClassWorldAdapter getInstance( org.codehaus.plexus.classworlds.ClassWorld newWorld )
    {
        if ( newWorld instanceof ClassWorldAdapter )
            return (ClassWorldAdapter) newWorld;

        if ( instances.containsKey( newWorld ) )
            return (ClassWorldAdapter) instances.get( newWorld );

        ClassWorldAdapter adapter = new ClassWorldAdapter( newWorld );
        instances.put( newWorld, adapter );

        return adapter;
    }

    private org.codehaus.plexus.classworlds.ClassWorld world;

    private ClassWorldAdapter( org.codehaus.plexus.classworlds.ClassWorld newWorld )
    {
        super();
        this.world = newWorld;
    }

    public ClassRealm newRealm( String id )
        throws DuplicateRealmException
    {
        try
        {
            return ClassRealmAdapter.getInstance( world.newRealm( id ) );
        }
        catch ( org.codehaus.plexus.classworlds.realm.DuplicateRealmException e )
        {
            throw new DuplicateRealmException( this, e.getId() );
        }
    }

    public ClassRealm newRealm( String id,
                                ClassLoader classLoader )
        throws DuplicateRealmException
    {
        try
        {
            return ClassRealmAdapter.getInstance( world.newRealm( id,
                                                                  classLoader ) );
        }
        catch ( org.codehaus.plexus.classworlds.realm.DuplicateRealmException e )
        {
            throw new DuplicateRealmException( this, e.getId() );
        }
    }

    public void disposeRealm( String id )
        throws NoSuchRealmException
    {
        try
        {
            world.disposeRealm( id );
        }
        catch ( org.codehaus.plexus.classworlds.realm.NoSuchRealmException e )
        {
            throw new NoSuchRealmException( this, e.getId() );
        }
    }

    public ClassRealm getRealm( String id )
        throws NoSuchRealmException
    {
        try
        {
            return ClassRealmAdapter.getInstance( world.getRealm( id ) );
        }
        catch ( org.codehaus.plexus.classworlds.realm.NoSuchRealmException e )
        {
            throw new NoSuchRealmException( this, e.getId() );
        }            
    }

    public Collection getRealms()
    {
        return world.getRealms();
    }
}
