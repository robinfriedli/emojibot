package core;

import util.EventListener;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Entry point to the persistence layer.
 * Holds temporary state of AbstractXmlElements in memory.
 * Changes are applied to the Element instances here first before saving to the XML file using the PersistenceManager#commit() method
 * Use the invoke() method instead of using the PersistenceManager directly.
 */
public interface Context {

    /**
     * @return the {@link ContextManager} for this Context
     */
    ContextManager getManager();

    /**
     * @return the {@link DefaultPersistenceManager} for this Context
     */
    DefaultPersistenceManager getPersistenceManager();

    /**
     * @return the path of the XML file of this Context
     */
    String getPath();

    /**
     * @return All Elements saved in this Context
     */
    List<XmlElement> getElements();

    /**
     * @return All Elements that are not in {@link AbstractXmlElement.State} DELETION
     */
    List<XmlElement> getUsableElements();

    /**
     * Return an {@link XmlElement} saved in this Context where {@link XmlElement#getId()} matches the provided id
     *
     * @param id to find the {@link XmlElement} with
     * @return found {@link XmlElement}
     */
    @Nullable
    XmlElement getElement(String id);

    /**
     * Return an {@link XmlElement} saved in this Context where {@link XmlElement#getId()} matches the provided id and
     * is an instance of the provided class.
     *
     * @param id to find the {@link XmlElement} with
     * @param type target Class
     * @param <E> Class extending {@link XmlElement}
     * @return found {@link XmlElement} cast to target Class
     */
    @Nullable
    <E extends XmlElement> E getElement(String id, Class<E> type);

    /**
     * Same as {@link #getElement(String)} but throws Exception when null
     * @param id to find the {@link XmlElement} with
     * @return found {@link XmlElement}
     */
    XmlElement requireElement(String id);

    /**
     * Get all XmlElements saved in this Context that are instance of {@link E}
     *
     * @param c Class to check
     * @param <E> Type of Class to check
     * @return All Elements that are an instance of specified Class
     */
    <E extends XmlElement> List<E> getInstancesOf(Class<E> c);

    /**
     * Get all XmlElements saved in this Context that are instance of {@link E}, ignoring elements that are instance of
     * any of the ignored subClasses. Used to exclude subclasses.
     *
     * @param c Class to check
     * @param ingoredSubClasses subclasses to exclude
     * @param <E> Type to return
     * @return All Elements that are an instance of specified Class but not specified subclasses
     */
    <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class... ingoredSubClasses);

    /**
     * Reload all Elements from the XML File. This overrides any uncommitted changes.
     */
    void reloadElements();

    /**
     * Add Element to memory
     *
     * @param element to add to context
     */
    void addElement(XmlElement element);

    /**
     * Add Elements to memory
     *
     * @param elements to add to context
     */
    void addElements(List<XmlElement> elements);

    /**
     * Add Elements to memory
     *
     * @param elements to add to context
     */
    void addElements(XmlElement... elements);

    void removeElement(XmlElement element);

    void removeElements(List<XmlElement> xmlElements);

    void removeElements(XmlElement... xmlElements);

    /**
     * Commit all previously uncommitted {@link Transaction}s on this Context
     */
    void commitAll();

    /**
     * remove and rollback all uncommitted {@link Transaction}s on this Context
     */
    void revertAll();

    /**
     * Run a Callable in a {@link Transaction}. For any actions that create, change or delete an {@link XmlElement}.
     *
     * @param commit defines if the changes will be committed to XML file immediately after applying or remain in Context
     * @param task any Callable
     * @param <E> return type of Callable
     * @return any Object of type E
     */
    <E> E invoke(boolean commit, Callable<E> task);

    /**
     * Quickly execute a one-liner that requires a {@link Transaction} like
     *
     * {@code context.invoke(true, () -> bla.setAttribute("name", "value"));}
     *
     * or several statements without returning anything like
     *
     * <pre>
     *     context.invoke(true, () -> {
     *        TestXmlElement elem = new TestXmlElement("test", context);
     *        elem.setAttribute("test", "test");
     *        elem.setTextContent("test");
     *     });
     * </pre>
     *
     * A {@link Transaction} is required by any action that creates, changes or deletes an {@link XmlElement}
     *
     * @param commit defines if the changes will be committed to XML file immediately after applying or remain in Context
     * @param task any Runnable
     */
    void invoke(boolean commit, Runnable task);

    /**
     * Like {@link #invoke(boolean, Callable)} but also sets this Context's environment variable to the
     * passed Object. For an explanation regarding envVar see {@link #getEnvVar()}.
     *
     * @param commit defines if the changes will be committed to XML file immediately after applying or remain in Context
     * @param task any Callable
     * @param envVar any Object to set as envVar in Context
     * @param <E> return type of Callable
     * @return any Object of type E
     */
    <E> E invoke(boolean commit, Callable<E> task, Object envVar);

    /**
     * Like {@link #invoke(boolean, Runnable)} but also sets this Context's environment variable to the
     * passed Object. For an explanation regarding envVar see {@link #getEnvVar()}.
     *
     * @param commit defines if the changes will be committed to XML file immediately after applying or remain in Context
     * @param task any Runnable
     * @param envVar any Object to set as envVar in Context
     */
    void invoke(boolean commit, Runnable task, Object envVar);

    /**
     * Runs a task in an apply-only {@link Transaction} that will never be committed or saved as uncommitted transaction
     * in this Context. Use cautiously if you want to apply the changes to the XML file using the {@link XmlPersister}
     * manually. Used for dealing with changes that would otherwise throw an exception when committing, e.g. dealing
     * with duplicate elements.
     *
     * @param task to run
     */
    void apply(Runnable task);

    /**
     * set the variable for this Context. For a description on envVar see {@link #getEnvVar()}
     *
     * @param envVar variable to set
     */
    void setEnvVar(Object envVar);

    /**
     * The environment variable can be anything you might need somewhere els in context with this transaction.
     * The environment variable is set when using either method {@link #invoke(boolean, Callable, Object)}
     * {@link #invoke(boolean, Runnable, Object)}
     *
     * E.g. say you're developing a Discord bot and you've implemented an {@link EventListener} that sends a message
     * after an Element has been added. In this case you could set the MessageChannel the command came from as envVar
     * to send the message to the right channel.
     *
     * @return the environment variable of this Context.
     */
    Object getEnvVar();

    /**
     * @return the currently active Transaction
     */
    @Nullable
    Transaction getTransaction();

    /**
     * Partitionable Context that can be bound to object of Type E
     *
     * @param <E> Type the Context may be bound to
     */
    interface BindableContext<E> extends Context {

        /**
         * Get the instance of Type E which the Context is bound to
         *
         * @return object of Type E
         */
        E getBindingObject();

        /**
         * The BindableContext should have an id the Context can consistently by identified with so that its XML file
         * can be loaded. Ideally the Object the BindableContext is bound to should have some sort of id.
         *
         * @return id of BindableContext, used as part of filename
         */
        String getId();

    }

}
