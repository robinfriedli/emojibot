package core;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * Holds all {@link Event} that are currently being applied. Makes commits and rollbacks easier.
 */
public class Transaction {

    private final List<Event> changes;
    private boolean rollback = false;
    private boolean applyOnly = false;

    public Transaction(List<Event> changes) {
        this.changes = changes;
    }

    public Transaction(List<Event> changes, boolean applyOnly) {
        this.changes = changes;
        this.applyOnly = applyOnly;
    }

    public void addChange(Event change) {
        changes.add(change);
    }

    public void addChanges(List<Event> changes) {
        this.changes.addAll(changes);
    }

    public void addChanges(Event... changes) {
        addChanges(Arrays.asList(changes));
    }

    public boolean isRollback() {
        return rollback;
    }

    public void apply() {
        for (Event change : changes) {
            change.apply();
            if (applyOnly && change instanceof ElementChangingEvent) {
                change.getSource().removeChange((ElementChangingEvent) change);
            }
        }
    }

    public void commit(DefaultPersistenceManager manager) throws CommitException {
        if (!applyOnly) {
            try {
                for (Event change : changes) {
                    if (change.isApplied()) {
                        XmlElement source = change.getSource();
                        XmlPersister persister = manager.getXmlPersister();
                        if (change instanceof ElementChangingEvent) {
                            manager.commitElementChanges((ElementChangingEvent) change);
                            source.removeChange((ElementChangingEvent) change);
                        }
                        if (change instanceof ElementDeletingEvent) {
                            persister.remove(source);
                            manager.getContext().removeElement(source);
                        }
                        if (change instanceof ElementCreatedEvent) {
                            if (!source.isSubElement()) {
                                persister.persistElement(source);
                            }
                            source.setState(XmlElement.State.CLEAN);
                            source.createShadow();
                        }
                    }
                }
                changes.stream().filter(change -> change instanceof ElementChangingEvent).forEach(change -> change.getSource().updateShadow());
            } catch (CommitException e) {
                e.printStackTrace();
                rollback();
                manager.reload();
                throw new CommitException("Exception during commit. Transaction rolled back.");
            }
        } else {
            throw new CommitException("Trying to commit an apply-only Transaction");
        }
    }

    public void rollback() {
        rollback = true;
        changes.forEach(Event::revert);
    }

    public static Transaction createTx() {
        return new Transaction(Lists.newArrayList());
    }

    public static Transaction createApplyOnlyTx() {
        return new Transaction(Lists.newArrayList(), true);
    }
}
