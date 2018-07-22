package core;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import api.DiscordEmoji;
import api.Emoji;
import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.entities.MessageChannel;
import util.AlertEventListener;
import util.AlertService;
import util.EventListener;

/**
 * Entry point to the persistence layer.
 * Holds temporary state of emojis in memory.
 * Changes are applied to the Emoji instances here first before saving to the XML file using the PersistenceManager#commit() method
 * Use the executePersistTask() method instead of using the PersistenceManager directly.
 */
public class Context {

    private final List<Emoji> inMemoryEmojis;
    private final PersistenceManager persistenceManager;
    //register listeners here
    private final Set<EventListener> eventListeners = ImmutableSet.of(new AlertEventListener(new AlertService(), this));

    private MessageChannel channel;

    public Context() {
        this.persistenceManager = new PersistenceManager(this, new XmlManager());
        this.inMemoryEmojis = persistenceManager.getAllEmojis();
    }

    public List<Emoji> getInMemoryEmojis() {
        return inMemoryEmojis;
    }

    /**
     * @return all Emojis filtering those in state DELETION
     */
    public List<Emoji> getUseableEmojis() {
        return inMemoryEmojis.stream().filter(e -> e.getState() != Emoji.State.DELETION).collect(Collectors.toList());
    }

    public List<Emoji> getUnicodeEmojis() {
        return getUseableEmojis().stream().filter(e -> !(e instanceof DiscordEmoji)).collect(Collectors.toList());
    }

    public List<DiscordEmoji> getDiscordEmojis() {
        return getUseableEmojis().stream()
            .filter(e -> e instanceof DiscordEmoji)
            .map(e -> (DiscordEmoji) e)
            .collect(Collectors.toList());
    }

    public void reloadEmojis() {
        inMemoryEmojis.clear();
        inMemoryEmojis.addAll(persistenceManager.getAllEmojis());
    }

    public <E extends Emoji> void addEmojiToMemory(E emoji) {
        inMemoryEmojis.add(emoji);
    }

    public <E extends Emoji> void addEmojisToMemory(E... emojis) {
        inMemoryEmojis.addAll(Arrays.asList(emojis));
    }

    public void executePersistTask(boolean commit, Function<PersistenceManager, Void> task) {
        task.apply(persistenceManager);

        if (commit) {
            persistenceManager.commit();
        }
    }

    public void executePersistTask(boolean commit, Function<PersistenceManager, Void> task, @Nullable MessageChannel channel) {
        this.channel = channel;
        task.apply(persistenceManager);

        if (commit) {
            persistenceManager.commit();
        }

    }

    public MessageChannel getChannel() {
        return this.channel;
    }

    public void fireEmojiCreating(Event event) {
        eventListeners.forEach(listener -> listener.emojiCreating(event));
    }

    public void fireEmojiDeleting(Event event) {
        eventListeners.forEach(listener -> listener.emojiDeleting(event));
    }

    public void fireEmojiChanging(EmojiChangingEvent event) {
        eventListeners.forEach(listener -> listener.emojiChanging(event));
    }

}
