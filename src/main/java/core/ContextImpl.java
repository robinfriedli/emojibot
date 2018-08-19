package core;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Entry point to the persistence layer.
 * Holds temporary state of emojis in memory.
 * Changes are applied to the Emoji instances here first before saving to the XML file using the PersistenceManager#commit() method
 * Use the invoke() method instead of using the PersistenceManager directly.
 */
public class ContextImpl implements Context {

    private final ContextManager manager;

    private final List<XmlElement> inMemoryElements;

    private final DefaultPersistenceManager persistenceManager;

    private final String path;

    private Transaction transaction;

    private List<Transaction> uncommittedTransactions = Lists.newArrayList();

    private Object envVar;

    public ContextImpl(ContextManager manager, DefaultPersistenceManager persistenceManager, String path) {
        this.manager = manager;
        this.path = path;
        persistenceManager.initialize(this);
        this.persistenceManager = persistenceManager;
        this.inMemoryElements = persistenceManager.getAllElements();
        persistenceManager.buildTree(inMemoryElements);
    }

    @Override
    public ContextManager getManager() {
        return this.manager;
    }

    @Override
    public DefaultPersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public List<XmlElement> getElements() {
        return inMemoryElements;
    }

    @Override
    public List<XmlElement> getUsableElements() {
        return inMemoryElements.stream().filter(e -> e.getState() != XmlElement.State.DELETION).collect(Collectors.toList());
    }

    @Override
    @Nullable
    public XmlElement getElement(String id) {
        List<XmlElement> foundElements = getUsableElements().stream().filter(element ->
            element.getId() != null && element.getId().equals(id)
        ).collect(Collectors.toList());

        if (foundElements.size() == 1) {
            return foundElements.get(0);
        } else if (foundElements.size() > 1) {
            throw new IllegalStateException("Id: " + id + " not unique. Element loading failed. " +
                "Add Elements using the #invoke method via the PersistenceManager instead of adding to Context directly");
        } else {
            return null;
        }
    }

    @Override
    @Nullable
    public <E extends XmlElement> E getElement(String id, Class<E> type) {
        List<E> foundElements = getUsableElements().stream()
                .filter(element -> element.getId() != null && element.getId().equals(id) && type.isInstance(element))
                .map(type::cast)
                .collect(Collectors.toList());

        if (foundElements.size() == 1) {
            return foundElements.get(0);
        } else if (foundElements.size() > 1) {
            throw new IllegalStateException("Id: " + id + " not unique. Element loading failed. " +
                    "Add Elements using the #invoke method via the PersistenceManager instead of adding to Context directly");
        } else {
            return null;
        }
    }

    @Override
    public XmlElement requireElement(String id) {
        XmlElement element = getElement(id);

        if (element != null) {
            return element;
        } else {
            throw new IllegalStateException("No element found for id " + id);
        }
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c) {
        return getUsableElements().stream()
            .filter(c::isInstance)
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public <E extends XmlElement> List<E> getInstancesOf(Class<E> c, Class... ignoredSubClasses) {
        return getUsableElements().stream()
            .filter(elem -> c.isInstance(elem) && Arrays.stream(ignoredSubClasses).noneMatch(clazz -> clazz.isInstance(elem)))
            .map(c::cast)
            .collect(Collectors.toList());
    }

    @Override
    public void reloadElements() {
        inMemoryElements.clear();
        inMemoryElements.addAll(persistenceManager.getAllElements());
    }

    @Override
    public void addElement(XmlElement element) {
        addElements(Lists.newArrayList(element));
    }

    @Override
    public void addElements(List<XmlElement> elements) {
        for (XmlElement element : elements) {
            if (element.isLocked()) {
                throw new PersistException("Attempting to add locked XmlElement to Context. Probably duplicate.");
            }
            if (element.getId() != null && getElement(element.getId()) != null) {
                throw new PersistException("Attempting to add duplicate XmlElement to Context." +
                    "Use DefaultPersistenceManager#addElement to automatically load and adjust existing XmlElement");
            }
            inMemoryElements.add(element);
        }
    }

    @Override
    public void addElements(XmlElement... elements) {
        addElements(Arrays.asList(elements));
    }

    @Override
    public void removeElement(XmlElement element) {
        inMemoryElements.remove(element);
    }

    @Override
    public void removeElements(List<XmlElement> elements) {
        inMemoryElements.removeAll(elements);
    }

    @Override
    public void removeElements(XmlElement... elements) {
        removeElements(Arrays.asList(elements));
    }

    @Override
    public void commitAll() {
        uncommittedTransactions.forEach(tx -> {
            try {
                tx.commit(persistenceManager);
                persistenceManager.write();
            } catch (CommitException e) {
                e.printStackTrace();
            }
        });

        uncommittedTransactions.clear();
    }

    @Override
    public void revertAll() {
        uncommittedTransactions.forEach(Transaction::rollback);
        uncommittedTransactions.clear();
    }

    @Override
    public <E> E invoke(boolean commit, Callable<E> task) {
        boolean encapsulated = false;
        if (transaction != null) {
            encapsulated = true;
        } else {
            getTx();
        }

        E returnValue = null;

        try {
            returnValue = task.call();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!encapsulated) {
            transaction.apply();
            if (commit) {
                try {
                    transaction.commit(persistenceManager);
                    persistenceManager.write();
                } catch (CommitException e) {
                    e.printStackTrace();
                }
            } else {
                uncommittedTransactions.add(transaction);
            }
        }

        if (!encapsulated) {
            closeTx();
        }

        return returnValue;
    }

    @Override
    public void invoke(boolean commit, Runnable task) {
        boolean encapsulated = false;
        if (transaction != null) {
            encapsulated = true;
        } else {
            getTx();
        }

        task.run();

        if (!encapsulated) {
            transaction.apply();
            if (commit) {
                try {
                    transaction.commit(persistenceManager);
                    persistenceManager.write();
                } catch (CommitException e) {
                    e.printStackTrace();
                }
            } else {
                uncommittedTransactions.add(transaction);
            }
        }

        if (!encapsulated) {
            closeTx();
        }
    }

    @Override
    public <E> E invoke(boolean commit, Callable<E> task, Object envVar) {
        this.envVar = envVar;
        return invoke(commit, task);
    }

    @Override
    public void invoke(boolean commit, Runnable task, Object envVar) {
        this.envVar = envVar;
        invoke(commit, task);
    }

    @Override
    public void apply(Runnable task) {
        // save the currently ongoing transaction
        Transaction currentTx = transaction;
        // switch to a different transaction
        transaction = Transaction.createApplyOnlyTx();
        task.run();
        transaction.apply();
        // switch back to old transaction
        transaction = currentTx;
    }

    @Override
    public void setEnvVar(Object envVar) {
        this.envVar = envVar;
    }

    @Override
    public Object getEnvVar() {
        return envVar;
    }

    @Override
    @Nullable
    public Transaction getTransaction() {
        return transaction;
    }

    private void getTx() {
        if (transaction != null) {
            return;
        } else {
            transaction =  Transaction.createTx();
        }
    }

    private void closeTx() {
        transaction = null;
    }

}