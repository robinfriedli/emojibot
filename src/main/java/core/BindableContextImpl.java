package core;

import java.io.File;

public class BindableContextImpl<E> extends ContextImpl implements Context.BindableContext<E> {

    private final E boundObject;
    private final String id;

    public BindableContextImpl(ContextManager manager, E boundObject, String id, DefaultPersistenceManager persistenceManager) {
        super(manager, persistenceManager, buildPath(manager, id));
        this.boundObject = boundObject;
        this.id = id;
    }

    @Override
    public E getBindingObject() {
        return boundObject;
    }

    @Override
    public String getId() {
        return id;
    }

    private static String buildPath(ContextManager manager, String id) {
        // add the id of the BindableContext in front of the last element of the path: ./resources/2343240923elements.xml
        String standardPath = manager.getPath();
        String[] pathElems = standardPath.split(File.separator);
        int lastElemIndex = pathElems.length - 1;
        String lastElem = pathElems[lastElemIndex];
        pathElems[lastElemIndex] = id + lastElem;
        return String.join(File.separator, pathElems);
    }
}
