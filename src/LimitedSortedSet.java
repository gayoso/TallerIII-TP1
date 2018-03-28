import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

// un sorted set que automaticamente borra elementos de si mismo si se pasa del maximo
class LimitedSortedSet<E> extends TreeSet<E> {

    private int maxSize;

    LimitedSortedSet( int maxSize ) {
        this.maxSize = maxSize;
    }

    LimitedSortedSet( int maxSize, Comparator<? super E> comparator ) {
        super(comparator);
        this.maxSize = maxSize;
    }

    @Override
    public boolean addAll( Collection<? extends E> c ) {
        boolean added = super.addAll( c );
        if( size() > maxSize ) {
            E firstToRemove = (E)toArray( )[maxSize];
            removeAll( tailSet( firstToRemove ) );
        }
        return added;
    }

    @Override
    public boolean add( E o ) {
        boolean added =  super.add( o );
        /*if( size() > maxSize ) {
            E firstToRemove = (E)toArray( )[maxSize];
            removeAll( tailSet( firstToRemove ) );
        }*/
        while (size() > maxSize) {
            remove(last());
        }
        return added;
    }
}